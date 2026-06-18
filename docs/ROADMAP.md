# SIEM Lite Strategic Roadmap

SIEM Lite is evolving from a manual log viewer into a personal Windows SIEM for cybersecurity students, educators, homelabs, and entry-level SOC learning environments.

The project should remain local, educational, privacy-conscious, installable, and simple to operate. It is not intended to replace enterprise SIEM platforms such as Splunk, Microsoft Sentinel, QRadar, or Wazuh.

## Product Direction

The long-term product statement is:

> SIEM Lite is a personal Windows SIEM designed for cybersecurity students, educators, homelabs, and entry-level SOC learning environments. It combines local Windows event monitoring, educational guidance, and basic security analysis in a single desktop application.

## v1.0.0 Target Scope

`v1.0.0` should be the first stable end-user release. It should include:

- Windows `.msi` installer with bundled Java runtime.
- Desktop/start menu shortcut.
- Local SQLite database stored under AppData.
- Local app configuration stored under AppData.
- Manual log import.
- Local Windows Event Log collection/import.
- Security, System, and Application log support.
- Dashboard.
- Event history.
- Event detail view.
- Basic Windows-focused detection rules.
- Basic correlation rules.
- Learning Mode.
- Professional Mode.
- First launch wizard.
- Settings screen.
- Knowledge Cards.
- Light Mode, Dark Mode, and System Default theme.
- Language selector with English fallback.
- Initial language bundles for English, Spanish, Chinese, Hindi, and Arabic.
- RTL-aware Arabic support for core views.
- Background mode.
- System tray.
- Windows notifications.
- Local privacy/data policy.
- Troubleshooting logs.
- Tests, release notes, changelog, screenshots, and user documentation.

## Explicitly Out Of Scope For v1.0.0

- Enterprise multi-host monitoring.
- Distributed ingestion.
- Cloud SIEM.
- Multi-tenant support.
- Compliance reporting.
- Advanced enterprise correlation.
- Replacing Splunk, Sentinel, QRadar, or Wazuh.
- Automatic audit policy changes without explicit user consent.
- Sending telemetry/events to the internet by default.
- PostgreSQL as a required dependency.

## Architecture Direction

Recommended long-term packages/modules:

- `ui`: JavaFX views/controllers, dashboard, history, settings, wizard, Knowledge Cards.
- `domain`: security events, rules, alerts, Knowledge Cards, settings, collector status.
- `parser`: generic log parser and Windows Event Log mapping.
- `collector`: Windows Event Log collector abstraction and PowerShell/wevtutil adapter.
- `detection`: rule matching, severity mapping, false-positive metadata.
- `correlation`: time-window logic and alert generation.
- `repository`: SQLite repositories and migrations/init.
- `service`: import, filtering, background jobs, notifications, settings coordination.
- `config`: AppData paths and application settings.
- `localization`: ResourceBundle-based language support.
- `theme`: appearance modes and runtime stylesheet switching.
- `education`: Knowledge Card content loading.
- `packaging`: jpackage/WiX scripts, checksums, release artifacts.

## Technical Decisions

- Use SQLite as the main local persistence layer.
- Keep PostgreSQL as a possible future advanced backend only.
- Store database, settings, and app logs under AppData.
- Do not store credentials.
- Do not transmit data externally by default.
- Use `ResourceBundle` for JavaFX localization.
- Use English as the canonical fallback language.
- Use separate JavaFX stylesheets for Light and Dark themes.
- Add shared design tokens for typography, spacing, semantic colors, table colors, and severity colors.
- Apply theme and language preferences through Settings.
- Use `jpackage` first for installer work; add WiX only if needed for MSI packaging.
- Wrap Windows Event Log access behind an interface before choosing a final collector implementation.

## Cross-Cutting Requirements

### Theme Support

Target modes:

- Light Mode
- Dark Mode
- System Default

Settings location:

- `Settings -> Appearance`

Recommended concepts:

- `AppearanceMode`
- `ThemeService`
- `SettingsService`
- `UserSettings`

Acceptance criteria:

- User can switch between Light, Dark, and System Default.
- Preference persists after restart.
- Theme applies without restarting where technically reasonable.
- Tables remain readable.
- Severity colors remain accessible.
- Dashboard cards remain legible.
- Dialogs, settings, empty states, and error messages are readable.

Risks:

- JavaFX `TableView` styling can be brittle.
- Dark Mode can break selected rows, table headers, charts, disabled controls, and severity colors.
- System Default theme detection may vary across Windows versions.

### Localization Support

Initial languages:

- English
- Spanish
- Chinese
- Hindi
- Arabic

Settings location:

- `Settings -> Language`

Recommended approach:

