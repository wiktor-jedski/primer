# Primer Android App — Implementation Plan

## Context

Building a personal daily priming app for Android. The app fetches the previous day's GitHub journal entry, extracts configurable sections, and combines them with locally-stored affirmation, habits, and strategies into one focused morning screen. An `AlarmManager`-based notification fires daily at a configured time to open the app.

Starting from a completely empty Android project (minSdk 33, compileSdk 36, AGP 9.1.0, no activities, no Compose). Full spec in `spec.md`.

**Coverage goal: 100% line coverage.** Automatic tests cover all logic reachable without a physical device. Manual tests cover what requires a real device (hardware alarms, encrypted key store, notification shade, actual GitHub network).

---

## Phase 1 — Project Infrastructure ✓ DONE

### 1.1 Update `gradle/libs.versions.toml`

Add to `[versions]`:
```toml
kotlin = "2.1.20"
compose-bom = "2025.04.00"
activity-compose = "1.10.1"
lifecycle = "2.9.0"
navigation-compose = "2.9.0"
okhttp = "4.12.0"
security-crypto = "1.1.0-alpha06"
gson = "2.11.0"
coroutines = "1.10.1"
# test
coroutines-test = "1.10.1"
robolectric = "4.14.1"
mockwebserver = "4.12.0"
```

Add to `[libraries]`:
```toml
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
# test
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines-test" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

Add to `[plugins]`:
```toml
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 1.2 Update `app/build.gradle.kts`

- Add plugins: `kotlin-android`, `compose-compiler`
- Add `buildFeatures { compose = true }` inside `android {}`
- Change `compileOptions` to `VERSION_17`; add `kotlinOptions { jvmTarget = "17" }`
- Add all new deps via platform BOM + individual entries; keep existing core-ktx/appcompat/material
- Add `debugImplementation(libs.compose.ui.tooling)` and `debugImplementation(libs.compose.ui.test.manifest)`
- Add test deps:
  ```kotlin
  testImplementation(libs.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.mockwebserver)
  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.ui.test.junit4)
  ```
- Add `testOptions { unitTests { isIncludeAndroidResources = true } }` inside `android {}` (needed for Robolectric)

### 1.3 Update `app/src/main/AndroidManifest.xml`

```xml
<!-- Permissions (before <application>) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Application: add android:name=".PrimerApplication" -->

<!-- Activity -->
<activity android:name=".MainActivity" android:exported="true"
    android:theme="@style/Theme.Primer">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- BroadcastReceiver -->
<receiver android:name=".notification.AlarmReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
    <intent-filter>
        <action android:name="com.example.primer.ACTION_ALARM_FIRE" />
    </intent-filter>
</receiver>
```

Use `USE_EXACT_ALARM` (not `SCHEDULE_EXACT_ALARM`): auto-granted at install on API 33+, no user settings page required.

### 1.4 Replace `res/values/themes.xml`

Use Material3 `Theme.Material3.Dark.NoActionBar` as parent. `NoActionBar` required to avoid conflict with Compose `TopAppBar`.

### Testing — Phase 1

**Automatic:** None — this phase contains only configuration files with no executable logic.

**Manual (you):**
- Run `./gradlew assembleDebug` — must succeed with zero errors or warnings about missing config
- Install APK on device (`./gradlew installDebug`) — app icon appears in launcher and tapping it does not crash (even with a blank screen, since no Activity is wired yet)

---

## Phase 2 — Data Layer ✓ DONE

### Files to create:

**`data/model/AppSettings.kt`**
```kotlin
data class AppSettings(
    val githubRepo: String, val githubBranch: String,
    val journalMarkers: List<String>, val affirmation: String,
    val habits: List<String>, val strategies: List<String>,
    val notificationHour: Int, val notificationMinute: Int
)
```

**`data/model/JournalSection.kt`**
```kotlin
data class JournalSection(val title: String, val items: List<String>)
```

