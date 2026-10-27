package justfatlard.crosshair_corsair.integration;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.Main;
import justfatlard.crosshair_corsair.crosshair.CrosshairStyle;
import justfatlard.crosshair_corsair.crosshair.ModItems;
import justfatlard.crosshair_corsair.crosshair.Policy;
import justfatlard.crosshair_corsair.crosshair.Shape;
import net.fabricmc.loader.api.FabricLoader;

/**
 * This mod's settings in Pandorical's mod menu, when Pandorical is there.
 *
 * <p>The config file stays the source of truth. Each setting reads the live config and writes a
 * change straight back to the file, so the menu and the file can never disagree, and a file edit is
 * pushed to the menu when the poll notices it. Colours stay file-only: the menu has no text field.
 */
public final class PandoricalSettings {
	private PandoricalSettings() {}

	// Suggested rather than depended on, so nothing pins the version: a Pandorical whose API has
	// moved would throw out of onInitializeClient and take the whole mod down with it.
	private static boolean available = FabricLoader.getInstance().isModLoaded("pandorical");

	public static boolean present() { return available; }

	public static void register() {
		if (!available) return;
		try {
			Reach.register();
		} catch (LinkageError incompatible) {
			available = false;
			Main.LOGGER.warn("Pandorical is present but its settings API does not match; "
				+ "the mod menu page is off and the config file still works", incompatible);
		}
	}

	public static void changed() {
		if (!available) return;
		try {
			Reach.changed();
		} catch (LinkageError incompatible) {
			available = false;
		}
	}

	/**
	 * Mod items' settings, added as their mods' assets load: on a server that ships them, that is
	 * after the rest of the menu was built. Each is added once, however many reloads follow.
	 */
	public static void modItems(List<ModItems.ModItem> items) {
		if (!available) return;
		try {
			Reach.modItems(items);
		} catch (LinkageError incompatible) {
			available = false;
		}
	}

	private static final class Reach {
		private static final Set<String> listed = new HashSet<>();

		static void changed() {
			justfatlard.pandorical.client.api.PandoricalClientApi.settings().changed();
		}

		private static justfatlard.pandorical.client.api.ClientSettingsApi.Group group() {
			return justfatlard.pandorical.client.api.PandoricalClientApi.settings()
				.group(Main.MOD_ID, "Crosshair Corsair");
		}

		private static Map<String, String> policies() {
			Map<String, String> policies = new LinkedHashMap<>();
			policies.put(Policy.ALWAYS.configName(), "Always");
			policies.put(Policy.TARGETING.configName(), "When aimed at something");
			policies.put(Policy.INTERACTABLE.configName(), "When it would work");
			policies.put(Policy.NEVER.configName(), "Never");
			return policies;
		}

		static void modItems(List<ModItems.ModItem> items) {
			var group = group();
			boolean added = false;
			for (ModItems.ModItem item : items) {
				if (!listed.add(item.id())) continue;
				added = true;
				String wouldWork = switch (item.like()) {
					case THROWABLE -> "aimed at a block or something you can hit";
					case RANGED_WEAPON, MELEE_WEAPON -> "aimed at something you can hit";
					case USABLE_ITEM -> "something to use it on";
				};
				String article = "aeiou".indexOf(item.name().charAt(0)) >= 0 ? "an " : "a ";
				held(group, policies(), "mod/" + item.id(), "Holding " + article + item.name(), wouldWork,
					() -> c().modItem(item).policy, v -> c().modItem(item).policy = v);

				Map<String, String> styles = new LinkedHashMap<>();
				Map<String, String> shapes = new LinkedHashMap<>();
				Shape own = item.ownShape();
				if (own != null) {
					styles.put(item.style(), "the mod's own");
					shapes.put(item.style(), own.texture());
				}
				for (CrosshairStyle style : CrosshairStyle.values()) {
					shapes.put(style.configName(), style.texture());
					styles.put(style.configName(), style.label());
				}
				group.choice("modStyle/" + item.id(), "Shape: " + item.name(), null, styles, shapes,
					() -> {
						String style = c().modItem(item).style;
						return styles.containsKey(style) ? style : CrosshairStyle.named(style).configName();
					},
					saving(v -> c().modItem(item).style = v));
			}
			if (added) changed();
		}

