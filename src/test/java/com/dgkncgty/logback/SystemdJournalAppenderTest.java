package com.dgkncgty.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.Encoder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Unit tests for SystemdJournalAppender using mocks
 *
 * These tests verify the internal behavior of the appender without
 * actually writing to the systemd journal.
 */
public class SystemdJournalAppenderTest {

    private LoggerContext loggerContext;
    private Logger logger;
    private SystemdJournalAppender appender;

    @Mock
    private Encoder<ILoggingEvent> mockEncoder;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        loggerContext = new LoggerContext();
        logger = loggerContext.getLogger(SystemdJournalAppenderTest.class);

        appender = new SystemdJournalAppender();
        appender.setContext(loggerContext);
        appender.start();
    }

    @Test
    public void testAppendWithBasicMessage() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, null);

        // This should not throw any exception
        appender.append(event);
    }

    @Test
    public void testAppendWithLogLocationDisabled() {
        appender.setLogLocation(false);

        Exception exception = new RuntimeException("test");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error message", exception, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogThreadNameDisabled() {
        appender.setLogThreadName(false);

        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogExceptionDisabled() {
        appender.setLogException(false);

        Exception exception = new RuntimeException("test");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error message", exception, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogStackTraceEnabled() {
        appender.setLogStackTrace(true);

        Exception exception = new RuntimeException("test");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error message", exception, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogLoggerNameEnabled() {
        appender.setLogLoggerName(true);

        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogMdcEnabled() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put("key1", "value1");
        mdc.put("key2", "value2");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithMdcKeyPrefix() {
        appender.setLogMdc(true);
        appender.setMdcKeyPrefix("APP_");

        Map<String, String> mdc = new HashMap<>();
        mdc.put("key1", "value1");
        mdc.put("key-with-dashes", "value2");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithSyslogIdentifier() {
        appender.setSyslogIdentifier("my-app");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithMessageId() {
        Map<String, String> mdc = new HashMap<>();
        mdc.put(SystemdJournal.MESSAGE_ID, "test-message-id-123");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogSourceLocationEnabled() {
        appender.setLogSourceLocation(true);

        // Create event with caller data
        LoggingEvent event = createLoggingEvent(Level.INFO, "test message", null, null);
        StackTraceElement[] callerData = new StackTraceElement[] {
            new StackTraceElement("com.example.MyClass", "myMethod", "MyClass.java", 42)
        };
        event.setCallerData(callerData);

        appender.append(event);
    }

    @Test
    public void testAppendWithLogSourceLocationAndException() {
        appender.setLogSourceLocation(true);

        Exception exception = new RuntimeException("test");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error message", exception, null);

        // When there's an exception, source location should not be added
        // since location is taken from exception stack trace
        appender.append(event);
    }

    @Test
    public void testAppendWithAllLogLevels() {
        for (Level level : new Level[]{Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR}) {
            LoggingEvent event = createLoggingEvent(level, "message at " + level, null, null);
            appender.append(event);
        }
    }

    @Test
    public void testAppendWithExceptionChain() {
        appender.setLogStackTrace(true);

        Exception root = new IllegalArgumentException("root cause");
        Exception middle = new IllegalStateException("middle", root);
        Exception top = new RuntimeException("top", middle);

        LoggingEvent event = createLoggingEvent(Level.ERROR, "error", top, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithNullExceptionMessage() {
        appender.setLogException(true);
        appender.setLogStackTrace(true);

        Exception exception = new RuntimeException((String) null);
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error", exception, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithEmptyMessage() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithNullMdc() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithEmptyMdc() {
        Map<String, String> emptyMdc = new HashMap<>();
        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, emptyMdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithMdcContainingNullKey() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put(null, "value");
        mdc.put("valid-key", "value");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        // Should not throw exception
        appender.append(event);
    }

    @Test
    public void testAppendWithMdcContainingNullValue() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put("key-with-null-value", null);

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithSpecialCharactersInMdcKeys() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put("key-with-dashes", "value1");
        mdc.put("key.with.dots", "value2");
        mdc.put("key@with#special$chars", "value3");
        mdc.put("key with spaces", "value4");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithUnicodeInMessage() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "Unicode: 你好 мир العربية 🌍", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithUnicodeInMdc() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put("unicode-key", "Unicode: 你好 мир");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithVeryLongMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longMessage.append("x");
        }

        LoggingEvent event = createLoggingEvent(Level.INFO, longMessage.toString(), null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithMessageContainingNewlines() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "line1\nline2\nline3", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithMessageContainingTabs() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "col1\tcol2\tcol3", null, null);

        appender.append(event);
    }

    @Test
    public void testAppendWithAllFeaturesEnabled() {
        appender.setLogLocation(true);
        appender.setLogSourceLocation(true);
        appender.setLogException(true);
        appender.setLogStackTrace(true);
        appender.setLogThreadName(true);
        appender.setLogLoggerName(true);
        appender.setLogMdc(true);
        appender.setMdcKeyPrefix("TEST_");
        appender.setSyslogIdentifier("test-app");

        Map<String, String> mdc = new HashMap<>();
        mdc.put(SystemdJournal.MESSAGE_ID, "msg-id-123");
        mdc.put("custom-key", "custom-value");

        Exception exception = new RuntimeException("test error");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "comprehensive test", exception, mdc);

        appender.append(event);
    }

    @Test
    public void testAppendWithAllFeaturesDisabled() {
        appender.setLogLocation(false);
        appender.setLogSourceLocation(false);
        appender.setLogException(false);
        appender.setLogStackTrace(false);
        appender.setLogThreadName(false);
        appender.setLogLoggerName(false);
        appender.setLogMdc(false);

        Exception exception = new RuntimeException("test error");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "minimal test", exception, null);

        appender.append(event);
    }

    @Test
    public void testAppendHandlesExceptionsGracefully() {
        // Create an event with a null message to potentially cause issues
        LoggingEvent event = new LoggingEvent(
            "com.example.Test",
            logger,
            Level.INFO,
            null,
            null,
            null
        );

        // Should not throw exception, should handle gracefully
        appender.append(event);
    }

    @Test
    public void testStartAndStop() {
        SystemdJournalAppender newAppender = new SystemdJournalAppender();
        newAppender.setContext(loggerContext);

        newAppender.start();
        // Start should be idempotent
        newAppender.start();

        newAppender.stop();
        // Stop should be idempotent
        newAppender.stop();
    }

    // Helper method to create logging events
    private LoggingEvent createLoggingEvent(Level level, String message, Throwable throwable, Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent(
            "com.example.TestClass",
            logger,
            level,
            message,
            throwable,
            null
        );

        if (mdc != null) {
            event.setMDCPropertyMap(mdc);
        }

        return event;
    }
}
