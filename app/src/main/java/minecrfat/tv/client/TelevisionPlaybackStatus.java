package minecrfat.tv.client;

public enum TelevisionPlaybackStatus {
    OFF("status.minecraft_tv.off"),
    FALLBACK("status.minecraft_tv.fallback"),
    NO_STREAM_CONFIGURED("status.minecraft_tv.no_stream_configured"),
    VLC_NOT_FOUND("status.minecraft_tv.vlc_not_found"),
    STARTING("status.minecraft_tv.starting"),
    LIVE("status.minecraft_tv.live"),
    ERROR("status.minecraft_tv.error");

    private final String translationKey;

    TelevisionPlaybackStatus(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
