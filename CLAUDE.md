# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**App name:** Dose · **applicationId / package:** `ca.alexlittle.dose`

A fast, private, offline-first medication reminder app for Android, written in Kotlin. It does what Medisafe's core does (remind you to take your meds, track whether you did, track supply) and nothing else. No accounts, no ads, no upsells, no social features, no "Medfriends", no analytics, no network calls.

The one job: **a reminder fires at the right time, every time, and logging a dose takes one tap.**

## Product principles

These override convenience. If a change violates one, stop and ask.

1. **Reliability over features.** A missed reminder is the worst possible bug. Alarm scheduling and notification delivery get the most testing and care.
2. **Local-first, zero network.** The app must not request the `INTERNET` permission. No analytics, crash reporters, ad SDKs, or remote config. Do not add a dependency that pulls in network access.
3. **No account, no sign-in, no cloud.** Data lives in a local Room database. Backup is a user-initiated export/import file.
4. **One-tap interaction.** Taking a dose from the notification or the home screen must be one tap. Never put a confirmation dialog in front of a routine action.
5. **No dark patterns.** No premium tier, nag screens, review prompts, streak guilt, or gamification. Empty states are plain and calm.
6. **Not a medical authority.** The app records what the user tells it. It does not give dosing advice, drug-interaction warnings, or medical claims. Do not add a drug database or "interaction checker".
7. **Accessible by default.** Large touch targets, proper content descriptions, supports font scaling, TalkBack, and high contrast. Many users are older or have impaired vision or dexterity.

## Scope

### MVP (build in this order)

1. Medication CRUD: name, strength/unit (e.g. 500 mg), form (tablet, capsule, liquid, injection, drops, inhaler, other), color/icon, notes.
2. Schedules per medication:
   - Fixed times daily (multiple per day)
   - Specific weekdays
   - Every N days / every N weeks
   - Cyclic (X days on, Y days off)
   - As-needed (PRN): no reminder, manual logging only, optional min interval between doses
   - Start date and optional end date
   - Dose quantity per time (supports fractions, e.g. 0.5 tablet)
3. Reminder notifications with actions: **Taken**, **Snooze**, **Skip**.
4. Today screen: chronological list of scheduled doses with status (pending, taken, skipped, missed), one-tap take.
5. Dose history log with the ability to edit or backfill entries.
6. Persistent re-reminders for unacknowledged doses (configurable interval and max repeats).
7. Reliable rescheduling after reboot, app update, timezone change, and system clock change.
8. Settings: snooze duration, re-reminder behavior, notification sound/vibration, theme, 12/24h follows system.
9. Export/import of all data as JSON (via Storage Access Framework).

### Phase 2

- Supply tracking: pills remaining, low-supply threshold, refill reminder
- Home screen widget (Glance): next dose + take button
- Adherence summary (simple percentage by week/month, plain chart)
- Per-medication "take with food" / instructions text shown in notification
- Optional full-screen alarm-style reminder for critical meds
- Share/export history as CSV or PDF for a clinician
- Quick-settings tile for logging a PRN dose

### Explicitly out of scope

