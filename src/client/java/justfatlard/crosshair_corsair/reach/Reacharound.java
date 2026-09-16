package justfatlard.crosshair_corsair.reach;

import justfatlard.crosshair_corsair.CorsairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ProjectileWeaponItem;
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
 * <p>It only fires on a click vanilla would spend on nothing: the crosshair on empty air, no item
 * in either hand that would have used the click itself, a replaceable destination, and a real block
 * to place against. It fills a gap rather than competing for the click.
 *
 * <p>One function answers both "where would it go" and "put it there", so the outline drawn ahead
 * of the click and the block that arrives after it cannot disagree.
 */
public final class Reacharound {
	private Reacharound() {}

	/** Where the pitch stops meaning "along the ground" and starts meaning "overhead". */
	private static final float OVERHEAD_PITCH = -45.0F;

	/** Vanilla's own gap between one right-click placement and the next. */
	public static final int PLACE_DELAY_TICKS = 4;

	/**
	 * How far up a side face the click lands when the block being extended sits in the top half of
	 * its space.
	 *
	 * <p>A slab or a stair decides which half it goes in from where on the face it was clicked, and
	 * a click at the exact centre makes a bottom slab every time - so a bridge of top slabs would
	 * drop half a block per placement. Aiming above the middle keeps the surface level.
	 */
	private static final double UPPER_HALF_CLICK = 0.25;

	/**
	 * A placement waiting to happen.
	 *
	 * @param hit     the click this placement stands for: the face of an existing block
	 * @param hand    the hand holding the block
	 * @param blocked whether the server would refuse it: something standing in the space, or a
	 *                block that cannot survive there. Still drawn, in a colour that says so,
	 *                because an outline that vanishes when a cow wanders in leaves the player
	 *                wondering what they did.
	 */
	public record Target(BlockHitResult hit, InteractionHand hand, boolean blocked) {
		public BlockPos against() { return hit.getBlockPos(); }

		public Direction face() { return hit.getDirection(); }

		/** Where the block will actually end up. */
		public BlockPos placeAt() {
			return against().relative(face());
		}
	}

	/** What the click would do: land where promised, land there but be refused, or land elsewhere. */
	private enum Outcome { LANDS, BLOCKED, ELSEWHERE }

	/**
	 * The reacharound placement available right now, or null if there is not one.
	 *
	 * <p>Called per frame rather than cached, which is what keeps the outline honest while the
	 * player is still turning. Everything past the {@code MISS} gate only runs on frames where the
	 * crosshair is already on open sky.
	 */
	public static Target find(Minecraft minecraft) {
		CorsairConfig.Reacharound settings = CorsairConfig.get().reacharound;
		if (!settings.enabled || (!settings.horizontal && !settings.vertical)) return null;

		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || minecraft.gameMode == null) return null;

