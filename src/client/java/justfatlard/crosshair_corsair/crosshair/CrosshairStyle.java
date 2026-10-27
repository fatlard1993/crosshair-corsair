package justfatlard.crosshair_corsair.crosshair;

import java.util.Locale;

import justfatlard.crosshair_corsair.Main;
import net.minecraft.resources.Identifier;

/**
 * The shapes a crosshair can take. Each is a fifteen-pixel sprite drawn by generate_crosshairs.py,
 * except {@link #CROSS}, which is vanilla's own sprite so the default look is exactly the default
 * look. The generator does not draw a cross for that reason; it checks this enum for the rest.
 */
public enum CrosshairStyle {
	CROSS(Identifier.withDefaultNamespace("hud/crosshair")),
	CROSS_OPEN,
	CROSS_OPEN_DIAGONAL,
	CROSS_DIAGONAL_SMALL,
	CIRCLE,
	CIRCLE_LARGE,
	SQUARE,
	SQUARE_LARGE,
	DIAMOND,
	DIAMOND_LARGE,
	CARET,
	/**
	 * Twenty-three tall, not fifteen. A ranging reticle needs its marks far enough apart to tell
	 * apart, and fifteen rows leave seven under the centre. The extra rows hang below: the centre
	 * pixel is at the same index whatever the height, so one centring sum still serves every
	 * shape. Kept in step with SCOPE_HEIGHT in generate_crosshairs.py.
	 */
	SCOPE(23),
	DOT,
	BRACKETS,
	BRACKETS_TOP,
	BRACKETS_BOTTOM,
	BRACKETS_ROUND,
	LINES,
	LINE_BOTTOM;

	/** Every shape is fifteen wide; most are fifteen tall. */
	public static final int WIDTH = 15;
	public final Identifier sprite;
	public final int height;

	CrosshairStyle() {
		this(WIDTH);
	}

	CrosshairStyle(int height) {
		this.sprite = Identifier.fromNamespaceAndPath(Main.MOD_ID, "crosshair/" + configName());
		this.height = height;
	}

	CrosshairStyle(Identifier sprite) {
		this.sprite = sprite;
		this.height = WIDTH;
	}

	/** The shape's own texture, for drawing it somewhere that is not the hud. */
	public String texture() {
		return Shape.of(this).texture();
	}

	/** The name this style goes by in the config file. */
	public String configName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The name this style goes by in the mod menu. */
	public String label() {
		return configName().replace('_', ' ');
	}

	/** The style with this config name, or the plain cross for a name nobody drew. */
	public static CrosshairStyle named(String name) {
		if (name == null) return CROSS;
		try {
			return valueOf(name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
		} catch (IllegalArgumentException unknown) {
			return CROSS;
		}
	}
}
