package justfatlard.crosshair_corsair.crosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import justfatlard.crosshair_corsair.CorsairConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.GameType;

/**
 * Draws the crosshair in vanilla's place: the shape the moment calls for, or nothing.
 *
 * <p>Vanilla's element is kept and asked to draw whenever this one would only be copying it - the
 * mod switched off, spectators, no player yet. Everything else is drawn here in vanilla's frame:
 * fifteen pixels, dead centre, the attack indicator sixteen below.
 */
public final class CrosshairElement implements HudElement {
	private static final int SIZE = 15;
	private static final Identifier ATTACK_FULL = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");
	private static final Identifier ATTACK_BACKGROUND = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
	private static final Identifier ATTACK_PROGRESS = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");

	private final HudElement vanilla;

	public CrosshairElement(HudElement vanilla) {
		this.vanilla = vanilla;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		CorsairConfig.Crosshair config = CorsairConfig.get().crosshair;
		if (!config.enabled || mc.player == null || mc.gameMode == null
				|| mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
			vanilla.extractRenderState(graphics, delta);
			return;
		}
		if (!mc.options.getCameraType().isFirstPerson() && !config.thirdPerson) return;
		// GameRenderer draws the three-dimensional debug crosshair out in the world pass, and
		// vanilla stands its own flat one down while that is on. Drawing here would be two.
		if (mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) return;

		CrosshairContext.Reading reading = CrosshairContext.read(mc, config);
		boolean crosshair = reading.state() != CrosshairContext.State.HIDDEN;
		boolean indicator = mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR;
		if (!crosshair && !indicator) return;

		graphics.nextStratum();
		int x = (graphics.guiWidth() - SIZE) / 2;
		int y = (graphics.guiHeight() - SIZE) / 2;

		if (crosshair) {
			RenderPipeline pipeline = config.blend ? RenderPipelines.CROSSHAIR : RenderPipelines.GUI_TEXTURED;
			draw(graphics, pipeline, styleFor(reading.state(), config.styles).sprite, x, y, config);
			if (reading.usable()) draw(graphics, pipeline, CrosshairStyle.BRACKETS_ROUND.sprite, x, y, config);
			if (reading.correctTool()) draw(graphics, pipeline, CrosshairStyle.DOT.sprite, x, y, config);
		}
		// The indicator is about the weapon's cooldown rather than about what is under the
		// crosshair, so hiding the crosshair does not take it with it. It keeps vanilla's pipeline
		// and vanilla's colour whatever the crosshair is set to.
		if (indicator) attackIndicator(mc, graphics);
	}

	private static void draw(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
			int x, int y, CorsairConfig.Crosshair config) {
		if (config.overrideColor) {
			graphics.blitSprite(pipeline, sprite, x, y, SIZE, SIZE, CorsairConfig.get().crosshairArgb());
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, SIZE, SIZE);
		}
	}

	/** Vanilla's attack indicator, in vanilla's place under the crosshair, on vanilla's terms. */
	private static void attackIndicator(Minecraft mc, GuiGraphicsExtractor graphics) {
		RenderPipeline pipeline = RenderPipelines.CROSSHAIR;
		float strength = mc.player.getAttackStrengthScale(0F);
		boolean ready = strength >= 1F
			&& mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive()
			&& mc.player.getCurrentItemAttackStrengthDelay() > 5F
			&& withinAttackRange(mc);
		int x = graphics.guiWidth() / 2 - 8;
		int y = graphics.guiHeight() / 2 - 7 + 16;
		if (ready) {
			graphics.blitSprite(pipeline, ATTACK_FULL, x, y, 16, 16);
		} else if (strength < 1F) {
			int width = (int) (strength * 17F);
			graphics.blitSprite(pipeline, ATTACK_BACKGROUND, x, y, 16, 4);
			graphics.blitSprite(pipeline, ATTACK_PROGRESS, 16, 4, 0, 0, x, y, width, 4);
		}
	}

	/** The reach test vanilla puts on a full indicator, so a target out of range does not read as ready. */
	private static boolean withinAttackRange(Minecraft mc) {
		if (mc.hitResult == null) return false;
		AttackRange range = mc.player.getActiveItem().get(DataComponents.ATTACK_RANGE);
		if (range == null) range = AttackRange.defaultFor(mc.player);
		return range.isInRange(mc.player, mc.hitResult.getLocation());
	}

	private static CrosshairStyle styleFor(CrosshairContext.State state, CorsairConfig.Crosshair.Styles styles) {
		return CrosshairStyle.named(switch (state) {
			case ON_BLOCK -> styles.onBlock;
			case ON_INTERACTABLE_BLOCK -> styles.onInteractableBlock;
			case ON_ENTITY -> styles.onEntity;
			case HOLDING_TOOL -> styles.holdingTool;
			case HOLDING_MELEE_WEAPON -> styles.holdingMeleeWeapon;
			case HOLDING_RANGED_WEAPON -> styles.holdingRangedWeapon;
			case HOLDING_THROWABLE -> styles.holdingThrowable;
			case HOLDING_SHIELD -> styles.holdingShield;
			case HOLDING_BLOCK -> styles.holdingBlock;
			case HOLDING_USABLE_ITEM -> styles.holdingUsableItem;
			case REACHAROUND_FLOOR -> styles.reacharoundFloor;
			case REACHAROUND_CEILING -> styles.reacharoundCeiling;
			default -> styles.regular;
		});
	}
}
