package justfatlard.crosshair_corsair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's opinions, in {@code config/crosshair-corsair.json}.
 *
 * <p>Written out in full on first run - defaults and all - so every knob is discoverable in the
 * file rather than only in a readme, and re-read whenever the file changes on disk. Tuning a colour
 * is a look-at-it-and-adjust loop, and a loop that costs a game restart per step is a loop nobody
 * runs twice.
 *
 * <p>The selection box defaults to vanilla's exact appearance and the reacharound defaults to on.
 * An opinionated mod should still arrive without opinions already applied to things the player did
 * not ask about: the styling is here to be reached for, while the reacharound is the reason the mod
 * exists.
 */
public final class CorsairConfig {

	public SelectionBox selectionBox = new SelectionBox();
	public Reacharound reacharound = new Reacharound();
	public Crosshair crosshair = new Crosshair();

	/**
	 * The crosshair itself: when it shows, and what shape it takes.
	 *
	 * <p>Policies are words rather than booleans because "show it" has more than two answers for
	 * a held item: {@code always}, {@code targeting} (something under the crosshair), or
	 * {@code interactable} (the item would do something to what is there), and {@code never}.
	 * Styles are names from {@link justfatlard.crosshair_corsair.crosshair.CrosshairStyle}.
	 */
	public static final class Crosshair {
		public boolean enabled = true;
		/** Nothing under the crosshair and nothing useful in hand: no crosshair. */
		public boolean hideWhenIdle = true;
		public boolean thirdPerson = false;

		public boolean onBlock = true;
		public boolean onInteractableBlock = true;
		public boolean onEntity = true;

		/** always | targeting */
		public String holdingTool = "always";
		/** A dot in the middle when the tool in hand is the right one for the block it is on. */
		public boolean correctToolDot = true;
		public boolean holdingMeleeWeapon = true;
		/** Only show the melee style when there is something to hit. */
		public boolean meleeOnlyOnEntity = false;
		/** always | interactable */
		public String holdingRangedWeapon = "always";
		/** always | interactable */
		public String holdingThrowable = "interactable";
		public boolean holdingShield = true;
		/** always | targeting | interactable | never */
		public String holdingBlock = "interactable";
		public boolean holdingBlockInOffhand = true;
		/** always | interactable */
		public String holdingUsableItem = "interactable";
		/** Round brackets around the crosshair when a use would do something. */
		public boolean usableBrackets = true;

		public boolean overrideColor = false;
		public String color = "#FFFFFF";
		/** Vanilla's inverted blend, so the crosshair reads on any background. Off draws it plain. */
		public boolean blend = true;

		public Styles styles = new Styles();

		public static final class Styles {
			public String regular = "cross";
			public String onBlock = "cross";
			public String onInteractableBlock = "cross";
			public String onEntity = "cross_open";
			public String holdingTool = "cross";
			public String holdingMeleeWeapon = "cross";
			public String holdingRangedWeapon = "circle";
			public String holdingThrowable = "circle_large";
			public String holdingShield = "brackets";
			public String holdingBlock = "square";
			public String holdingUsableItem = "cross";
			/** The reacharound would place at your feet: a line under the crosshair, like a floor. */
			public String reacharoundFloor = "line_bottom";
			/** Or over your head: a caret, pointing up. */
			public String reacharoundCeiling = "caret";
		}
	}

	/** How the block you are looking at is outlined. */
	public static final class SelectionBox {
		/** Draw it at all. False hides the outline entirely. */
		public boolean enabled = true;

		/** Line colour, {@code #RRGGBB}. Vanilla's is black. */
		public String color = "#000000";

		/** Line opacity, 0-255. Vanilla's is 102. */
		public int alpha = 102;

		/**
		 * Line thickness in pixels. Zero means "whatever the game would have used", which is not a
		 * constant - vanilla scales it with the window so the outline stays the same apparent
		 * weight at any resolution. Setting a number here opts out of that scaling.
		 */
		public float lineWidth = 0.0F;
		/**
		 * A pulse on the outline's alpha: how far it swings, in alpha units, and how many swings a
		 * second. Zero swing is a steady line, which is vanilla.
		 */
		public int blinkAlpha = 0;
		public float blinkSpeed = 1.0F;
		/** none | shrink | down | alpha: what the box does as the block under it breaks. */
		public String breakAnimation = "none";
	}

	/** Placing against a block you are not looking at. */
	public static final class Reacharound {
		/** The whole feature, which the toggle key also flips. Both modes below sit under it. */
		public boolean enabled = true;

		/**
		 * Extend the floor you are standing on, in the direction you face.
		 *
		 * <p>The bridging case: you are at the edge of a drop with a block in hand, looking ahead
		 * rather than down at your feet, and vanilla has nothing under the crosshair to place
		 * against.
		 */
		public boolean horizontal = true;

		/** The same, overhead: extend the ceiling above you while looking up. */
		public boolean vertical = false;

		/** Outline where the block would land, so the placement is never a surprise. */
		public boolean showGhost = true;

