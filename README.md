# Signus Android

Android client for Signus, a traffic light style app that helps couples communicate their availability for intimacy in a simple, discreet, and low-pressure way, supporting communication rather than replacing it.

## Product Summary

Signus provides a shared semaphore between two linked users so each person can communicate availability through a lightweight status signal. It is designed to reduce friction and support communication in situations where direct verbal expression may be difficult or uncomfortable.

It is not intended to replace conversation, but to make it easier to approach.

For the full product context and positioning, see [PRODUCT_OVERVIEW.md](./PRODUCT_OVERVIEW.md).

## Architecture

The app follows an MVVM + Clean Architecture style within a single Android module.

Main flow:

`Compose UI -> ViewModel -> Use Case -> Repository -> Remote API / Data Source`

Key characteristics:

- Jetpack Compose for UI
- ViewModels expose screen state with `StateFlow`
- `Flow` is used for observable user and realtime streams
- Domain use cases orchestrate application actions
- Repository interfaces isolate the domain layer from implementation details
- Data layer integrates HTTP, WebSocket, secure local storage, and FCM

## Core Features

- JWT authentication against the backend
- Linking between two users via code or QR flow
- Shared semaphore status: `AVAILABLE`, `BUSY`, `OFFLINE`
- Realtime partner updates via WebSocket
- FCM push notifications as fallback
- Backend-driven session and user state

## Realtime and Push Flow

- Foreground: partner status updates are primarily received through WebSocket events
- Background or unavailable realtime channel: FCM is used as fallback notification delivery
- The backend is the source of truth for user state, linking state, and event semantics

## Networking

- HTTP via Ktor client
- JWT sent as `Authorization: Bearer <token>`
- WebSocket connection authenticated with the current access token
- High-level backend integrations include:
  - authentication
  - current user
  - linking sessions
  - partner state
  - semaphore status updates
  - device FCM token registration
  - websocket events

## Project Structure

```text
app/
  src/main/java/es/cronos/duo/
    data/
    domain/
    presentation/
```

- `data/`: repository implementations, remote APIs, websocket, FCM, local secure storage
- `domain/`: use cases, repository contracts, domain models
- `presentation/`: Compose screens, ViewModels, UI state, navigation

## Setup / Run

Requirements:

- Android Studio
- Android SDK configured
- Running Signus backend
- Firebase project configured for FCM
- `google-services.json` added to the app module

Run steps:

1. Start the Signus backend.
2. Open the Android project in Android Studio.
3. Configure Firebase Cloud Messaging for the Android app.
4. Run the app on an emulator or device.

## Backend Configuration

The Android app communicates with a Signus backend instance.

### Local Development

The project is fully reproducible in a local environment.

- HTTP base URL is defined in:
  `app/src/main/java/es/cronos/duo/data/network/NetworkHttpClientProvider.kt`
- Default emulator URL:
  `http://10.0.2.2:8080`
- WebSocket endpoint:
  `ws://10.0.2.2:8080/ws`

To run the system locally:

1. Start the backend on your machine (port 8080).
2. Run the Android app on an emulator.
3. Ensure the emulator can reach the host machine (`10.0.2.2`).

### Deployment

The backend can be deployed in any environment that supports the required stack (Ktor, PostgreSQL, etc.).

A private deployment is used for the current product, but no public endpoint is provided.

The backend source code is available in a separate public repository.

Users interested in running the system should deploy their own backend instance.

## Notes

- The app is designed for sensitive communication contexts, so both backend and client behavior should prioritize reliability, consistency, and clarity over complexity.
- The backend must be available for the app to function correctly.
- WebSocket connectivity requires a valid JWT.
- FCM is used only for push notification fallback.
- Backend contracts define the effective behavior of authentication, linking, status, and realtime events.
