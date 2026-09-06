package justfatlard.crosshair_corsair.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How far the block under the crosshair is broken, which vanilla keeps to itself. */
@Mixin(MultiPlayerGameMode.class)
public interface GameModeDestroyAccessor {
	@Accessor("destroyProgress") float corsair$destroyProgress();
	@Accessor("destroyBlockPos") BlockPos corsair$destroyBlockPos();
}