		public String ghostColor = "#FFFFFF";
		public int ghostAlpha = 120;
		/**
		 * The outline when the block would land there but the server would refuse it: something
		 * standing in the space, or a block that cannot stand there. Same opacity as the ghost.
		 */
		public String blockedColor = "#FF5555";
	}

	// --- derived, so the strings are parsed once rather than per frame ---

	private transient int outlineArgb = argb("#000000", 102);
	private transient int ghostArgb = argb("#FFFFFF", 120);
	private transient int blockedArgb = argb("#FF5555", 120);

	public int outlineArgb() { return outlineArgb; }

	public int ghostArgb() { return ghostArgb; }

	public int blockedArgb() { return blockedArgb; }

	/**
	 * Whether the configured style differs from what vanilla would draw anyway.
	 *
	 * <p>When it does not, vanilla is left to draw its own outline rather than being replaced by an
	 * identical-looking copy. That keeps the default install on the game's own code path, and it
	 * keeps the debug shape overlays - which live inside the method being replaced - working for
	 * anyone who has them switched on.
	 */
	public boolean stylesSelectionBox() {
		return outlineArgb != VANILLA_OUTLINE_ARGB || selectionBox.lineWidth > 0.0F;
	}

	/** Vanilla's outline: black at alpha 102. */
	private static final int VANILLA_OUTLINE_ARGB = 102 << 24;

	// --- loading ---

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "crosshair-corsair.json";

	private static CorsairConfig current = new CorsairConfig();
	private static long lastModified = Long.MIN_VALUE;

	public static CorsairConfig get() { return current; }

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/**
	 * Re-read the file if it has been touched since the last look.
	 *
	 * <p>Called on a slow tick rather than per frame: one stat call a second is free, and a config
	 * edit that takes a second to show up still feels immediate to the person who just saved it.
	 */
	public static void reloadIfChanged() {
		Path path = path();
		try {
			if (!Files.exists(path)) {
				// Gone, or never written. Write the current shape back out so the file the player
				// is looking for exists, and so a hand-deleted file resets to defaults rather than
				// leaving the mod running on settings with no visible source.
				current = new CorsairConfig();
				current.derive();
				Files.createDirectories(path.getParent());
				Files.writeString(path, GSON.toJson(current));
				lastModified = Files.getLastModifiedTime(path).toMillis();
				return;
			}

			long modified = Files.getLastModifiedTime(path).toMillis();
			if (modified == lastModified) return;
			lastModified = modified;

			CorsairConfig loaded = GSON.fromJson(Files.readString(path), CorsairConfig.class);
			if (loaded == null) return;

			// A hand-edited file can be missing whole sections; Gson leaves those null rather than
			// defaulting them, and a null here would be a crash inside the render loop.
			if (loaded.selectionBox == null) loaded.selectionBox = new SelectionBox();
			if (loaded.reacharound == null) loaded.reacharound = new Reacharound();
			if (loaded.crosshair == null) loaded.crosshair = new Crosshair();
			if (loaded.crosshair.styles == null) loaded.crosshair.styles = new Crosshair.Styles();

			loaded.derive();
			current = loaded;
			justfatlard.crosshair_corsair.integration.PandoricalSettings.changed();
		} catch (Exception e) {
			Main.LOGGER.warn("Could not read {} - keeping the settings already loaded",
				FILE_NAME, e);
		}
	}

	/**
	 * Write the current settings out, for a change that came from somewhere other than the file:
	 * the mod menu. The file stays the source of truth, so the change goes there first and the
	 * poll picks it back up like any other edit.
	 */
	public static void save() {
		try {
			Path path = path();
			Files.createDirectories(path.getParent());
			current.derive();
			Files.writeString(path, GSON.toJson(current));
			lastModified = Files.getLastModifiedTime(path).toMillis();
		} catch (Exception e) {
			Main.LOGGER.warn("Could not write {}", FILE_NAME, e);
		}
	}

	private void derive() {
		outlineArgb = argb(selectionBox.color, selectionBox.alpha);
		ghostArgb = argb(reacharound.ghostColor, reacharound.ghostAlpha);
		blockedArgb = argb(reacharound.blockedColor, reacharound.ghostAlpha);
		crosshairArgb = argb(crosshair.color, 255);
	}

	private transient int crosshairArgb;

	/** The crosshair's override colour, opaque; only meaningful when the override is on. */
	public int crosshairArgb() { return crosshairArgb; }

	/**
	 * A {@code #RRGGBB} string and an alpha, packed the way the renderer wants them.
	 *
	 * <p>Colours are written as hex in the file because that is the form a person can copy out of
	 * whatever they picked it in. A value that will not parse falls back to opaque white and says
	 * so, rather than throwing from inside a frame.
	 */
	private static int argb(String hex, int alpha) {
		int clamped = Math.clamp(alpha, 0, 255);
		try {
			String digits = hex.startsWith("#") ? hex.substring(1) : hex;
			return (clamped << 24) | (Integer.parseInt(digits, 16) & 0xFFFFFF);
		} catch (RuntimeException e) {
			Main.LOGGER.warn("'{}' is not a #RRGGBB colour - using white", hex);
			return (clamped << 24) | 0xFFFFFF;
		}
	}
}
