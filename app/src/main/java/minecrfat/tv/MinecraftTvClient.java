package minecrfat.tv;

import minecrfat.tv.client.TelevisionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public class MinecraftTvClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            var state = world.getBlockState(hitResult.getBlockPos());
            if (!state.is(MinecraftTv.TELEVISION)) {
                return InteractionResult.PASS;
            }

            Minecraft.getInstance().setScreenAndShow(new TelevisionScreen(
                    hitResult.getBlockPos(),
                    state.getValue(TelevisionBlock.CHANNEL)
            ));
            return InteractionResult.SUCCESS;
        });
    }
}
