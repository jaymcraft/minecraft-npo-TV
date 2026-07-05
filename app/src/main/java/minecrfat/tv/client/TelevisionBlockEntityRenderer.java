package minecrfat.tv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import minecrfat.tv.TelevisionBlock;
import minecrfat.tv.TelevisionBlockEntity;
import minecrfat.tv.TelevisionChannel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TelevisionBlockEntityRenderer implements BlockEntityRenderer<TelevisionBlockEntity, TelevisionBlockEntityRenderState> {
    private static final float MIN = 0.002F;
    private static final float MAX = 0.998F;
    private static final float OUT = 0.003F;

    public TelevisionBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TelevisionBlockEntityRenderState createRenderState() {
        return new TelevisionBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(TelevisionBlockEntity blockEntity, TelevisionBlockEntityRenderState renderState, float tickDelta, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, tickDelta, cameraPos, crumblingOverlay);
        BlockState blockState = blockEntity.getBlockState();
        renderState.facing = blockState.getValue(TelevisionBlock.FACING);
        renderState.channel = blockState.getValue(TelevisionBlock.CHANNEL);
        renderState.liveTexture = TelevisionStreamManager.liveTexture(blockEntity.getBlockPos(), renderState.channel);
    }

    @Override
    public void submit(TelevisionBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        Identifier texture = renderState.liveTexture;
        if (texture == null || renderState.channel == TelevisionChannel.OFF) {
            return;
        }

        int light = renderState.lightCoords;
        Direction facing = renderState.facing;
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> drawScreenQuad(pose, consumer, facing, light)
        );
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static void drawScreenQuad(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int light) {
        switch (facing) {
            case NORTH -> {
                float z = -OUT;
                vertex(pose, consumer, MIN, MIN, z, 1.0F, 1.0F, light, 0.0F, 0.0F, -1.0F);
                vertex(pose, consumer, MAX, MIN, z, 0.0F, 1.0F, light, 0.0F, 0.0F, -1.0F);
                vertex(pose, consumer, MAX, MAX, z, 0.0F, 0.0F, light, 0.0F, 0.0F, -1.0F);
                vertex(pose, consumer, MIN, MAX, z, 1.0F, 0.0F, light, 0.0F, 0.0F, -1.0F);
            }
            case SOUTH -> {
                float z = 1.0F + OUT;
                vertex(pose, consumer, MAX, MIN, z, 1.0F, 1.0F, light, 0.0F, 0.0F, 1.0F);
                vertex(pose, consumer, MIN, MIN, z, 0.0F, 1.0F, light, 0.0F, 0.0F, 1.0F);
                vertex(pose, consumer, MIN, MAX, z, 0.0F, 0.0F, light, 0.0F, 0.0F, 1.0F);
                vertex(pose, consumer, MAX, MAX, z, 1.0F, 0.0F, light, 0.0F, 0.0F, 1.0F);
            }
            case WEST -> {
                float x = -OUT;
                vertex(pose, consumer, x, MIN, MAX, 1.0F, 1.0F, light, -1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MIN, MIN, 0.0F, 1.0F, light, -1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MIN, 0.0F, 0.0F, light, -1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MAX, 1.0F, 0.0F, light, -1.0F, 0.0F, 0.0F);
            }
            case EAST -> {
                float x = 1.0F + OUT;
                vertex(pose, consumer, x, MIN, MIN, 1.0F, 1.0F, light, 1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MIN, MAX, 0.0F, 1.0F, light, 1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MAX, 0.0F, 0.0F, light, 1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MIN, 1.0F, 0.0F, light, 1.0F, 0.0F, 0.0F);
            }
            default -> {
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
