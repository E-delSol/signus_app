# 📱 Signus Android

Android client for a real-time system designed to help couples communicate availability in a simple, discreet, and low-pressure way.

---

## 🚀 Project Summary

This application is part of the **Signus ecosystem**, a real-time system composed of:

* **signus_app** — Android client (this repository)
* [signus_back](https://github.com/E-delSol/signus_back) — Backend API (Ktor + WebSockets)
* [signus_infra](https://github.com/E-delSol/signus_infra) — Infrastructure and deployment
* [signus_landing](https://github.com/E-delSol/signus_landing) — Landing page

🌐 **Live:** [e-delsol.github.io/signus_landing](https://e-delsol.github.io/signus_landing/)

The app provides a shared **"traffic light" style status** between two users, allowing lightweight communication without replacing conversation.

---

## 🧩 What this project demonstrates

* Building a real-world Android app with **Jetpack Compose**
* Applying **MVVM + Clean Architecture**
* Integrating **HTTP + WebSockets + FCM**
* Managing real-time state between users
* Designing UI for **sensitive communication contexts**
* Coordinating client behavior with backend-driven state
* **Backend-driven version enforcement** with forced update flow
* **Automatic token refresh** with Ktor bearer auth
* **Separate debug/release** Firebase projects and endpoints

---

## 🏗️ Architecture

The app follows an MVVM + Clean Architecture approach within a single Android module.

```text
Compose UI
   ↓
ViewModel
   ↓
Use Case
   ↓
Repository
   ↓
Remote API / WebSocket / Local Storage
```

### Key characteristics

* Jetpack Compose for UI
* ViewModels expose state using `StateFlow`
* `Flow` used for observable streams and realtime updates
* Use cases orchestrate domain logic
* Repository layer isolates data sources from domain logic
* Data layer integrates HTTP, WebSocket, secure storage, and FCM
* **Koin** for dependency injection
* **Kotlinx Serialization** for JSON

---

## ⚡ Core Features

* JWT-based authentication with automatic access token refresh
* Linking between two users via code or QR flow
* Shared status:

  * `AVAILABLE`
  * `BUSY`
  * `OFFLINE`
* Real-time partner updates via WebSocket
* FCM push notifications as fallback
* Backend-driven session and user state
* **Backend-driven forced update** — blocks the app when the client version is no longer supported
* **Firebase Crashlytics** for crash reporting (separate projects per build type)

---

## 🔌 Realtime & Push Flow

The system is designed around **real-time communication with graceful degradation**.

* **Foreground** → WebSocket is the primary channel
* **Background / fallback** → FCM is used for notifications
* Backend remains the **source of truth**

### Client Instance Tracking

Each device identifies itself with a persistent `X-Client-Instance-Id` header, enabling the backend to detect stale or duplicate WebSocket connections and enforce a single active session per device.

---

## 🌐 Networking

* HTTP via Ktor client with OkHttp engine
* JWT authentication via `Authorization: Bearer <token>` with automatic refresh via Ktor's `BearerAuthProvider`
* WebSocket authenticated with access token
* Every request includes `X-App-Version` header for version enforcement

<!-- markdownlint-disable MD013 -->
### ⬆️ Version Enforcement

The backend enforces a minimum app version. When the client's version falls below the threshold:

1. Backend responds with `426 Upgrade Required` and the minimum supported version
2. An OkHttp interceptor parses the response and updates the app state
3. `SplashScreen` detects the state change and navigates to a **non-dismissible** `ForceUpdateScreen`
4. The user is blocked from using the app and directed to Google Play to update
5. WebSocket closure with `VIOLATED_POLICY` reason code also triggers the same flow

See [`ANDROID_APP_VERSION_FLOW.md`](ANDROID_APP_VERSION_FLOW.md) for the full backend contract and sequence diagram.

<!-- markdownlint-enable MD013 -->
### 🔑 Automatic Token Refresh

The app uses Ktor's `bearer` auth provider to automatically refresh expired access tokens:

* A `/auth/refresh` endpoint exchanges the refresh token for a new access token
* The `refreshTokens` lambda runs transparently when the server returns `401 Unauthorized`
* Protected endpoints skip the refresh — avoiding infinite loops
* Cleared tokens trigger logout (webhook deactivation)

---

## 📂 Project Structure

```text
app/
  src/
    main/java/es/cronos/duo/
      data/          — repositories, APIs, WebSocket, FCM, secure storage
      domain/        — use cases, contracts, models
      presentation/  — UI, ViewModels, navigation
    debug/           — debug Firebase project (google-services.json)
    release/         — release Firebase project (google-services.json)
  build.gradle.kts
```

### Data layer

| Package | Role |
|---------|------|
| `data.local` | `EncryptedSharedPreferences` via `TokenStore` |
| `data.network` | Ktor client, version enforcement, endpoint config |
| `data.remote` | `AuthApi`, `DeviceApi`, `MeApi`, `PartnerApi`, `PairingApi`, ... |
| `data.remote.socket` | WebSocket client with lifecycle management |
| `data.repository` | `AuthRepositoryImpl`, `UserRepositoryImpl`, ... |

### Domain layer

| Package | Role |
|---------|------|
| `domain.repository` | Repository interfaces (source of truth contracts) |
| `domain.usecase` | `AppStartupUseCase`, `LoginWithEmailUseCase`, `RegisterWithEmailUseCase`, `LinkPartnerUseCase`, `ObserveUserUseCase`, `UpdateUserStatusUseCase`, ... |

### Presentation layer

| Package | Role |
|---------|------|
| `presentation.splash` | Startup check + version enforcement routing |
| `presentation.forceupdate` | Non-dismissible forced update screen |
| `presentation.login` | Email/password login |
| `presentation.pairing` | Link/unlink partner flow |
| `presentation.semaphore` | Main status screen (traffic light) |
| `presentation.settings` | Settings and account management |
| `presentation.navigation` | Routes and NavHost |

---

## ▶️ Setup / Run

### Requirements

* Android Studio
* Android SDK configured
* Running Signus backend
* Firebase project configured (FCM + Crashlytics)
* `google-services.json` added per build type

### Steps

1. Start the Signus backend
2. Open the project in Android Studio
3. Configure Firebase Cloud Messaging + Crashlytics
4. Run on emulator or device

### Quick Demo

Want to try it immediately? Use the one-command demo launcher:

```bash
git clone https://github.com/E-delSol/signus_infra.git
cd signus_infra
./demo/demo.sh
```

This sets up the backend, creates two linked users (Alice & Bob),
builds the APK, and launches two emulators ready to use.

See [signus_infra/demo](https://github.com/E-delSol/signus_infra/tree/main/demo)
for details.

### Running Tests

```shell
./gradlew testDebugUnitTest
```

---

## 🔗 Backend Configuration

The app requires a running backend instance.

### Local Development

* Base URL:
  `http://10.0.2.2:8080`
* WebSocket endpoint:
  `ws://10.0.2.2:8080/ws`

Steps:

1. Run backend on port 8080
2. Launch emulator
3. Ensure connectivity via `10.0.2.2`

---

### Deployment

The backend can be deployed in any compatible environment.

* No public endpoint is provided
* Users must deploy their own backend instance

---

## 📦 Dependencies

* **Compose BOM** (2024.12.01) — UI toolkit
* **Ktor** (3.0.3) — HTTP client + WebSocket
* **Koin** (4.0.2) — Dependency injection
* **Kotlinx Serialization** — JSON parsing
* **Navigation Compose** (2.8.5) — Screen navigation
* **Firebase** (BOM 33.7.0) — FCM + Crashlytics
* **Encrypted SharedPreferences** (1.1.0-alpha06) — Secure token storage
* **ZXing** (embedded) — QR code generation
* **JUnit 5** + **Turbine** + **MockK** + **kotlinx-coroutines-test** — Testing

---

## 🧠 Notes

* Designed for **sensitive communication scenarios**
* Prioritizes clarity and reliability over complexity
* Backend defines final behavior (auth, linking, state, realtime, version enforcement)
* WebSocket requires valid JWT
* FCM is fallback only

---

## 📄 License

MIT License
See [LICENSE](LICENSE)

---

## 👤 Author

E-delSol

---
