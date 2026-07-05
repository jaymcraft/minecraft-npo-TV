package minecrfat.tv;

import minecrfat.tv.network.SetChannelPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MinecraftTv implements ModInitializer {
    public static final String MOD_ID = "minecraft_tv";
    public static final Identifier TELEVISION_ID = id("television");
    public static final Identifier TELEVISION_BLOCK_ENTITY_ID = id("television");
    public static final Identifier REMOTE_CONTROL_ID = id("remote_control");
    public static final ResourceKey<Block> TELEVISION_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, TELEVISION_ID);
    public static final ResourceKey<Item> TELEVISION_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, TELEVISION_ID);
    public static final ResourceKey<Item> REMOTE_CONTROL_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, REMOTE_CONTROL_ID);

    public static final TelevisionBlock TELEVISION = new TelevisionBlock(
            BlockBehaviour.Properties.of()
                    .setId(TELEVISION_BLOCK_KEY)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );

    public static final Item TELEVISION_ITEM = new BlockItem(
            TELEVISION,
            new Item.Properties().setId(TELEVISION_ITEM_KEY)
    );

    public static final Item REMOTE_CONTROL_ITEM = new Item(
            new Item.Properties().setId(REMOTE_CONTROL_ITEM_KEY).stacksTo(1)
    );

    public static final BlockEntityType<TelevisionBlockEntity> TELEVISION_BLOCK_ENTITY =
            FabricBlockEntityTypeBuilder.create(TelevisionBlockEntity::new, TELEVISION).build();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, TELEVISION_ID, TELEVISION);
        Registry.register(BuiltInRegistries.ITEM, TELEVISION_ID, TELEVISION_ITEM);
        Registry.register(BuiltInRegistries.ITEM, REMOTE_CONTROL_ID, REMOTE_CONTROL_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TELEVISION_BLOCK_ENTITY_ID, TELEVISION_BLOCK_ENTITY);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> {
                    entries.accept(TELEVISION_ITEM);
                    entries.accept(REMOTE_CONTROL_ITEM);
                });

        PayloadTypeRegistry.serverboundPlay().register(SetChannelPayload.TYPE, SetChannelPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetChannelPayload.TYPE, (payload, context) ->
                context.server().execute(() -> applyChannel(context.player(), payload))
        );
    }

    private static void applyChannel(ServerPlayer player, SetChannelPayload payload) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = payload.pos();
        if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0D) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(TELEVISION)) {
            return;
        }

        TelevisionWall wall = TelevisionWall.find(level, pos, state);
        for (BlockPos wallPos : wall.positions()) {
            if (!level.isLoaded(wallPos)) {
                continue;
            }
            BlockState wallState = level.getBlockState(wallPos);
            if (wallState.is(TELEVISION) && wallState.getValue(TelevisionBlock.FACING) == wall.facing()) {
                level.setBlock(wallPos, wallState.setValue(TelevisionBlock.CHANNEL, payload.channel()), Block.UPDATE_ALL);
            }
        }
    }
}
