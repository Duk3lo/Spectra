package org.astral.spectyle.playback;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import static org.lwjgl.openal.ALC10.*;

public class OpenALContext {
    private static long device, context;

    public static void init() {
        device = alcOpenDevice((ByteBuffer) null);
        ALCCapabilities caps = ALC.createCapabilities(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            context = alcCreateContext(device, stack.ints(0));
        }
        alcMakeContextCurrent(context);
        AL.createCapabilities(caps);
    }

    public static void destroy() {
        if (context != 0) alcDestroyContext(context);
        if (device != 0) alcCloseDevice(device);
    }
}