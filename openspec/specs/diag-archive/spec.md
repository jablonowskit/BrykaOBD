# diag-archive

## Purpose

Defines diagnostic observability for ELM/OBD sessions so Aveo and clone-adapter issues can be diagnosed after a drive: in-memory UI log, Android Logcat tag, durable per-session files on device storage, listing/preview, and share/export. Logcat alone is not a durable archive.

## Requirements

### Requirement: Session diagnostic events

While a Demo or live ELM session runs, the system SHALL append diagnostic entries covering at least: session lifecycle, AT steps, raw TX commands, raw RX (truncated) replies, PID results or errors, and Bluetooth connect notes when applicable.

#### Scenario: Demo produces TX and PID lines

- GIVEN Demo PID is started
- WHEN init and the first dashboard poll complete
- THEN the Diagnostyka panel shows entries with categories such as `TX`, `RX`, `AT`, and `PID`

### Requirement: Logcat mirror on Android

On Android each diagnostic append MUST also be written to Logcat under the tag `BrykaOBD` so USB debugging can follow a live session. Agents and docs MUST treat Logcat as ephemeral, not as the post-drive source of truth.

#### Scenario: Filter live logs

- GIVEN a running session on a USB-debuggable phone
- WHEN an operator runs `adb logcat -s BrykaOBD`
- THEN session diagnostic lines appear in the stream

### Requirement: Durable session files

When a Demo or live session starts, the system MUST create a new text session file under the app diag directory (Android: app-specific `files/diag`, typically under `Android/data/app.brykaobd/files/diag/`) and append each diagnostic line for the full session, even if the in-memory UI ring buffer drops older lines.

#### Scenario: File survives app restart

- GIVEN a session that wrote lines to a `brykaobd_*.txt` file
- WHEN the user force-stops or relaunches the app
- THEN Zapisane sesje still lists that file and can show its contents

### Requirement: Saved sessions UI and share

The UI MUST provide Zapisane sesje to list persisted files, preview content, and on Android offer share via the system share sheet (FileProvider). Clearing the in-memory Diagnostyka panel MUST NOT delete already written session files.

#### Scenario: Share session file

- GIVEN at least one saved session file on Android
- WHEN the user chooses Udostępnij for that file
- THEN the system share sheet opens with the session text file

### Requirement: Session retention bound

The archive MUST prune old session files beyond a configured maximum (currently on the order of ~40 files) so long Aveo testing does not unbounded-fill device storage.

#### Scenario: Prune oldest

- GIVEN more than the max allowed session files
- WHEN a new session file is created
- THEN the oldest excess `.txt` session files are deleted