**`data/SettingsRepository.kt`**
- Opens `SharedPreferences("primer_prefs")` for all non-sensitive data
- Opens `EncryptedSharedPreferences("primer_secure_prefs")` for PAT only
- `EncryptedSharedPreferences` must be accessed on `Dispatchers.IO`
- Uses `Gson` for list serialization: `Gson().toJson(list)` / `Gson().fromJson(json, Array<String>::class.java).toList()`
- Methods: `loadSettings(): AppSettings`, `loadPat(): String`, and individual `save*()` setters

```kotlin
// EncryptedSharedPreferences setup:
val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
EncryptedSharedPreferences.create(context, "primer_secure_prefs", masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
```

**`data/GitHubRepository.kt`**
- `suspend fun fetchJournalSections(markers: List<String>): Result<List<JournalSection>>`
- Date: `LocalDate.now().minusDays(1).toString()` → `YYYY-MM-DD`
- URL: `https://api.github.com/repos/{owner}/{repo}/contents/{date}.txt?ref={branch}`
- Header: `Authorization: token {pat}`
- Parse JSON with `com.google.gson.JsonParser` (Gson, already a dep) — `org.json.JSONObject` is stubbed in unit tests and throws RuntimeException, so Gson is used instead
- Decode: `Base64.decode(content.replace("\n",""), Base64.DEFAULT).toString(Charsets.UTF_8)`
- Parser: iterate lines; when a line matches a marker exactly (trimmed), collect following non-empty lines until blank line/EOF as items
- Returns sections in configured marker order, skips absent markers

### Testing — Phase 2

**Automatic (Claude generates):**

`test/.../data/JournalParserTest.kt` — pure JVM unit test, no Android deps:
- Extract the marker parsing logic into a `internal fun parseJournal(text: String, markers: List<String>): List<JournalSection>` top-level function (or companion object) so it's directly testable without OkHttp
- Cases to cover:
  - Single marker, multiple items → correct section produced
  - Multiple markers in order → sections returned in marker-list order, not file order
  - Marker present in file but not in configured list → ignored
  - Marker in configured list but absent from file → skipped (not in result)
  - Items end at EOF (no trailing blank line) → still captured
  - Multiple consecutive blank lines between sections → handled
  - Empty file → empty result
  - Marker appears but has zero items before next blank → section omitted or section with empty list (define behavior explicitly)
  - Whitespace-only lines between items → treated as blank (ends section)
  - Marker line with leading/trailing spaces in file → trimmed before comparison

`test/.../data/GitHubRepositoryTest.kt` — JVM unit test using `MockWebServer`:
- 200 response with valid Base64-encoded file content → returns `Result.success` with parsed sections
- 404 response → returns `Result.failure`
- Network timeout / `IOException` → returns `Result.failure`
- Response with `content` field containing newline characters in the Base64 string (GitHub API wraps at 60 chars) → still decoded correctly (`replace("\n","")`)
- PAT included in `Authorization` header → assert header value on captured request
- Correct URL path constructed from repo/branch/date

`test/.../data/SettingsRepositoryTest.kt` — Robolectric (`@RunWith(RobolectricTestRunner::class)`):
- Note: `EncryptedSharedPreferences` cannot be tested with Robolectric (requires real Android Keystore hardware/emulator). Split `SettingsRepository` so that plain-prefs methods are testable and PAT methods are isolated behind an interface.
- `loadSettings()` on fresh prefs → returns correct defaults (empty strings, empty lists, default time 8:00)
- Round-trip for each save method: `saveAffirmation("x")` → `loadSettings().affirmation == "x"`
- List round-trip: `saveMarkers(listOf("a","b"))` → `loadSettings().journalMarkers == listOf("a","b")`
- Empty list saved → loads as empty list (not null crash)
- Overwrite: save value twice → second value wins

**Manual (you):** *(requires Settings Screen — moved to Phase 5)*

---

## Phase 3 — Notifications ✓ DONE

**`notification/NotificationHelper.kt`**
- `CHANNEL_ID = "primer_daily"`
- `fun createChannel(context)` — idempotent, call from Application.onCreate()
- `fun postNotification(context)` — `NotificationCompat` with title "Time to prime your day", `PendingIntent` to `MainActivity`, `FLAG_IMMUTABLE`

