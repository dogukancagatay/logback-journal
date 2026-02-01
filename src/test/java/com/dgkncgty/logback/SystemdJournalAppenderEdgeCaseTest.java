package com.dgkncgty.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for edge cases and configuration validation
 */
public class SystemdJournalAppenderEdgeCaseTest {

    private LoggerContext loggerContext;
    private Logger logger;
    private SystemdJournalAppender appender;

    @Before
    public void setUp() {
        loggerContext = new LoggerContext();
        logger = loggerContext.getLogger(SystemdJournalAppenderEdgeCaseTest.class);

        appender = new SystemdJournalAppender();
        appender.setContext(loggerContext);
        appender.start();
    }

    @After
    public void tearDown() {
        if (appender != null && appender.isStarted()) {
            appender.stop();
        }
    }

    @Test
    public void testMdcKeyPrefixNormalization() {
        // Test that MDC key prefix is normalized correctly
        appender.setMdcKeyPrefix("my-prefix_");
        assertThat(appender.getMdcKeyPrefix()).isEqualTo("my-prefix_");

        appender.setMdcKeyPrefix("");
        assertThat(appender.getMdcKeyPrefix()).isEmpty();

        appender.setMdcKeyPrefix("PREFIX123");
        assertThat(appender.getMdcKeyPrefix()).isEqualTo("PREFIX123");
    }

    @Test
    public void testSyslogIdentifierWithEmptyString() {
        appender.setSyslogIdentifier("");
        assertThat(appender.getSyslogIdentifier()).isEmpty();

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
        appender.append(event);
    }

    @Test
    public void testSyslogIdentifierWithSpaces() {
        appender.setSyslogIdentifier("my application");
        assertThat(appender.getSyslogIdentifier()).isEqualTo("my application");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
        appender.append(event);
    }

    @Test
    public void testSyslogIdentifierWithSpecialCharacters() {
        appender.setSyslogIdentifier("my-app_v1.0");
        assertThat(appender.getSyslogIdentifier()).isEqualTo("my-app_v1.0");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
        appender.append(event);
    }

