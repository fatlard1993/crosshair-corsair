package justfatlard.crosshair_corsair;

import justfatlard.crosshair_corsair.integration.PandoricalSettings;
import justfatlard.crosshair_corsair.reach.ToggleKey;
import justfatlard.crosshair_corsair.render.BlockOutlines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opinions about the crosshair: what it looks like given what is under it and what is in your hand,
 * what the thing under it looks like, and what happens when there is nothing under it at all.
 *
 * <p>Client-side and nothing else. The reacharound places blocks through the same packet an
 * ordinary click sends, against a face that is genuinely there and well inside reach, so a vanilla
 * server sees an ordinary placement and no server half is needed.
 */
public class Main implements ClientModInitializer {

	public static final String MOD_ID = "crosshair-corsair-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * How often to look at the config file's timestamp.
	 *
	 * <p>Once a second. One stat call at that rate costs nothing, and a colour tweak that shows up
	 * within a second of saving still reads as immediate to the person who just saved it - which
	 * turns tuning into a loop instead of a series of game restarts.
	 */
	private static final int CONFIG_POLL_TICKS = 20;

	private static int ticksSinceConfigCheck = 0;

	@Override
	public void onInitializeClient() {
		CorsairConfig.reloadIfChanged();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++ticksSinceConfigCheck < CONFIG_POLL_TICKS) return;
			ticksSinceConfigCheck = 0;
			CorsairConfig.reloadIfChanged();
		});

		ToggleKey.register();
		PandoricalSettings.register();

		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(BlockOutlines::beforeBlockOutline);
		// The crosshair itself, in vanilla's slot so it keeps vanilla's order against the rest of
		// the HUD, with vanilla's element kept for the cases this one would only be copying.
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.replaceElement(
			net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CROSSHAIR,
			justfatlard.crosshair_corsair.crosshair.CrosshairElement::new);

		LOGGER.info("Crosshair Corsair loaded");
	}
}
