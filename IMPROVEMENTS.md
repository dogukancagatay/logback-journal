# Recommended Improvement Areas

## 1. Migrate from JUnit 4 to JUnit 5

**Priority: Medium**
**Files:** `pom.xml`, all test files under `src/test/`

The project uses JUnit 4.13.2, which has been in maintenance-only mode since 2021. JUnit 5 (Jupiter) is the current standard and offers:

- Parameterized tests via `@ParameterizedTest` — useful for the level-mapping tests and MDC key normalization tests that currently repeat similar patterns.
- `@Nested` test classes for better test organization.
- `assertThrows` for exception testing (replacing `@Test(expected=...)`).
- `@DisplayName` for readable test output.
- Lifecycle extensions (`@BeforeEach`/`@AfterEach`) instead of `@Before`/`@After`.
- `assumeTrue` from `org.junit.jupiter.api.Assumptions` instead of `org.junit.Assume`.

The integration test `SystemdJournalAppenderITest.java` uses `Assume.assumeTrue()` from JUnit 4 — this would map directly to JUnit 5's `Assumptions.assumeTrue()`.

## 2. Add Code Coverage Reporting

**Priority: Medium**
**Files:** `pom.xml`, `.github/workflows/pr.yml`

There is no code coverage measurement configured. Adding JaCoCo would provide:

- Coverage reports during `mvn verify`.
- Visibility into untested paths in the appender logic (e.g., the `default` branch in `levelToInt` that throws `IllegalArgumentException`).
- Coverage badges in the README alongside the existing Maven Central badge.
- Integration with GitHub Actions to post coverage summaries on PRs.

## 3. Run Integration Tests in CI

**Priority: High**
**Files:** `.github/workflows/pr.yml`, `.github/workflows/main.yml`

The CI workflows only run `mvn -B package`, which executes unit tests via Surefire but **skips integration tests**. The Failsafe plugin (integration tests) only runs during `mvn verify` or `mvn integration-test`. The integration tests in `SystemdJournalAppenderITest.java` already gracefully skip when `journalctl` is unavailable, so running `mvn -B verify` instead of `mvn -B package` would:

- Execute integration tests on systems where systemd is available (Ubuntu GitHub runners have it).
- Catch regressions in actual journal interaction.
- Validate the Failsafe plugin configuration in CI.

## 4. Validate Configuration in `start()`

**Priority: Medium**
**Files:** `SystemdJournalAppender.java`

The appender's `start()` method (inherited from `AppenderBase`) is not overridden to perform any configuration validation. Consider adding:

- Verification that the native systemd library can be loaded, with a clear error message via `addError()` if it fails.
- Validation that `mdcKeyPrefix` is set when `logMdc` is enabled (or at least a warning).
- Verification that the encoder, if set, is started before the appender begins processing events.

Currently, if the native library is unavailable, the error only surfaces at the first `append()` call, making it harder to diagnose configuration issues at startup.

## 5. Manage Encoder Lifecycle

**Priority: Medium**
**Files:** `SystemdJournalAppender.java`

When a custom `Encoder` is configured, the appender does not call `encoder.start()` during its own `start()` or `encoder.stop()` during its own `stop()`. Logback appenders that use encoders (e.g., `OutputStreamAppender`) typically manage the encoder lifecycle. Without this:

- An encoder that hasn't been started may behave unexpectedly.
- Resources held by the encoder may not be released on shutdown.

## 6. Check Return Values from JNA Calls

**Priority: Medium**
**Files:** `SystemdJournalAppender.java`

The `sd_journal_send()` call returns an `int` (negative errno on failure), but the return value is ignored at `SystemdJournalAppender.java:173`. Checking this value would allow the appender to report failures via `addError()` or `addWarn()`, improving observability when journal writes fail silently (e.g., due to rate limiting or journal corruption).

## 7. Make Instance Fields Private

**Priority: Low**
**Files:** `SystemdJournalAppender.java`

All configuration fields (`logLocation`, `logSourceLocation`, `logException`, etc.) at lines 37–55 use package-private (default) visibility instead of `private`. Since public getters/setters exist for all of them, the fields should be `private` for proper encapsulation. The current visibility allows other classes in the same package to bypass the setter methods.

