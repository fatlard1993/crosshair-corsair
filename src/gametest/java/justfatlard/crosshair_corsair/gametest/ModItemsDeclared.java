package justfatlard.crosshair_corsair.gametest;

import java.nio.file.Files;

import justfatlard.crosshair_corsair.CorsairConfig;
import justfatlard.crosshair_corsair.crosshair.CrosshairContext;
import justfatlard.crosshair_corsair.crosshair.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A crosshair declared in another mod's assets reaches the hud: read on reload, written to the
 * config file for the player, and drawn when the item is in hand. The declaration is this test
 * mod's own, claiming a stick.
 */
public final class ModItemsDeclared implements FabricClientGameTest {
	private static final String ID = "crosshair-corsair-gametest:test_stick";

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			ModItems.ModItem item = ModItems.of(new ItemStack(Items.STICK));
			if (item == null) throw new AssertionError("the stick's declaration was not loaded");
			if (!item.id().equals(ID)) throw new AssertionError("the stick is claimed by " + item.id());
			if (item.like() != ModItems.Like.THROWABLE) throw new AssertionError("like read as " + item.like());

			var setting = CorsairConfig.get().crosshair.modItems.get(ID);
			if (setting == null || !"diamond".equals(setting.style) || !"always".equals(setting.policy)) {
				throw new AssertionError("the player's setting did not start from the declaration");
			}
			try {
				String file = Files.readString(FabricLoader.getInstance().getConfigDir().resolve("crosshair-corsair.json"));
				if (!file.contains(ID)) throw new AssertionError("the declaration never reached the config file");
			} catch (java.io.IOException e) {
				throw new AssertionError(e);
			}
		});

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runOnServer(server -> world.getConnection().getServerPlayer()
				.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)));
			context.waitTicks(10);
			context.runOnClient(mc -> {
				var reading = CrosshairContext.read(mc, CorsairConfig.get().crosshair);
				if (reading.state() != CrosshairContext.State.HOLDING_MOD_ITEM || !"diamond".equals(reading.style())) {
					throw new AssertionError("a stick in hand read as " + reading);
				}
			});
		}
		// Closing a world still coming out of a pause deadlocks the harness's own tick lock.
		context.waitTicks(10);
	}
}