**`notification/AlarmReceiver.kt`** — `BroadcastReceiver`:
- `ACTION_ALARM_FIRE` → post notification + call `AlarmScheduler.scheduleNext()` (re-arms for next day)
- `BOOT_COMPLETED` → call `AlarmScheduler.scheduleNext()`

**`notification/AlarmScheduler.kt`**
- `fun scheduleNext(context)`: compute next fire time (today at H:M, or tomorrow if already past); use `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)`; PendingIntent targets `AlarmReceiver` with `ACTION_ALARM_FIRE`, `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`
- `fun cancel(context)`: cancel via matching PendingIntent
- Manual re-arm (not `setRepeating`) for reliability

### Testing — Phase 3

**Automatic (Claude generates):**

`test/.../notification/NextAlarmTimeTest.kt` — pure JVM unit test:
- Extract the "compute next fire time" logic into `internal fun computeNextAlarmMillis(nowMillis: Long, hour: Int, minute: Int): Long` so it has no Android dep
- Cases:
  - Configured time is 2 hours from now → result is today at that time
  - Configured time was 1 minute ago → result is tomorrow at that time
  - Configured time is exactly now (same minute) → result is tomorrow (don't fire twice)
  - Midnight edge case: configured 00:00, current time 23:59 → fires tomorrow at 00:00
  - Configured 23:59, current 00:00 → fires today at 23:59

`test/.../notification/AlarmSchedulerTest.kt` — Robolectric:
- `scheduleNext()` with a saved notification time → `ShadowAlarmManager` has exactly one pending alarm with the correct trigger time
- `cancel()` after `scheduleNext()` → `ShadowAlarmManager` has no pending alarms
- Calling `scheduleNext()` twice → only one alarm registered (FLAG_UPDATE_CURRENT replaces prior)

`test/.../notification/AlarmReceiverTest.kt` — Robolectric:
- Send broadcast with `ACTION_ALARM_FIRE` → `ShadowNotificationManager` has one notification posted + a new alarm is scheduled
- Send broadcast with `BOOT_COMPLETED` → no notification posted, but alarm is scheduled

`test/.../notification/NotificationHelperTest.kt` — Robolectric:
- `createChannel()` called once → `ShadowNotificationManager` has one channel with id `"primer_daily"`
- `createChannel()` called twice → still only one channel (idempotency)
- `postNotification()` → notification exists in `ShadowNotificationManager` with correct title

**Manual (you):** *(requires Settings Screen — moved to Phase 5)*

---

## Phase 4 — Main Screen ✓ DONE

**`ui/main/MainViewModel.kt`** — `AndroidViewModel`:
```kotlin
sealed class MainUiState {
    object Loading : MainUiState()
    data class Ready(val settings: AppSettings, val journalSections: List<JournalSection>, val journalAvailable: Boolean) : MainUiState()
}
```
- `init` launches `viewModelScope` on `IO`: load settings → fetch journal → emit `Ready`
- `journalAvailable = false` when fetch fails (no separate error state needed)
- `fun refresh()` re-runs fetch (called when returning from Settings)

**`ui/main/MainScreen.kt`**
- `LazyColumn` with: Affirmation (headlineLarge) → Journal sections (each: title + items) → Habits section → Strategies section
- Journal sections only rendered when `journalAvailable = true`
- Toast on `journalAvailable = false`:
  ```kotlin
  LaunchedEffect(uiState) {
      if (uiState is Ready && !uiState.journalAvailable)
          Toast.makeText(context, "Missing journal entry", Toast.LENGTH_SHORT).show()
  }
  ```
- `TopAppBar` with "Primer" title + overflow `DropdownMenu` with "Settings" item
- `CircularProgressIndicator` while Loading

### Testing — Phase 4

**Automatic (Claude generates):**

`test/.../ui/main/MainViewModelTest.kt` — JVM unit test using `kotlinx-coroutines-test`:
- Use `TestCoroutineScheduler` + `UnconfinedTestDispatcher` and inject dispatcher into ViewModel constructor (add `dispatcher: CoroutineDispatcher = Dispatchers.IO` param)
- Use fake implementations of `SettingsRepository` and `GitHubRepository` (interfaces or open classes with overridable methods) instead of mocking framework
- Cases:
  - Successful fetch → state transitions `Loading → Ready(journalAvailable=true)` with correct sections
  - Failed fetch (repo returns `Result.failure`) → `Ready(journalAvailable=false)`, settings still populated
  - `refresh()` called → triggers another fetch, state goes back to `Loading` then `Ready`
  - `refresh()` called while still loading (previous job) → previous job cancelled, new fetch starts
  - Empty markers list → fetch still called, result is empty sections list

`androidTest/.../ui/main/MainScreenTest.kt` — Compose UI test (`@get:Rule val composeTestRule = createComposeRule()`):
- Render `MainScreen` with fake ViewModel in `Loading` state → `CircularProgressIndicator` is displayed, affirmation text is not
- Render with `Ready(journalAvailable=true, ...)` → affirmation text visible, journal section headers visible, habits visible, strategies visible
- Render with `Ready(journalAvailable=false, ...)` → journal sections not displayed; affirmation/habits/strategies still visible
- Overflow menu: click "⋮" → "Settings" menu item appears; click it → `onNavigateToSettings` lambda is invoked (use a flag to assert)
- Affirmation text matches the value in `AppSettings`
- Each habit item text appears in the list
- Each strategy item text appears in the list

**Manual (you):** *(requires fully wired app — moved to Phase 6)*

---

## Phase 5 — Settings Screen ✓ DONE

**`ui/settings/SettingsViewModel.kt`** — `AndroidViewModel`:
- `MutableStateFlow` per settings group
- `init` loads from `SettingsRepository` on IO
- Save methods for each field; notification time change also calls `AlarmScheduler.scheduleNext()`
- List mutation methods: `addMarker/removeMarker/moveMarkerUp/moveMarkerDown`, same for habits; `addStrategy/removeStrategy`

**`ui/settings/SettingsScreen.kt`** — `LazyColumn` with sections:
1. **GitHub** — `OutlinedTextField` for repo, branch; PAT with `PasswordVisualTransformation`; save on focus lost
2. **Journal Markers** — `Column` with each marker row: text + ↑↓ buttons + delete; "Add" row at bottom
3. **Affirmation** — single multiline `OutlinedTextField`, save on focus lost
4. **Daily Habits** — same reorderable list pattern as markers
5. **Strategies** — list with add/delete only (no reorder needed)
6. **Notification** — text showing "HH:MM" + "Change" button opening `TimePickerDialog` wrapped in `Dialog()`

### Testing — Phase 5

**Automatic (Claude generates):**

`test/.../ui/settings/SettingsViewModelTest.kt` — JVM unit test with `TestCoroutineScheduler`:
- `addMarker("x")` → markers list contains `"x"`
- `addMarker` with duplicate value → list contains both (no dedup, user's responsibility)
- `removeMarker(0)` on single-item list → empty list
- `removeMarker(1)` on two-item list → only first item remains
- `moveMarkerUp(0)` → no-op (already at top), list unchanged
- `moveMarkerDown(last)` → no-op (already at bottom), list unchanged
- `moveMarkerUp(1)` on `[a, b, c]` → `[b, a, c]`
- `moveMarkerDown(1)` on `[a, b, c]` → `[a, c, b]`
- Same boundary/swap tests for `addHabit/removeHabit/moveHabitUp/moveHabitDown`
- `addStrategy/removeStrategy` basic cases
- `saveNotificationTime(8, 30)` → `notificationHour == 8 && notificationMinute == 30` in state; `AlarmScheduler.scheduleNext()` was called (use a fake `AlarmScheduler` with a call-count flag)
- `init` loads persisted values from fake `SettingsRepository` and emits them to StateFlow

`androidTest/.../ui/settings/SettingsScreenTest.kt` — Compose UI test:
- Repo field displays the value from ViewModel state
- PAT field shows masked characters (not plaintext)
- Typing in affirmation field and losing focus → ViewModel save method called
- "Add" marker row: type text → tap confirm → marker appears in the list
- Delete button on first marker → marker removed from list
- ↑ button on second marker → it moves above the first
- Notification row shows "HH:MM" formatted correctly

**Manual (you):**
- On device, open settings → enter PAT → kill app → reopen → PAT field shows masked value (not blank, not plaintext) — confirms `EncryptedSharedPreferences` round-trip works on real Keystore *(from Phase 2)*
- Set notification time to 2 minutes from now → lock phone → notification "Time to prime your day" appears at the correct time → tap it → app opens on main screen *(from Phase 3)*
- Reboot the device → wait for the configured notification time → notification still fires (confirms `BOOT_COMPLETED` re-arm) *(from Phase 3)*
- Set notification time to a past time → change it to 2 minutes from now → notification fires at the new time (confirms rescheduling on settings change) *(from Phase 3)*
- Full settings walkthrough: fill in all fields, kill app, reopen → all values persisted
- Add 3 markers, reorder them with ↑↓, delete one → list reflects changes immediately and persists after app restart
- Change notification time via the time picker dialog → confirm new time shows in the settings row

---

## Phase 6 — Wire Together ✓ DONE

**`ui/theme/Color.kt`** — dark-only palette:
```kotlin
val Background = Color(0xFF0D0D0D)
val Surface = Color(0xFF1A1A1A)
val OnBackground = Color(0xFFEEEEEE)
val Accent = Color(0xFF9E9E9E)
```

**`ui/theme/Type.kt`** — `Typography` with `headlineLarge` (affirmation), `titleMedium` (section headers), `bodyMedium` (items)

**`ui/theme/Theme.kt`** — `PrimerTheme` always uses `darkColorScheme(...)`, no dynamic color, no light theme

**`PrimerApplication.kt`** — `Application` subclass:
- `onCreate()` calls `NotificationHelper.createChannel(this)`

**`MainActivity.kt`** — `ComponentActivity`:
- `enableEdgeToEdge()` before `setContent`
- `NavHost` with two routes: `"main"` → `MainScreen`, `"settings"` → `SettingsScreen`
- Navigate to settings: `navController.navigate("settings")`
- On return to main (using `NavBackStackEntry` lifecycle observer on the `"main"` entry reaching `RESUMED`): call `viewModel.refresh()`
- Request `POST_NOTIFICATIONS` runtime permission on first launch via `ActivityResultContracts.RequestPermission()`
- On first launch (SharedPrefs flag `"alarm_initialized"`): call `AlarmScheduler.scheduleNext(this)`

### Testing — Phase 6

**Automatic (Claude generates):**

`androidTest/.../MainActivityTest.kt` — instrumented Compose UI test (`createAndroidComposeRule<MainActivity>()`):
- App launches → main screen is displayed (affirmation text node or loading indicator is present)
- Tap "⋮" → "Settings" → settings screen title or a settings-specific element is displayed
- Press system back from settings → main screen is displayed again
- Covers `MainActivity` navigation wiring and the `NavHost` routes

`test/.../PrimerApplicationTest.kt` — Robolectric:
- `onCreate()` invoked → `ShadowNotificationManager` has the `"primer_daily"` channel registered

**Manual (you — full end-to-end flow):**
- Open app with valid settings → visually verify dark theme, affirmation large at top, journal sections with correct content, habits list, strategies list *(from Phase 4)*
- Disable internet → reopen app → "Missing journal entry" toast appears; rest of the screen shows local data correctly *(from Phase 4)*
- Tap "⋮" → "Settings" → settings screen opens; press back → main screen returns *(from Phase 4)*
- Enter repo/branch/PAT for your actual journal repo → return to main screen → journal sections appear with correct content for yesterday's date *(from Phase 2)*
- Fresh install (no prior data) → open app → loading spinner → "Missing journal entry" toast → local sections show (all empty since no settings yet)
- Go to settings → fill in all fields including valid GitHub PAT, repo, branch, markers, affirmation, one habit, one strategy, notification time 2 minutes from now → back to main → app refreshes and journal sections appear
- Lock screen → wait 2 minutes → notification fires → tap it → app opens correctly on main screen
- Reboot device → wait for notification time → notification still fires

---

## Complete File Tree

```
app/src/
├── main/
│   ├── AndroidManifest.xml                           (modify)
│   └── java/com/example/primer/
│       ├── MainActivity.kt                           (create)
│       ├── PrimerApplication.kt                      (create)
│       ├── data/
│       │   ├── model/
│       │   │   ├── AppSettings.kt                    (create)
│       │   │   └── JournalSection.kt                 (create)
│       │   ├── SettingsRepository.kt                 (create)
│       │   └── GitHubRepository.kt                   (create)
│       ├── ui/
│       │   ├── theme/
│       │   │   ├── Color.kt                          (create)
│       │   │   ├── Type.kt                           (create)
│       │   │   └── Theme.kt                          (create)
│       │   ├── main/
│       │   │   ├── MainScreen.kt                     (create)
│       │   │   └── MainViewModel.kt                  (create)
│       │   └── settings/
│       │       ├── SettingsScreen.kt                 (create)
│       │       └── SettingsViewModel.kt              (create)
│       └── notification/
│           ├── AlarmScheduler.kt                     (create)
│           ├── AlarmReceiver.kt                      (create)
│           └── NotificationHelper.kt                 (create)
├── test/java/com/example/primer/
│   ├── data/
│   │   ├── JournalParserTest.kt                      (create — pure JVM)
│   │   ├── GitHubRepositoryTest.kt                   (create — MockWebServer)
│   │   └── SettingsRepositoryTest.kt                 (create — Robolectric)
│   ├── notification/
│   │   ├── NextAlarmTimeTest.kt                      (create — pure JVM)
│   │   ├── AlarmSchedulerTest.kt                     (create — Robolectric)
│   │   ├── AlarmReceiverTest.kt                      (create — Robolectric)
│   │   └── NotificationHelperTest.kt                 (create — Robolectric)
│   └── ui/
│       ├── main/
│       │   └── MainViewModelTest.kt                  (create — coroutines-test)
│       └── settings/
│           └── SettingsViewModelTest.kt              (create — coroutines-test)
└── androidTest/java/com/example/primer/
    ├── MainActivityTest.kt                           (create — instrumented)
    └── ui/
        ├── main/
        │   └── MainScreenTest.kt                     (create — Compose UI test)
        └── settings/
            └── SettingsScreenTest.kt                 (create — Compose UI test)
```

---

## Critical Files to Modify

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | All new version/library/plugin entries including test deps |
| `app/build.gradle.kts` | Kotlin + Compose plugins, buildFeatures, JVM 17, all new deps, testOptions |
| `app/src/main/AndroidManifest.xml` | Permissions, Application class, Activity, BroadcastReceiver |
| `app/src/main/res/values/themes.xml` | Replace with Material3 Dark NoActionBar |

---

## Test Responsibility Summary

| Test file | Runner | Who |
|-----------|--------|-----|
| `JournalParserTest` | JVM (pure Kotlin) | Claude |
| `NextAlarmTimeTest` | JVM (pure Kotlin) | Claude |
| `GitHubRepositoryTest` | JVM + MockWebServer | Claude |
| `SettingsRepositoryTest` | Robolectric | Claude |
| `AlarmSchedulerTest` | Robolectric | Claude |
| `AlarmReceiverTest` | Robolectric | Claude |
| `NotificationHelperTest` | Robolectric | Claude |
| `MainViewModelTest` | JVM + coroutines-test | Claude |
| `SettingsViewModelTest` | JVM + coroutines-test | Claude |
| `MainScreenTest` | Compose UI (instrumented) | Claude |
| `SettingsScreenTest` | Compose UI (instrumented) | Claude |
| `MainActivityTest` | Instrumented (real device/emulator) | Claude |
| PAT encryption round-trip | Real device | You |
| Notification fires at set time | Real device | You |
| Boot persistence | Real device | You |
| Full settings persistence | Real device | You |
| End-to-end GitHub fetch | Real device + network | You |
| Visual dark theme check | Real device | You |
