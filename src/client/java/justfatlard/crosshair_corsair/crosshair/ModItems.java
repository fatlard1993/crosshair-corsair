package justfatlard.crosshair_corsair.crosshair;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.Main;
import justfatlard.crosshair_corsair.integration.PandoricalSettings;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

/**
 * Crosshairs other mods declare for their own items, at
 * {@code assets/<namespace>/crosshair_corsair/<name>.json}:
 *
 * <pre>{@code
 * {
 *   "items": ["fletch_craft:flint_throwing_knife", "fletch_craft:obsidian_throwing_knife"],
 *   "like": "throwable",
 *   "style": "circle_large",
 *   "policy": "interactable"
 * }
 * }</pre>
 *
 * <p>A client resource rather than an API call, for two reasons. A mod declaring one needs no build
 * dependency on this one. And a server mod's items reach a Pandorical client as plain stand-ins
 * with none of their classes, so nothing on the client can tell a knife from a stick - but
 * Pandorical ships the mod's assets along with them, and this file rides in the same folder.
 *
 * <p>{@code like} is the built-in kind of item it behaves as, which decides what "it would work"
 * means for its policy. {@code style} and {@code policy} default to that kind's; {@code style}
 * also takes a sprite id, for a shape the mod drew itself. What a mod declares is only where the
 * player starts: the choice itself lives in the config file under {@code modItems}.
 */
public final class ModItems implements SimpleSynchronousResourceReloadListener {

	private static final String DIRECTORY = "crosshair_corsair";
	private static final String SUFFIX = ".json";
	private static final Gson GSON = new Gson();

	/** The built-in kinds a mod item can behave as. */
	public enum Like {
		THROWABLE, RANGED_WEAPON, MELEE_WEAPON, USABLE_ITEM;

		private static final CorsairConfig.Crosshair DEFAULTS = new CorsairConfig.Crosshair();

		String defaultPolicy() {
			return switch (this) {
				case THROWABLE -> DEFAULTS.holdingThrowable;
				case RANGED_WEAPON -> DEFAULTS.holdingRangedWeapon;
				case MELEE_WEAPON -> DEFAULTS.holdingMeleeWeapon;
				case USABLE_ITEM -> DEFAULTS.holdingUsableItem;
			};
		}

		String defaultStyle() {
			return switch (this) {
				case THROWABLE -> DEFAULTS.styles.holdingThrowable;
				case RANGED_WEAPON -> DEFAULTS.styles.holdingRangedWeapon;
				case MELEE_WEAPON -> DEFAULTS.styles.holdingMeleeWeapon;
				case USABLE_ITEM -> DEFAULTS.styles.holdingUsableItem;
			};
		}
	}

	/**
	 * One declaration.
	 *
	 * @param id     {@code namespace:name}, from where the file sits; the key in the config file
	 * @param name   what the menu calls it, from the file name
	 * @param style  the mod's default, a style name or a sprite id
	 * @param policy the mod's default, a {@link Policy} word
	 */
	public record ModItem(String id, String name, Like like, String style, String policy) {
		/** The mod's own sprite, if the default is one rather than a built-in name. */
		public Shape ownShape() {
			return style.indexOf(':') >= 0 ? Shape.named(style) : null;
		}
	}

	private static volatile Map<Identifier, ModItem> byItem = Map.of();
	private static volatile List<ModItem> all = List.of();

	public static ModItem of(ItemStack stack) {
		if (stack.isEmpty()) return null;
		return byItem.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	public static List<ModItem> all() {
		return all;
	}

	@Override
	public Identifier getFabricId() {
		return Identifier.fromNamespaceAndPath(Main.MOD_ID, "mod_items");
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		// Sorted, so two mods claiming one item settle it the same way on every reload.
		Map<Identifier, Resource> files = new TreeMap<>(
			manager.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX)));
		Map<Identifier, ModItem> items = new HashMap<>();
		List<ModItem> declared = new ArrayList<>();

		for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
			Identifier file = entry.getKey();
			String path = file.getPath();
			String name = path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length());
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
				ModItem item = parse(file.getNamespace() + ":" + name, name, json);
				Set<Identifier> ids = itemIds(json);
				if (ids.isEmpty()) {
					Main.LOGGER.warn("{} names no items, so it changes nothing", file);
					continue;
				}
				for (Identifier id : ids) {
					ModItem earlier = items.putIfAbsent(id, item);
					if (earlier != null) Main.LOGGER.warn("{} is claimed by both {} and {}; {} keeps it", id, earlier.id(), item.id(), earlier.id());
				}
				declared.add(item);
			} catch (Exception e) {
				Main.LOGGER.warn("Could not read {}", file, e);
			}
		}

		byItem = Map.copyOf(items);
		all = Collections.unmodifiableList(declared);

		if (CorsairConfig.get().crosshair.adoptModItems()) CorsairConfig.save();
		PandoricalSettings.modItems(all);
	}

	private static ModItem parse(String id, String name, JsonObject json) {
		Like like = Like.USABLE_ITEM;
		if (json.has("like")) {
			String word = json.get("like").getAsString().trim().toUpperCase(Locale.ROOT);
			try {
				like = Like.valueOf(word);
			} catch (IllegalArgumentException unknown) {
				Main.LOGGER.warn("{}: '{}' is not a kind of item this knows; treating it as a usable item", id, word);
			}
		}
		String style = json.has("style") ? json.get("style").getAsString() : like.defaultStyle();
		String policy = json.has("policy") ? Policy.named(json.get("policy").getAsString()).configName() : like.defaultPolicy();
		return new ModItem(id, name.replace('_', ' '), like, style, policy);
	}

	private static Set<Identifier> itemIds(JsonObject json) {
		Set<Identifier> ids = new LinkedHashSet<>();
		JsonArray items = json.has("items") ? json.getAsJsonArray("items") : new JsonArray();
		for (JsonElement element : items) {
			Identifier id = Identifier.tryParse(element.getAsString());
			if (id != null) ids.add(id);
		}
		return ids;
	}
}
