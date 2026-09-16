package justfatlard.crosshair_corsair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import justfatlard.crosshair_corsair.integration.PandoricalSettings;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's opinions, in {@code config/crosshair-corsair.json}.
 *
 * <p>Written out in full whenever the file on disk is missing anything, so every knob is
 * discoverable in the file rather than only in a readme, and re-read whenever the file changes.
 * Tuning a colour is a look-at-it-and-adjust loop, and a loop that costs a game restart per step is
 * a loop nobody runs twice.
 *
 * <p>The selection box defaults to vanilla's exact appearance and the reacharound defaults to on:
 * the styling is here to be reached for, while the reacharound is the reason the mod exists.
 */
public final class CorsairConfig {

	public SelectionBox selectionBox = new SelectionBox();
	public Reacharound reacharound = new Reacharound();
	public Crosshair crosshair = new Crosshair();

	/** What the outline does to itself as the block under it is mined. */
	public enum BreakAnimation {
		NONE, SHRINK, DOWN, ALPHA;

		public static BreakAnimation named(String name) {
			if (name == null) return NONE;
			try {
				return valueOf(name.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException unknown) {
				return NONE;
			}
		}

		public String configName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * The crosshair itself: when it shows, and what shape it takes.
	 *
	 * <p>Held-item settings take a word from {@link justfatlard.crosshair_corsair.crosshair.Policy};
	 * styles take a name from {@link justfatlard.crosshair_corsair.crosshair.CrosshairStyle}.
	 */
	public static final class Crosshair {
		public boolean enabled = true;
		/** Nothing under the crosshair and nothing useful in hand: no crosshair. */
		public boolean hideWhenIdle = true;
		public boolean thirdPerson = false;

		public boolean onBlock = true;
		public boolean onInteractableBlock = true;
		public boolean onEntity = true;

		public String holdingTool = "always";
		/** A dot in the middle when the tool in hand is the right one for the block it is on. */
		public boolean correctToolDot = true;
		public boolean holdingMeleeWeapon = true;
		/** Only show the melee style when there is something to hit. */
		public boolean meleeOnlyOnEntity = false;
		public String holdingRangedWeapon = "always";
		public String holdingThrowable = "interactable";
		public boolean holdingShield = true;
		public String holdingBlock = "interactable";
		/** Whether a block in the offhand counts when the main hand is empty. Gates the reacharound too. */
		public boolean holdingBlockInOffhand = true;
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
		/** A word from {@link BreakAnimation}. */
		public String breakAnimation = "none";
	}

	/** Placing against a block you are not looking at. */
	public static final class Reacharound {
		/** The whole feature, which the toggle key also flips. Both modes below sit under it. */
		public boolean enabled = true;

		/**
		 * Extend the floor you are standing on, in the direction you face.
		 *
		 * <p>The bridging case: at the edge of a drop with a block in hand, looking ahead rather
		 * than down at your feet, and vanilla has nothing under the crosshair to place against.
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
	private transient int crosshairArgb = argb("#FFFFFF", 255);
	private transient BreakAnimation breakAnimation = BreakAnimation.NONE;

	public int outlineArgb() { return outlineArgb; }

	public int ghostArgb() { return ghostArgb; }

	public int blockedArgb() { return blockedArgb; }

	/** The crosshair's override colour, opaque; only meaningful when the override is on. */
	public int crosshairArgb() { return crosshairArgb; }

	public BreakAnimation breakAnimation() { return breakAnimation; }

	private void derive() {
		outlineArgb = argb(selectionBox.color, selectionBox.alpha);
		ghostArgb = argb(reacharound.ghostColor, reacharound.ghostAlpha);
		blockedArgb = argb(reacharound.blockedColor, reacharound.ghostAlpha);
		crosshairArgb = argb(crosshair.color, 255);
		breakAnimation = BreakAnimation.named(selectionBox.breakAnimation);
	}

	/**
	 * Whether anything here would draw the outline differently from vanilla.
	 *
	 * <p>When nothing would, vanilla is left to draw its own rather than being replaced by an
	 * identical-looking copy - which keeps the default install on the game's own code path and
	 * keeps the debug shape overlays, which live inside the method being replaced, working. The
	 * blink and the break animation have to be counted here: they are invisible on any frame this
	 * returns false, and leaving them out made both of them silently inert at default colours.
	 */
	public boolean stylesSelectionBox() {
		return outlineArgb != VANILLA_OUTLINE_ARGB
			|| selectionBox.lineWidth > 0.0F
			|| (selectionBox.blinkAlpha > 0 && selectionBox.blinkSpeed > 0)
			|| breakAnimation != BreakAnimation.NONE;
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
				// Gone, or never written. Write the current shape out so the file the player is
				// looking for exists, and so a hand-deleted file resets to defaults rather than
				// leaving the mod running on settings with no visible source.
				current = new CorsairConfig();
				write(path, current);
				return;
			}

			long modified = Files.getLastModifiedTime(path).toMillis();
			if (modified == lastModified) return;
			lastModified = modified;

			String text = Files.readString(path);
			CorsairConfig loaded = GSON.fromJson(text, CorsairConfig.class);
			if (loaded == null) return;

			// A hand-edited or older file can be missing whole sections; Gson leaves those null
			// rather than defaulting them, and a null here would be a crash inside the render loop.
			if (loaded.selectionBox == null) loaded.selectionBox = new SelectionBox();
			if (loaded.reacharound == null) loaded.reacharound = new Reacharound();
			if (loaded.crosshair == null) loaded.crosshair = new Crosshair();
			if (loaded.crosshair.styles == null) loaded.crosshair.styles = new Crosshair.Styles();

			loaded.derive();
			current = loaded;

			// An upgrade adds keys the old file has never heard of, and filling them in memory
			// alone leaves them undiscoverable in the one place this mod says to look for them.
			String canonical = GSON.toJson(loaded);
			if (!canonical.equals(text)) write(path, loaded);

			PandoricalSettings.changed();
		} catch (Exception e) {
			Main.LOGGER.warn("Could not read {} - keeping the settings already loaded", FILE_NAME, e);
		}
	}

