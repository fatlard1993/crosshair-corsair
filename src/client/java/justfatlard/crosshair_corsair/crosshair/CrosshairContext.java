package justfatlard.crosshair_corsair.crosshair;

import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.integration.PandoricalBlocks;
import justfatlard.crosshair_corsair.reach.Reacharound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * What the crosshair is for right now: what is under it, what is in the hand, and what one would
 * do to the other.
 *
 * <p>Worked out from the client's own picture of the world, which is all a crosshair has. Vanilla
 * keeps its interaction rules behind methods a client cannot ask without acting, so the questions
 * here are answered by kind: a door is a thing you use, a villager is a thing you talk to, a block
 * with a menu opens. Wrong at the edges, right where anyone looks.
 *
 * <p>The branches are a priority list, first match wins, most specific first.
 */
public final class CrosshairContext {
	private CrosshairContext() {}

	public enum State {
		HIDDEN, REGULAR, ON_BLOCK, ON_INTERACTABLE_BLOCK, ON_ENTITY,
		HOLDING_TOOL, HOLDING_MELEE_WEAPON, HOLDING_RANGED_WEAPON, HOLDING_THROWABLE,
		HOLDING_SHIELD, HOLDING_BLOCK, HOLDING_USABLE_ITEM,
		REACHAROUND_FLOOR, REACHAROUND_CEILING
	}

	/** The state to draw, and the two overlays that ride on top of whichever shape it takes. */
	public record Reading(State state, boolean correctTool, boolean usable) {}

	private enum Target { NONE, BLOCK, ENTITY }

