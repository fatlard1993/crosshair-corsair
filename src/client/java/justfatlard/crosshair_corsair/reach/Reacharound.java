package justfatlard.crosshair_corsair.reach;

import justfatlard.crosshair_corsair.CorsairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Placing a block against something you are standing on rather than something you are looking at.
 *
 * <p>The case this exists for is bridging. You are at the edge of a drop with blocks in hand and
 * you want the next one out in front of you, but the only face that would give you it is on the
 * side of the block under your feet - so vanilla asks you to look down at it, place, look back up,
 * step, and look down again. The block you want is unambiguous; the looking is ceremony.
 *
 * <p>The rule that keeps this from being a nuisance is that it only ever fires when vanilla would
 * do nothing at all: the crosshair has to be on empty air, the destination has to be replaceable,
 * and there has to be a real block to place against. Aim at anything and the reacharound is not
 * consulted. It fills a gap rather than competing for the click.
 *
 * <p>One function answers both "where would it go" and "put it there", so the outline drawn ahead
 * of the click and the block that arrives after it cannot disagree.
 */
public final class Reacharound {
	private Reacharound() {}

	/**
	 * Where the pitch stops meaning "along the ground" and starts meaning "overhead", in degrees
	 * above the horizon.
	 *
	 * <p>A single boundary rather than two with a dead zone between them: every angle belongs to
	 * exactly one of the two modes, so there is no band where the mod silently does nothing for
	 * reasons the player cannot see. Each mode is still switchable on its own, and a mode that is
	 * off simply yields nothing in its half of the sky.
	 */
	private static final float OVERHEAD_PITCH = -45.0F;

	/** Vanilla's own gap between one right-click placement and the next. */
	public static final int PLACE_DELAY_TICKS = 4;

	/**
	 * A placement waiting to happen.
	 *
	 * @param against the existing block whose face is being clicked
	 * @param face    the side of it that the new block goes on
	 * @param hand    the hand holding the block
	 */
	public record Target(BlockPos against, Direction face, InteractionHand hand) {
		/** Where the block will actually end up. */
		public BlockPos placeAt() {
			return against.relative(face);
		}
	}

	/**
	 * The reacharound placement available right now, or null if there is not one.
	 *
	 * <p>Cheap enough to call every frame: a couple of block lookups and no allocation beyond the
	 * result. Calling it per frame rather than caching a tick's answer is what keeps the outline
	 * honest while the player is still turning.
	 */
	public static Target find(Minecraft minecraft) {
		CorsairConfig.Reacharound settings = CorsairConfig.get().reacharound;
		if (!settings.horizontal && !settings.vertical) return null;

		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || minecraft.gameMode == null) return null;

		// The whole premise: vanilla has nothing under the crosshair. A hit on a block or an entity
		// is a placement or an interaction the player already asked for by name.
		if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.MISS) {
			return null;
		}

		// Mid-swing at a block, or already eating, drawing a bow, riding something. Checked here
		// rather than at the click so that the outline disappears at the same moment the placement
		// stops being available, instead of promising something the click would refuse.
		if (minecraft.gameMode.isDestroying() || player.isHandsBusy()) return null;

		InteractionHand hand = handHoldingABlock(player);
		if (hand == null) return null;

		BlockPos reference = referenceBlock(player, settings);
		if (reference == null) return null;

		if (!canPlaceAgainst(level.getBlockState(reference))) return null;

		Direction facing = player.getDirection();
		if (!level.getBlockState(reference.relative(facing)).canBeReplaced()) return null;

		return new Target(reference, facing, hand);
	}

	/**
	 * The block the placement extends: underfoot when looking along the ground, overhead when
	 * looking up.
	 *
	 * <p>Both are one step off a block the player is already occupying space against, which is why
	 * the destination can never land inside the player: it is a storey below their feet or a storey
	 * above their head, and always one block horizontally away besides.
	 */
	private static BlockPos referenceBlock(LocalPlayer player, CorsairConfig.Reacharound settings) {
		// Pitch runs positive downward, so "above the horizon" is the negative half.
		boolean overhead = player.getXRot() <= OVERHEAD_PITCH;

		if (overhead) {
			return settings.vertical ? BlockPos.containing(player.getEyePosition()).above() : null;
		}
		// The block actually underfoot, which is not always the one a storey below the feet: stand
		// on a bottom slab and your feet are half way up their own block, so stepping down from
		// them lands on the empty space under the slab rather than on the slab holding you up.
		return settings.horizontal ? player.getOnPos() : null;
	}

	/**
	 * Whether this is a block with a face worth clicking.
	 *
	 * <p>Replaceable blocks are excluded because clicking one places <em>into</em> it rather than
	 * beside it, which would put the block somewhere other than where the outline promised.
	 */
	private static boolean canPlaceAgainst(BlockState state) {
		return !state.isAir() && !state.canBeReplaced();
	}

	/** Main hand first, then off, which is the order vanilla tries them in. */
	private static InteractionHand handHoldingABlock(LocalPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).getItem() instanceof BlockItem) return hand;
		}
		return null;
	}

	/**
	 * The click this placement stands for.
	 *
	 * <p>Aimed at the centre of the face, derived from the two block positions rather than from a
	 * direction vector - the arithmetic is the same and it cannot be broken by a renamed accessor.
	 * The server checks that the hit location sits on the block it claims to be on, and a face
	 * centre satisfies that with room to spare.
	 */
	public static BlockHitResult hitFor(Target target) {
		Vec3 centre = Vec3.atCenterOf(target.against());
		Vec3 towardFace = Vec3.atCenterOf(target.placeAt()).subtract(centre).scale(0.5);
		return new BlockHitResult(centre.add(towardFace), target.face(), target.against(), false);
	}

	/**
	 * Send the placement.
	 *
	 * @return whether it was taken, so the caller knows whether vanilla still needs its turn
	 */
	public static boolean place(Minecraft minecraft, Target target) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null) return false;

		// Read before the placement, because a successful one shrinks the stack it came from and
		// an emptied stack has no animation left to ask about.
		SwingAnimation animation = player.getItemInHand(target.hand()).getInteractAnimation();

		InteractionResult result = minecraft.gameMode.useItemOn(player, target.hand(), hitFor(target));
		if (!(result instanceof InteractionResult.Success success)) return false;

		if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
			player.swing(target.hand(), animation, true);
		}
		return true;
	}
}
