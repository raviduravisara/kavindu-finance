# Kavindu Finance

An Android personal finance app built for one person: **Kavindu Silva**, a 25-year-old junior software engineer in Colombo. He earned over LKR 1.6 million in 12 months from four irregular sources (salary, freelance work, AdSense in USD and crypto) but saved only LKR 11,200. He wants LKR 490,000 for a MacBook within 12 months.

The app shows him what he really earns, where his money goes and what his goal needs each pay cycle. It is built around his habits: entry is fast, recurring bills are added for him, nothing turns red or shames him, and missing a few days never makes the app look broken.

SE3092 Assignment 01 · Kotlin · Jetpack Compose · Material 3 · Firebase Authentication · Cloud Firestore

## Features

| Feature | What Kavindu can do |
| --- | --- |
| Login and register | Email and password with Firebase Authentication; his data follows him to any device |
| Add income | Salary, Freelance, AdSense or Other, in LKR, USD or USDT. For USD and USDT he also enters the LKR that reached his bank; the rate is worked out once and stored, so a later rate never changes past income |
| Quick-add expense | Amount, category, save: 3 taps from the dashboard |
| Committed vs discretionary | Every category carries the flag, so spending splits into "must pay" and "chose to spend" |
| Recurring items | Rent, gym and subscriptions are added each cycle; utilities are added as an estimate to confirm |
| Savings goal | Saved so far, needed this cycle, projected finish and "What changed your date" |
| Dashboard | This cycle's income, spending, the two-bucket split and goal progress, updating live |
| Transaction history | See, edit and delete any entry |
| Pay-cycle setting | "My month starts on day ...", default 25, limited to 1-28 |
| Freelance projects | Milestones go Pending → Received; marking one received creates the linked income |
| Crypto card | Money moved into and out of Binance, net position and realised profit (never saved as income, so never counted twice) |
| Weekly check-in | He enters his bank balance and wallet cash once a week; any gap becomes one "unrecorded spending" entry |
| Safe-to-spend and goal impact | One number he can spend this cycle, and how many days a purchase moves his goal date |

## Tech stack

| Area | Choice |
| --- | --- |
| Language and build | Kotlin 2.4.21, Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), AGP 9.4.1 |
| UI | Jetpack Compose, Material 3 (own light and dark colour schemes, no dynamic colour) |
| Architecture | MVVM, single activity, Navigation Compose with type-safe `@Serializable` routes |
| Dependency injection | Hilt, with KSP |
| Cloud | Firebase Authentication (email and password), Cloud Firestore (source of truth) |
| Local database | Room (offline copy the screens read from) |
| Async | Kotlin coroutines, `StateFlow`, `kotlinx-coroutines-play-services` for `.await()` on Firebase tasks |
| Tests | JUnit unit tests for all money, date and goal calculations |
| Devices | minSdk 26 (Android 8.0), targetSdk 37 |

## Architecture

Every screen follows the same MVVM path, and nothing skips a layer:

```
Screen (Compose)  →  ViewModel (one StateFlow<UiState>)  →  Repository  →  Firestore / Room
```

How one save travels through the app:

1. The user taps Save. The screen calls an event on its ViewModel, for example `onSaveClicked()`.
2. The ViewModel checks the input with a plain Kotlin validator and calls the repository inside `viewModelScope`.
3. The repository writes to **Firestore**. Firestore keeps the write on the phone at once, so saving also works offline.
4. A Firestore **real-time listener** (`core/data/remote/FirestoreSync.kt`) receives the change and copies it into **Room**.
5. Room returns a `Flow`, the ViewModel turns it into its `StateFlow`, and the screen updates by itself, on this phone and on any other signed-in device.

Design rules the code follows:

- **Firestore is the source of truth, Room is the local mirror.** Screens read only from Room, so they open instantly and never go blank without a network. Room is cleared on logout.
- **Money is `Long` cents**, never `Double` (LKR 1,250.50 = `125050`), so totals never drift.
- **The month is a pay cycle** (25th to 24th by default), not a calendar month.
- **All calculations run on the phone** in plain Kotlin classes with unit tests. There are no Cloud Functions, so the project stays on the free Spark plan.
- Every screen has loading, error (with retry) and empty states, using the shared components in `core/ui/components`.

