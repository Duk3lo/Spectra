package org.astral.spectyle.audio.decode;

import org.astral.spectyle.audio.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class OggDecoder {

    public static @NotNull AudioBuffer loadAudio(String path) {
        try {
            return loadAudio(Path.of(path));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer el archivo: " + path, e);
        }
    }

    public static @NotNull AudioBuffer loadAudio(@NotNull Path path) {
        ByteBuffer vorbisBuffer;

        try {
            vorbisBuffer = ioResourceToByteBuffer(path);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + path, e);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_memory(vorbisBuffer, error, null);
            if (decoder == 0) {
                throw new RuntimeException("Error al decodificar OGG (Error code: " + error.get(0) + "): " + path);
            }

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int sampleRate = info.sample_rate();

            int totalSamplesInStream = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            float duration = (float) totalSamplesInStream / sampleRate;

            int totalSamples = totalSamplesInStream * channels;
            ShortBuffer pcmData = BufferUtils.createShortBuffer(totalSamples);

            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcmData);
            STBVorbis.stb_vorbis_close(decoder);

            return new AudioBuffer(pcmData, channels, sampleRate, duration);
        }
    }

    private static @NotNull ByteBuffer ioResourceToByteBuffer(@NotNull Path path) throws IOException {
        ByteBuffer buffer;

        if (!Files.exists(path)) {
            throw new IOException("Archivo no encontrado: " + path);
        }

        try (InputStream source = Files.newInputStream(path);
             ReadableByteChannel rbc = Channels.newChannel(source)) {

            buffer = BufferUtils.createByteBuffer(1024 * 1024);

            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) break;

                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
        }

        buffer.flip();
        return buffer;
    }
}