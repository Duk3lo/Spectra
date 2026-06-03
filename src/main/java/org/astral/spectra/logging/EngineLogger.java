package org.astral.spectra.logging;

public interface EngineLogger {
    void info(String msg);
    void warn(String msg);
    void error(String msg);
    void error(String msg, Throwable throwable);
}