## 8. Make `MESSAGE_ID` Constant `final`

**Priority: Low**
**Files:** `SystemdJournal.java`

The constant `MESSAGE_ID` at `SystemdJournal.java:35` is declared as `public static String` but is missing the `final` modifier. This means it can be reassigned at runtime, which is almost certainly unintended for a constant. It should be `public static final String`.

## 9. Add a CHANGELOG

**Priority: Low**
**Files:** New `CHANGELOG.md`

The project has published releases to Maven Central (currently 0.5.1) but has no CHANGELOG documenting what changed between versions. A CHANGELOG (following [Keep a Changelog](https://keepachangelog.com/) format) would help users understand:

- What's new in each release.
- Breaking changes when upgrading.
- Bug fixes and security patches (e.g., the CVE-2026-24400 fix via assertj-core upgrade).

## 10. Add Dependabot Configuration for GitHub Actions

**Priority: Low**
**Files:** `.github/dependabot.yml`

Dependabot is configured for Maven dependencies but not for GitHub Actions workflows. The workflows pin action versions by major tag (e.g., `actions/checkout@v4`), which is good, but adding a `github-actions` ecosystem entry to `dependabot.yml` would automate updates when new versions of actions are released, including security patches.

```yaml
- package-ecosystem: "github-actions"
  directory: "/"
  schedule:
    interval: "weekly"
```

## 11. Add XML Logback Configuration Example

**Priority: Low**
**Files:** `README.md`

The README shows only a minimal configuration example. Consider adding a more complete example demonstrating:

- Enabling stack traces and MDC logging.
- Using a custom encoder with a pattern layout.
- Setting the syslog identifier.
- Combining the journal appender with a console appender for development.

This would reduce the learning curve for new users.

## 12. Consider Thread Safety for Configuration Changes

**Priority: Low**
**Files:** `SystemdJournalAppender.java`

The boolean configuration fields are not `volatile` and have no synchronization. If configuration is changed at runtime (e.g., via JMX), the `append()` method running on a different thread might not see the updated values due to Java Memory Model visibility rules. While runtime reconfiguration is uncommon, Logback does support it (e.g., via `<configuration scan="true">`). Making the fields `volatile` or documenting that runtime changes are not supported would clarify the contract.

## 13. Improve `normalizeKey` Regex Performance

**Priority: Low**
**Files:** `SystemdJournalAppender.java:210`

The `normalizeKey` method calls `String.replaceAll()`, which compiles a new `Pattern` on every invocation. Since this is called for every MDC key on every log event when `logMdc` is enabled, pre-compiling the pattern as a `static final Pattern` field would avoid repeated regex compilation in high-throughput logging scenarios.

## 14. Add `module-info.java` for JPMS Support

**Priority: Low**
**Files:** New `src/main/java/module-info.java`

The project targets Java 11+, which fully supports the Java Platform Module System (JPMS). Adding a `module-info.java` would allow modular applications to cleanly depend on `logback-journal` and would declare explicit dependencies on `com.sun.jna`, `ch.qos.logback.classic`, and `org.slf4j`.

## Summary Table

| # | Area | Priority | Effort |
|---|------|----------|--------|
| 1 | JUnit 4 → JUnit 5 migration | Medium | Medium |
| 2 | Code coverage reporting (JaCoCo) | Medium | Low |
| 3 | Run integration tests in CI | High | Low |
| 4 | Validate configuration in `start()` | Medium | Low |
| 5 | Manage encoder lifecycle | Medium | Low |
| 6 | Check JNA return values | Medium | Low |
| 7 | Make fields private | Low | Low |
| 8 | Make `MESSAGE_ID` final | Low | Trivial |
| 9 | Add CHANGELOG | Low | Low |
| 10 | Dependabot for GitHub Actions | Low | Trivial |
| 11 | Expanded configuration examples in README | Low | Low |
| 12 | Thread safety for config fields | Low | Low |
| 13 | Pre-compile normalizeKey regex | Low | Trivial |
| 14 | JPMS module-info.java | Low | Low |
