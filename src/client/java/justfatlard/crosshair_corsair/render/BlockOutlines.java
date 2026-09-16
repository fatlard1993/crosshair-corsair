package justfatlard.crosshair_corsair.render;

import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.CorsairConfig.BreakAnimation;
import justfatlard.crosshair_corsair.reach.Reacharound;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Both of the mod's outlines: the one on the block you are looking at, and the one on the block the
 * reacharound would place.
 *
 * <p>They share a hook because they answer the same question from opposite sides. Fabric fires
 * {@code BEFORE_BLOCK_OUTLINE} every frame that outlines are being drawn at all, handing over a
 * null state when the crosshair is on empty air - which is exactly the frame the reacharound wants
 * to draw in and the selection box has nothing to say.
 */
public final class BlockOutlines {
	private BlockOutlines() {}

	/**
	 * @return false to take vanilla's own outline draw off the table, true to leave it to it
	 */
	public static boolean beforeBlockOutline(LevelRenderContext context,
			@Nullable BlockOutlineRenderState outline) {
		CorsairConfig config = CorsairConfig.get();

		drawReacharoundGhost(context, config);

		if (outline == null) return true;
		if (!config.selectionBox.enabled) return false;

		// High contrast is an accessibility setting, and somebody who turned it on asked for a
		// specific, legible outline by name. A cosmetic preference does not get to overrule that.
		if (outline.highContrast()) return true;

		if (!config.stylesSelectionBox()) return true;

		Breaking breaking = breaking(outline.pos(), config);
		int argb = blink(config.outlineArgb(), config.selectionBox);
		if (breaking.mode() == BreakAnimation.ALPHA) {
			argb = withAlpha(argb, (int) ((argb >>> 24) * (1 - breaking.progress())));
		}
		draw(context, outline.shape(), outline.pos(), argb, outline.isTranslucent(), breaking);
		return false;
	}

	/** The outline's colour with the blink applied: a sine on the alpha, clamped to sense. */
	private static int blink(int argb, CorsairConfig.SelectionBox box) {
		if (box.blinkAlpha <= 0 || box.blinkSpeed <= 0) return argb;
		double seconds = System.currentTimeMillis() / 1000.0;
		int swing = (int) (box.blinkAlpha * Math.sin(seconds * box.blinkSpeed * Math.PI * 2));
		return withAlpha(argb, Math.clamp((argb >>> 24) + swing, 0, 255));
	}

	private static int withAlpha(int argb, int alpha) {
		return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0xFFFFFF);
	}

	private record Breaking(BreakAnimation mode, float progress) {
		static final Breaking NONE = new Breaking(BreakAnimation.NONE, 0F);
	}

	private static Breaking breaking(BlockPos pos, CorsairConfig config) {
		if (config.breakAnimation() == BreakAnimation.NONE) return Breaking.NONE;
		var gameMode = Minecraft.getInstance().gameMode;
		if (gameMode == null || !gameMode.isDestroying()) return Breaking.NONE;
		var digging = (justfatlard.crosshair_corsair.mixin.GameModeDestroyAccessor) gameMode;
		if (!pos.equals(digging.corsair$destroyBlockPos())) return Breaking.NONE;
		return new Breaking(config.breakAnimation(), Math.clamp(digging.corsair$destroyProgress(), 0F, 1F));
	}

	private static void drawReacharoundGhost(LevelRenderContext context, CorsairConfig config) {
		if (!config.reacharound.showGhost) return;

		Reacharound.Target target = Reacharound.find(Minecraft.getInstance());
		if (target == null) return;

		int argb = target.blocked() ? config.blockedArgb() : config.ghostArgb();
		draw(context, Shapes.block(), target.placeAt(), argb, false, Breaking.NONE);
	}

	private static void draw(LevelRenderContext context, VoxelShape shape, BlockPos pos, int argb,
			boolean translucentShape, Breaking breaking) {
		// The incoming stack is camera-relative world space - vanilla does its own push and
		// translate further down the method this runs in front of, so nothing has been applied yet.
		PoseStack pose = context.poseStack();
		Vec3 camera = context.levelState().cameraRenderState.pos;

		pose.pushPose();
		pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
		// The break animations are transforms on the pose rather than on the shape, so a stair or a
		// fence shrinks as the shape it is and not as its bounding box.
		if (breaking.mode() == BreakAnimation.SHRINK) {
			float scale = 1F - breaking.progress();
			pose.translate(0.5, 0.5, 0.5);
			pose.scale(scale, scale, scale);
			pose.translate(-0.5, -0.5, -0.5);
		} else if (breaking.mode() == BreakAnimation.DOWN) {
			pose.scale(1F, 1F - breaking.progress(), 1F);
		}
		context.submitNodeCollector()
			.submitShapeOutline(pose, shape, lineType(context), argb, lineWidth(context), translucentShape);
		pose.popPose();
	}

	/** Vanilla's own choice between the two translucent line passes, made the same way it makes it. */
	private static RenderType lineType(LevelRenderContext context) {
		return context.gameRenderer().useImprovedTransparency()
			? RenderTypes.linesTranslucentNoDepthWrite()
			: RenderTypes.linesTranslucent();
	}

	/** Configured thickness, or the game's own if none was asked for. */
	private static float lineWidth(LevelRenderContext context) {
		float configured = CorsairConfig.get().selectionBox.lineWidth;
		if (configured > 0.0F) return configured;

		return context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth;
	}
}
