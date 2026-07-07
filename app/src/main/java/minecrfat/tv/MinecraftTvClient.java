package minecrfat.tv;

import minecrfat.tv.client.TelevisionBlockEntityRenderer;
import minecrfat.tv.client.TelevisionScreen;
import minecrfat.tv.client.TelevisionStreamManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MinecraftTvClient implements ClientModInitializer {
    private static final double REMOTE_RANGE = 16.0D;

    @Override
    public void onInitializeClient() {
        TelevisionStreamManager.loadConfig();
        BlockEntityRendererRegistry.register(MinecraftTv.TELEVISION_BLOCK_ENTITY, TelevisionBlockEntityRenderer::new);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            ItemStack heldStack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && heldStack.is(MinecraftTv.REMOTE_CONTROL_ITEM)) {
                return InteractionResult.PASS;
            }

            var state = world.getBlockState(hitResult.getBlockPos());
            if (!state.is(MinecraftTv.TELEVISION)) {
                return InteractionResult.PASS;
            }

            openTelevisionScreen(hitResult.getBlockPos(), state.getValue(TelevisionBlock.CHANNEL));
            return InteractionResult.SUCCESS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(MinecraftTv.REMOTE_CONTROL_ITEM)) {
                return InteractionResult.PASS;
            }

            HitResult hit = player.pick(REMOTE_RANGE, 0.0F, false);
            if (player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            var bound = RemoteControlItem.boundTelevision(stack);
            if (bound.isPresent()) {
                InteractionResult result = openBoundTelevision(player, world, bound.get());
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }

            if (!(hit instanceof BlockHitResult blockHit)) {
                return InteractionResult.PASS;
            }

            var state = world.getBlockState(blockHit.getBlockPos());
            if (!state.is(MinecraftTv.TELEVISION)) {
                return InteractionResult.PASS;
            }

            openTelevisionScreen(blockHit.getBlockPos(), state.getValue(TelevisionBlock.CHANNEL));
            return InteractionResult.SUCCESS;
        });
    }

    private static InteractionResult openBoundTelevision(net.minecraft.world.entity.player.Player player,
                                                         net.minecraft.world.level.Level world,
                                                         RemoteControlItem.BoundTelevision bound) {
        if (!world.dimension().identifier().equals(bound.dimension())) {
            player.sendOverlayMessage(Component.translatable("message.minecraft_tv.remote_control.wrong_dimension"));
            return InteractionResult.PASS;
        }

        BlockPos pos = bound.pos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > REMOTE_RANGE * REMOTE_RANGE) {
            player.sendOverlayMessage(Component.translatable("message.minecraft_tv.remote_control.too_far"));
            return InteractionResult.PASS;
        }

        var state = world.getBlockState(pos);
        if (!state.is(MinecraftTv.TELEVISION)) {
            player.sendOverlayMessage(Component.translatable("message.minecraft_tv.remote_control.not_found"));
            return InteractionResult.PASS;
        }

        openTelevisionScreen(pos, state.getValue(TelevisionBlock.CHANNEL));
        return InteractionResult.SUCCESS;
    }

    private static void openTelevisionScreen(net.minecraft.core.BlockPos pos, TelevisionChannel channel) {
        Minecraft.getInstance().setScreenAndShow(new TelevisionScreen(pos, channel));
    }
}
