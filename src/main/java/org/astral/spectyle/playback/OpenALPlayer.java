package org.astral.spectyle.playback;

import org.astral.spectyle.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SEC_OFFSET;

public class OpenALPlayer {
    private int bufferId = 0;
    private int sourceId = 0;

    public void load(@NotNull AudioBuffer audioBuffer, float volume) {
        cleanup();
        bufferId = alGenBuffers();
        int format = (audioBuffer.channels() == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        alBufferData(bufferId, format, audioBuffer.pcmData(), audioBuffer.sampleRate());

        sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcef(sourceId, AL_GAIN, volume);
    }

    public void play(boolean loop) {
        if (sourceId != 0) {
            alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
            alSourcePlay(sourceId);
        }
    }

    public void pause() {
        if (sourceId != 0) alSourcePause(sourceId);
    }

    public void stop() {
        if (sourceId != 0) alSourceStop(sourceId);
    }

    public void setVolume(float volume) {
        if (sourceId != 0) alSourcef(sourceId, AL_GAIN, volume);
    }

    public float getOffsetSeconds() {
        return (sourceId != 0) ? alGetSourcef(sourceId, AL_SEC_OFFSET) : 0f;
    }

    public int getState() {
        return (sourceId != 0) ? alGetSourcei(sourceId, AL_SOURCE_STATE) : AL_STOPPED;
    }

    public void cleanup() {
        if (sourceId != 0) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
            alDeleteBuffers(bufferId);
            sourceId = 0;
            bufferId = 0;
        }
    }
}