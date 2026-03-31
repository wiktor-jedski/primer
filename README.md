# Primer

A personal Android app that primes your day for productivity each morning.

## What It Does

Each time you open Primer, it displays:

- **Affirmation** — a personalized motivational text you've written
- **Journal sections** — relevant excerpts from yesterday's GitHub journal file, extracted by configurable markers
- **Daily habits** — a static task list for the day
- **Strategies** — action items for current challenges

A daily notification reminds you to open the app at a time you choose.

## Setup

1. Open **Settings** in the app
2. Enter your GitHub repo (e.g. `username/journal`)
3. Enter the branch name (e.g. `main`)
4. Enter a GitHub Personal Access Token with repo read access
5. Add journal markers matching your journal file headers (e.g. `what I want to do next day:`)
6. Write your affirmation, habits, and strategies
7. Set your daily notification time
8. Return to the main screen — journal data is fetched fresh each launch

### Journal File Format

The app fetches `YYYY-MM-DD.txt` (yesterday's date) from your configured GitHub repo. It extracts sections that start at a configured marker and end at the next marker or end of file.

## Building

Requirements: Android SDK 33+, Java 17

```bash
./gradlew assembleDebug    # build APK
./gradlew installDebug     # install on device/emulator
./gradlew testDebug        # run unit tests
```

Minimum Android version: API 33 (Android 13)

## Tech Stack

- Kotlin + Jetpack Compose (Material3)
- OkHttp for GitHub API calls
- AndroidX Security/Crypto for encrypted PAT storage
- AlarmManager for reliable daily notifications
- MVVM with StateFlow, Repository pattern

## License

See [LICENSE](LICENSE).