	/**
	 * Write the current settings out, for a change that came from somewhere other than the file:
	 * the mod menu, or the toggle key. The file stays the source of truth.
	 */
	public static void save() {
		try {
			write(path(), current);
		} catch (Exception e) {
			Main.LOGGER.warn("Could not write {}", FILE_NAME, e);
		}
	}

	private static void write(Path path, CorsairConfig config) throws java.io.IOException {
		config.derive();
		Files.createDirectories(path.getParent());
		Files.writeString(path, GSON.toJson(config));
		lastModified = Files.getLastModifiedTime(path).toMillis();
	}

	/**
	 * A {@code #RRGGBB} string and an alpha, packed the way the renderer wants them.
	 *
	 * <p>Colours are hex in the file because that is the form a person can copy out of whatever
	 * they picked it in. {@code #abc} is accepted as the shorthand everyone means by it; anything
	 * else that will not parse falls back to opaque white and says so, rather than throwing from
	 * inside a frame or - worse - parsing to a colour nobody asked for.
	 */
	private static int argb(String hex, int alpha) {
		int clamped = Math.clamp(alpha, 0, 255);
		String digits = hex == null ? "" : hex.trim();
		if (digits.startsWith("#")) digits = digits.substring(1);
		if (digits.length() == 3) {
			StringBuilder expanded = new StringBuilder(6);
			for (int i = 0; i < 3; i++) expanded.append(digits.charAt(i)).append(digits.charAt(i));
			digits = expanded.toString();
		}
		if (digits.length() != 6 || !isHex(digits)) {
			Main.LOGGER.warn("'{}' is not a #RRGGBB colour - using white", hex);
			return (clamped << 24) | 0xFFFFFF;
		}
		return (clamped << 24) | (Integer.parseInt(digits, 16) & 0xFFFFFF);
	}

	private static boolean isHex(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.digit(text.charAt(i), 16) < 0) return false;
		}
		return true;
	}
}
