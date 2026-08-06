package com.portfolio.siemlite.windows;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerShellWindowsEventLogCommandRunnerTest {

    @Test
    void returnsPartialResultWhenOneLogWasSkipped() {
        String stdout = """
                {"type":"event","timestamp":"2026-08-06T12:00:00Z","level":3,"logName":"System","providerName":"Provider","eventId":7,"recordId":8,"computer":"HOST","message":"Warning","rawXml":"<Event />"}
                not-json
                {"type":"summary","logsQueried":2,"logsWithData":1,"logsSkipped":1,"capReached":false}
                """;
        FakeProcess process = new FakeProcess(stdout, "", true, 0);
        AtomicReference<List<String>> capturedCommand = new AtomicReference<>();
        PowerShellWindowsEventLogCommandRunner runner = runner(command -> {
            capturedCommand.set(command);
            return process;
        });

        WindowsEventLogCommandResult result = runner.run(query(1_024 * 1_024));

        assertEquals(1, result.events().size());
        assertEquals(2, result.logsQueried());
        assertEquals(1, result.logsWithData());
        assertEquals(1, result.logsSkipped());
        assertTrue(result.metadataComplete());
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.INVALID_NDJSON));
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.LOG_SKIPPED));
        assertFalse(result.timedOut());
        assertEquals("powershell.exe", capturedCommand.get().getFirst());
        assertTrue(capturedCommand.get().contains("-NoProfile"));
        assertTrue(capturedCommand.get().contains("-NonInteractive"));
    }

    @Test
    void sanitizesStderrAndNonZeroExitCode() {
        String secretError = "C:\\Users\\private\\script.ps1: access denied with internal stack trace";
        String stdout = """
                {"type":"summary","logsQueried":0,"logsWithData":0,"logsSkipped":0,"capReached":false}
                """;
        PowerShellWindowsEventLogCommandRunner runner = runner(
                command -> new FakeProcess(stdout, secretError, true, 1));

        WindowsEventLogCommandResult result = runner.run(query(1_024));

        assertTrue(result.metadataComplete());
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.NON_ZERO_EXIT));
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.PROCESS_ERROR_OUTPUT));
        assertEquals(2, result.warnings().size());
    }

    @Test
    void preservesPartialEventsAndMarksMetadataIncompleteOnTimeoutWithoutSummary() {
        String stdout = """
                {"type":"event","timestamp":"2026-08-06T12:00:00Z","level":2,"logName":"System","message":"Failed login"}
                """;
        PowerShellWindowsEventLogCommandRunner runner = runner(
                command -> new FakeProcess(stdout, "", false, 0));

        WindowsEventLogCommandResult result = runner.run(query(1_024));

        assertEquals(1, result.events().size());
        assertFalse(result.metadataComplete());
        assertEquals(0, result.logsQueried());
        assertTrue(result.timedOut());
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.TIMEOUT));
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.SUMMARY_MISSING));
    }

    @Test
    void preservesCompleteEventsBeforeStdoutCapAndMarksMetadataIncomplete() {
        String eventLine =
                "{\"type\":\"event\",\"timestamp\":\"2026-08-06T12:00:00Z\",\"level\":4,\"logName\":\"Application\"}";
        String summaryLine =
                "{\"type\":\"summary\",\"logsQueried\":1,\"logsWithData\":1,\"logsSkipped\":0,\"capReached\":false}";
        String stdout = eventLine + System.lineSeparator() + summaryLine;
        PowerShellWindowsEventLogCommandRunner runner = runner(
                command -> new FakeProcess(stdout, "", true, 0));

        WindowsEventLogCommandResult result = runner.run(
                query(eventLine.getBytes(StandardCharsets.UTF_8).length));

        assertEquals(1, result.events().size());
        assertFalse(result.metadataComplete());
        assertTrue(result.capReached());
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.STDOUT_CAP_REACHED));
        assertTrue(result.warnings().contains(WindowsEventLogWarningCode.SUMMARY_MISSING));
    }

    @Test
    void returnsSanitizedWarningWhenProcessCannotStart() {
        PowerShellWindowsEventLogCommandRunner runner = runner(command -> {
            throw new IOException("C:\\sensitive\\powershell.exe was not found");
        });

        WindowsEventLogCommandResult result = runner.run(query(1_024));

        assertTrue(result.events().isEmpty());
        assertEquals(
                List.of(WindowsEventLogWarningCode.PROCESS_START_FAILED),
                result.warnings());
        assertFalse(result.metadataComplete());
    }

    private PowerShellWindowsEventLogCommandRunner runner(
            PowerShellWindowsEventLogCommandRunner.ProcessStarter processStarter) {
        return new PowerShellWindowsEventLogCommandRunner(
                new Gson(),
                new PowerShellWindowsEventLogScriptBuilder(),
                processStarter,
                () -> Optional.of("powershell.exe"));
    }

    private WindowsEventLogQuery query(long stdoutLimit) {
        return new WindowsEventLogQuery(
                Instant.parse("2026-08-05T15:30:00Z"),
                5,
                10,
                Duration.ofMillis(10),
                stdoutLimit);
    }

    private static final class FakeProcess extends Process {

        private final InputStream stdout;
        private final InputStream stderr;
        private final int exitCode;
        private volatile boolean completed;

        private FakeProcess(String stdout, String stderr, boolean completed, int exitCode) {
            this.stdout = new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8));
            this.stderr = new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8));
            this.completed = completed;
            this.exitCode = exitCode;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return stderr;
        }

        @Override
        public int waitFor() {
            completed = true;
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return completed;
        }

        @Override
        public int exitValue() {
            if (!completed) {
                throw new IllegalThreadStateException("Process is still running.");
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            completed = true;
        }

        @Override
        public Process destroyForcibly() {
            completed = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return !completed;
        }
    }
}
