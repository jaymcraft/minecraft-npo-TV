package minecrfat.tv.client;

import minecrfat.tv.MinecraftTv;
import minecrfat.tv.TelevisionChannel;
import minecrfat.tv.network.SetChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class TelevisionScreen extends Screen {
    private final BlockPos pos;
    private TelevisionChannel selectedChannel;

    public TelevisionScreen(BlockPos pos, TelevisionChannel selectedChannel) {
        super(Component.translatable("screen.minecraft_tv.television"));
        this.pos = pos;
        this.selectedChannel = selectedChannel;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int top = height / 2 - 58;
        addChannelButton(centerX - 102, top, TelevisionChannel.NPO1, "button.minecraft_tv.npo1");
        addChannelButton(centerX + 2, top, TelevisionChannel.NPO2, "button.minecraft_tv.npo2");
        addChannelButton(centerX - 102, top + 24, TelevisionChannel.NPO3, "button.minecraft_tv.npo3");
        addChannelButton(centerX + 2, top + 24, TelevisionChannel.CUSTOM, "button.minecraft_tv.custom");
        addChannelButton(centerX - 102, top + 48, TelevisionChannel.OFF, "button.minecraft_tv.off");

        addRenderableWidget(new TelevisionVolumeSlider(
                centerX - 102,
                top + 74,
                204,
                20,
                pos,
                TelevisionStreamManager.volume(pos)
        ));

        addRenderableWidget(Button.builder(Component.translatable("button.minecraft_tv.open_stream"), button -> openStream())
                .bounds(centerX - 102, top + 100, 204, 20)
                .build());
    }

    private void addChannelButton(int x, int y, TelevisionChannel channel, String translationKey) {
        addRenderableWidget(Button.builder(Component.translatable(translationKey), button -> selectChannel(channel))
                .bounds(x, y, 100, 20)
                .build());
    }

    private void selectChannel(TelevisionChannel channel) {
        selectedChannel = channel;
        ClientPlayNetworking.send(new SetChannelPayload(pos, channel));
    }

    private void openStream() {
        String url = selectedChannel == TelevisionChannel.CUSTOM
                ? TelevisionStreamConfig.streamUrl(selectedChannel)
                : selectedChannel.url();
        if (!url.isBlank()) {
            Util.getPlatform().openUri(url);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.centeredText(font, title, width / 2, height / 2 - 100, 0xFFFFFF);
        graphics.centeredText(
                font,
                Component.translatable("screen.minecraft_tv.current_channel",
                        Component.translatable("channel." + MinecraftTv.MOD_ID + "." + selectedChannel.getSerializedName())),
                width / 2,
                height / 2 - 84,
                0xA0D8FF
        );
        TelevisionPlaybackStatus status = TelevisionStreamManager.status(pos, selectedChannel);
        graphics.centeredText(
                font,
                Component.translatable("screen.minecraft_tv.playback_status", Component.translatable(status.translationKey())),
                width / 2,
                height / 2 - 72,
                0xD6E8FF
        );
        graphics.centeredText(
                font,
                Component.translatable("screen.minecraft_tv.speaker_status", Component.translatable(TelevisionStreamManager.speakerConnected(pos)
                        ? "status.minecraft_tv.speaker_connected"
                        : "status.minecraft_tv.speaker_not_connected")),
                width / 2,
                height / 2 - 60,
                0xD6E8FF
        );
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