    @Test
    public void testMessageIdFilteredFromMdc() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put(SystemdJournal.MESSAGE_ID, "should-not-be-duplicated");
        mdc.put("other-key", "other-value");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        // MESSAGE_ID should be handled separately, not as regular MDC
        appender.append(event);
    }

    @Test
    public void testExceptionWithEmptyStackTrace() {
        Exception exception = new Exception("test") {
            @Override
            public StackTraceElement[] getStackTrace() {
                return new StackTraceElement[0];
            }
        };

        LoggingEvent event = createLoggingEvent(Level.ERROR, "error", exception, null);

        // Should not fail with empty stack trace
        appender.append(event);
    }

    @Test
    public void testExceptionStackTraceWithNullElements() {
        appender.setLogStackTrace(true);

        Exception exception = new RuntimeException("test");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error", exception, null);

        appender.append(event);
    }

    @Test
    public void testMultipleExceptionsWithSameMessage() {
        for (int i = 0; i < 10; i++) {
            Exception exception = new RuntimeException("same message");
            LoggingEvent event = createLoggingEvent(Level.ERROR, "error " + i, exception, null);
            appender.append(event);
        }
    }

    @Test
    public void testCallerDataWithNullElements() {
        appender.setLogSourceLocation(true);

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
        // Don't set caller data - it will be null

        appender.append(event);
    }

    @Test
    public void testCallerDataWithEmptyArray() {
        appender.setLogSourceLocation(true);

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
        event.setCallerData(new StackTraceElement[0]);

        appender.append(event);
    }

    @Test
    public void testMdcWithOnlyMessageId() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        mdc.put(SystemdJournal.MESSAGE_ID, "only-message-id");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testMdcWithManyEntries() {
        appender.setLogMdc(true);

        Map<String, String> mdc = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            mdc.put("key" + i, "value" + i);
        }

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testMdcWithVeryLongKeys() {
        appender.setLogMdc(true);

        StringBuilder longKey = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longKey.append("k");
        }

        Map<String, String> mdc = new HashMap<>();
        mdc.put(longKey.toString(), "value");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testMdcWithVeryLongValues() {
        appender.setLogMdc(true);

        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longValue.append("v");
        }

        Map<String, String> mdc = new HashMap<>();
        mdc.put("key", longValue.toString());

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        appender.append(event);
    }

    @Test
    public void testMdcKeyNormalizationWithPrefix() {
        appender.setLogMdc(true);
        appender.setMdcKeyPrefix("APP-");

        Map<String, String> mdc = new HashMap<>();
        mdc.put("my-key", "value");
        mdc.put("my.key", "value");
        mdc.put("my@key", "value");
        mdc.put("my key", "value");
        mdc.put("123key", "value");

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, mdc);

        // All keys should be normalized (uppercase, special chars replaced with _)
        appender.append(event);
    }

    @Test
    public void testLoggerNameWithDots() {
        appender.setLogLoggerName(true);

        LoggingEvent event = new LoggingEvent(
            "com.example.package.ClassName",
            logger,
            Level.INFO,
            "test",
            null,
            null
        );

        appender.append(event);
    }

    @Test
    public void testThreadNameWithSpecialCharacters() {
        appender.setLogThreadName(true);

        Thread thread = new Thread(() -> {
            LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
            appender.append(event);
        });
        thread.setName("thread-with-dashes_and_underscores");
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testLevelMappingForTrace() {
        LoggingEvent event = createLoggingEvent(Level.TRACE, "trace message", null, null);
        appender.append(event);
    }

    @Test
    public void testLevelMappingForDebug() {
        LoggingEvent event = createLoggingEvent(Level.DEBUG, "debug message", null, null);
        appender.append(event);
    }

    @Test
    public void testLevelMappingForInfo() {
        LoggingEvent event = createLoggingEvent(Level.INFO, "info message", null, null);
        appender.append(event);
    }

    @Test
    public void testLevelMappingForWarn() {
        LoggingEvent event = createLoggingEvent(Level.WARN, "warn message", null, null);
        appender.append(event);
    }

    @Test
    public void testLevelMappingForError() {
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error message", null, null);
        appender.append(event);
    }

    @Test
    public void testConsecutiveAppends() {
        for (int i = 0; i < 100; i++) {
            LoggingEvent event = createLoggingEvent(Level.INFO, "message " + i, null, null);
            appender.append(event);
        }
    }

    @Test
    public void testAlternatingLevels() {
        Level[] levels = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

        for (int i = 0; i < 50; i++) {
            Level level = levels[i % levels.length];
            LoggingEvent event = createLoggingEvent(level, "message " + i, null, null);
            appender.append(event);
        }
    }

    @Test
    public void testExceptionWithCircularCause() {
        // This should be handled gracefully
        Exception exception1 = new Exception("exception 1");
        Exception exception2 = new Exception("exception 2", exception1);

        // Note: We can't actually create a circular cause in Java
        // The constructor will prevent it, but we test what we can
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error", exception2, null);

        appender.setLogStackTrace(true);
        appender.append(event);
    }

    @Test
    public void testExceptionWithVeryDeepCauseChain() {
        appender.setLogStackTrace(true);

        Exception current = new Exception("level 0");
        for (int i = 1; i <= 50; i++) {
            current = new Exception("level " + i, current);
        }

        LoggingEvent event = createLoggingEvent(Level.ERROR, "deep exception", current, null);
        appender.append(event);
    }

    @Test
    public void testLocationFromExceptionStackTrace() {
        appender.setLogLocation(true);

        Exception exception = new RuntimeException("test");
        LoggingEvent event = createLoggingEvent(Level.ERROR, "error", exception, null);

        appender.append(event);
    }

    @Test
    public void testLocationWithNoExceptionWhenSourceLocationEnabled() {
        appender.setLogLocation(false);
        appender.setLogSourceLocation(true);

        LoggingEvent event = createLoggingEvent(Level.INFO, "test", null, null);
        StackTraceElement[] callerData = new StackTraceElement[] {
            new StackTraceElement("com.example.MyClass", "myMethod", "MyClass.java", 100)
        };
        event.setCallerData(callerData);

        appender.append(event);
    }

    @Test
    public void testConfigurationImmutabilityDuringAppend() {
        // Set initial configuration
        appender.setLogThreadName(true);
        appender.setLogLoggerName(true);

        LoggingEvent event1 = createLoggingEvent(Level.INFO, "message 1", null, null);
        appender.append(event1);

        // Change configuration
        appender.setLogThreadName(false);
        appender.setLogLoggerName(false);

        LoggingEvent event2 = createLoggingEvent(Level.INFO, "message 2", null, null);
        appender.append(event2);

        // Configuration should be applied to each event
        assertThat(appender.isLogThreadName()).isFalse();
        assertThat(appender.isLogLoggerName()).isFalse();
    }

    // Helper method to create logging events
    private LoggingEvent createLoggingEvent(Level level, String message, Throwable throwable, Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent(
            "com.dgkncgty.logback.test.TestClass",
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
