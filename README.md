# SIEM Lite

![Java](https://img.shields.io/badge/Java-21-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/Build-Maven-red)
![Tests](https://img.shields.io/badge/Tests-JUnit%205-brightgreen)
![Status](https://img.shields.io/badge/Status-V1%20Complete-success)

SIEM Lite is a lightweight desktop application for basic log analysis. It allows users to import `.log` and `.txt` files, parse them line by line, detect suspicious events using simple hardcoded keywords, and filter the results in a JavaFX table.

This project was built as a cybersecurity/SOC portfolio project. The goal is to demonstrate a practical SOC Analyst Level 1 workflow, clean Java structure, and a small but extensible foundation for future improvements.

## Project Status

**Version:** V1.0.0  
**Status:** Technical V1 complete and ready for portfolio presentation.  
**Scope:** Local desktop log triage with import, parsing, keyword detection, table display, filtering, sample logs, and basic unit tests.

This is not intended to be a full SIEM platform. PostgreSQL persistence, historical views, dashboards, and configurable detection rules are planned for later versions.

## SOC / Portfolio Objective

The V1 simulates a simple SOC-style log review flow:

- Load log evidence from a local file.
- Identify basic event severity.
- Flag potentially suspicious events.
- Filter events by text or severity.
- Keep the codebase simple enough to explain during a technical interview.

## Tech Stack

- Java 21
- JavaFX
- Maven
- JUnit 5

## V1 Features

- JavaFX desktop interface.
- Import `.log` and `.txt` files.
- Generic line-by-line parser.
- `LogEvent` model.
- Event table with line number, timestamp, severity, source, suspicious flag, matched keyword, and message.
- Simple keyword-based detection:
  - `failed`
  - `denied`
  - `unauthorized`
  - `error`
  - `critical`
  - `malware`
  - `bruteforce`
- Text filter.
- Severity filter.
- Sample log file at `samples/sample-system.log`.
- Basic unit tests for parser, detection, and filtering services.

## Screenshots

### Main screen

![Main screen](docs/screenshots/main-screen.png)

### Imported log

![Imported log](docs/screenshots/imported-log.png)

### Filtered suspicious events

![Filtered suspicious events](docs/screenshots/filtered-suspicious-events.png)

## How To Run

Requirements:

- JDK 21
- Maven

Compile the project:

```bash
mvn clean compile
```

Run the application:

```bash
mvn javafx:run
```

Run tests:

```bash
mvn clean test
```

## How To Test With The Sample Log

1. Run the application with `mvn javafx:run`.
2. Click `Importar log`.
3. Select `samples/sample-system.log`.
4. Review the imported events in the table.
5. Try text filters such as:
   - `admin`
   - `malware`
   - `unauthorized`
   - `bruteforce`
6. Try severity filters such as:
   - `ERROR`
   - `CRITICAL`
   - `WARN`

## V1.1 Roadmap

- Add PostgreSQL persistence.
- Allow analysts to save relevant events.
- Add a simple history view for saved events.
- Add a basic summary dashboard.
- Improve parsing for common SOC log formats.
- Add more sample logs and unit tests.

## Out Of Scope For V1

- PostgreSQL integration.
- Dashboard.
- Event history.
- Configurable detection rules.
- CSV/PDF export.
- User login or role management.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
