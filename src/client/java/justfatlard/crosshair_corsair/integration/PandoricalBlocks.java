package justfatlard.crosshair_corsair.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pandorical's synced blocks know whether the server takes their right-click, and say so.
 *
 * <p>Compiled against Pandorical and never bundled; the class that reaches into it is only
 * loaded when Pandorical is present, so this mod runs without it as it always did.
 */
public final class PandoricalBlocks {
	private PandoricalBlocks() {}

	private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("pandorical");

	public static boolean isInteractive(BlockState state) {
		return PRESENT && Reach.isInteractive(state);
	}

	private static final class Reach {
		static boolean isInteractive(BlockState state) {
			return justfatlard.pandorical.client.content.ContentManager.isInteractive(state);
		}
	}
}
