package justfatlard.crosshair_corsair.mixin;

import justfatlard.crosshair_corsair.reach.Reacharound;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the reacharound first refusal on a right-click.
 *
 * <p>Standing in front of the method rather than inside it, because the whole condition for acting
 * is that vanilla is about to do nothing: {@link Reacharound#find} has already established that the
 * crosshair is on air, so letting the original run first would only walk it to the same conclusion.
 * When there is no reacharound to make - which is nearly every click - this costs two field reads
 * and gets out of the way.
 */
@Mixin(Minecraft.class)
public abstract class UseItemMixin {

	@Shadow private int rightClickDelay;

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void corsair$reacharound(CallbackInfo ci) {
		Minecraft minecraft = (Minecraft) (Object) this;

		Reacharound.Target target = Reacharound.find(minecraft);
		if (target == null) return;
		if (!Reacharound.place(minecraft, target)) return;

		// Vanilla sets this at the top of the method being cancelled, and it is the whole of what
		// stops a held right mouse button from placing twenty blocks a second. Skipping it would
		// leave the reacharound the one placement in the game with no cadence.
		rightClickDelay = Reacharound.PLACE_DELAY_TICKS;
		ci.cancel();
	}
}
