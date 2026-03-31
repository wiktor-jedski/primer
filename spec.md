# Primer — App Specification

## Overview

A personal Android app (for exclusive personal use) that primes you for the day. It fetches the previous day's journal entry from a private GitHub repo, extracts configurable sections, and combines them with locally-stored affirmation, habits, and strategies into a single focused morning screen.

**Platform:** Android (minSdk 33)
**Stack:** Kotlin + Jetpack Compose
**Theme:** Dark, minimal

---

## Core Screens

### 1. Main Screen (Daily Primer)

Single scrollable screen. Sections appear top to bottom in this order:

1. **Affirmation** — large text, set in Settings, shown prominently at the top
2. **Journal Sections** — extracted from yesterday's GitHub journal file; each configured marker becomes a titled section with its items listed below; sections appear in the order defined in Settings
3. **Daily Habits** — static list from Settings, display-only (no checkboxes)
4. **Strategies** — list of items from Settings (for current issues being dealt with)

**Behavior on open:**
- App attempts to fetch yesterday's journal file (`YYYY-MM-DD.txt` where the date is the previous calendar day)
- If online and file exists: parse and display journal sections
- If offline or file missing/unreachable: show a `"Missing journal entry"` toast and skip journal sections entirely (affirmation, habits, and strategies still show normally)

### 2. Settings Screen

Accessible via overflow/options menu from the main screen. Contains:

#### GitHub
- **Repository** — owner/name (e.g. `username/journal`)
- **Branch** — branch to read from (e.g. `main`)
- **Personal Access Token** — stored securely using `EncryptedSharedPreferences`

#### Journal Markers
- Configurable list of section headers to extract from journal files
- Each entry is the exact header text as it appears in the file (e.g. `what I want to do next day:`, `microsuck:`)
- Add, remove, reorder markers
- Extraction rule: everything from the marker line until the next blank line (or EOF) is captured as that section's content, split into individual items by newline

#### Affirmation
- Single text input field; saved persistently; shown every day until changed

#### Daily Habits
- Ordered list of habit items
- Add, remove, reorder (drag handle)

#### Strategies
- List of strategy items (for issues currently being dealt with)
- Add, remove (no specific order requirement)

#### Notification
- Time picker for daily notification time

---

## Notifications

- **Schedule:** One daily notification at the user-configured time, repeating every day
- **Content:** Generic — `"Time to prime your day"` (title), no body text needed
- **Action:** Tapping opens the Main Screen

---

## GitHub Journal File Format

**Filename:** `YYYY-MM-DD.txt` (previous calendar day)
**Format:** Plain text with custom section markers

### Marker extraction rules:
- A marker is a line that exactly matches a configured header string (case-sensitive, trimmed)
- All non-empty lines following the marker (until the next blank line or EOF) are the section's items
- Items are stored as an ordered list and displayed as such in the app

### Example file:
```
what I want to do next day:
go to the gym
dance
build a tower

microsuck:
clean the mess
```

With markers `what I want to do next day:` and `microsuck:` configured, the app extracts:

- **what I want to do next day:** → `go to the gym`, `dance`, `build a tower`
- **microsuck:** → `clean the mess`

---

## Data Storage

| Data | Storage |
|------|---------|
| GitHub PAT | `EncryptedSharedPreferences` |
| GitHub repo, branch | `SharedPreferences` |
| Affirmation | `SharedPreferences` |
| Habits (ordered list) | `SharedPreferences` (JSON array) |
| Strategies (list) | `SharedPreferences` (JSON array) |
| Journal markers (ordered list) | `SharedPreferences` (JSON array) |
| Notification time | `SharedPreferences` |
| Journal data | **Not cached** — fetched fresh each time; skipped if unavailable |

---

## GitHub API Integration

- Use GitHub REST API (`https://api.github.com/repos/{owner}/{repo}/contents/{path}`) with `Authorization: token {PAT}` header
- File content is Base64-encoded in the API response — decode before parsing
- HTTP client: OkHttp or Ktor (to be decided at implementation time)

---

## Permissions Required

- `INTERNET` — for GitHub API calls
- `POST_NOTIFICATIONS` — for daily notification (Android 13+, runtime permission)
- `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` — for reliable daily notification scheduling
- `RECEIVE_BOOT_COMPLETED` — to re-register notification alarm after device reboot

---

## Architecture

- **UI:** Jetpack Compose
- **State management:** ViewModel + StateFlow
- **Settings persistence:** `SharedPreferences` (wrapped in a repository class)
- **Notifications:** `AlarmManager` + `BroadcastReceiver` → `NotificationManager`
- **Networking:** Executed in a coroutine (IO dispatcher) on app launch

---

## Non-Goals

- No habit completion tracking or streaks
- No writing back to GitHub
- No date header on the main screen
- No motivational quotes or extra elements
- No multi-user support
- No cloud backup of settings
