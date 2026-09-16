package justfatlard.crosshair_corsair.reach;

import com.mojang.blaze3d.platform.InputConstants;
import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.Main;
import justfatlard.crosshair_corsair.integration.PandoricalSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A key that switches the reacharound off and on without leaving the game.
 *
 * <p>Unbound until the player binds it. The feature is on by default and most players will never
 * want it off, but the ones who do want it off want it off <em>now</em> - halfway through a build
 * where every click near an edge is a block they did not mean to place - and a config file is the
 * wrong distance away for that.
 *
 * <p>The flip goes to the file, the same as a change from the mod menu, so all three ways of
 * setting it stay in agreement.
 */
public final class ToggleKey {
	private ToggleKey() {}

	private static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key." + Main.MOD_ID + ".reacharound",
		InputConstants.UNKNOWN.getValue(),
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Main.MOD_ID, "main"))));

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ToggleKey::tick);
	}

	private static void tick(Minecraft minecraft) {
		// Any number of presses inside one tick is one flip. Pairing them off would make a hurried
		// double tap do nothing at all, silently, which is the exact moment this key is for.
		boolean pressed = false;
		while (KEY.consumeClick()) pressed = true;
		if (!pressed) return;

		CorsairConfig.Reacharound settings = CorsairConfig.get().reacharound;
		settings.enabled = !settings.enabled;
		CorsairConfig.save();
		PandoricalSettings.changed();

		// Over the hotbar rather than in chat: this is a state, not a conversation.
		minecraft.gui.hud.setOverlayMessage(
			Component.translatable(Main.MOD_ID + ".reacharound." + (settings.enabled ? "on" : "off")), false);
	}
}
