package minecrfat.tv.client;

import minecrfat.tv.TelevisionChannel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public class TelevisionBlockEntityRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public TelevisionChannel channel = TelevisionChannel.OFF;
    public Identifier liveTexture;
    public BlockPos wallOrigin = BlockPos.ZERO;
    public int wallWidth = 1;
    public int wallHeight = 1;
    public int wallColumn = 0;
    public int wallRow = 0;
}
