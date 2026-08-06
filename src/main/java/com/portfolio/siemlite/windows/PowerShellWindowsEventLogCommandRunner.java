package com.portfolio.siemlite.windows;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class PowerShellWindowsEventLogCommandRunner implements WindowsEventLogCommandRunner {

    static final String WARNING_NOT_WINDOWS = "Windows Event Logs are only available on Windows.";
    static final String WARNING_START_FAILED = "The Windows Event Log command could not be started.";
    static final String WARNING_INTERRUPTED = "The Windows Event Log command was interrupted.";
    static final String WARNING_TIMED_OUT = "The Windows Event Log command reached its safety timeout.";
    static final String WARNING_OUTPUT_LIMIT = "Windows Event Log output reached its safety limit.";
    static final String WARNING_INVALID_OUTPUT = "Some Windows Event Log output could not be parsed.";
    static final String WARNING_COMMAND_FAILED = "The Windows Event Log command did not complete successfully.";
    static final String WARNING_SKIPPED_LOGS = "Some Windows Event Logs could not be read.";

    private static final long MAX_STDERR_BYTES = 64L * 1024L;
    private static final long STREAM_CAPTURE_TIMEOUT_SECONDS = 5L;

    private final Gson gson;
    private final PowerShellWindowsEventLogScriptBuilder scriptBuilder;
    private final ProcessStarter processStarter;
    private final Supplier<Optional<String>> executableResolver;

    public PowerShellWindowsEventLogCommandRunner() {
        this(
                new Gson(),
                new PowerShellWindowsEventLogScriptBuilder(),
                command -> new ProcessBuilder(command).start(),
                PowerShellWindowsEventLogCommandRunner::resolvePowerShellExecutable);
    }

    PowerShellWindowsEventLogCommandRunner(
            Gson gson,
            PowerShellWindowsEventLogScriptBuilder scriptBuilder,
            ProcessStarter processStarter,
            Supplier<Optional<String>> executableResolver) {
        this.gson = Objects.requireNonNull(gson, "gson");
        this.scriptBuilder = Objects.requireNonNull(scriptBuilder, "scriptBuilder");
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
        this.executableResolver = Objects.requireNonNull(executableResolver, "executableResolver");
    }

    @Override
    public WindowsEventLogCommandResult run(WindowsEventLogQuery query) {
        Objects.requireNonNull(query, "query");

        Optional<String> executable = executableResolver.get();
        if (executable.isEmpty()) {
            return WindowsEventLogCommandResult.empty(WARNING_NOT_WINDOWS);
        }

        List<String> command = List.of(
                executable.get(),
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                scriptBuilder.build(query));

        Process process;
        try {
            process = processStarter.start(command);
        } catch (IOException exception) {
            return WindowsEventLogCommandResult.empty(WARNING_START_FAILED);
        }

        ExecutorService streamExecutor = Executors.newFixedThreadPool(2);
        Future<StreamCapture> stdoutFuture = streamExecutor.submit(
                () -> capture(process.getInputStream(), query.maxStdoutBytes()));
        Future<StreamCapture> stderrFuture = streamExecutor.submit(
                () -> capture(process.getErrorStream(), MAX_STDERR_BYTES));

        try {
            boolean finished = process.waitFor(query.timeout().toMillis(), TimeUnit.MILLISECONDS);
            boolean timedOut = !finished;
            if (timedOut) {
                process.destroyForcibly();
            }

            StreamCapture stdout = awaitCapture(stdoutFuture);
            StreamCapture stderr = awaitCapture(stderrFuture);
            ParsedOutput parsedOutput = parseOutput(stdout.content());

            Set<String> warnings = new LinkedHashSet<>(parsedOutput.warnings());
            if (timedOut) {
                warnings.add(WARNING_TIMED_OUT);
            } else if (process.exitValue() != 0) {
                warnings.add(WARNING_COMMAND_FAILED);
            }
            if (stdout.truncated()) {
                warnings.add(WARNING_OUTPUT_LIMIT);
            }
            if (stderr.truncated() || !stderr.content().isBlank()) {
                warnings.add(WARNING_COMMAND_FAILED);
            }
            if (parsedOutput.logsSkipped() > 0) {
                warnings.add(WARNING_SKIPPED_LOGS);
            }

            return new WindowsEventLogCommandResult(
                    parsedOutput.events(),
                    parsedOutput.logsQueried(),
                    parsedOutput.logsWithData(),
                    parsedOutput.logsSkipped(),
                    List.copyOf(warnings),
                    parsedOutput.capReached() || stdout.truncated(),
                    timedOut);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return WindowsEventLogCommandResult.empty(WARNING_INTERRUPTED);
        } finally {
            streamExecutor.shutdownNow();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    ParsedOutput parseOutput(String output) {
        List<WindowsEventLogEntry> events = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        int logsQueried = 0;
        int logsWithData = 0;
        int logsSkipped = 0;
        boolean capReached = false;
        boolean summarySeen = false;

        try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                try {
                    JsonElement parsed = JsonParser.parseString(line);
                    if (!parsed.isJsonObject()) {
                        warnings.add(WARNING_INVALID_OUTPUT);
                        continue;
                    }

                    JsonObject object = parsed.getAsJsonObject();
                    String type = stringValue(object, "type");
                    if ("event".equals(type)) {
                        events.add(gson.fromJson(object, WindowsEventLogEntry.class));
                    } else if ("summary".equals(type)) {
                        logsQueried = nonNegativeInt(object, "logsQueried");
                        logsWithData = Math.min(logsQueried, nonNegativeInt(object, "logsWithData"));
                        logsSkipped = nonNegativeInt(object, "logsSkipped");
                        capReached = booleanValue(object, "capReached");
                        summarySeen = true;
                    } else {
                        warnings.add(WARNING_INVALID_OUTPUT);
                    }
                } catch (JsonParseException | IllegalStateException | NumberFormatException exception) {
                    warnings.add(WARNING_INVALID_OUTPUT);
                }
            }
        } catch (IOException exception) {
            warnings.add(WARNING_INVALID_OUTPUT);
        }

        if (!output.isBlank() && !summarySeen) {
            warnings.add(WARNING_INVALID_OUTPUT);
        }

        return new ParsedOutput(
                events,
                logsQueried,
                logsWithData,
                logsSkipped,
                warnings,
                capReached);
    }

    private StreamCapture awaitCapture(Future<StreamCapture> future) throws InterruptedException {
        try {
            return future.get(STREAM_CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException exception) {
            future.cancel(true);
            return new StreamCapture("", true);
        }
    }

    private static StreamCapture capture(InputStream inputStream, long limit) throws IOException {
        ByteArrayOutputStream stored = new ByteArrayOutputStream((int) Math.min(limit, 8_192L));
        byte[] buffer = new byte[8_192];
        long totalRead = 0;
        boolean truncated = false;

        try (inputStream) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                long remaining = limit - totalRead;
                if (remaining > 0) {
                    int bytesToStore = (int) Math.min(read, remaining);
                    stored.write(buffer, 0, bytesToStore);
                }
                totalRead += read;
                if (totalRead > limit) {
                    truncated = true;
                }
            }
        }

        return new StreamCapture(stored.toString(StandardCharsets.UTF_8), truncated);
    }

    private static Optional<String> resolvePowerShellExecutable() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("windows")) {
            return Optional.empty();
        }

        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null && !systemRoot.isBlank()) {
            Path windowsPowerShell = Path.of(
                    systemRoot,
                    "System32",
                    "WindowsPowerShell",
                    "v1.0",
                    "powershell.exe");
            if (Files.isRegularFile(windowsPowerShell)) {
                return Optional.of(windowsPowerShell.toString());
            }
        }

        return Optional.of("pwsh.exe");
    }

    private static String stringValue(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int nonNegativeInt(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? 0 : Math.max(0, value.getAsInt());
    }

    private static boolean booleanValue(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(List<String> command) throws IOException;
    }

    record ParsedOutput(
            List<WindowsEventLogEntry> events,
            int logsQueried,
            int logsWithData,
            int logsSkipped,
            Set<String> warnings,
            boolean capReached) {

        ParsedOutput {
            events = List.copyOf(events);
            warnings = Set.copyOf(warnings);
        }
    }

    private record StreamCapture(String content, boolean truncated) {
    }
}
