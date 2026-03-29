package org.astral.spectyle.state;

import java.nio.ShortBuffer;

public record AudioBuffer(ShortBuffer pcmData, int channels, int sampleRate, float durationSeconds) {
}