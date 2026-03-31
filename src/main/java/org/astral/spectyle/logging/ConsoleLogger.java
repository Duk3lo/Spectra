package org.astral.spectyle.logging;

public final class ConsoleLogger implements EngineLogger {
    @Override
    public void info(String msg) {
        System.out.println("[INFO] " + msg);
    }

    @Override
    public void warn(String msg) {
        System.out.println("[WARN] " + msg);
    }

    @Override
    public void error(String msg) {
        System.err.println("[ERROR] " + msg);
    }

    @Override
    public void error(String msg, Throwable throwable) {
        System.err.println("[ERROR] " + msg);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}