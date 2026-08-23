# Phase 4: Android Deep Links for Notifications

## Scope
Add deep link support to navigate from FCM notifications to specific screens in the app.

## Rationale
When a user taps a push notification, they should land on the relevant screen (e.g., Semaphore when their partner changes status) instead of always opening to the splash screen.

## Requirements

### Functional
- REQ-4.1: The app SHALL define a `signus://` URI scheme for deep linking.
- REQ-4.2: The app SHALL register deep link patterns for Semaphore, Settings, and Pairing screens.
- REQ-4.3: When a user taps a notification with navigation context, the app SHALL navigate to the corresponding screen.
- REQ-4.4: The `SignusMessagingService` SHALL extract `navigateTo` from the FCM data payload and include it in the notification's PendingIntent.
- REQ-4.5: Deep links MUST NOT bypass authentication — unauthenticated users follow the normal auth flow.

### Non-functional
- Follow Navigation 2.8+ type-safe deep link patterns.
- URI patterns: `signus://semaphore`, `signus://settings`, `signus://pairing`

## Scenarios

### Scenario 4.1: Partner changes status while app is in background
Given a logged-in user with the app in background
When the partner changes status
And an FCM notification arrives with `navigateTo: "semaphore"`
And the user taps the notification
Then the app opens on the Semaphore screen

### Scenario 4.2: Notification without navigation context
Given a logged-in user
When an FCM notification arrives without `navigateTo`
Then tapping the notification opens the app normally (Splash → startup logic)
