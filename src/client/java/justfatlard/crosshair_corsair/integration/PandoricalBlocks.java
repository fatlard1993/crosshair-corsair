package justfatlard.crosshair_corsair.integration;

import justfatlard.crosshair_corsair.Main;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pandorical's synced blocks know whether the server takes their right-click, and say so.
 *
 * <p>Compiled against Pandorical; the class that reaches into it is only loaded when Pandorical is
 * present, so the mod runs without it at runtime. Building it, on the other hand, needs Pandorical's
 * source tree - see the note in build.gradle.
 */
public final class PandoricalBlocks {
	private PandoricalBlocks() {}

	// The mod is suggested rather than depended on, so nothing pins its version and a Pandorical
	// whose API has moved would throw from inside a render frame. First failure switches it off.
	private static boolean available = FabricLoader.getInstance().isModLoaded("pandorical");

	public static boolean isInteractive(BlockState state) {
		if (!available) return false;
		try {
			return Reach.isInteractive(state);
		} catch (LinkageError incompatible) {
			available = false;
			Main.LOGGER.warn("Pandorical is present but its content API does not match; "
				+ "synced-block crosshair hints are off", incompatible);
			return false;
		}
	}

	private static final class Reach {
		static boolean isInteractive(BlockState state) {
			return justfatlard.pandorical.client.content.ContentManager.isInteractive(state);
		}
	}
}
