package minecrfat.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class TelevisionWall {
    public static final int MAX_WIDTH = 16;
    public static final int MAX_HEIGHT = 9;
    private static final int MAX_BLOCKS = MAX_WIDTH * MAX_HEIGHT;

    private final BlockPos origin;
    private final Direction facing;
    private final Direction horizontal;
    private final List<BlockPos> positions;
    private final int minHorizontal;
    private final int minY;
    private final int width;
    private final int height;

    private TelevisionWall(BlockPos origin, Direction facing, Direction horizontal, List<BlockPos> positions,
                           int minHorizontal, int minY, int width, int height) {
        this.origin = origin;
        this.facing = facing;
        this.horizontal = horizontal;
        this.positions = positions;
        this.minHorizontal = minHorizontal;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    public static TelevisionWall find(BlockGetter level, BlockPos start, BlockState startState) {
        Direction facing = startState.getValue(TelevisionBlock.FACING);
        Direction horizontal = horizontalDirection(facing);
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        visited.add(start.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_BLOCKS) {
            BlockPos pos = queue.remove();
            addNeighbor(level, facing, horizontal, visited, queue, pos.relative(horizontal));
            addNeighbor(level, facing, horizontal, visited, queue, pos.relative(horizontal.getOpposite()));
            addNeighbor(level, facing, horizontal, visited, queue, pos.above());
            addNeighbor(level, facing, horizontal, visited, queue, pos.below());
        }

        int minHorizontal = Integer.MAX_VALUE;
        int maxHorizontal = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos pos : visited) {
            int h = horizontalCoordinate(pos, horizontal);
            minHorizontal = Math.min(minHorizontal, h);
            maxHorizontal = Math.max(maxHorizontal, h);
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }

        int width = Math.min(MAX_WIDTH, maxHorizontal - minHorizontal + 1);
        int height = Math.min(MAX_HEIGHT, maxY - minY + 1);
        BlockPos origin = originFrom(start, horizontal, minHorizontal, minY);
        return new TelevisionWall(origin, facing, horizontal, List.copyOf(visited), minHorizontal, minY, width, height);
    }

    private static void addNeighbor(BlockGetter level, Direction facing, Direction horizontal, Set<BlockPos> visited,
                                    Queue<BlockPos> queue, BlockPos neighbor) {
        if (visited.size() >= MAX_BLOCKS || visited.contains(neighbor)) {
            return;
        }

        BlockState state = level.getBlockState(neighbor);
        if (!state.is(MinecraftTv.TELEVISION) || state.getValue(TelevisionBlock.FACING) != facing) {
            return;
        }

        if (wouldExceedBounds(visited, neighbor, horizontal)) {
            return;
        }

        visited.add(neighbor.immutable());
        queue.add(neighbor.immutable());
    }

    private static boolean wouldExceedBounds(Set<BlockPos> visited, BlockPos candidate, Direction horizontal) {
        int minHorizontal = horizontalCoordinate(candidate, horizontal);
        int maxHorizontal = minHorizontal;
        int minY = candidate.getY();
        int maxY = candidate.getY();

        for (BlockPos pos : visited) {
            int h = horizontalCoordinate(pos, horizontal);
            minHorizontal = Math.min(minHorizontal, h);
            maxHorizontal = Math.max(maxHorizontal, h);
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }

        return maxHorizontal - minHorizontal + 1 > MAX_WIDTH || maxY - minY + 1 > MAX_HEIGHT;
    }

    private static Direction horizontalDirection(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
    }

    private static int horizontalCoordinate(BlockPos pos, Direction horizontal) {
        return horizontal.getAxis() == Direction.Axis.X ? pos.getX() : pos.getZ();
    }

    private static BlockPos originFrom(BlockPos start, Direction horizontal, int minHorizontal, int minY) {
        if (horizontal.getAxis() == Direction.Axis.X) {
            return new BlockPos(minHorizontal, minY, start.getZ());
        }
        return new BlockPos(start.getX(), minY, minHorizontal);
    }

    public BlockPos origin() {
        return origin;
    }

    public Direction facing() {
        return facing;
    }

    public List<BlockPos> positions() {
        return new ArrayList<>(positions);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int column(BlockPos pos) {
        return Math.clamp(horizontalCoordinate(pos, horizontal) - minHorizontal, 0, width - 1);
    }

    public int row(BlockPos pos) {
        return Math.clamp(pos.getY() - minY, 0, height - 1);
    }
}
