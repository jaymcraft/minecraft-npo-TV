package minecrfat.tv.client;

import minecrfat.tv.TelevisionChannel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public class TelevisionBlockEntityRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public TelevisionChannel channel = TelevisionChannel.OFF;
    public Identifier liveTexture;
}
