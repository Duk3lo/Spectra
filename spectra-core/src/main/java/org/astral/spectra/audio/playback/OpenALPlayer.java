package org.astral.spectra.audio.playback;

import org.astral.spectra.audio.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SEC_OFFSET;

public class OpenALPlayer {
    private int bufferId = 0;
    private int sourceId = 0;

    private boolean isOpenALNotReady() {
        return !OpenALContext.isReady();
    }

    public void load(@NotNull AudioBuffer audioBuffer, float volume) {
        cleanup();

        if (isOpenALNotReady()) {
            return;
        }

        bufferId = alGenBuffers();
        int format = (audioBuffer.channels() == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        alBufferData(bufferId, format, audioBuffer.pcmData(), audioBuffer.sampleRate());

        sourceId = alGenSources();
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcef(sourceId, AL_GAIN, volume);
    }

    public void play(boolean loop) {
        if (isOpenALNotReady() || sourceId == 0) {
            return;
        }

        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSourcePlay(sourceId);
    }

    public void setOffsetSeconds(float seconds) {
        if (isOpenALNotReady() || sourceId == 0) {
            return;
        }

        alSourcef(sourceId, AL_SEC_OFFSET, seconds);
    }

    public void pause() {
        if (isOpenALNotReady() || sourceId == 0) {
            return;
        }

        alSourcePause(sourceId);
    }

    public void stop() {
        if (isOpenALNotReady() || sourceId == 0) {
            return;
        }

        alSourceStop(sourceId);
    }

    public void setVolume(float volume) {
        if (isOpenALNotReady() || sourceId == 0) {
            return;
        }

        alSourcef(sourceId, AL_GAIN, volume);
    }

    public float getOffsetSeconds() {
        if (isOpenALNotReady() || sourceId == 0) {
            return 0f;
        }

        return alGetSourcef(sourceId, AL_SEC_OFFSET);
    }

    public int getState() {
        if (isOpenALNotReady() || sourceId == 0) {
            return AL_STOPPED;
        }

        return alGetSourcei(sourceId, AL_SOURCE_STATE);
    }

    public void cleanup() {
        if (isOpenALNotReady()) {
            sourceId = 0;
            bufferId = 0;
            return;
        }

        if (sourceId != 0) {
            try {
                alSourceStop(sourceId);
            } catch (Throwable ignored) {
            }

            try {
                alDeleteSources(sourceId);
            } catch (Throwable ignored) {
            }

            try {
                if (bufferId != 0) {
                    alDeleteBuffers(bufferId);
                }
            } catch (Throwable ignored) {
            }

            sourceId = 0;
            bufferId = 0;
        }
    }
}