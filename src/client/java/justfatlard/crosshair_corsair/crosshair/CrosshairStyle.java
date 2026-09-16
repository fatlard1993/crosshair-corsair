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
	DOT,
	BRACKETS,
	BRACKETS_TOP,
	BRACKETS_BOTTOM,
	BRACKETS_ROUND,
	LINES,
	LINE_BOTTOM;

	public final Identifier sprite;

	CrosshairStyle() {
		this.sprite = Identifier.fromNamespaceAndPath(Main.MOD_ID, "crosshair/" + configName());
	}

	CrosshairStyle(Identifier sprite) {
		this.sprite = sprite;
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
