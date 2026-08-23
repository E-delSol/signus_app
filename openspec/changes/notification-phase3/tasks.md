# Phase 3 Tasks: Android Notification Permissions

## Task 3.1: Declare POST_NOTIFICATIONS in AndroidManifest
**File:** `app/src/main/AndroidManifest.xml`
**Action:** Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` before the `<application>` tag.

## Task 3.2: Create NotificationPermissionRequest composable
**File:** `app/src/main/java/es/cronos/duo/components/NotificationPermissionRequest.kt` (NEW)
**Action:** Create a reusable composable using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` following the same pattern as `QrCodeScanner.kt`.
- Guard with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`
- Show rationale dialog explaining notifications are useful for partner status updates
- Handle granted/denied states gracefully
- Auto-request on first composition

## Task 3.3: Integrate in SemaphoreScreen
**File:** `app/src/main/java/es/cronos/duo/presentation/semaphore/SemaphoreScreen.kt`
**Action:** Add `NotificationPermissionRequest()` call at the top-level composable.

## Task 3.4: Add string resources
**File:** `app/src/main/res/values/strings.xml`
**Action:** Add notification permission rationale strings in Spanish.