	public static Reading read(Minecraft mc, CorsairConfig.Crosshair config) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null) return new Reading(State.REGULAR, false, false);

		HitResult hit = mc.hitResult;
		Target target = Target.NONE;
		BlockState block = null;
		BlockPos at = null;
		Entity entity = null;
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			target = Target.BLOCK;
			at = blockHit.getBlockPos();
			block = level.getBlockState(at);
		} else if (hit instanceof EntityHitResult entityHit && hit.getType() == HitResult.Type.ENTITY) {
			target = Target.ENTITY;
			entity = entityHit.getEntity();
		}

		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		boolean targeting = target != Target.NONE;
		boolean blockInteractable = block != null && interactable(block, level, at);
		boolean entityInteractable = entity != null && interactable(entity, main);
		boolean entityAttackable = entity != null && entity.isAttackable();
		boolean brackets = config.usableBrackets;

		// The hand first: what you are holding says what you mean to do. A shield only counts when
		// it is the item a click would actually raise, so sword-and-board still reads as the sword.
		if (config.holdingShield && (blocks(main) || (main.isEmpty() && blocks(off)))) {
			return new Reading(State.HOLDING_SHIELD, false, false);
		}
		if (main.getItem() instanceof ProjectileWeaponItem && Policy.named(config.holdingRangedWeapon)
				.shows(new Policy.Facts(targeting, entityAttackable))) {
			return new Reading(State.HOLDING_RANGED_WEAPON, false, false);
		}
		if (main.getItem() instanceof ProjectileItem && Policy.named(config.holdingThrowable)
				.shows(new Policy.Facts(targeting, entityAttackable || target == Target.BLOCK))) {
			return new Reading(State.HOLDING_THROWABLE, false, false);
		}

		ItemStack held = main;
		if (main.isEmpty() && config.holdingBlockInOffhand && off.getItem() instanceof BlockItem) held = off;
		if (held.getItem() instanceof BlockItem) {
			Policy policy = Policy.named(config.holdingBlock);
			if (policy != Policy.NEVER) {
				if (target == Target.NONE) {
					// The click would bridge. Its own state, because a square where nothing is says
					// "a block goes here" and the point of the reacharound is that it goes somewhere
					// you are not looking: at your feet, or over your head.
					Reacharound.Target reach = Reacharound.find(mc);
					if (reach != null) {
						return new Reading(reach.placeAt().getY() > player.getBlockY()
							? State.REACHAROUND_CEILING : State.REACHAROUND_FLOOR, false, false);
					}
				}
				boolean placeable = target == Target.BLOCK && placeable(player, level, held,
					held == main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, (BlockHitResult) hit);
				if (policy.shows(new Policy.Facts(target == Target.BLOCK, placeable))) {
					return new Reading(State.HOLDING_BLOCK, false, blockInteractable && brackets);
				}
			}
		}

		if (main.has(DataComponents.TOOL) && !main.has(DataComponents.WEAPON)) {
			boolean correct = block != null && main.isCorrectToolForDrops(block);
			// "Would work" for a tool means it is the right tool for the block, not merely that a
			// block is there - otherwise this policy would have two words for one behaviour.
			if (Policy.named(config.holdingTool).shows(new Policy.Facts(target == Target.BLOCK, correct))) {
				return new Reading(State.HOLDING_TOOL, config.correctToolDot && correct,
					blockInteractable && brackets);
			}
		}
		if (config.holdingMeleeWeapon && main.has(DataComponents.WEAPON)
				&& (!config.meleeOnlyOnEntity || entityAttackable)) {
			// An axe is a weapon that is also a tool; on a block it is the tool that matters.
			boolean correct = config.correctToolDot && block != null && main.has(DataComponents.TOOL)
				&& main.isCorrectToolForDrops(block);
			return new Reading(State.HOLDING_MELEE_WEAPON, correct, blockInteractable && brackets);
		}
		if (usable(main)) {
			// Narrower than targeting, never wider: the item has to have something to act on.
			boolean acts = main.has(DataComponents.CONSUMABLE) || blockInteractable || entityInteractable;
			if (Policy.named(config.holdingUsableItem).shows(new Policy.Facts(targeting, acts))) {
				return new Reading(State.HOLDING_USABLE_ITEM, false, acts && brackets);
			}
		}

		// Then the target on its own terms.
		if (target == Target.ENTITY && config.onEntity && (entityAttackable || entityInteractable)) {
			return new Reading(State.ON_ENTITY, false, entityInteractable && brackets);
		}
		if (target == Target.BLOCK) {
			if (blockInteractable && config.onInteractableBlock) {
				return new Reading(State.ON_INTERACTABLE_BLOCK, false, brackets);
			}
			if (config.onBlock) return new Reading(State.ON_BLOCK, false, false);
		}

		return new Reading(config.hideWhenIdle ? State.HIDDEN : State.REGULAR, false, false);
	}

	/** A shield, or anything else that answers a click by guarding. */
	private static boolean blocks(ItemStack stack) {
		return stack.has(DataComponents.BLOCKS_ATTACKS);
	}

	/**
	 * Something a right-click would do with this item on its own, or on what is there.
	 *
	 * <p>Narrower than {@link Reacharound#wouldUse}, which asks whether vanilla would spend the
	 * click: projectiles are missing here because the branches above already claimed them.
	 */
	private static boolean usable(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() instanceof BlockItem) return false;
		if (stack.has(DataComponents.CONSUMABLE)) return true;
		return stack.getUseAnimation() != ItemUseAnimation.NONE;
	}

	/**
	 * A block a right-click does something to.
	 *
	 * <p>By kind. Anything with a menu opens; the rest are the everyday things that answer a click
	 * without one. Pandorical's synced blocks say for themselves whether the server takes the
	 * click, which is the one case a client can know exactly.
	 */
	private static boolean interactable(BlockState state, Level level, BlockPos pos) {
		if (PandoricalBlocks.isInteractive(state)) return true;
		if (state.getMenuProvider(level, pos) != null) return true;
		var block = state.getBlock();
		return block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock
			|| block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof BedBlock
			|| block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof BellBlock
			|| block instanceof NoteBlock || block instanceof JukeboxBlock || block instanceof LecternBlock
			|| block instanceof CakeBlock || block instanceof FlowerPotBlock || block instanceof CampfireBlock
			|| block instanceof ComposterBlock || block instanceof DaylightDetectorBlock
			|| block instanceof RespawnAnchorBlock;
	}

	/** An entity a right-click does something to. */
	private static boolean interactable(Entity entity, ItemStack held) {
		if (entity instanceof AbstractVillager || entity instanceof ItemFrame || entity instanceof ArmorStand) return true;
		if (entity instanceof AbstractBoat || entity instanceof AbstractMinecart) return true;
		return entity instanceof Animal animal && animal.isFood(held);
	}

	/** Whether this block would go where a click on that face puts it. */
	private static boolean placeable(LocalPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit) {
		BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hit);
		if (!context.canPlace()) return false;
		BlockState placed = ((BlockItem) stack.getItem()).getBlock().getStateForPlacement(context);
		if (placed == null) return false;
		BlockPos pos = context.getClickedPos();
		return placed.canSurvive(level, pos)
			&& level.isUnobstructed(placed, pos, CollisionContext.of(player));
	}
}
