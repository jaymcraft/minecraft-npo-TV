package minecrfat.tv.client;

import com.mojang.blaze3d.platform.NativeImage;
import minecrfat.tv.MinecraftTv;
import minecrfat.tv.TelevisionChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TelevisionStreamManager {
    private static final int FRAME_WIDTH = 256;
    private static final int FRAME_HEIGHT = 144;
    private static final Map<BlockPos, StreamSession> SESSIONS = new HashMap<>();

    private TelevisionStreamManager() {
    }

    public static void loadConfig() {
        TelevisionStreamConfig.load();
    }

    public static TelevisionPlaybackStatus status(BlockPos pos, TelevisionChannel channel) {
        if (channel == TelevisionChannel.OFF) {
            return TelevisionPlaybackStatus.OFF;
        }
        String url = TelevisionStreamConfig.streamUrl(channel);
        if (url.isBlank()) {
            return TelevisionPlaybackStatus.NO_STREAM_CONFIGURED;
        }
        StreamSession session = SESSIONS.get(pos);
        return session == null ? TelevisionPlaybackStatus.FALLBACK : session.status();
    }

    public static Identifier liveTexture(BlockPos pos, TelevisionChannel channel) {
        if (channel == TelevisionChannel.OFF) {
            stop(pos);
            return null;
        }

        String url = TelevisionStreamConfig.streamUrl(channel);
        if (url.isBlank()) {
            stop(pos);
            return null;
        }

        StreamSession session = SESSIONS.get(pos);
        if (session == null || session.channel() != channel || !session.url().equals(url)) {
            stop(pos);
            session = new StreamSession(pos, channel, url);
            SESSIONS.put(pos.immutable(), session);
            session.start();
        }

        session.uploadPendingFrame();
        return session.hasFrame() ? session.textureId() : null;
    }

    public static void stop(BlockPos pos) {
        StreamSession removed = SESSIONS.remove(pos);
        if (removed != null) {
            removed.close();
        }
    }

    public static void stopUnused(Iterable<BlockPos> activePositions) {
        Iterator<Map.Entry<BlockPos, StreamSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, StreamSession> entry = iterator.next();
            boolean active = false;
            for (BlockPos pos : activePositions) {
                if (entry.getKey().equals(pos)) {
                    active = true;
                    break;
                }
            }
            if (!active) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    private static final class StreamSession implements AutoCloseable {
        private final BlockPos pos;
        private final TelevisionChannel channel;
        private final String url;
        private final Identifier textureId;
        private final NativeImage image;
        private final DynamicTexture texture;
        private final VlcjStreamPlayer player;
        private volatile int[] pendingFrame;
        private boolean hasFrame;

        private StreamSession(BlockPos pos, TelevisionChannel channel, String url) {
            this.pos = pos.immutable();
            this.channel = channel;
            this.url = url;
            this.textureId = MinecraftTv.id("dynamic/television/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
            this.image = new NativeImage(FRAME_WIDTH, FRAME_HEIGHT, false);
            this.texture = new DynamicTexture(() -> "Minecraft TV " + pos.toShortString(), image);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            this.player = new VlcjStreamPlayer(FRAME_WIDTH, FRAME_HEIGHT, this::receiveFrame);
        }

        private TelevisionChannel channel() {
            return channel;
        }

        private String url() {
            return url;
        }

        private Identifier textureId() {
            return textureId;
        }

        private boolean hasFrame() {
            return hasFrame;
        }

        private TelevisionPlaybackStatus status() {
            if (player.status() == TelevisionPlaybackStatus.LIVE && !hasFrame) {
                return TelevisionPlaybackStatus.STARTING;
            }
            return player.status();
        }

        private void start() {
            player.play(url);
        }

        private void receiveFrame(int[] frame) {
            pendingFrame = frame;
        }

        private void uploadPendingFrame() {
            int[] frame = pendingFrame;
            if (frame == null) {
                return;
            }
            pendingFrame = null;
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                int row = y * FRAME_WIDTH;
                for (int x = 0; x < FRAME_WIDTH; x++) {
                    image.setPixelABGR(x, y, frame[row + x]);
                }
            }
            texture.upload();
            hasFrame = true;
        }

        @Override
        public void close() {
            player.close();
            Minecraft.getInstance().getTextureManager().release(textureId);
            texture.close();
        }
    }
}