		static void register() {
			var group = group();

			Map<String, String> policies = policies();
			Map<String, String> styles = new LinkedHashMap<>();
			// Beside every shape's name, the shape. Fourteen settings all offering the same
			// nineteen options is a lot of reading to find out what "brackets round" looks like.
			Map<String, String> shapes = new LinkedHashMap<>();
			for (CrosshairStyle style : CrosshairStyle.values()) {
				shapes.put(style.configName(), style.texture());
				styles.put(style.configName(), style.label());
			}
			Map<String, String> breaking = new LinkedHashMap<>();
			breaking.put(CorsairConfig.BreakAnimation.NONE.configName(), "None");
			breaking.put(CorsairConfig.BreakAnimation.SHRINK.configName(), "Shrink");
			breaking.put(CorsairConfig.BreakAnimation.DOWN.configName(), "Sink");
			breaking.put(CorsairConfig.BreakAnimation.ALPHA.configName(), "Fade");

			// The menu is one flat list in this order, so each item's policy sits beside its shape.

			// The reacharound
			toggle(group, "reachEnabled", "Reacharound", "Place against the block under or over you when the crosshair is on air; the toggle key flips this too",
				() -> r().enabled, v -> r().enabled = v);
			toggle(group, "reachHorizontal", "Bridge placement", "Look ahead at an edge to place the next block out",
				() -> r().horizontal, v -> r().horizontal = v);
			style(group, styles, shapes, "styleReachFloor", "Shape: bridging", () -> s().reacharoundFloor, v -> s().reacharoundFloor = v);
			toggle(group, "reachVertical", "Overhead placement", "Look well up to extend the ceiling",
				() -> r().vertical, v -> r().vertical = v);
			style(group, styles, shapes, "styleReachCeiling", "Shape: bridging overhead", () -> s().reacharoundCeiling, v -> s().reacharoundCeiling = v);
			toggle(group, "reachGhost", "Show where it lands", "Outlines the destination before the click",
				() -> r().showGhost, v -> r().showGhost = v);
			number(group, "reachGhostAlpha", "Landing outline opacity, of 255", null, 0, 255, 17,
				() -> r().ghostAlpha, v -> r().ghostAlpha = v);

			// The crosshair
			toggle(group, "crosshair", "Contextual crosshair", "Shape and presence follow what is under it and in hand",
				() -> c().enabled, v -> c().enabled = v);
			toggle(group, "hideWhenIdle", "Hide when idle", "No crosshair with nothing under it and nothing useful in hand",
				() -> c().hideWhenIdle, v -> c().hideWhenIdle = v);
			style(group, styles, shapes, "regular", "Shape: idle", "Only seen with Hide when idle off",
				() -> s().regular, v -> s().regular = v);
			toggle(group, "thirdPerson", "Show in third person", null, () -> c().thirdPerson, v -> c().thirdPerson = v);
			toggle(group, "blend", "Inverted blend", "Vanilla's inverting crosshair; off draws it plain", () -> c().blend, v -> c().blend = v);
			toggle(group, "overrideColor", "Tint the crosshair", "Paints it the colour set in the config file, which is white until you set one",
				() -> c().overrideColor, v -> c().overrideColor = v);
			toggle(group, "usableBrackets", "Brackets when a use would work", null, () -> c().usableBrackets, v -> c().usableBrackets = v);

			// What it is on
			toggle(group, "onBlock", "Show on blocks", null, () -> c().onBlock, v -> c().onBlock = v);
			style(group, styles, shapes, "styleOnBlock", "Shape: on a block", () -> s().onBlock, v -> s().onBlock = v);
			toggle(group, "onInteractableBlock", "Show on usable blocks", null, () -> c().onInteractableBlock, v -> c().onInteractableBlock = v);
			style(group, styles, shapes, "styleOnInteractable", "Shape: on a usable block", () -> s().onInteractableBlock, v -> s().onInteractableBlock = v);
			toggle(group, "onEntity", "Show on entities", "Something you could hit or use", () -> c().onEntity, v -> c().onEntity = v);
			style(group, styles, shapes, "styleOnEntity", "Shape: on an entity", () -> s().onEntity, v -> s().onEntity = v);

			// What is in hand
			held(group, policies, "holdingTool", "Holding a tool", "the right tool for the block",
				() -> c().holdingTool, v -> c().holdingTool = v);
			style(group, styles, shapes, "styleTool", "Shape: tool", () -> s().holdingTool, v -> s().holdingTool = v);
			toggle(group, "correctToolDot", "Dot for the right tool", null, () -> c().correctToolDot, v -> c().correctToolDot = v);
			held(group, policies, "holdingWeapon", "Holding a weapon", "aimed at something you can hit",
				() -> c().holdingMeleeWeapon, v -> c().holdingMeleeWeapon = v);
			style(group, styles, shapes, "styleMelee", "Shape: weapon", () -> s().holdingMeleeWeapon, v -> s().holdingMeleeWeapon = v);
			held(group, policies, "holdingRangedWeapon", "Holding a bow", "aimed at something you can hit",
				() -> c().holdingRangedWeapon, v -> c().holdingRangedWeapon = v);
			style(group, styles, shapes, "styleRanged", "Shape: bow", () -> s().holdingRangedWeapon, v -> s().holdingRangedWeapon = v);
			held(group, policies, "holdingThrowable", "Holding a throwable", "aimed at a block or something you can hit",
				() -> c().holdingThrowable, v -> c().holdingThrowable = v);
			style(group, styles, shapes, "styleThrowable", "Shape: throwable", () -> s().holdingThrowable, v -> s().holdingThrowable = v);
			toggle(group, "holdingShield", "Holding a shield", "When a click would raise it, its shape wins over both hands",
				() -> c().holdingShield, v -> c().holdingShield = v);
			style(group, styles, shapes, "styleShield", "Shape: shield", () -> s().holdingShield, v -> s().holdingShield = v);
			held(group, policies, "holdingBlock", "Holding a block", "aimed where it would place",
				() -> c().holdingBlock, v -> c().holdingBlock = v);
			style(group, styles, shapes, "styleBlock", "Shape: block", () -> s().holdingBlock, v -> s().holdingBlock = v);
			toggle(group, "holdingBlockInOffhand", "Offhand blocks count", "With an empty main hand; also lets the reacharound use them",
				() -> c().holdingBlockInOffhand, v -> c().holdingBlockInOffhand = v);
			held(group, policies, "holdingUsableItem", "Holding a usable item", "always for food, otherwise something to use it on",
				() -> c().holdingUsableItem, v -> c().holdingUsableItem = v);
			style(group, styles, shapes, "styleUsable", "Shape: usable item", () -> s().holdingUsableItem, v -> s().holdingUsableItem = v);

			// The selection box
			toggle(group, "selectionBox", "Block outline", "Off hides the outline entirely",
				() -> b().enabled, v -> b().enabled = v);
			number(group, "outlineAlpha", "Outline opacity, of 255", "Vanilla's is 102", 0, 255, 17,
				() -> b().alpha, v -> b().alpha = v);
			number(group, "outlineWidth", "Outline width, tenths of a pixel", "0 is vanilla's own, which scales with the window", 0, 80, 5,
				() -> Math.round(b().lineWidth * 10), v -> b().lineWidth = v / 10F);
			number(group, "blinkAlpha", "Outline blink swing, of 255", "How far the opacity pulses; 0 is a steady line", 0, 255, 17,
				() -> b().blinkAlpha, v -> b().blinkAlpha = v);
			number(group, "blinkSpeed", "Outline blinks per ten seconds", null, 0, 50, 5,
				() -> Math.round(b().blinkSpeed * 10), v -> b().blinkSpeed = v / 10F);
			choice(group, "breakAnimation", "Outline while mining", null, breaking, () -> b().breakAnimation, v -> b().breakAnimation = v);
		}

