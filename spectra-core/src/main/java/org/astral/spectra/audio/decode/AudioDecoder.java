package org.astral.spectra.audio.decode;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.astral.spectra.audio.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioDecoder {

    public static @NotNull AudioBuffer loadAudio(@NotNull Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return decodeDispatcher(is, path.getFileName().toString().toLowerCase());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load audio file: " + path, e);
        }
    }

    public static @NotNull AudioBuffer loadResource(@NotNull String resourcePath) {
        try (InputStream is = AudioDecoder.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Resource not found: " + resourcePath);
            return decodeDispatcher(is, resourcePath.toLowerCase());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load internal resource: " + resourcePath, e);
        }
    }

    private static AudioBuffer decodeDispatcher(InputStream is, @NotNull String name) throws Exception {
        if (name.endsWith(".ogg")) return decodeOgg(is, name);
        if (name.endsWith(".mp3")) return decodeMp3(is, name);
        if (name.endsWith(".wav") || name.endsWith(".aiff")) return decodeStandard(is);

        throw new UnsupportedOperationException("Unsupported audio format: " + name);
    }

    private static @NotNull AudioBuffer decodeOgg(InputStream is, String source) throws IOException {
        ByteBuffer vorbisBuffer = streamToByteBuffer(is);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_memory(vorbisBuffer, error, null);
            if (decoder == 0) throw new RuntimeException("OGG Error (" + error.get(0) + "): " + source);

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

            ShortBuffer pcm = BufferUtils.createShortBuffer(totalSamples * channels);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            STBVorbis.stb_vorbis_close(decoder);

            return new AudioBuffer(pcm, channels, sampleRate, (float) totalSamples / sampleRate);
        }
    }

    private static @NotNull AudioBuffer decodeMp3(InputStream is, String source) throws Exception {
        Decoder decoder = new Decoder();
        Bitstream bitstream = new Bitstream(is);

        java.util.List<short[]> pcmFrames = new java.util.ArrayList<>();
        int totalSamples = 0;
        int sampleRate = -1;
        int channels = -1;

        try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                if (sampleRate == -1) {
                    sampleRate = output.getSampleFrequency();
                    channels = output.getChannelCount();
                }

                short[] pcm = output.getBuffer().clone();
                short[] actualPcm = new short[output.getBufferLength()];
                System.arraycopy(pcm, 0, actualPcm, 0, output.getBufferLength());

                pcmFrames.add(actualPcm);
                totalSamples += actualPcm.length;
                bitstream.closeFrame();
            }
        } catch (Exception e) {
            throw new RuntimeException("JLayer decoding error: " + source, e);
        } finally {
            bitstream.close();
        }

        ShortBuffer finalPcm = BufferUtils.createShortBuffer(totalSamples);
        for (short[] frame : pcmFrames) {
            finalPcm.put(frame);
        }
        finalPcm.flip();

        float duration = (float) totalSamples / (channels * sampleRate);
        return new AudioBuffer(finalPcm, channels, sampleRate, duration);
    }

    private static @NotNull AudioBuffer decodeStandard(InputStream is) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is))) {
            AudioFormat format = ais.getFormat();
            byte[] bytes = ais.readAllBytes();

            ShortBuffer pcm = ByteBuffer.wrap(bytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer();

            float duration = (float) bytes.length / (format.getFrameSize() * format.getSampleRate());
            return new AudioBuffer(pcm, format.getChannels(), (int) format.getSampleRate(), duration);
        }
    }

    private static @NotNull ByteBuffer streamToByteBuffer(InputStream source) throws IOException {
        ByteBuffer buffer = BufferUtils.createByteBuffer(1024 * 1024);
        try (ReadableByteChannel rbc = Channels.newChannel(source)) {
            while (rbc.read(buffer) != -1) {
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