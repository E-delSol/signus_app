# Phase 4 Tasks: Android Deep Links

## Task 4.1: Add deep link intent filter to AndroidManifest
**File:** `app/src/main/AndroidManifest.xml`
**Action:** Add intent filter to the MainActivity `<activity>` for `signus://` scheme with `BROWSEABLE` category.

## Task 4.2: Register deep links in NavHost
**File:** `app/src/main/java/es/cronos/duo/presentation/navigation/AppNavigation.kt`
**Action:** Add `navDeepLink { uriPattern = "signus://semaphore" }` to each composable destination (Semaphore, Settings, Pairing).

## Task 4.3: Update SignusMessagingService for deep link navigation
**File:** `app/src/main/java/es/cronos/duo/data/service/SignusMessagingService.kt`
**Action:**
- Extract `navigateTo` from FCM data payload
- Build deep link URI using the scheme
- Set URI data on the PendingIntent
- Fall back to opening MainActivity normally when no `navigateTo` is present
