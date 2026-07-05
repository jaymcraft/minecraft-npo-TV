package minecrfat.tv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import minecrfat.tv.MinecraftTv;
import minecrfat.tv.TelevisionBlock;
import minecrfat.tv.TelevisionBlockEntity;
import minecrfat.tv.TelevisionChannel;
import minecrfat.tv.TelevisionWall;
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
    private static final Identifier NPO1_FALLBACK = MinecraftTv.id("textures/block/tv_screen_npo1.png");
    private static final Identifier NPO2_FALLBACK = MinecraftTv.id("textures/block/tv_screen_npo2.png");
    private static final Identifier NPO3_FALLBACK = MinecraftTv.id("textures/block/tv_screen_npo3.png");
    private static final Identifier CUSTOM_FALLBACK = MinecraftTv.id("textures/block/tv_screen_custom.png");

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
        TelevisionWall wall = TelevisionWall.find(blockEntity.getLevel(), blockEntity.getBlockPos(), blockState);

        renderState.facing = blockState.getValue(TelevisionBlock.FACING);
        renderState.channel = blockState.getValue(TelevisionBlock.CHANNEL);
        renderState.wallOrigin = wall.origin();
        renderState.wallWidth = wall.width();
        renderState.wallHeight = wall.height();
        renderState.wallColumn = wall.column(blockEntity.getBlockPos());
        renderState.wallRow = wall.row(blockEntity.getBlockPos());
        renderState.liveTexture = TelevisionStreamManager.liveTexture(blockEntity.getBlockPos(), renderState.channel, wall);
    }

    @Override
    public void submit(TelevisionBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.channel == TelevisionChannel.OFF) {
            return;
        }

        Identifier texture = renderState.liveTexture != null ? renderState.liveTexture : fallbackTexture(renderState.channel);
        if (texture == null) {
            return;
        }

        int light = renderState.lightCoords;
        Direction facing = renderState.facing;
        float uMin = (float) renderState.wallColumn / (float) renderState.wallWidth;
        float uMax = (float) (renderState.wallColumn + 1) / (float) renderState.wallWidth;
        float vMin = 1.0F - (float) (renderState.wallRow + 1) / (float) renderState.wallHeight;
        float vMax = 1.0F - (float) renderState.wallRow / (float) renderState.wallHeight;
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> drawScreenQuad(pose, consumer, facing, light, uMin, uMax, vMin, vMax)
        );
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static Identifier fallbackTexture(TelevisionChannel channel) {
        return switch (channel) {
            case NPO1 -> NPO1_FALLBACK;
            case NPO2 -> NPO2_FALLBACK;
            case NPO3 -> NPO3_FALLBACK;
            case CUSTOM -> CUSTOM_FALLBACK;
            case OFF -> null;
        };
    }

    private static void drawScreenQuad(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int light,
                                       float uMin, float uMax, float vMin, float vMax) {
        switch (facing) {
            case NORTH -> {
                float z = -OUT;
                vertex(pose, consumer, MIN, MIN, z, 1.0F - uMin, vMax, light, 0.0F, 0.0F, -1.0F);
                vertex(pose, consumer, MAX, MIN, z, 1.0F - uMax, vMax, light, 0.0F, 0.0F, -1.0F);
                vertex(pose, consumer, MAX, MAX, z, 1.0F - uMax, vMin, light, 0.0F, 0.0F, -1.0F);
                vertex(pose, consumer, MIN, MAX, z, 1.0F - uMin, vMin, light, 0.0F, 0.0F, -1.0F);
            }
            case SOUTH -> {
                float z = 1.0F + OUT;
                vertex(pose, consumer, MAX, MIN, z, uMax, vMax, light, 0.0F, 0.0F, 1.0F);
                vertex(pose, consumer, MIN, MIN, z, uMin, vMax, light, 0.0F, 0.0F, 1.0F);
                vertex(pose, consumer, MIN, MAX, z, uMin, vMin, light, 0.0F, 0.0F, 1.0F);
                vertex(pose, consumer, MAX, MAX, z, uMax, vMin, light, 0.0F, 0.0F, 1.0F);
            }
            case WEST -> {
                float x = -OUT;
                vertex(pose, consumer, x, MIN, MAX, uMax, vMax, light, -1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MIN, MIN, uMin, vMax, light, -1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MIN, uMin, vMin, light, -1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MAX, uMax, vMin, light, -1.0F, 0.0F, 0.0F);
            }
            case EAST -> {
                float x = 1.0F + OUT;
                vertex(pose, consumer, x, MIN, MIN, 1.0F - uMin, vMax, light, 1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MIN, MAX, 1.0F - uMax, vMax, light, 1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MAX, 1.0F - uMax, vMin, light, 1.0F, 0.0F, 0.0F);
                vertex(pose, consumer, x, MAX, MIN, 1.0F - uMin, vMin, light, 1.0F, 0.0F, 0.0F);
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