		private static CorsairConfig.Crosshair c() { return CorsairConfig.get().crosshair; }
		private static CorsairConfig.Crosshair.Styles s() { return CorsairConfig.get().crosshair.styles; }
		private static CorsairConfig.SelectionBox b() { return CorsairConfig.get().selectionBox; }
		private static CorsairConfig.Reacharound r() { return CorsairConfig.get().reacharound; }

		private static void toggle(justfatlard.pandorical.client.api.ClientSettingsApi.Group group, String key,
				String label, String description, Supplier<Boolean> get, Consumer<Boolean> set) {
			group.toggle(key, label, description, get, saving(set));
		}

		private static void choice(justfatlard.pandorical.client.api.ClientSettingsApi.Group group, String key,
				String label, String description, Map<String, String> options, Supplier<String> get, Consumer<String> set) {
			group.choice(key, label, description, options, get, saving(set));
		}

		private static void style(justfatlard.pandorical.client.api.ClientSettingsApi.Group group, Map<String, String> styles,
				Map<String, String> shapes, String key, String label, Supplier<String> get, Consumer<String> set) {
			style(group, styles, shapes, key, label, null, get, set);
		}


		private static void style(justfatlard.pandorical.client.api.ClientSettingsApi.Group group, Map<String, String> styles,
				Map<String, String> shapes, String key, String label, String description, Supplier<String> get, Consumer<String> set) {
			group.choice(key, label, description, styles, shapes,
				() -> CrosshairStyle.named(get.get()).configName(), saving(set));
		}

		/**
		 * A held item's policy. Off, the crosshair is not hidden: it reads what is under it
		 * instead, which the description has to say or "Never" reads as "no crosshair".
		 */
		private static void held(justfatlard.pandorical.client.api.ClientSettingsApi.Group group, Map<String, String> policies,
				String key, String label, String wouldWork, Supplier<String> get, Consumer<String> set) {
			choice(group, key, label, "When its shape takes over from what you aim at. It would work: " + wouldWork,
				policies, () -> Policy.named(get.get()).configName(), set);
		}

		private static void number(justfatlard.pandorical.client.api.ClientSettingsApi.Group group, String key,
				String label, String description, int min, int max, int step, Supplier<Integer> get, Consumer<Integer> set) {
			group.number(key, label, description, min, max, step, get, saving(set));
		}

		/** Every change goes to the file, which is what the rest of the mod reads. */
		private static <T> Consumer<T> saving(Consumer<T> set) {
			return value -> {
				set.accept(value);
				CorsairConfig.save();
			};
		}
	}
}
