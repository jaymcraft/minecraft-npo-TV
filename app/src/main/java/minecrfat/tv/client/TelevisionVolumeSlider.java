package minecrfat.tv.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class TelevisionVolumeSlider extends AbstractSliderButton {
    private final BlockPos pos;

    public TelevisionVolumeSlider(int x, int y, int width, int height, BlockPos pos, int volume) {
        super(x, y, width, height, message(volume), Math.clamp(volume, 0, 100) / 100.0D);
        this.pos = pos;
    }

    @Override
    protected void updateMessage() {
        setMessage(message(volume()));
    }

    @Override
    protected void applyValue() {
        TelevisionStreamManager.setVolume(pos, volume());
    }

    private int volume() {
        return (int) Math.round(value * 100.0D);
    }

    private static Component message(int volume) {
        return Component.translatable("slider.minecraft_tv.volume", Math.clamp(volume, 0, 100));
    }
}
