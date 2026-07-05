package minecrfat.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TelevisionBlockEntity extends BlockEntity {
    public TelevisionBlockEntity(BlockPos pos, BlockState state) {
        super(MinecraftTv.TELEVISION_BLOCK_ENTITY, pos, state);
    }
}
