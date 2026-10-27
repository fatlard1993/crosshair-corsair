package justfatlard.crosshair_corsair.crosshair;

import net.minecraft.resources.Identifier;

/**
 * A sprite and the height it is drawn at: one of the built-in styles, or a shape another mod drew
 * for its own item.
 */
public record Shape(Identifier sprite, int height) {

	public static Shape of(CrosshairStyle style) {
		return new Shape(style.sprite, style.height);
	}

	/**
	 * A style's config name, or a sprite id - {@code namespace:path} under
	 * {@code textures/gui/sprites} - for a shape a mod shipped. The colon is the difference: no
	 * built-in name has one. A sprite is drawn in the same fifteen-pixel frame as the rest.
	 */
	public static Shape named(String name) {
		if (name != null && name.indexOf(':') >= 0) {
			Identifier sprite = Identifier.tryParse(name.trim());
			if (sprite != null) return new Shape(sprite, CrosshairStyle.WIDTH);
		}
		return of(CrosshairStyle.named(name));
	}

	/**
	 * The shape's own texture, for drawing it somewhere that is not the hud.
	 *
	 * <p>A sprite id and a texture path are not the same string: the hud draws these off the gui
	 * atlas, and a menu wanting to show one needs the file. Built from the sprite so the two
	 * cannot drift.
	 */
	public String texture() {
		return sprite.getNamespace() + ":textures/gui/sprites/" + sprite.getPath() + ".png";
	}
}
