package minecrfat.tv.client;

import com.mojang.blaze3d.platform.NativeImage;
import minecrfat.tv.MinecraftTv;
import minecrfat.tv.TelevisionChannel;
import minecrfat.tv.TelevisionWall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class TelevisionStreamManager {
    private static final int FRAME_WIDTH = 256;
    private static final int FRAME_HEIGHT = 144;
    private static final int DEFAULT_VOLUME = 80;
    private static final int SPEAKER_BOOST = 20;
    private static final int MAX_REDSTONE_STEPS = 16;
    private static final Map<WallKey, StreamSession> SESSIONS = new HashMap<>();
    private static final Map<WallKey, Integer> VOLUMES = new HashMap<>();

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

        TelevisionWall wall = findClientWall(pos);
        WallKey key = wall == null ? WallKey.single(pos) : WallKey.from(wall);
        StreamSession session = SESSIONS.get(key);
        return session == null ? TelevisionPlaybackStatus.FALLBACK : session.status();
    }

    public static Identifier liveTexture(BlockPos pos, TelevisionChannel channel, TelevisionWall wall) {
        if (channel == TelevisionChannel.OFF) {
            stop(wall);
            return null;
        }

        String url = TelevisionStreamConfig.streamUrl(channel);
        if (url.isBlank()) {
            stop(wall);
            return null;
        }

        WallKey key = WallKey.from(wall);
        Set<BlockPos> positions = new HashSet<>(wall.positions());
        stopOverlappingSessions(key, positions);

        StreamSession session = SESSIONS.get(key);
        if (session == null || session.channel() != channel || !session.url().equals(url)) {
            stop(wall);
            session = new StreamSession(key, positions, channel, url);
            session.setVolume(effectiveVolume(pos));
            SESSIONS.put(key, session);
            session.start();
        }

        session.setVolume(effectiveVolume(pos));
        session.uploadPendingFrame();
        return session.hasFrame() ? session.textureId() : null;
    }

    public static int volume(BlockPos pos) {
        TelevisionWall wall = findClientWall(pos);
        WallKey key = wall == null ? WallKey.single(pos) : WallKey.from(wall);
        return VOLUMES.getOrDefault(key, DEFAULT_VOLUME);
    }

    public static int effectiveVolume(BlockPos pos) {
        int baseVolume = volume(pos);
        return speakerConnected(pos) ? Math.clamp(baseVolume + SPEAKER_BOOST, 0, 100) : baseVolume;
    }

    public static boolean speakerConnected(BlockPos pos) {
        TelevisionWall wall = findClientWall(pos);
        if (wall == null) {
            return false;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        Set<BlockPos> visited = new HashSet<>();
        java.util.Queue<CableNode> queue = new java.util.ArrayDeque<>();
        for (BlockPos tvPos : wall.positions()) {
            for (Direction direction : Direction.values()) {
                BlockPos cablePos = tvPos.relative(direction);
                if (level.getBlockState(cablePos).is(Blocks.REDSTONE_WIRE) && visited.add(cablePos.immutable())) {
                    queue.add(new CableNode(cablePos.immutable(), 1));
                }
            }
        }

        while (!queue.isEmpty()) {
            CableNode node = queue.remove();
            if (touchesSpeaker(level, node.pos())) {
                return true;
            }
            if (node.steps() >= MAX_REDSTONE_STEPS) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = node.pos().relative(direction);
                if (visited.contains(next) || !level.getBlockState(next).is(Blocks.REDSTONE_WIRE)) {
                    continue;
                }
                visited.add(next.immutable());
                queue.add(new CableNode(next.immutable(), node.steps() + 1));
            }
        }

        return false;
    }

    public static void setVolume(BlockPos pos, int volume) {
        TelevisionWall wall = findClientWall(pos);
        WallKey key = wall == null ? WallKey.single(pos) : WallKey.from(wall);
        int clamped = Math.clamp(volume, 0, 100);
        VOLUMES.put(key, clamped);
        StreamSession session = SESSIONS.get(key);
        if (session != null) {
            session.setVolume(effectiveVolume(pos));
        }
    }

    public static void stop(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(MinecraftTv.TELEVISION)) {
            return;
        }
        stop(TelevisionWall.find(level, pos, state));
    }

    public static void stop(TelevisionWall wall) {
        StreamSession removed = SESSIONS.remove(WallKey.from(wall));
        if (removed != null) {
            removed.close();
        }
    }

    public static void stopUnused(Iterable<BlockPos> activePositions) {
        Set<BlockPos> active = new HashSet<>();
        for (BlockPos pos : activePositions) {
            active.add(pos.immutable());
        }

        Iterator<Map.Entry<WallKey, StreamSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WallKey, StreamSession> entry = iterator.next();
            if (!entry.getValue().intersects(active)) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    private static boolean touchesSpeaker(Level level, BlockPos cablePos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(cablePos.relative(direction)).is(MinecraftTv.TV_SPEAKER)) {
                return true;
            }
        }
        return false;
    }

    private static TelevisionWall findClientWall(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(MinecraftTv.TELEVISION)) {
            return null;
        }
        return TelevisionWall.find(level, pos, state);
    }

    private static void stopOverlappingSessions(WallKey key, Set<BlockPos> positions) {
        Iterator<Map.Entry<WallKey, StreamSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WallKey, StreamSession> entry = iterator.next();
            if (!entry.getKey().equals(key) && entry.getValue().intersects(positions)) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    private record CableNode(BlockPos pos, int steps) {
    }

    private record WallKey(BlockPos origin, Direction facing) {
        private static WallKey from(TelevisionWall wall) {
            return new WallKey(wall.origin().immutable(), wall.facing());
        }

        private static WallKey single(BlockPos pos) {
            return new WallKey(pos.immutable(), Direction.NORTH);
        }

        private String id() {
            return origin.getX() + "_" + origin.getY() + "_" + origin.getZ() + "_" + facing.getSerializedName();
        }
    }

    private static final class StreamSession implements AutoCloseable {
        private final WallKey key;
        private final Set<BlockPos> positions;
        private final TelevisionChannel channel;
        private final String url;
        private final Identifier textureId;
        private final NativeImage image;
        private final DynamicTexture texture;
        private final VlcjStreamPlayer player;
        private volatile int[] pendingFrame;
        private boolean hasFrame;

        private StreamSession(WallKey key, Set<BlockPos> positions, TelevisionChannel channel, String url) {
            this.key = key;
            this.positions = Set.copyOf(positions);
            this.channel = channel;
            this.url = url;
            this.textureId = MinecraftTv.id("dynamic/television_wall/" + key.id());
            this.image = new NativeImage(FRAME_WIDTH, FRAME_HEIGHT, false);
            this.texture = new DynamicTexture(() -> "Minecraft TV Wall " + key.id(), image);
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

        private boolean intersects(Set<BlockPos> otherPositions) {
            for (BlockPos pos : positions) {
                if (otherPositions.contains(pos)) {
                    return true;
                }
            }
            return false;
        }

        private void start() {
            player.play(url);
        }

        private void setVolume(int volume) {
            player.setVolume(volume);
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