### Project structure

```
app/src/main/java/com/fintrack/kavindu/
├── core/
│   ├── data/local/        Room database, entities, DAOs
│   ├── data/remote/       Firestore paths, mappers, real-time sync
│   ├── data/repository/   Repository interfaces and implementations
│   ├── di/                Hilt modules
│   ├── model/             Shared data classes and enums
│   ├── ui/                Theme and shared components
│   └── util/              Money and PayCycle
├── feature/               auth, settings, balancecheck, income, freelance, crypto, goal,
│                          expense, recurring, dashboard (each with ui/, viewmodel/, data/)
└── navigation/            Type-safe routes and the navigation graph
```

### Firestore data

All of a user's data lives under their own document, `users/{uid}`, with the sub-collections `incomes`, `expenses`, `categories`, `recurringItems`, `goals`, `freelanceProjects`, `milestones`, `cryptoTransfers` and `balanceChecks`.

The security rules (`firestore.rules`) let a signed-in user read and write only their own `users/{uid}/...` data. Every save is also checked: exact fields, whole-number cents, a valid currency and date, and the data rules (for example, a recurring entry must use its fixed ID such as `rent_2026-10-25`, so it can never be added twice).

## Firebase configuration

The repository already contains `app/google-services.json` for the team's Firebase project, so a team member can build straight away. To run the app against your own Firebase project instead:

1. Create a project in the [Firebase console](https://console.firebase.google.com/) (the free Spark plan is enough).
2. Add an Android app with the package name **`com.fintrack.kavindu`**.
3. Download its `google-services.json` and put it in the `app/` folder, replacing the existing one.
4. In **Authentication → Sign-in method**, enable **Email/Password**.
5. In **Firestore Database**, create a database (production mode; we use `asia-south1`, Mumbai).
6. Deploy the security rules and indexes from the repository with the [Firebase CLI](https://firebase.google.com/docs/cli):

   ```
   firebase login
   firebase use --add          (choose your project)
   firebase deploy --only firestore:rules,firestore:indexes
   ```

   Without this step the database denies everything, and registering fails.

## Build and run

**Requirements:** Android Studio (latest stable), a full **JDK 25** (for example Temurin 25), and an emulator or phone with Android 8.0 or newer.

> `JAVA_HOME` (and Android Studio's *Gradle JDK* setting) must point to a full JDK. A Java runtime without `jlink`, such as the one bundled with VS Code's Java extension, fails with "jlink executable ... does not exist".

1. Clone the repository and open the folder in Android Studio. Let Gradle sync.
2. Start an emulator or connect a phone.
3. Press **Run**, or from a terminal in the project folder:

   | Task | Windows | macOS / Linux |
   | --- | --- | --- |
   | Build a debug APK | `.\gradlew.bat assembleDebug` | `./gradlew assembleDebug` |
   | Install on the running device | `.\gradlew.bat installDebug` | `./gradlew installDebug` |
   | Run the unit tests | `.\gradlew.bat testDebugUnitTest` | `./gradlew testDebugUnitTest` |

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Release APK

The release build is signed only when these four lines are in `local.properties`, which is never committed:

```
release.storeFile=C:/path/to/your-release-key.jks
release.storePassword=...
release.keyAlias=...
release.keyPassword=...
```

Then run `.\gradlew.bat assembleRelease`. The APK is written to `app/build/outputs/apk/release/app-release.apk`. Keystore files (`*.jks`, `*.keystore`) are ignored by git.

## Team

| Member | Area |
| --- | --- |
| Ravidu Ravisara | Project setup and architecture, login and register, Room and Firestore sync, security rules, settings, weekly check-in |
| Pinithi Ransara | Income, freelance projects, crypto, savings goal and the money calculations |
| Sahan Jayathilake | Quick-add expense, categories, recurring items, transaction history, dashboard |