- Use Java `ResourceBundle`.
- Store base strings in `messages.properties`.
- Add locale bundles such as `messages_es.properties`, `messages_zh.properties`, `messages_hi.properties`, and `messages_ar.properties`.
- Use FXML resource keys for static UI text.
- Use `LocalizationService` for dynamic controller text.
- Persist selected language.
- Fallback safely to English.
- Use UTF-8.
- Add RTL support for Arabic progressively.

Acceptance criteria:

- User can select language from Settings.
- Language preference persists after restart.
- Labels, buttons, menus, dialogs, errors, tooltips, settings, and table headers are localizable.
- Missing translations fall back to English.
- Arabic is usable in core views.
- Long translated strings do not break critical layouts.
- Knowledge Cards can load localized content with English fallback.

## Roadmap

### v0.1.x - Current Base Stabilization

- `v0.1.0`: Initial portfolio release.
- `v0.1.1`: Documentation and release polish.
- `v0.1.2`: Minor UI copy and bugfixes.
- `v0.1.3`: GitHub Actions CI foundation.
- `v0.1.4`: UI text inventory for future i18n.

### v0.2.x - Local Persistence With SQLite

- `v0.2.0`: SQLite base, local database creation, initial schema, repository layer.
- `v0.2.1`: Database error handling and startup validation.
- `v0.2.2`: Repository test coverage.
- `v0.2.3`: AppData path resolver for config/data/logs.
- `v0.2.4`: User settings persistence for future theme/language preferences.
- `v0.2.5`: Settings error handling and default fallback.
- `v0.2.6`: Basic backup/export for local data.
- `v0.2.7`: Persistence UI polish and documentation.

### v0.3.x - i18n Foundation and Event History

- `v0.3.0`: ResourceBundle-based i18n architecture and English base bundle.
- `v0.3.1`: Language selector in Settings with persisted preference.
- `v0.3.2`: Initial UI translation bundles for English, Spanish, Chinese, Hindi, and Arabic.
- `v0.3.3`: Arabic RTL foundation for core views.
- `v0.3.4`: Event history screen.
- `v0.3.5`: Persisted event search and filters.
- `v0.3.6`: Event detail view.
- `v0.3.7`: Analyst notes.
- `v0.3.8`: Sorting and pagination/lazy loading.
- `v0.3.9`: History hardening.

All new UI added in this milestone should use localization keys from the start.

### v0.4.x - Windows Event Log Support

- `v0.4.0`: Manual local Windows Event Log import.
- `v0.4.1`: Permission handling.
- `v0.4.2`: Collection limits and progress UI.
- `v0.4.3`: Cancellation support.
- `v0.4.4`: Windows sample datasets.
- `v0.4.5`: Collector reliability pass.
- `v0.4.6`: Localized Windows collector messages.

Initial sources:

- Security Event Log
- System Event Log
- Application Event Log

Initial Event IDs:

- `4625` - Failed logon
- `4624` - Successful logon
- `4672` - Special privileges assigned
- `4688` - Process creation
- `4720` - User account created
- `4726` - User account deleted
- `1102` - Audit log cleared
- `7045` - Service installed

### v0.5.x - Windows-Focused Detection Rules and Theme Foundation

- `v0.5.0`: Detection rules for initial Windows Event IDs.
- `v0.5.1`: Rule metadata.
- `v0.5.2`: Severity mapping.
- `v0.5.3`: False-positive notes foundation.
- `v0.5.4`: Detection UI polish.
- `v0.5.5`: Detection hardening.
- `v0.5.6`: Theme token foundation.
- `v0.5.7`: Dark Mode.
- `v0.5.8`: System Default theme.
- `v0.5.9`: Theme accessibility polish.

Theme architecture should land before dashboard work to avoid restyling dashboard cards and charts later.

### v0.6.x - Dashboard

- `v0.6.0`: Basic dashboard.
- `v0.6.1`: Dashboard data queries.
- `v0.6.2`: Learning dashboard cards.
- `v0.6.3`: Professional dashboard view.
- `v0.6.4`: Dashboard performance pass.
- `v0.6.5`: Dashboard theme and localization polish.

Every dashboard release should work in Light Mode, Dark Mode, and localized UI contexts.

### v0.7.x - Correlation

- `v0.7.0`: Multiple failed logons over a time window.
- `v0.7.1`: Successful login after repeated failures.
- `v0.7.2`: Service installed after privilege event.
- `v0.7.3`: Audit log cleared alert.
- `v0.7.4`: Correlation UI.
- `v0.7.5`: Correlation tuning and deduplication.
- `v0.7.6`: Localized alert summaries.

### v0.8.x - Educational Experience

