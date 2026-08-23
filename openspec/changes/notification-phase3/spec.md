# Phase 3: Android Notification Permissions

## Scope
Add POST_NOTIFICATIONS permission support for Android 13+ (API 33+) within the Duo Android app.

## Rationale
The app uses FCM for push notifications to alert users when their partner changes status. On Android 13+, the `POST_NOTIFICATIONS` runtime permission is REQUIRED to display any notification. Without it, users receive FCM payloads but see no visual notification.

## Requirements

### Functional
- REQ-3.1: The app SHALL declare `android.permission.POST_NOTIFICATIONS` in the manifest.
- REQ-3.2: On Android 13+ (API 33+), the app SHALL request the `POST_NOTIFICATIONS` runtime permission at the appropriate moment.
- REQ-3.3: If the permission is denied, the app SHALL continue functioning without crash (notifications simply won't display).
- REQ-3.4: The permission request SHALL include a rationale dialog explaining why notifications are needed.

### Non-functional
- The permission request MUST NOT block the main user flow.
- The implementation MUST follow the existing `rememberLauncherForActivityResult` pattern used for CAMERA permission in `QrCodeScanner.kt`.
- String resources SHALL be in Spanish to match existing app language.

## Scenarios

### Scenario 3.1: First launch on Android 13+
Given a user on Android 13+ launching the app for the first time
When they reach the main screen
Then a rationale dialog SHALL explain why notifications are useful
And the system permission dialog SHALL appear
And if granted, the app SHALL display notifications

### Scenario 3.2: Permission denied
Given a user who denied POST_NOTIFICATIONS
When the app runs
Then the app SHALL NOT crash
And the app SHALL function normally without notifications

### Scenario 3.3: Android 12 or below
Given a user on Android 12 or below
When the app runs
Then no runtime permission request SHALL be made
And notifications SHALL work as before (pre-13 behavior)
