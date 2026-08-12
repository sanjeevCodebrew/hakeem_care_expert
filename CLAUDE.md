# Hakeem Care — Expert App (Android)

> Extends `../../_standards/core/org-claude-md.md`. Project-specific context lives here.
> Keep this file up to date — it is the first thing Claude reads when working in this repo.
> **Target: < 200 lines.**

---

## Project Overview

| Field | Value |
|---|---|
| **Name** | Hakeem Care — Expert App |
| **Owner** | @sanjeevchaudhary |
| **Status** | 🟢 Active |
| **App ID** | `com.hakeem.expert` |
| **Package** | `com.consultantvendor` |
| **Version** | 2.3.6 (build 135) |
| **Prod URL** | https://hakeemcare.hakeemcare.com/ |

**One-line summary**: Android doctor-facing app for the Hakeem Care telehealth platform — consultation queue, video calls (Agora + Jitsi), patient chat, prescription writing, revenue tracking, availability management, and live classes.

---

## Foundation Phase

| Field | Value |
|---|---|
| **Foundation Status** | 🔴 Not Started |
| **Scope Breakdown** | `../../proposals/scope-breakdown.md` |
| **System Design** | `../../architecture/system-design.md` |
| **Database Schema** | N/A (API-only, no local DB) |

---

## Roadmap

| Field | Value |
|---|---|
| **Roadmap** | 🔴 Not Generated |
| **Lifecycle Stage** | 🔨 Building |
| **Location** | `ROADMAP.md` |

---

## Workspace Location

```
hakeem_care/
├── knowledge_base/
│   └── knowledge-base-template/    # KB root (../../ from this repo)
│       ├── CLAUDE.md
│       ├── _standards/
│       ├── registries/
│       ├── architecture/
│       ├── sprints/
│       └── existing-code/
│           ├── hakeem_care_user/   # sibling patient app
│           └── hakeem_care_expert/ # ← this repo
```

- **Knowledge base**: `../../` (relative to this repo)
- **Standards**: `../../_standards/`
- **Sibling repo**: `../hakeem_care_user/`

---

## Standards

- **Quality baselines**: `../../_standards/core/baseline-standards.md`
- **Conventions**: `../../_standards/core/conventions.md`
- **Platform profile**: `../../_standards/profiles/native-android.md`

---

## Registries

> Registry pointers only — do not embed full tables here.

- **Tech stack**: [`../../registries/tech-stack.md`](../../registries/tech-stack.md)
- **Services**: [`../../registries/services.md`](../../registries/services.md)
- **Integrations**: [`../../registries/integrations.md`](../../registries/integrations.md)
- **Decisions**: [`../../registries/decisions.md`](../../registries/decisions.md)
- **Risks**: [`../../registries/risks.md`](../../registries/risks.md)
- **Team**: [`../../registries/team.md`](../../registries/team.md)

---

## Tech Stack

- **Language**: Kotlin 2.1.10 (legacy Java in DI layer)
- **Min / Target SDK**: 26 / 36
- **Architecture**: MVVM + Dagger 2 + AndroidX Navigation
- **Network**: Retrofit 2.11 + OkHttp 4.12 (single `UserRepository`, 98 endpoints)
- **Video calls**: Jitsi Meet SDK 11.5.1 + Agora 4.6.3
- **Payments**: Braintree card form (doctor-side earnings only — no patient checkout SDKs)
- **Analytics**: Firebase (Analytics, Crashlytics, FCM, Dynamic Links), AppsFlyer 6.16.1
- **Charts**: MPAndroidChart v2.2.4 (revenue analytics)
- **Build**: AGP 8.8.2, product flavor `consult`, build types `debug` / `release`

---

## Getting Started

```bash
# 1. Copy secrets into local.properties (never commit this file)
#    Add: APP_UNIQUE_ID=<value>  BASE_URL=<value>
#    See team vault for values.

# 2. Sync Gradle (Android Studio) or:
./gradlew assembleConsultDebug

# 3. Run tests
./gradlew test
./gradlew connectedAndroidTest   # requires device/emulator
```

Build: `./gradlew assembleConsultRelease`
Lint: `./gradlew lint`

---

## Repo Structure

```
app/src/main/java/com/consultantvendor/
├── data/          # WebService.kt (98 endpoints), UserRepository, network/response utils
├── di/            # Dagger 2 component + 6 modules, NetworkModule, push notifications
├── pushNotifications/  # FCM messaging service + token ViewModel
├── ui/            # ~83 Activities & Fragments
│   ├── loginSignUp/   # Auth (phone, email, OTP, signup, availability, category, docs)
│   ├── dashboard/     # Consultation queue, appointments, prescriptions, reports, feeds
│   │   ├── home/          # Queue management + AppointmentViewModel
│   │   ├── prescription/  # Create & publish prescriptions
│   │   ├── wallet/        # Earnings, bank accounts, add funds
│   │   └── revenue/       # Revenue charts (MPAndroidChart)
│   ├── drawermenu/    # Profile, history, notifications, classes/webinars
│   ├── calling/       # Voice/video call entry point
│   ├── aghora/        # Agora video integration
│   ├── jitsimeet/     # Jitsi video integration
│   ├── chat/          # Real-time patient chat (Socket.io)
│   └── webview/       # In-app web content
└── utils/         # PrefsManager, AppSocket, GeneralFunction, dialogs, DateUtils
gaugelibrary/      # Health metrics gauge UI (Gradle module)
filepicker/        # Custom file/document picker (Gradle module)
```

---

## Active Work

**Current Sprint**: [`sprint-001`](../../sprints/sprint-001.md) — Foundation & Security Baseline

- [ ] Migrate auth token to EncryptedSharedPreferences (task #8)
- [ ] Add GitHub Actions CI workflow (task #5)
- [ ] Add unit tests — LoginViewModel + dashboard (task #7)

**Known debt / gotchas**:
- `UserRepository.kt` handles all 98 endpoints — do not extend further; split by domain if you touch it
- `PrefsManager` uses unencrypted SharedPreferences — sprint-001 task #8 covers the fix; don't add new token writes there
- RxJava2 and Coroutines coexist — use Coroutines for all new code
- Signing credentials (`consultapp.jks`, password `codebrew`) are in `app/` — do not commit to a public repo; rotate before Play Store submission
- `MultiLoginManager.kt` exists but is lightly used — verify behaviour before adding multi-account features

---

## Sensitive Areas

| Path | Why sensitive |
|---|---|
| `app/src/main/java/com/consultantvendor/di/` | Dagger wiring — changes break the entire DI graph |
| `app/src/main/java/com/consultantvendor/data/apis/WebService.kt` | 98 endpoints — changes affect every network call |
| `app/src/main/java/com/consultantvendor/ui/calling/` + `aghora/` + `jitsimeet/` | Live call logic — must test on real device |
| `app/src/main/java/com/consultantvendor/dashboard/home/prescription/` | Clinical data — prescription publish is irreversible from patient's view |
| `app/consultapp.jks` | Signing keystore — never commit credentials; rotate before production |
| `local.properties` | Contains `APP_UNIQUE_ID` + `BASE_URL` — gitignored, never commit |

---

## Contacts

- **Project owner**: @sanjeevchaudhary
- **Questions / reviews**: @sanjeevchaudhary
