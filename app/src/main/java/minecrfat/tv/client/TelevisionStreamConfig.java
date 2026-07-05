package minecrfat.tv.client;

import minecrfat.tv.TelevisionChannel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public final class TelevisionStreamConfig {
    private static final String FILE_NAME = "minecraft_tv_streams.properties";
    private static final Map<TelevisionChannel, String> URLS = new EnumMap<>(TelevisionChannel.class);

    private TelevisionStreamConfig() {
    }

    public static void load() {
        URLS.clear();
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
                properties.clear();
            }
        } else {
            writeDefaultFile(path);
        }

        for (TelevisionChannel channel : TelevisionChannel.values()) {
            if (channel == TelevisionChannel.OFF) {
                continue;
            }
            URLS.put(channel, properties.getProperty(channel.getSerializedName(), "").trim());
        }
    }

    public static String streamUrl(TelevisionChannel channel) {
        return URLS.getOrDefault(channel, "");
    }

    private static void writeDefaultFile(Path path) {
        Properties defaults = new Properties();
        defaults.setProperty("npo1", "");
        defaults.setProperty("npo2", "");
        defaults.setProperty("npo3", "");
        defaults.setProperty("custom", "");

        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                defaults.store(output, "Directe media/HLS URLs voor Minecraft TV. Laat leeg voor fallback textures.");
            }
        } catch (IOException ignored) {
        }
    }
}
