package com.portfolio.siemlite.windows;

final class PowerShellWindowsEventLogScriptBuilder {

    private static final String START_TIME_TOKEN = "__SIEM_START_TIME__";
    private static final String MAX_PER_LOG_TOKEN = "__SIEM_MAX_PER_LOG__";
    private static final String MAX_TOTAL_TOKEN = "__SIEM_MAX_TOTAL__";

    private static final String SCRIPT_TEMPLATE = """
            $ErrorActionPreference = 'Continue'
            $ProgressPreference = 'SilentlyContinue'
            $utf8 = New-Object System.Text.UTF8Encoding($false)
            [Console]::OutputEncoding = $utf8
            $OutputEncoding = $utf8

            $startTime = [DateTimeOffset]::Parse('__SIEM_START_TIME__', [Globalization.CultureInfo]::InvariantCulture).UtcDateTime
            $maxEventsPerLog = __SIEM_MAX_PER_LOG__
            $maxTotalEvents = __SIEM_MAX_TOTAL__
            $eventsWritten = 0
            $logsQueried = 0
            $logsWithData = 0
            $logsSkipped = 0
            $capReached = $false
            $catalogErrors = @()
            $allLogs = @(Get-WinEvent -ListLog '*' -ErrorAction SilentlyContinue -ErrorVariable +catalogErrors)
            $logsSkipped += $catalogErrors.Count
            $eligibleLogs = @()

            foreach ($log in $allLogs) {
                if ($log.IsEnabled -and ($null -eq $log.RecordCount -or $log.RecordCount -gt 0)) {
                    $eligibleLogs += $log
                }
            }

            for ($index = 0; $index -lt $eligibleLogs.Count; $index += 1) {
                $log = $eligibleLogs[$index]
                $remainingLogs = $eligibleLogs.Count - $index
                $remainingCapacity = $maxTotalEvents - $eventsWritten

                if ($remainingCapacity -le 0) {
                    $capReached = $true
                    $logsSkipped += $remainingLogs
                    break
                }

                $fairShare = [Math]::Max(1, [Math]::Floor($remainingCapacity / $remainingLogs))
                $maxForLog = [int][Math]::Min($maxEventsPerLog, $fairShare)

                try {
                    $events = @(Get-WinEvent -FilterHashtable @{ LogName = $log.LogName; StartTime = $startTime } -MaxEvents $maxForLog -ErrorAction Stop)
                    $logsQueried += 1

                    if ($events.Count -gt 0) {
                        $logsWithData += 1
                    }
                    if ($events.Count -ge $maxForLog) {
                        $capReached = $true
                    }

                    foreach ($event in $events) {
                        if ($eventsWritten -ge $maxTotalEvents) {
                            $capReached = $true
                            break
                        }

                        $timestamp = $null
                        if ($null -ne $event.TimeCreated) {
                            $timestamp = $event.TimeCreated.ToUniversalTime().ToString('o', [Globalization.CultureInfo]::InvariantCulture)
                        }

                        $message = $null
                        try {
                            $message = $event.FormatDescription()
                        } catch {
                            $message = $null
                        }

                        $rawXml = $null
                        try {
                            $rawXml = $event.ToXml()
                        } catch {
                            $rawXml = $null
                        }

                        [pscustomobject]@{
                            type = 'event'
                            timestamp = $timestamp
                            level = $event.Level
                            logName = $event.LogName
                            providerName = $event.ProviderName
                            eventId = $event.Id
                            recordId = $event.RecordId
                            computer = $event.MachineName
                            message = $message
                            rawXml = $rawXml
                        } | ConvertTo-Json -Depth 3 -Compress

                        $eventsWritten += 1
                    }
                } catch {
                    $logsSkipped += 1
                }
            }

            [pscustomobject]@{
                type = 'summary'
                logsQueried = $logsQueried
                logsWithData = $logsWithData
                logsSkipped = $logsSkipped
                capReached = $capReached
            } | ConvertTo-Json -Depth 2 -Compress
            """;

    String build(WindowsEventLogQuery query) {
        return SCRIPT_TEMPLATE
                .replace(START_TIME_TOKEN, query.startTime().toString())
                .replace(MAX_PER_LOG_TOKEN, Integer.toString(query.maxEventsPerLog()))
                .replace(MAX_TOTAL_TOKEN, Integer.toString(query.maxTotalEvents()));
    }
}