		// The whole premise: vanilla has nothing under the crosshair.
		if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.MISS) return null;

		// Mid-swing at a block, or already eating, drawing a bow, riding something. Checked here so
		// the outline disappears at the same moment the placement stops being available.
		if (minecraft.gameMode.isDestroying() || player.isHandsBusy()) return null;

		InteractionHand hand = handForPlacement(player);
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
		Outcome outcome = resolve(player, level, hand, hit, at);
		if (outcome == Outcome.ELSEWHERE) return null;
		return new Target(hit, hand, outcome == Outcome.BLOCKED);
	}

	/**
	 * The block the placement extends: underfoot when looking along the ground, overhead when
	 * looking up.
	 */
	private static BlockPos referenceBlock(LocalPlayer player, CorsairConfig.Reacharound settings) {
		// Pitch runs positive downward, so "above the horizon" is the negative half.
		boolean overhead = player.getXRot() <= OVERHEAD_PITCH;

		if (overhead) {
			return settings.vertical ? BlockPos.containing(player.getEyePosition()).above() : null;
		}
		// The block actually underfoot, which is not always the one a storey below the feet: stand
		// on a bottom slab and your feet are half way up their own block.
		return settings.horizontal ? player.getOnPos() : null;
	}

	/**
	 * Whether this is a block with a face worth clicking.
	 *
	 * <p>Replaceable blocks are excluded because clicking one places <em>into</em> it rather than
	 * beside it.
	 */
	private static boolean canPlaceAgainst(BlockState state) {
		return !state.isAir() && !state.canBeReplaced();
	}

	/**
	 * The hand this placement comes from, or null if the click is not ours to take.
	 *
	 * <p>Vanilla tries the main hand and then the offhand, so an item in either that would have used
	 * the click keeps it. The offhand only supplies a block when the main hand is empty, which is
	 * the same condition the crosshair uses to decide it is holding a block - the two have to agree
	 * or the shape and the placement part ways.
	 */
	private static InteractionHand handForPlacement(LocalPlayer player) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();

		if (main.getItem() instanceof BlockItem) {
			return wouldUse(off) ? null : InteractionHand.MAIN_HAND;
		}
		if (main.isEmpty() && CorsairConfig.get().crosshair.holdingBlockInOffhand
				&& off.getItem() instanceof BlockItem) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	/**
	 * An item vanilla would spend a click on even with nothing under the crosshair: food, a bow, a
	 * pearl, anything with a use of its own. A block is not one, which is the whole point.
	 */
	public static boolean wouldUse(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() instanceof BlockItem) return false;
		return stack.has(DataComponents.CONSUMABLE)
			|| stack.getItem() instanceof ProjectileItem
			|| stack.getItem() instanceof ProjectileWeaponItem
			|| stack.getUseAnimation() != ItemUseAnimation.NONE;
	}

	/**
	 * The click this placement stands for.
	 *
	 * <p>Aimed at the centre of the face, derived from the two block positions. The server checks
	 * that the hit location sits on the block it claims to be on, and a face centre satisfies that
	 * with room to spare. On a side face of a block that lives in the top half of its space, the
	 * click moves up to match.
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
	 * What this click would actually do, asked of the context the click will actually build.
	 *
	 * <p>The context decides for itself which position a click resolves to, and it is not always the
	 * neighbour the outline promised: scaffolding treats the block clicked as replaceable and then
	 * walks the placement up, so a click meant to extend a bridge lands over the player's head.
	 * A placement that would go somewhere else is not this placement, and is refused outright
	 * rather than drawn in the wrong place.
	 */
	private static Outcome resolve(LocalPlayer player, ClientLevel level, InteractionHand hand,
			BlockHitResult hit, BlockPos promised) {
		ItemStack stack = player.getItemInHand(hand);
		BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hit);
		if (!context.canPlace() || !context.getClickedPos().equals(promised)) return Outcome.ELSEWHERE;

		BlockState placed = ((BlockItem) stack.getItem()).getBlock().getStateForPlacement(context);
		if (placed == null) return Outcome.BLOCKED;
		boolean lands = placed.canSurvive(level, promised)
			&& level.isUnobstructed(placed, promised, CollisionContext.of(player));
		return lands ? Outcome.LANDS : Outcome.BLOCKED;
	}

	/**
	 * Send the placement.
	 *
	 * @return whether this click is spent, so the caller knows whether vanilla still needs its turn
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

		// The packet goes out before the result is inspected, so from here the click is spent
		// whatever the server makes of it. Reporting failure would let vanilla send two more.
		if (result instanceof InteractionResult.Success success) {
			if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
				player.swing(target.hand(), animation, true);
			}
			// The hand's own bob, on the same terms vanilla grants it: something left the stack, or
			// nothing ever leaves it.
			if (!stack.isEmpty() && (stack.getCount() != count || player.hasInfiniteMaterials())) {
				player.itemUsed(target.hand());
			}
		}
		return true;
	}
}
