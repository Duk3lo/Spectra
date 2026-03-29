package org.astral.spectyle.decode;

import org.astral.spectyle.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class OggDecoder {
    public static @NotNull AudioBuffer loadAudio(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            ByteBuffer pathBuffer = stack.UTF8(path);
            long decoder = STBVorbis.stb_vorbis_open_filename(pathBuffer, error, null);
            if (decoder == 0) throw new RuntimeException("Error: archivo no encontrado: " + path);

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
}