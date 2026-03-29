package org.astral.spectyle.audio.api;


public record ReactiveSnapshot(
        float kick,
        float snare,
        float hat,
        float energy,
        float bassEnergy,
        float midEnergy,
        float highEnergy,
        float vocalPresence,
        float combo,
        float transientLevel,
        float brightness,
        float subBass
) {}