package com.faultcraft.weaver.util;

import java.io.PrintStream;

/** Structured logging facade for weaver. All output goes through this class. */
public final class Log {

    private static final String PREFIX = "[weaver]";
    private static boolean debug = false;
    private static PrintStream out = System.err;

    private Log() {}

    /** Enable or disable debug-level messages. */
    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    /** Returns true if debug logging is enabled. */
    public static boolean isDebug() {
        return debug;
    }

    /** Override the output stream (for testing). */
    public static void setOutput(PrintStream stream) {
        out = stream;
    }

    /** Reset to default output stream. */
    public static void resetOutput() {
        out = System.err;
    }

    /** Log an informational message. */
    public static void info(String message) {
        out.println(PREFIX + " " + message);
    }

    /** Log an informational message with formatting. */
    public static void info(String format, Object... args) {
        out.println(PREFIX + " " + String.format(format, args));
    }

    /** Log a warning message. */
    public static void warn(String message) {
        out.println(PREFIX + " WARNING: " + message);
    }

    /** Log a warning message with formatting. */
    public static void warn(String format, Object... args) {
        out.println(PREFIX + " WARNING: " + String.format(format, args));
    }

    /** Log an error message. */
    public static void error(String message) {
        out.println(PREFIX + " ERROR: " + message);
    }

    /** Log an error message with formatting. */
    public static void error(String format, Object... args) {
        out.println(PREFIX + " ERROR: " + String.format(format, args));
    }

    /** Log a debug message (only if debug mode is enabled). */
    public static void debug(String message) {
        if (debug) {
            out.println(PREFIX + " DEBUG: " + message);
        }
    }

    /** Log a debug message with formatting (only if debug mode is enabled). */
    public static void debug(String format, Object... args) {
        if (debug) {
            out.println(PREFIX + " DEBUG: " + String.format(format, args));
        }
    }
}
