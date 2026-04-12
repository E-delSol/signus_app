# 📱 Signus Android

Android client for a real-time system designed to help couples communicate availability in a simple, discreet, and low-pressure way.

---

## 🚀 Project Summary

This application is part of the **Signus ecosystem**, a real-time system composed of:

* [signus_app](https://github.com/E-delSol/signus_app) — Android client (this repository)
* [signus_back](https://github.com/E-delSol/signus_back) — Backend API (Ktor + WebSockets)
* signus_infra — Infrastructure and deployment

The app provides a shared **“traffic light” style status** between two users, allowing lightweight communication without replacing conversation.

---

## 🧩 What this project demonstrates

* Building a real-world Android app with **Jetpack Compose**
* Applying **MVVM + Clean Architecture**
* Integrating **HTTP + WebSockets + FCM**
* Managing real-time state between users
* Designing UI for **sensitive communication contexts**
* Coordinating client behavior with backend-driven state

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

---

## ⚡ Core Features

* JWT-based authentication
* Linking between two users via code or QR flow
* Shared status:

  * `AVAILABLE`
  * `BUSY`
  * `OFFLINE`
* Real-time partner updates via WebSocket
* FCM push notifications as fallback
* Backend-driven session and user state

---

## 🔌 Realtime & Push Flow

The system is designed around **real-time communication with graceful degradation**.

* **Foreground** → WebSocket is the primary channel
* **Background / fallback** → FCM is used for notifications
* Backend remains the **source of truth**

---

## 🌐 Networking

* HTTP via Ktor client
* JWT authentication via `Authorization: Bearer <token>`
* WebSocket authenticated with access token

Main integrations:

* authentication
* current user
* linking sessions
* partner state
* status updates
* FCM token registration
* realtime events

---

## 📂 Project Structure

```text
app/
  src/main/java/es/cronos/duo/
    data/
    domain/
    presentation/
```

* `data/` → repositories, APIs, WebSocket, FCM, secure storage
* `domain/` → use cases, contracts, models
* `presentation/` → UI, ViewModels, navigation

---

## ▶️ Setup / Run

### Requirements

* Android Studio
* Android SDK configured
* Running Signus backend
* Firebase project configured (FCM)
* `google-services.json` added

### Steps

1. Start the Signus backend
2. Open the project in Android Studio
3. Configure Firebase Cloud Messaging
4. Run on emulator or device

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

## 🧠 Notes

* Designed for **sensitive communication scenarios**
* Prioritizes clarity and reliability over complexity
* Backend defines final behavior (auth, linking, state, realtime)
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
