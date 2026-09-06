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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

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
	 * How far up a side face the click lands when the block being extended sits in the top half of
	 * its space.
	 *
	 * <p>A slab or a stair decides which half it goes in from where on the face it was clicked, so a
	 * click at the exact centre of the face makes a bottom slab every time - and a bridge of top
	 * slabs drops half a block with each placement. Aiming above the middle keeps the surface level
	 * with the one you are standing on.
	 */
	private static final double UPPER_HALF_CLICK = 0.25;

	/**
	 * A placement waiting to happen.
	 *
	 * @param hit     the click this placement stands for: the face of an existing block, aimed
	 *                where a hand would aim it
	 * @param hand    the hand holding the block
	 * @param blocked whether the server would refuse it anyway: something standing in the space,
	 *                or a block that cannot survive there. Still worth drawing, in a colour that
	 *                says so, because an outline that vanishes when a cow wanders in leaves the
	 *                player wondering what they did.
	 */
	public record Target(BlockHitResult hit, InteractionHand hand, boolean blocked) {
		public BlockPos against() { return hit.getBlockPos(); }

		public Direction face() { return hit.getDirection(); }

		/** Where the block will actually end up. */
		public BlockPos placeAt() {
			return against().relative(face());
		}
	}

	/**
	 * The reacharound placement available right now, or null if there is not one.
	 *
	 * <p>Cheap enough to call every frame: a few block lookups and a dry run of the placement, with
	 * no world changed. Calling it per frame rather than caching a tick's answer is what keeps the
	 * outline honest while the player is still turning.
	 */
	public static Target find(Minecraft minecraft) {
		CorsairConfig.Reacharound settings = CorsairConfig.get().reacharound;
		if (!settings.enabled || (!settings.horizontal && !settings.vertical)) return null;

		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || minecraft.gameMode == null) return null;

		// The whole premise: vanilla has nothing under the crosshair. A hit on a block or an entity
		// is a placement or an interaction the player already asked for by name.
		if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.MISS) return null;

		// Mid-swing at a block, or already eating, drawing a bow, riding something. Checked here
		// rather than at the click so that the outline disappears at the same moment the placement
		// stops being available, instead of promising something the click would refuse.
		if (minecraft.gameMode.isDestroying() || player.isHandsBusy()) return null;

		InteractionHand hand = handHoldingABlock(player);
		if (hand == null) return null;

		BlockPos reference = referenceBlock(player, settings);
		if (reference == null) return null;

		BlockState against = level.getBlockState(reference);
		if (!canPlaceAgainst(against)) return null;

		Direction facing = player.getDirection();
		BlockPos at = reference.relative(facing);
		if (level.isOutsideBuildHeight(at)) return null;
		if (!level.getBlockState(at).canBeReplaced()) return null;

		BlockHitResult hit = hitFor(reference, facing, against);
		return new Target(hit, hand, !wouldSucceed(player, level, hand, hit, at));
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
	 * centre satisfies that with room to spare. On a side face of a block that lives in the top
	 * half of its space, the click moves up to match.
	 */
	private static BlockHitResult hitFor(BlockPos against, Direction face, BlockState againstState) {
		Vec3 centre = Vec3.atCenterOf(against);
		Vec3 towardFace = Vec3.atCenterOf(against.relative(face)).subtract(centre).scale(0.5);
		Vec3 location = centre.add(towardFace);
		if (face.getAxis().isHorizontal() && upperHalf(againstState)) {
			location = location.add(0, UPPER_HALF_CLICK, 0);
		}
		return new BlockHitResult(location, face, against, false);
	}

	/** A top slab, an upside-down stair, a trapdoor on the ceiling: anything drawn in its top half. */
	private static boolean upperHalf(BlockState state) {
		if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
			return state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP;
		}
		return state.hasProperty(BlockStateProperties.HALF)
			&& state.getValue(BlockStateProperties.HALF) == Half.TOP;
	}

	/**
	 * The two checks the server's placement code runs after everything else has passed: the block
	 * has to be able to stand where it lands, and nothing can be standing there already.
	 *
	 * <p>Asked of the same state the click will produce, from the same context, so a torch with no
	 * wall or a cow in the way is known before the click rather than after it.
	 */
	private static boolean wouldSucceed(LocalPlayer player, ClientLevel level, InteractionHand hand,
			BlockHitResult hit, BlockPos at) {
		ItemStack stack = player.getItemInHand(hand);
		BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hit);
		BlockState placed = ((BlockItem) stack.getItem()).getBlock().getStateForPlacement(context);
		return placed != null
			&& placed.canSurvive(level, at)
			&& level.isUnobstructed(placed, at, CollisionContext.placementContext(player));
	}

	/**
	 * Send the placement.
	 *
	 * @return whether it was taken, so the caller knows whether vanilla still needs its turn
	 */
	public static boolean place(Minecraft minecraft, Target target) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null || target.blocked()) return false;

		// Read before the placement, because a successful one shrinks the stack it came from and
		// an emptied stack has no animation left to ask about.
		ItemStack stack = player.getItemInHand(target.hand());
		SwingAnimation animation = stack.getInteractAnimation();
		int count = stack.getCount();

		InteractionResult result = minecraft.gameMode.useItemOn(player, target.hand(), target.hit());
		if (!(result instanceof InteractionResult.Success success)) return false;

		if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
			player.swing(target.hand(), animation, true);
		}
		// The hand's own bob on a placement, on the same terms vanilla grants it: something left
		// the stack, or nothing ever leaves it.
		if (!stack.isEmpty() && (stack.getCount() != count || player.hasInfiniteMaterials())) {
			player.itemUsed(target.hand());
		}
		return true;
	}
}
