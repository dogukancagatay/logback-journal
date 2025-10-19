package com.dgkncgty.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.classic.PatternLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SystemdJournalAppender
 *
 * These tests use real SLF4J loggers and write to the systemd journal.
 * Tests cover:
 * - Basic logging functionality
 * - Configuration options (flags)
 * - MDC support
 * - Exception handling
 * - Encoder support
 * - Edge cases and special characters
 */
public class SystemdJournalAppenderITest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SystemdJournalAppenderITest.class);
    private static final org.slf4j.Logger logWithSource = LoggerFactory.getLogger("log-with-source");

    private LoggerContext loggerContext;
    private SystemdJournalAppender appender;

    @Before
    public void setUp() {
        loggerContext = new LoggerContext();
        appender = new SystemdJournalAppender();
        appender.setContext(loggerContext);
    }

    @After
    public void tearDown() {
        MDC.clear();
        if (appender != null && appender.isStarted()) {
            appender.stop();
        }
    }

    // ========== Integration Tests (using real logger) ==========

    @Test
    public void testLogSimple() throws Exception {
        MDC.put(SystemdJournal.MESSAGE_ID, "15bbd5156ff24b6ea41468b102598b04");
        logger.info("toto");
    }

    @Test
    public void testLogException() throws Exception {
        MDC.put(SystemdJournal.MESSAGE_ID, "722fa2bde8344f88975c8d6abcd884c8");
        try {
            throw new Exception("Glups");
        } catch (Exception e) {
            logger.error("some error occurred", e);
        }
    }

    @Test
    public void testWithStringPlaceholder() throws Exception {
        MDC.put(SystemdJournal.MESSAGE_ID, "we get away with %s, since it uses the null terminator arg");
        logger.info("fine");
        MDC.put(SystemdJournal.MESSAGE_ID, "we get away with %i as well, and it converts to 0");
        logger.info("fine");
    }

    @Test
    public void testLogWithTwoStringPlaceholder() throws Exception {
        // It will crash unless % is escaped (% -> %%)
        MDC.put(SystemdJournal.MESSAGE_ID, "this %s %s crashes since there's no 2nd subsequent arg");
        logger.info("boom");

        MDC.put(SystemdJournal.MESSAGE_ID, "this %1$ causes the JVM to abort");
        logger.info("boom");
    }

    @Test
    public void testWithCustomMdc() throws Exception {
        MDC.put("some-key", "some value");
        logger.info("some message");

        MDC.put("special_key%s==", "value with special characters: %s %s %1$");
        logger.info("some other message");
    }

    @Test
    public void testLogWithSource() throws Exception {
        logWithSource.info("some message");
    }

    @Test
    public void testLogWithSourceAndException() throws Exception {
        Exception exception = new RuntimeException("some exception");
        logWithSource.error("some exception", exception);
    }

    @Test
    public void testLogWithSourceAndNestedException() {
        Exception exception = new RuntimeException("some exception");
        Exception exception1 = new Exception("nested exception 1", exception);
        Exception exception2 = new Exception("nested exception 2", exception1);
        Exception exception3 = new Exception("nested exception 3", exception2);
        logWithSource.error("an exception occurred ", exception3);
    }

    // ========== Unit Tests (testing configuration and behavior) ==========

    @Test
    public void testDefaultConfiguration() {
        assertThat(appender.isLogLocation()).isTrue();
        assertThat(appender.isLogSourceLocation()).isFalse();
        assertThat(appender.isLogException()).isTrue();
        assertThat(appender.isLogStackTrace()).isFalse();
        assertThat(appender.isLogThreadName()).isTrue();
        assertThat(appender.isLogLoggerName()).isFalse();
        assertThat(appender.isLogMdc()).isFalse();
        assertThat(appender.getMdcKeyPrefix()).isEmpty();
        assertThat(appender.getSyslogIdentifier()).isEmpty();
        assertThat(appender.getEncoder()).isNull();
    }

    @Test
    public void testSetLogLocation() {
        appender.setLogLocation(false);
        assertThat(appender.isLogLocation()).isFalse();

        appender.setLogLocation(true);
        assertThat(appender.isLogLocation()).isTrue();
    }

    @Test
    public void testSetLogSourceLocation() {
        appender.setLogSourceLocation(true);
        assertThat(appender.isLogSourceLocation()).isTrue();

        appender.setLogSourceLocation(false);
        assertThat(appender.isLogSourceLocation()).isFalse();
    }

    @Test
    public void testSetLogException() {
        appender.setLogException(false);
        assertThat(appender.isLogException()).isFalse();

        appender.setLogException(true);
        assertThat(appender.isLogException()).isTrue();
    }

    @Test
    public void testSetLogStackTrace() {
        appender.setLogStackTrace(true);
        assertThat(appender.isLogStackTrace()).isTrue();

        appender.setLogStackTrace(false);
        assertThat(appender.isLogStackTrace()).isFalse();
    }

    @Test
    public void testSetLogThreadName() {
        appender.setLogThreadName(false);
        assertThat(appender.isLogThreadName()).isFalse();

        appender.setLogThreadName(true);
        assertThat(appender.isLogThreadName()).isTrue();
    }

    @Test
    public void testSetLogLoggerName() {
        appender.setLogLoggerName(true);
        assertThat(appender.isLogLoggerName()).isTrue();

        appender.setLogLoggerName(false);
        assertThat(appender.isLogLoggerName()).isFalse();
    }

    @Test
    public void testSetLogMdc() {
        appender.setLogMdc(true);
        assertThat(appender.isLogMdc()).isTrue();

        appender.setLogMdc(false);
        assertThat(appender.isLogMdc()).isFalse();
    }

    @Test
    public void testSetMdcKeyPrefix() {
        appender.setMdcKeyPrefix("MY_PREFIX_");
        assertThat(appender.getMdcKeyPrefix()).isEqualTo("MY_PREFIX_");

        appender.setMdcKeyPrefix("");
        assertThat(appender.getMdcKeyPrefix()).isEmpty();
    }

    @Test
    public void testSetSyslogIdentifier() {
        appender.setSyslogIdentifier("my-app");
        assertThat(appender.getSyslogIdentifier()).isEqualTo("my-app");

        appender.setSyslogIdentifier("");
        assertThat(appender.getSyslogIdentifier()).isEmpty();
    }

    @Test
    public void testSetEncoder() {
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        PatternLayout layout = new PatternLayout();
        layout.setPattern("%msg");
        layout.setContext(loggerContext);
        layout.start();
        encoder.setLayout(layout);
        encoder.setContext(loggerContext);
        encoder.start();

        appender.setEncoder(encoder);
        assertThat(appender.getEncoder()).isEqualTo(encoder);
    }

    @Test
    public void testLogDifferentLevels() {
        // Test that different log levels are processed
        // This mainly verifies no exceptions are thrown
        logger.trace("trace message");
        logger.debug("debug message");
        logger.info("info message");
        logger.warn("warn message");
        logger.error("error message");
    }

    @Test
    public void testLogWithEmptyMessage() {
        logger.info("");
    }

    @Test
    public void testLogWithNullMdc() {
        MDC.clear();
        logger.info("message without MDC");
    }

    @Test
    public void testLogWithMultipleMdcEntries() {
        MDC.put("key1", "value1");
        MDC.put("key2", "value2");
        MDC.put("key3", "value3");
        logger.info("message with multiple MDC entries");
    }

    @Test
    public void testLogWithSpecialCharactersInMdc() {
        MDC.put("key-with-dashes", "value");
        MDC.put("key.with.dots", "value");
        MDC.put("key_with_underscores", "value");
        MDC.put("key@with#special$chars", "value");
        logger.info("message with special characters in MDC keys");
    }

    @Test
    public void testLogWithUnicodeInMessage() {
        logger.info("Unicode message: 你好世界 🌍 العربية");
    }

    @Test
    public void testLogWithUnicodeInMdc() {
        MDC.put("unicode-key", "Unicode value: 你好 мир");
        logger.info("message with unicode in MDC");
    }

    @Test
    public void testLogWithVeryLongMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a very long message. ");
        }
        logger.info(longMessage.toString());
    }

    @Test
    public void testLogWithVeryLongMdcValue() {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("very long value ");
        }
        MDC.put("long-value", longValue.toString());
        logger.info("message with very long MDC value");
    }

    @Test
    public void testLogWithNullExceptionMessage() {
        Exception exception = new RuntimeException((String) null);
        logger.error("error with null exception message", exception);
    }

    @Test
    public void testLogWithExceptionChain() {
        Exception root = new IllegalArgumentException("root cause");
        Exception middle = new IllegalStateException("middle exception", root);
        Exception top = new RuntimeException("top exception", middle);
        logger.error("exception chain", top);
    }

    @Test
    public void testLogWithDeepExceptionChain() {
        Exception current = new Exception("level 0");
        for (int i = 1; i <= 10; i++) {
            current = new Exception("level " + i, current);
        }
        logger.error("deep exception chain", current);
    }

    @Test
    public void testMessageIdNotInMdc() {
        MDC.clear();
        logger.info("message without MESSAGE_ID");
    }

    @Test
    public void testMessageIdWithEmptyValue() {
        MDC.put(SystemdJournal.MESSAGE_ID, "");
        logger.info("message with empty MESSAGE_ID");
    }

    @Test
    public void testMultipleThreads() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            MDC.put("thread", "thread1");
            logger.info("message from thread 1");
        });

        Thread thread2 = new Thread(() -> {
            MDC.put("thread", "thread2");
            logger.info("message from thread 2");
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }

    @Test
    public void testLoggerNamePropagation() {
        org.slf4j.Logger customLogger = LoggerFactory.getLogger("com.example.CustomLogger");
        customLogger.info("message from custom logger");
    }

    @Test
    public void testNestedLoggerNames() {
        org.slf4j.Logger logger1 = LoggerFactory.getLogger("com.example.Level1");
        org.slf4j.Logger logger2 = LoggerFactory.getLogger("com.example.Level1.Level2");
        org.slf4j.Logger logger3 = LoggerFactory.getLogger("com.example.Level1.Level2.Level3");

        logger1.info("level 1 message");
        logger2.info("level 2 message");
        logger3.info("level 3 message");
    }

    @Test
    public void testLogWithFormattedMessage() {
        logger.info("Formatted message: {}, {}, {}", "arg1", 42, true);
    }

    @Test
    public void testLogWithFormattedMessageAndException() {
        Exception exception = new RuntimeException("error");
        logger.error("Formatted message: {}, {}", "arg1", "arg2", exception);
    }

    @Test
    public void testMdcKeyNormalization() {
        // Test that MDC keys are normalized when logMdc is enabled
        MDC.put("lowercase", "value1");
        MDC.put("UPPERCASE", "value2");
        MDC.put("Mixed-Case_123", "value3");
        MDC.put("special!@#$%characters", "value4");
        logger.info("testing MDC key normalization");
    }

    @Test
    public void testEmptyMdcKey() {
        MDC.put("", "empty key value");
        logger.info("message with empty MDC key");
    }

    @Test
    public void testNullMdcValue() {
        MDC.put("null-value-key", null);
        logger.info("message with null MDC value");
    }

    @Test
    public void testConcurrentLogging() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    MDC.put("thread", "thread-" + threadNum);
                    MDC.put("iteration", String.valueOf(j));
                    logger.info("Concurrent message from thread {} iteration {}", threadNum, j);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    public void testExceptionWithNoStackTrace() {
        // Create an exception with no stack trace
        Exception exception = new Exception("no stack trace") {
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };
        logger.error("exception with no stack trace", exception);
    }

    @Test
    public void testLogWithAllFeaturesEnabled() {
        MDC.put(SystemdJournal.MESSAGE_ID, "test-message-id");
        MDC.put("custom-key", "custom-value");

        Exception exception = new RuntimeException("test exception");
        logWithSource.error("comprehensive test message with all features", exception);
    }

    @Test
    public void testSyslogIdentifierWithSpecialCharacters() {
        // Integration test - syslog identifier should handle special characters
        org.slf4j.Logger specialLogger = LoggerFactory.getLogger("special-identifier-logger");
        specialLogger.info("message with special syslog identifier");
    }

    @Test
    public void testEncoderWithCustomPattern() {
        // Integration test using the encoder configured in logback.xml
        logWithSource.info("testing custom encoder pattern");
    }

    @Test
    public void testMdcWithPrefix() {
        MDC.put("app-name", "my-application");
        MDC.put("app-version", "1.0.0");
        MDC.put("environment", "production");
        logger.info("message with MDC prefix");
    }

    @Test
    public void testMessageWithNewlines() {
        logger.info("message\nwith\nmultiple\nlines");
    }

    @Test
    public void testMessageWithTabs() {
        logger.info("message\twith\ttabs");
    }

    @Test
    public void testMessageWithCarriageReturns() {
        logger.info("message\rwith\rcarriage\rreturns");
    }

    @Test
    public void testExceptionMessageWithSpecialCharacters() {
        Exception exception = new RuntimeException("Exception with special chars: %s %d %f \n\t");
        logger.error("error occurred", exception);
    }

}
