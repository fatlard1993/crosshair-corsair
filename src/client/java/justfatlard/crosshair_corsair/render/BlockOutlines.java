package justfatlard.crosshair_corsair.render;

import justfatlard.crosshair_corsair.CorsairConfig;
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
		// specific, legible outline by name. A cosmetic preference does not get to overrule that,
		// so vanilla keeps the whole job whenever it is on.
		if (outline.highContrast()) return true;

		if (!config.stylesSelectionBox()) return true;

		draw(context, outline.shape(), outline.pos(), config.outlineArgb(), outline.isTranslucent());
		return false;
	}

	private static void drawReacharoundGhost(LevelRenderContext context, CorsairConfig config) {
		if (!config.reacharound.showGhost) return;

		Reacharound.Target target = Reacharound.find(Minecraft.getInstance());
		if (target == null) return;

		draw(context, Shapes.block(), target.placeAt(), config.ghostArgb(), false);
	}

	private static void draw(LevelRenderContext context, VoxelShape shape, BlockPos pos, int argb,
			boolean translucentShape) {
		// The incoming stack is camera-relative world space - vanilla does its own push and
		// translate further down the method this runs in front of, so nothing has been applied yet.
		PoseStack pose = context.poseStack();
		Vec3 camera = context.levelState().cameraRenderState.pos;

		pose.pushPose();
		pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
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

	/**
	 * Configured thickness, or the game's own if none was asked for.
	 *
	 * <p>Vanilla's is not a constant: it scales with the window so that an outline keeps the same
	 * apparent weight whatever the resolution. Naming a number in the config opts out of that,
	 * which is the point of being able to name one.
	 */
	private static float lineWidth(LevelRenderContext context) {
		float configured = CorsairConfig.get().selectionBox.lineWidth;
		if (configured > 0.0F) return configured;

		return context.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth;
	}
}
