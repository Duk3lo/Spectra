package org.astral.spectra.audio.playback;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.ALC10.*;

public class OpenALContext {
    private static long device = 0L;
    private static long context = 0L;
    private static boolean ready = false;

    public static void init() {
        destroy();

        try {
            device = alcOpenDevice((ByteBuffer) null);
            if (device == 0L) {
                ready = false;
                return;
            }

            ALCCapabilities caps = ALC.createCapabilities(device);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                context = alcCreateContext(device, stack.ints(0));
            }

            if (context == 0L) {
                alcCloseDevice(device);
                device = 0L;
                ready = false;
                return;
            }

            alcMakeContextCurrent(context);
            AL.createCapabilities(caps);
            ready = true;
        } catch (Throwable t) {
            destroy();
            ready = false;
        }
    }

    public static boolean isReady() {
        return ready;
    }

    public static void destroy() {
        try {
            alcMakeContextCurrent(0L);
        } catch (Throwable ignored) {
        }

        try {
            if (context != 0L) {
                alcDestroyContext(context);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (device != 0L) {
                alcCloseDevice(device);
            }
        } catch (Throwable ignored) {
        }

        context = 0L;
        device = 0L;
        ready = false;
    }
}