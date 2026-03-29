package org.astral.spectyle.audio.state;

import java.nio.ShortBuffer;

public record AudioBuffer(ShortBuffer pcmData, int channels, int sampleRate, float durationSeconds) {
}