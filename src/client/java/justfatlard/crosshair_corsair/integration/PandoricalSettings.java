package justfatlard.crosshair_corsair.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.Main;
import justfatlard.crosshair_corsair.crosshair.CrosshairStyle;
import justfatlard.crosshair_corsair.crosshair.Policy;
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

	private static final class Reach {
		static void changed() {
			justfatlard.pandorical.client.api.PandoricalClientApi.settings().changed();
		}

		static void register() {
			var group = justfatlard.pandorical.client.api.PandoricalClientApi.settings()
				.group(Main.MOD_ID, "Crosshair Corsair");

			Map<String, String> policies = new LinkedHashMap<>();
			policies.put(Policy.ALWAYS.configName(), "Always");
			policies.put(Policy.TARGETING.configName(), "When aimed at something");
			policies.put(Policy.INTERACTABLE.configName(), "When it would work");
			Map<String, String> blockPolicies = new LinkedHashMap<>(policies);
			blockPolicies.put(Policy.NEVER.configName(), "Never");
			Map<String, String> styles = new LinkedHashMap<>();
			for (CrosshairStyle style : CrosshairStyle.values()) {
				styles.put(style.configName(), style.label());
			}
			Map<String, String> breaking = new LinkedHashMap<>();
			breaking.put(CorsairConfig.BreakAnimation.NONE.configName(), "None");
			breaking.put(CorsairConfig.BreakAnimation.SHRINK.configName(), "Shrink");
			breaking.put(CorsairConfig.BreakAnimation.DOWN.configName(), "Sink");
			breaking.put(CorsairConfig.BreakAnimation.ALPHA.configName(), "Fade");

			// The crosshair
			toggle(group, "crosshair", "Contextual crosshair", "Shape and presence follow what is under it and in hand",
				() -> c().enabled, v -> c().enabled = v);
			toggle(group, "hideWhenIdle", "Hide when idle", "No crosshair with nothing under it and nothing useful in hand",
				() -> c().hideWhenIdle, v -> c().hideWhenIdle = v);
			toggle(group, "thirdPerson", "Show in third person", null, () -> c().thirdPerson, v -> c().thirdPerson = v);
			toggle(group, "onBlock", "Show on blocks", null, () -> c().onBlock, v -> c().onBlock = v);
			toggle(group, "onInteractableBlock", "Show on usable blocks", null, () -> c().onInteractableBlock, v -> c().onInteractableBlock = v);
			toggle(group, "onEntity", "Show on entities", null, () -> c().onEntity, v -> c().onEntity = v);
			choice(group, "holdingTool", "Holding a tool", "\"When it would work\" means the right tool for that block",
				policies, () -> c().holdingTool, v -> c().holdingTool = v);
			toggle(group, "correctToolDot", "Dot for the right tool", null, () -> c().correctToolDot, v -> c().correctToolDot = v);
			toggle(group, "holdingMeleeWeapon", "Holding a weapon", null, () -> c().holdingMeleeWeapon, v -> c().holdingMeleeWeapon = v);
			toggle(group, "meleeOnlyOnEntity", "Weapon only on a target", null, () -> c().meleeOnlyOnEntity, v -> c().meleeOnlyOnEntity = v);
			choice(group, "holdingRangedWeapon", "Holding a bow", null, policies, () -> c().holdingRangedWeapon, v -> c().holdingRangedWeapon = v);
			choice(group, "holdingThrowable", "Holding a throwable", null, policies, () -> c().holdingThrowable, v -> c().holdingThrowable = v);
			toggle(group, "holdingShield", "Holding a shield", "Takes priority over everything else in that hand",
				() -> c().holdingShield, v -> c().holdingShield = v);
			choice(group, "holdingBlock", "Holding a block", null, blockPolicies, () -> c().holdingBlock, v -> c().holdingBlock = v);
			toggle(group, "holdingBlockInOffhand", "Offhand blocks count", "With an empty main hand; also lets the reacharound use them",
				() -> c().holdingBlockInOffhand, v -> c().holdingBlockInOffhand = v);
			choice(group, "holdingUsableItem", "Holding a usable item", null, policies, () -> c().holdingUsableItem, v -> c().holdingUsableItem = v);
			toggle(group, "usableBrackets", "Brackets when a use would work", null, () -> c().usableBrackets, v -> c().usableBrackets = v);
			toggle(group, "blend", "Inverted blend", "Vanilla's inverting crosshair; off draws it plain", () -> c().blend, v -> c().blend = v);
			toggle(group, "overrideColor", "Tint the crosshair", "Paints it the crosshair colour set in the config file",
				() -> c().overrideColor, v -> c().overrideColor = v);
			style(group, styles, "regular", "Shape: idle", () -> s().regular, v -> s().regular = v);
			style(group, styles, "styleOnBlock", "Shape: on a block", () -> s().onBlock, v -> s().onBlock = v);
			style(group, styles, "styleOnInteractable", "Shape: on a usable block", () -> s().onInteractableBlock, v -> s().onInteractableBlock = v);
			style(group, styles, "styleOnEntity", "Shape: on an entity", () -> s().onEntity, v -> s().onEntity = v);
			style(group, styles, "styleTool", "Shape: tool", () -> s().holdingTool, v -> s().holdingTool = v);
			style(group, styles, "styleMelee", "Shape: weapon", () -> s().holdingMeleeWeapon, v -> s().holdingMeleeWeapon = v);
			style(group, styles, "styleRanged", "Shape: bow", () -> s().holdingRangedWeapon, v -> s().holdingRangedWeapon = v);
			style(group, styles, "styleThrowable", "Shape: throwable", () -> s().holdingThrowable, v -> s().holdingThrowable = v);
			style(group, styles, "styleShield", "Shape: shield", () -> s().holdingShield, v -> s().holdingShield = v);
			style(group, styles, "styleBlock", "Shape: block", () -> s().holdingBlock, v -> s().holdingBlock = v);
			style(group, styles, "styleUsable", "Shape: usable item", () -> s().holdingUsableItem, v -> s().holdingUsableItem = v);
			style(group, styles, "styleReachFloor", "Shape: bridging", () -> s().reacharoundFloor, v -> s().reacharoundFloor = v);
			style(group, styles, "styleReachCeiling", "Shape: bridging overhead", () -> s().reacharoundCeiling, v -> s().reacharoundCeiling = v);

			// The selection box
			toggle(group, "selectionBox", "Block outline", "Off hides the outline entirely",
				() -> b().enabled, v -> b().enabled = v);
			number(group, "outlineAlpha", "Outline opacity", null, 0, 255, 17, () -> b().alpha, v -> b().alpha = v);
			number(group, "outlineWidth", "Outline width, tenths", "0 is vanilla's own", 0, 80, 5,
				() -> Math.round(b().lineWidth * 10), v -> b().lineWidth = v / 10F);
			number(group, "blinkAlpha", "Outline blink swing", "0 is a steady line", 0, 255, 17,
				() -> b().blinkAlpha, v -> b().blinkAlpha = v);
			number(group, "blinkSpeed", "Outline blinks a second, tenths", null, 0, 50, 5,
				() -> Math.round(b().blinkSpeed * 10), v -> b().blinkSpeed = v / 10F);
			choice(group, "breakAnimation", "Outline while mining", null, breaking, () -> b().breakAnimation, v -> b().breakAnimation = v);

			// The reacharound
			toggle(group, "reachEnabled", "Reacharound", "Place against the block under or over you when the crosshair is on air; the toggle key flips this too",
				() -> r().enabled, v -> r().enabled = v);
			toggle(group, "reachHorizontal", "Bridge placement", "Look ahead at an edge to place the next block out",
				() -> r().horizontal, v -> r().horizontal = v);
			toggle(group, "reachVertical", "Overhead placement", "Look well up to extend the ceiling",
				() -> r().vertical, v -> r().vertical = v);
			toggle(group, "reachGhost", "Show where it lands", "Outlines the destination before the click",
				() -> r().showGhost, v -> r().showGhost = v);
			number(group, "reachGhostAlpha", "Ghost opacity", null, 0, 255, 17, () -> r().ghostAlpha, v -> r().ghostAlpha = v);
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
				String key, String label, Supplier<String> get, Consumer<String> set) {
			group.choice(key, label, null, styles, () -> CrosshairStyle.named(get.get()).configName(), saving(set));
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
