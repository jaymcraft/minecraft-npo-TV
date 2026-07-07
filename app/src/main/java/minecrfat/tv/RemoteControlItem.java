package minecrfat.tv;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

public class RemoteControlItem extends Item {
    private static final double REMOTE_RANGE = 16.0D;
    private static final String TAG_ROOT = "minecraft_tv_remote";
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";

    public RemoteControlItem(Properties properties) {
        super(properties);
    }

    public static void bind(ItemStack stack, Identifier dimension, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag remote = new CompoundTag();
            remote.putString(TAG_DIMENSION, dimension.toString());
            remote.putInt(TAG_X, pos.getX());
            remote.putInt(TAG_Y, pos.getY());
            remote.putInt(TAG_Z, pos.getZ());
            tag.put(TAG_ROOT, remote);
        });
    }

    @Override
    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        HitResult hit = pickTelevisionRange(level, player);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResult.PASS;
        }

        if (!level.getBlockState(blockHit.getBlockPos()).is(MinecraftTv.TELEVISION)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            bind(player.getItemInHand(hand), level.dimension().identifier(), blockHit.getBlockPos());
            player.sendOverlayMessage(Component.translatable("message.minecraft_tv.remote_control.bound"));
        }
        return InteractionResult.SUCCESS;
    }

    private static HitResult pickTelevisionRange(Level level, net.minecraft.world.entity.player.Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(REMOTE_RANGE));
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    public static Optional<BoundTelevision> boundTelevision(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        CompoundTag tag = customData.copyTag();
        Optional<CompoundTag> remote = tag.getCompound(TAG_ROOT);
        if (remote.isEmpty()) {
            return Optional.empty();
        }

        String dimension = remote.get().getStringOr(TAG_DIMENSION, "");
        Identifier dimensionId = Identifier.tryParse(dimension);
        if (dimensionId == null) {
            return Optional.empty();
        }

        BlockPos pos = new BlockPos(
                remote.get().getIntOr(TAG_X, 0),
                remote.get().getIntOr(TAG_Y, 0),
                remote.get().getIntOr(TAG_Z, 0)
        );
        return Optional.of(new BoundTelevision(dimensionId, pos));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Optional<BoundTelevision> bound = boundTelevision(stack);
        if (bound.isPresent()) {
            BlockPos pos = bound.get().pos();
            tooltip.accept(Component.translatable(
                    "tooltip.minecraft_tv.remote_control.bound",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            ).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.minecraft_tv.remote_control.unbound")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public record BoundTelevision(Identifier dimension, BlockPos pos) {
    }
}
