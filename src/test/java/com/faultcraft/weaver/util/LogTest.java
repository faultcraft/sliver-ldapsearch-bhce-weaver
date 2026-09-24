package com.faultcraft.weaver.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTest {

    private ByteArrayOutputStream capture;

    @BeforeEach
    void setUp() {
        capture = new ByteArrayOutputStream();
        Log.setOutput(new PrintStream(capture));
        Log.setDebug(false);
    }

    @AfterEach
    void tearDown() {
        Log.resetOutput();
        Log.setDebug(false);
    }

    @Test
    void info_simpleMessage_printsWithPrefix() {
        Log.info("test message");
        assertEquals("[weaver] test message\n", capture.toString());
    }

    @Test
    void info_formattedMessage_interpolatesArgs() {
        Log.info("count: %d, name: %s", 42, "alpha");
        assertEquals("[weaver] count: 42, name: alpha\n", capture.toString());
    }

    @Test
    void warn_simpleMessage_includesWarningLabel() {
        Log.warn("something wrong");
        assertEquals("[weaver] WARNING: something wrong\n", capture.toString());
    }

    @Test
    void error_simpleMessage_includesErrorLabel() {
        Log.error("fatal problem");
        assertEquals("[weaver] ERROR: fatal problem\n", capture.toString());
    }

    @Test
    void debug_disabled_producesNoOutput() {
        Log.setDebug(false);
        Log.debug("hidden message");
        assertEquals("", capture.toString());
    }

    @Test
    void debug_enabled_printsWithDebugLabel() {
        Log.setDebug(true);
        Log.debug("visible message");
        assertEquals("[weaver] DEBUG: visible message\n", capture.toString());
    }

    @Test
    void setDebug_togglesState() {
        assertFalse(Log.isDebug());
        Log.setDebug(true);
        assertTrue(Log.isDebug());
        Log.setDebug(false);
        assertFalse(Log.isDebug());
    }
}