- `v0.8.0`: Learning Mode, Professional Mode, first launch wizard, settings switch.
- `v0.8.1`: Locale-aware Knowledge Card model with English fallback.
- `v0.8.2`: English Knowledge Cards for core detections.
- `v0.8.3`: Spanish Knowledge Cards.
- `v0.8.4`: Chinese, Hindi, and Arabic Knowledge Card drafts.
- `v0.8.5`: RTL Knowledge Card polish.
- `v0.8.6`: Student guide.
- `v0.8.7`: Teacher guide.
- `v0.8.8`: Practice datasets.
- `v0.8.9`: Report export.
- `v0.8.10`: Educational UX polish.

### v0.9.x - Productization and Release Candidate Phase

- `v0.9.0`: Background mode foundation.
- `v0.9.1`: System tray.
- `v0.9.2`: Windows notifications.
- `v0.9.3`: Installer prototype.
- `v0.9.4`: AppData migration/finalization.
- `v0.9.5`: Privacy and security hardening.
- `v0.9.6`: Performance hardening.
- `v0.9.7`: Release automation.
- `v0.9.8`: Documentation finalization.
- `v0.9.9`: Release candidate.
- `v0.9.10`: Localization QA pass.
- `v0.9.11`: Theme QA pass.

### v1.0.0 - Stable Personal Windows SIEM

Release only when:

- MSI installer works.
- Bundled Java runtime works.
- SQLite local database works.
- Windows Event Logs can be collected/imported.
- Dashboard works.
- Event history works.
- Rules work.
- Correlation works.
- Learning Mode works.
- Professional Mode works.
- Knowledge Cards work.
- Light Mode works.
- Dark Mode works.
- System Default theme works or falls back clearly.
- English, Spanish, Chinese, Hindi, and Arabic language options exist.
- English fallback works.
- Arabic RTL is usable for core views.
- Background mode works.
- Notifications work.
- Performance is acceptable.
- Privacy/data policy is clear.
- Docs are complete.
- Tests pass.
- App can be installed and used in 7 steps or fewer.
- Limitations clearly state it is a personal/educational SIEM, not an enterprise SIEM.

## Suggested GitHub Issues

### Theme

1. `design: define theme tokens for JavaFX UI`
2. `feature: add Appearance settings model`
3. `feature: add Light and Dark theme stylesheets`
4. `feature: add System Default theme option`
5. `test: add theme preference persistence tests`
6. `qa: verify table readability in Light and Dark Mode`
7. `qa: verify dashboard contrast in Light and Dark Mode`

### Localization

1. `i18n: inventory hardcoded user-facing strings`
2. `i18n: add ResourceBundle-based localization foundation`
3. `i18n: externalize FXML labels and table headers`
4. `i18n: externalize controller status and error messages`
5. `feature: add Language settings selector`
6. `i18n: add Spanish UI bundle`
7. `i18n: add Chinese UI bundle`
8. `i18n: add Hindi UI bundle`
9. `i18n: add Arabic UI bundle`
10. `test: add missing-key fallback tests`
11. `qa: validate Arabic RTL layout`
12. `i18n: make Knowledge Cards locale-aware`
13. `docs: document language support and translation limitations`

### Core Product

1. `ci: add GitHub Actions test workflow`
2. `docs: add privacy and local data policy draft`
3. `feature: add SQLite local database foundation`
4. `feature: add AppData path resolver`
5. `feature: add SecurityEvent persistence model`
6. `test: add SQLite repository integration tests`
7. `feature: add event history screen`
8. `research: evaluate Windows Event Log collection approaches`
9. `feature: add Windows Event Log collector interface`
10. `feature: add Windows Event ID detection rules`

## Branch Strategy

- `main`: always clean, tested, and releasable.
- `feature/<short-name>`: focused feature branches.
- `fix/<short-name>`: bugfix branches.
- `docs/<short-name>`: documentation-only branches.
- `release/v0.x.0`: optional stabilization branch for larger milestones.

Examples:

- `feature/sqlite-persistence`
- `feature/i18n-foundation`
- `feature/theme-support`
- `feature/windows-event-import`
- `docs/product-vision`

## Release Strategy

- Use `v0.x.0` for public functional milestones.
- Use `v0.x.y` for bugfixes, docs, tests, hardening, performance, and polish.
- Use `v1.0.0` only when the app is installable, reliable, documented, and usable by non-developer Windows users.

Every public release should include:

- Tag.
- GitHub Release notes.
- Summary.
- Added/Changed/Fixed/Known limitations.
- Validation result.
- Screenshots when UI changes.
- Checksums once packaging begins.
- Clear statement that SIEM Lite is not an enterprise SIEM.

## What Not To Do Yet

- Do not add PostgreSQL as a main dependency.
- Do not build the MSI before AppData paths and SQLite are stable.
- Do not add background mode before collector lifecycle is reliable.
- Do not add notifications before alert semantics are stable.
- Do not use online translation APIs.
- Do not promise perfect translations without review.
- Do not create a custom translation management system.
- Do not build a plugin system for themes.
- Do not add cloud sync or telemetry.
- Do not modify Windows audit policy automatically.
