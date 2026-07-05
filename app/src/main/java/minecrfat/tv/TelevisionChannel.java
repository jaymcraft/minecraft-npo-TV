package minecrfat.tv;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TelevisionChannel implements StringRepresentable {
    OFF("off", ""),
    NPO1("npo1", "https://www.npo.nl/live/npo-1"),
    NPO2("npo2", "https://www.npo.nl/live/npo-2"),
    NPO3("npo3", "https://www.npo.nl/live/npo-3"),
    CUSTOM("custom", "");

    private final String name;
    private final String url;

    TelevisionChannel(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String url() {
        return url;
    }

    public boolean hasStream() {
        return !url.isBlank();
    }

    public static TelevisionChannel byName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (TelevisionChannel channel : values()) {
            if (channel.name.equals(normalized)) {
                return channel;
            }
        }
        return OFF;
    }
}
