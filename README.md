# Hakeem Care — Expert App (Doctor)

**Overall: 5/10 | Grade: C | Last audit: 2026-05-27**

> Android doctor app for the Hakeem Care telehealth platform. Used by healthcare providers to manage consultations, communicate with patients, track wallet/revenue, and manage their availability.

---

## Health Summary

| Dimension | Score | Grade |
|---|---|---|
| Auth & Access Control | 7/10 | B |
| Database & Data Resilience | 5/10 | C |
| API Quality & Security | 5/10 | C |
| Security Practices | 9/10 | A |
| Code Quality & DX | 5/10 | C |
| Testing & Validation | 3/10 | F |
| DevOps & Deployment | 2/10 | F |
| Frontend Quality | 6/10 | C |
| Project Structure & Conventions | 6/10 | C |
| **Overall** | **5/10** | **C** |

> Full health report: `../../health-report.md`

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 2.1.10 | Main language |
| Android SDK | compileSdk 36, minSdk 26 | Platform |
| Dagger 2 | 2.55 | Dependency injection |
| Retrofit2 | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | HTTP engine + interceptors |
| Kotlin Coroutines | 1.10.1 | Async |
| Agora SDK | 4.6.3 | Video/audio calls |
| Jitsi Meet | 11.5.1 | Video calls (alternative) |
| Firebase Messaging | 24.1.0 | Push notifications |
| Firebase Crashlytics | 19.4.1 | Crash reporting |
| Timber | 5.0.1 | Logging (release-safe) |
| EncryptedSharedPreferences | 1.1.0-alpha06 | Secure token storage |
| Lottie | 6.6.2 | Animations |
| Glide | 5.0.5 | Image loading |
| Google Maps + Places | — | Location features |

---

## Prerequisites

- Android Studio (latest stable)
- JDK 17+
- `local.properties` at the repo root with the following keys:

```properties
# API config (obtain from team vault)
APP_UNIQUE_ID=<value>
BASE_URL=<value>

# Signing credentials (obtain from team vault)
KEY_STORE_FILE=consultapp.jks
KEY_ALIAS=<alias>
KEY_PASSWORD=<password>
STORE_PASSWORD=<password>
```

> `local.properties` is gitignored — never commit it.

---

## Getting Started

```bash
# 1. Clone the repo
git clone <repo-url>

# 2. Create local.properties at the repo root (see Prerequisites above)

# 3. Sync Gradle in Android Studio, or build from terminal:
./gradlew assembleConsultDebug

# 4. Run on device or emulator (minSdk 26 / Android 8.0+)
```

---

## Project Structure

```
app/src/main/java/com/consultantvendor/
├── ConsultantApplication.kt         ← App entry point + Timber setup
├── data/
│   ├── apis/WebService.kt           ← Retrofit interface (98 endpoints)
│   ├── models/                      ← API request/response models
│   ├── network/                     ← Config, interceptors, response utils
│   └── repos/UserRepository.kt      ← Single data repository
├── di/                              ← Dagger 2 modules
│   ├── AppComponent.kt
│   ├── AppModule.kt
│   └── NetworkModule.kt
├── ui/
│   ├── loginSignUp/                 ← Auth flows (login, OTP, signup, social)
│   ├── dashboard/
│   │   ├── home/                    ← Consultation queue + appointment management
│   │   │   └── appointment/
│   │   │       └── patientfile/     ← Patient file view (RTL-fixed 2026-05-27)
│   │   ├── feeds/                   ← Content feeds
│   │   ├── settings/                ← App settings + contact list
│   │   ├── location/                ← Location
│   │   ├── language/                ← Language selection
│   │   ├── wallet/                  ← Earnings wallet
│   │   └── revenue/                 ← Revenue tracking
│   ├── calling/                     ← Call entry point
│   ├── aghora/                      ← Agora SDK integration
│   ├── jitsimeet/                   ← Jitsi Meet integration
│   ├── chat/                        ← Patient chat
│   ├── drawermenu/                  ← Navigation drawer
│   ├── webview/                     ← In-app web views
│   └── walkthrough/                 ← Onboarding
├── pushNotifications/               ← FCM messaging service
└── utils/                           ← Base classes, dialogs, general helpers
```

---

## App Details

| Property | Value |
|---|---|
| Application ID | `com.hakeem.expert` |
| Version | 2.3.6 (build 135) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |
| Source files (Kotlin) | 262 |
| Screens (Fragment + Activity) | 83 |
| API endpoints | 98 |
| LOC | ~38,244 |

---

## Available Scripts

| Command | Description |
|---|---|
| `./gradlew assembleConsultDebug` | Build debug APK |
| `./gradlew assembleConsultRelease` | Build release APK |
| `./gradlew test` | Run unit tests |
| `./gradlew lint` | Run lint checks |
| `./gradlew connectedAndroidTest` | Run instrumentation tests (requires device/emulator) |

---

## Environment Variables

All secrets are managed via `local.properties` (gitignored — never committed).

| Variable | Description | Required |
|---|---|---|
| `APP_UNIQUE_ID` | App identifier for API auth | ✅ Required |
| `BASE_URL` | API base URL | ✅ Required |
| `KEY_STORE_FILE` | Path to signing keystore file | ✅ Required |
| `KEY_ALIAS` | Keystore key alias | ✅ Required |
| `KEY_PASSWORD` | Key password | ✅ Required |
| `STORE_PASSWORD` | Keystore store password | ✅ Required |

---

## Testing

**Current state**: 22 meaningful unit tests across 2 test files.

| File | Tests | Coverage |
|---|---|---|
| `LoginViewModelTest.kt` | 11 | Login, drLogin, register, logout, forgotPassword, sendSms (success/error/network-failure) |
| `HomeViewModelTest.kt` | 11 | home, banners, notificationCount, getprofile1, postLanguage1 (success/error/network-failure) |

- Test-to-source ratio: 1:131 (262 source files)
- Pattern: MockK + `InstantTaskExecutorRule` + LiveData assertions
- No CI runner yet (planned for sprint-003)

Run tests: `./gradlew test`

---

## Deployment

No CI/CD pipeline yet — deferred to sprint-003. APKs are built manually via Android Studio or Gradle CLI.

---

## Known Issues

| # | Issue | Severity | Status |
|---|---|---|---|
| 1 | No CI/CD pipeline | 🔴 Critical | Deferred to sprint-003 |
| 2 | Expand unit test coverage | 🟡 Medium | Sprint-002 task 6 — AppointmentViewModel + RequestViewModel |
| 3 | Rotate signing password before Play Store submission | 🟡 Medium | Pre-release ops task |
| 4 | No offline caching (Room DB) | 🟢 Low | Future sprint |

---

## Security Checklist

| Check | Status |
|---|---|
| Secrets in `local.properties` (gitignored) | ✅ |
| Signing credentials in `local.properties` | ✅ |
| Auth token in `EncryptedSharedPreferences` (AES-256-GCM) | ✅ |
| Debug logs stripped in release builds (Timber `ReleaseTree`) | ✅ |
| Rotate signing password before Play Store | ⬜ Pre-release |

---

## Contributing

- Branch naming: `feat/`, `fix/`, `chore/` prefixes
- Commits: conventional commits (`feat:`, `fix:`, etc.)
- PRs: keep under 400 lines; tag reviewer
