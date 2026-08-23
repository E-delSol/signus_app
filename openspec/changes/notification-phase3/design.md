# Phase 3 Design: Android Notification Permissions

## Architecture Decision

### ADR-P3-1: Permission request location
**Decision:** Request POST_NOTIFICATIONS permission at the `SemaphoreScreen` level using a dedicated composable component.

**Rationale:**
- SplashScreen is transient and the dialog would be dismissed before user interaction.
- SemaphoreScreen is the main screen where notifications actually matter.
- A dedicated `NotificationPermissionRequest` composable keeps the concern isolated and reusable.

### ADR-P3-2: Non-blocking approach
**Decision:** The permission request SHALL be non-blocking. If denied, the app works normally without notifications.

**Rationale:** Notifications enhance the UX but are not critical for app functionality.

## Implementation

### Files to modify
| File | Change |
|------|--------|
| `app/src/main/AndroidManifest.xml` | Add `POST_NOTIFICATIONS` permission declaration |
| `app/src/main/java/es/cronos/duo/components/NotificationPermissionRequest.kt` | (NEW) Composable for permission request |
| `app/src/main/java/es/cronos/duo/presentation/semaphore/SemaphoreScreen.kt` | Call `NotificationPermissionRequest` composable |
| `app/src/main/res/values/strings.xml` | Add notification-related string resources |

### Flow
```
SemaphoreScreen enters composition
  → Check if SDK >= 33 AND permission not granted
    → YES: Show rationale dialog → Launch system permission dialog
      → Grant: proceed (notifications enabled)
      → Deny: proceed silently (notifications disabled)
    → NO: Do nothing (pre-13 or already granted)
```