Accounts, cloud sync, caregiver/family sharing, drug database, interaction checking, pharmacy integration, health-metric tracking (BP, glucose, weight), ads, subscriptions, telemetry, AI features, in-app chat or support.

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin (latest stable), coroutines + Flow |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Persistence | Room 2.x (`androidx.room`, with schema export enabled). Decided: stay on Room 2 rather than Room 3 (`androidx.room3`) |
| Settings | Jetpack DataStore (Preferences) |
| DI | Hilt (KSP). Decided; do not switch to Koin |
| Scheduling | `AlarmManager` exact alarms for reminders; `WorkManager` only for housekeeping (e.g. rescheduling, missed-dose sweep) |
| Time | `java.time` (core library desugaring enabled for older API levels) |
| Widgets | Jetpack Glance (Phase 2) |
| Build | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`) |
| Lint/format | ktlint (`android_studio` style, trailing commas allowed) + detekt 2.0.0-alpha (deliberate pre-release: 1.23.x cannot parse Kotlin 2.4 or run on JDK 25). **Move to detekt 2.0 stable as soon as it is released.**. Config in `config/detekt/detekt.yml` |
| Tests | JUnit, Turbine, MockK, Room in-memory, Compose UI tests, Robolectric where sensible |

Always use the latest stable versions from the version catalog. Never hard-code dependency versions in module build files. Do not add a dependency without a one-line justification in the PR/commit message.

- `minSdk`: 26 (Android 8.0). Revisit only if there is a strong reason.
- `targetSdk` / `compileSdk`: latest stable.

## Architecture

Single-module to start (`app`), with strict package boundaries so it can be split later. Unidirectional data flow. MVVM with a clean-ish domain layer.

```
app/src/main/java/ca/alexlittle/dose/
├── DoseApp.kt                  # Application, Hilt entry, notification channels
├── MainActivity.kt
├── di/                         # Hilt modules, qualifiers (Clock, dispatchers)
├── data/
│   ├── db/                     # Room: entities, DAOs, database, migrations, converters
│   ├── repository/             # Repository implementations
│   ├── prefs/                  # DataStore settings
│   └── backup/                 # JSON export/import
├── domain/
│   ├── model/                  # Pure Kotlin models (no Android imports)
│   ├── schedule/               # Recurrence engine: schedule -> list of dose instants
│   └── usecase/                # TakeDose, SkipDose, SnoozeDose, GetTodayDoses, etc.
├── reminders/
│   ├── AlarmScheduler.kt       # Schedules/cancels exact alarms
│   ├── ReminderReceiver.kt     # Fires on alarm, posts notification
│   ├── ActionReceiver.kt       # Handles Taken / Snooze / Skip from notification
│   ├── BootReceiver.kt         # BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIMEZONE/TIME_CHANGED
│   └── NotificationFactory.kt
├── ui/
│   ├── theme/
│   ├── navigation/
│   ├── today/
│   ├── medications/            # list, detail, edit
│   ├── history/
│   ├── settings/
│   └── components/             # Shared composables
└── util/
```

Rules:
- `domain/` has **no Android imports**. It must be unit-testable on the JVM.
- ViewModels expose a single immutable `UiState` via `StateFlow` and accept `UiEvent`/function calls. No mutable state leaks out.
- Composables are stateless where possible: state hoisted, events passed up.
- Repositories return `Flow` for observable data and `suspend` for one-shots. Room is the single source of truth.
- Receivers do minimal work: hand off to a use case on a coroutine (use `goAsync()` with a bounded scope) and return quickly.

## Data model (starting point)

Keep the schedule definition separate from the dose log. **Never store future doses as rows; derive them from the schedule.** Store only what actually happened.

- `Medication`: id, name, strengthValue, strengthUnit, form, colorArgb, iconKey, notes, instructions, archived, createdAt
- `Schedule`: id, medicationId, type (DAILY, WEEKDAYS, INTERVAL, CYCLIC, PRN), params (days of week bitmask, interval N, on/off counts), startDate, endDate?, timesOfDay (list of `LocalTime`), doseQuantity (decimal), minIntervalMinutes? (PRN)
- `DoseLog`: id, medicationId, scheduleId?, scheduledAt (UTC instant, nullable for PRN), takenAt (UTC instant, nullable), status (TAKEN, SKIPPED, MISSED, SNOOZED_PENDING), quantity, note
- `Supply` (Phase 2): medicationId, remaining, lowThreshold

Time handling decisions (do not change without discussion):
- Schedules store **local wall-clock times** (`LocalTime`) so "8:00 AM" stays 8:00 AM across DST and travel. Resolve to instants at scheduling time using the device's current zone.
- Log entries store **UTC instants** plus enough info to display correctly later.
- A dose is identified by `(scheduleId, scheduledAt)`. This identity must be stable so re-scheduling never creates duplicates.

## Scheduling and reminders (the critical part)

- Use `AlarmManager.setExactAndAllowWhileIdle()` / `setAlarmClock()` for dose reminders. Schedule only the **next** upcoming alarm (or a short rolling window, e.g. next 24-48h) and re-arm after each fires. Do not schedule weeks of alarms.
- Permissions and behavior to handle explicitly:
  - `POST_NOTIFICATIONS` (Android 13+): request with clear rationale; handle denial gracefully with an in-app banner linking to settings.
  - `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` (Android 12+): this is a medication reminder, so evaluate `USE_EXACT_ALARM` eligibility honestly and fall back to `canScheduleExactAlarms()` checks with a user-facing prompt otherwise.
  - Battery optimization / OEM background killers: provide a one-time, dismissible, non-naggy help screen explaining how to exempt the app. Link to dontkillmyapp.com as reference.
  - `RECEIVE_BOOT_COMPLETED`: re-arm everything on boot, app update, timezone change, and manual time change.
- Notification channels: separate channels for dose reminders (high importance), re-reminders, and low-supply. The user controls sound/vibration through the system channel settings; link to them.
- Notification actions use `PendingIntent` with `FLAG_IMMUTABLE` and unique request codes derived from the dose identity.
- Idempotency: Taking a dose twice (notification + app) must not log twice. Use the dose identity and a unique constraint.
- Missed-dose logic: a dose becomes MISSED after a configurable grace window if untouched. Handle this lazily (on app open and on the next alarm cycle), not with a precise timer per dose.
- Every scheduling path needs a test that simulates: reboot, DST change, timezone change, schedule edit, schedule deletion, and medication archive.

## Build and run

```bash
./gradlew assembleDebug            # Build debug APK
./gradlew installDebug             # Install on connected device/emulator
./gradlew test                     # JVM unit tests
./gradlew connectedAndroidTest     # Instrumented tests (needs device/emulator)
./gradlew ktlintCheck detekt       # Lint
./gradlew ktlintFormat             # Auto-format
./gradlew lint                     # Android lint
```

Before declaring any task done, run at minimum: `./gradlew ktlintCheck detekt test assembleDebug`. If you could not run something, say so explicitly rather than implying it passed.

## Testing expectations

- **Schedule engine** (`domain/schedule`): exhaustive JVM unit tests. Table-driven where possible. Cover DST transitions, leap days, month ends, cyclic on/off boundaries, start/end dates, multiple times per day, and fractional doses.
- **Use cases**: unit tests with fake repositories.
- **DAOs**: Room in-memory tests, including migrations (`MigrationTestHelper`). Every schema change needs a migration and a migration test. No destructive migrations.
- **UI**: Compose tests for the Today screen take/skip flow and the schedule editor.
- **Reminders**: test the scheduler with a fake `AlarmManager` wrapper. Keep Android framework calls behind thin interfaces so the logic stays testable.
- Inject a `Clock` everywhere. Never call `Instant.now()` / `LocalDateTime.now()` directly in logic. The app-wide `Clock` is `util/SystemZoneClock`, which re-reads the device zone on every call (`Clock.systemDefaultZone()` would freeze the zone).

## Code style

- Idiomatic Kotlin: immutable data classes, sealed interfaces for states/events, `when` exhaustiveness, extension functions sparingly.
- Prefer small, focused functions and files. No god ViewModels.
- No `!!`. No `GlobalScope`. No blocking calls on the main thread.
- Inject dispatchers; do not hard-code `Dispatchers.IO`.
- Strings in `strings.xml` from day one (localization-ready). No hard-coded user-visible text. Plurals via `plurals`.
- Comments explain *why*, not *what*. Do not leave commented-out code or unexplained TODOs.
- Compose: previews for every screen-level composable; keep `@Preview` data in a separate file.
- Public API surface is small: default to `internal`/`private`.

## UI and UX guidelines

- Material 3 with dynamic color, plus a fixed fallback palette. Support light, dark, and system.
- The **Today** screen is the home screen. Top of screen answers: "What do I need to take next?"
- Medication colors/icons must never be the *only* way to distinguish things (accessibility).
- Minimum touch target 48dp; primary actions larger. Respect system font scale up to 200% without clipping.
- Time pickers follow the system 12/24h setting.
- Destructive actions (delete medication, clear history) require confirmation and offer undo via snackbar where feasible. Routine actions (take, skip, snooze) never prompt.
- Copy is short, plain, and human. No exclamation-mark enthusiasm, no medical jargon, no guilt ("You missed 3 doses!"). Say "Missed" neutrally.

## Privacy and security

- No `INTERNET` permission. If a task seems to need it, stop and ask.
- Android Auto Backup is **off** (decided): `android:allowBackup="false"`, and `data_extraction_rules.xml` excludes every domain from cloud backup and device-to-device transfer. Backup is the user-initiated JSON export only.
- Do not log medication names or any health data to Logcat in release builds.
- Notifications on the lock screen: provide a setting to hide medication names (`VISIBILITY_PRIVATE` with a public version).
- JSON export is plain, documented, and versioned (`"schemaVersion"`). Import validates strictly and never silently overwrites data without confirmation.

## Working agreements for Claude Code

- Plan first for anything touching scheduling, the data model, or notifications. Summarize the approach in a few lines before writing code.
- Make small, reviewable changes. One concern per commit, imperative-mood messages.
- Do not refactor unrelated code while implementing a feature. Mention it instead.
- Do not add features outside the Scope section. If something seems missing, ask.
- When unsure about an Android version-specific behavior (alarms, notification permissions, foreground restrictions), check the official docs rather than guessing, and note the API level in a comment.
- If a requirement here conflicts with what is practical, say so plainly and propose an alternative instead of silently deviating.
- Keep this file current. When an architectural decision changes, update CLAUDE.md in the same commit.

## Definition of done

A task is done when:
1. It builds and `ktlintCheck`, `detekt`, and relevant tests pass.
2. New logic has tests; schedule or reminder changes have edge-case tests.
3. No new permissions or dependencies were added without justification.
4. User-facing strings are in resources and the UI works in dark mode and at large font size.
5. It was verified on an emulator or device where behavior depends on the Android framework (notifications, alarms, permissions), or you stated clearly that it was not.

## Decided

- App name "Dose", applicationId and package `ca.alexlittle.dose`
- Hilt for DI
- Room 2.x (not Room 3)
- Auto Backup off
- detekt 2.0.0-alpha until 2.0 stable ships, then upgrade

## Open decisions (ask the owner before assuming)

- `USE_EXACT_ALARM` vs `SCHEDULE_EXACT_ALARM` strategy
- Licensing (suggest GPLv3 or Apache-2.0) and distribution (F-Droid / Play Store)
