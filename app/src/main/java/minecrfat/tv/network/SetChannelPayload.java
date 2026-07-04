package minecrfat.tv.network;

import minecrfat.tv.MinecraftTv;
import minecrfat.tv.TelevisionChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetChannelPayload(BlockPos pos, TelevisionChannel channel) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetChannelPayload> TYPE =
            new CustomPacketPayload.Type<>(MinecraftTv.id("set_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetChannelPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public SetChannelPayload decode(RegistryFriendlyByteBuf buf) {
                    return new SetChannelPayload(buf.readBlockPos(), TelevisionChannel.byName(buf.readUtf(16)));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SetChannelPayload payload) {
                    buf.writeBlockPos(payload.pos());
                    buf.writeUtf(payload.channel().getSerializedName());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
