package minecrfat.tv.client;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public final class VlcjStreamPlayer implements AutoCloseable {
    private final int width;
    private final int height;
    private final Consumer<int[]> frameConsumer;
    private CallbackMediaPlayerComponent component;
    private volatile TelevisionPlaybackStatus status = TelevisionPlaybackStatus.STARTING;

    public VlcjStreamPlayer(int width, int height, Consumer<int[]> frameConsumer) {
        this.width = width;
        this.height = height;
        this.frameConsumer = frameConsumer;
    }

    public void play(String url) {
        try {
            if (!new NativeDiscovery().discover()) {
                status = TelevisionPlaybackStatus.VLC_NOT_FOUND;
                return;
            }

            BufferFormatCallback bufferFormatCallback = new BufferFormatCallback() {
                @Override
                public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                    return new RV32BufferFormat(width, height);
                }

                @Override
                public void allocatedBuffers(ByteBuffer[] buffers) {
                }
            };

            RenderCallback renderCallback = new RenderCallback() {
                @Override
                public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
                    ByteBuffer buffer = nativeBuffers[0].order(ByteOrder.LITTLE_ENDIAN);
                    IntBuffer pixels = buffer.asIntBuffer();
                    int[] frame = new int[width * height];
                    pixels.get(frame, 0, Math.min(frame.length, pixels.remaining()));
                    for (int i = 0; i < frame.length; i++) {
                        int rv32 = frame[i];
                        int red = rv32 & 0xFF;
                        int green = (rv32 >> 8) & 0xFF;
                        int blue = (rv32 >> 16) & 0xFF;
                        frame[i] = 0xFF000000 | blue << 16 | green << 8 | red;
                    }
                    frameConsumer.accept(frame);
                    status = TelevisionPlaybackStatus.LIVE;
                }
            };

            component = new CallbackMediaPlayerComponent(null, null, null, true, renderCallback, bufferFormatCallback, null);
            status = TelevisionPlaybackStatus.STARTING;
            component.mediaPlayer().media().play(url);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError error) {
            status = TelevisionPlaybackStatus.VLC_NOT_FOUND;
            close();
        } catch (Throwable throwable) {
            status = TelevisionPlaybackStatus.ERROR;
            close();
        }
    }

    public TelevisionPlaybackStatus status() {
        return status;
    }

    @Override
    public void close() {
        if (component != null) {
            try {
                component.mediaPlayer().controls().stop();
                component.release();
            } catch (Throwable ignored) {
            }
            component = null;
        }
    }
}
