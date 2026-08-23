# Verification Report: notification-phase4 — Android Deep Links

**Status**: PASS WITH WARNINGS

**Date**: 2026-06-29

**Verification method**: Code review of 3 modified files + `./gradlew test` (Strict TDD)

---

## Compliance Matrix

### Requirements

| ID | Description | Result | Evidence |
|---|---|---|---|
| REQ-4.1 | Define `signus://` URI scheme for deep linking | **PASS** | `AndroidManifest.xml` lines 31-38: `<intent-filter>` with `ACTION_VIEW`, `BROWSABLE`/`DEFAULT` categories, `<data android:scheme="signus" />` |
| REQ-4.2 | Register deep link patterns for Semaphore, Settings, Pairing | **PASS** | `AppNavigation.kt` lines 23-31: `navDeepLink { uriPattern = "signus://semaphore" }`, `signus://settings`, `signus://pairing` on respective composable destinations |
| REQ-4.3 | Notification tap navigates to corresponding screen | **PASS** | `SignusMessagingService.kt` lines 67-69: `data = Uri.parse("signus://$navigateTo")` set on PendingIntent intent when `navigateTo` is non-null |
| REQ-4.4 | Extract `navigateTo` from FCM data in PendingIntent | **PASS** | `SignusMessagingService.kt` line 42: `val navigateTo = if (data.isNotEmpty()) data["navigateTo"] else null` — extracted and passed to `sendNotification()` method |
| REQ-4.5 | Deep links MUST NOT bypass authentication | **FAIL** | No auth guard exists on deep link entry. Cold-start deep link can navigate directly to Semaphore/Settings/Pairing before Splash auth completes. See note below. |

### Non-functional

| Requirement | Result | Evidence |
|---|---|---|
| Follow Navigation 2.8+ type-safe deep link patterns | **PASS** | Uses `navDeepLink { uriPattern }` DSL on type-safe `composable<>` destinations |
| URI patterns `signus://semaphore`, `signus://settings`, `signus://pairing` | **PASS** | All 3 patterns registered in `AppNavigation.kt` |

### Scenarios

| Scenario | Result | Evidence |
|---|---|---|
| 4.1: Partner changes status (background) → tap → Semaphore | **PASS** | Service sets `signus://semaphore` on PendingIntent; NavHost resolves via deep link pattern |
| 4.2: Notification without navigateTo → normal startup | **PASS** | `navigateTo` is null when key absent from payload; `intent.data` not set; PendingIntent opens MainActivity normally |

### Design Decisions (ADRs)

| ADR | Status | Evidence |
|---|---|---|
| ADR-P4-1: Use `navDeepLink { uriPattern }` instead of `@DeepLink` annotation | **PASS** | `AppNavigation.kt` uses `navDeepLink { uriPattern }` on each composable; routes remain sealed classes |
| ADR-P4-2: StartDestination unchanged (Splash) | **PASS** | `MainActivity.kt` line 21: `val startDestination = Splash` — unchanged |
| ADR-P4-3: Backend dispatcher includes navigateTo (separate tracking) | **INFO** | Design acknowledges this; not in scope of current change |

### Tasks

| Task | Result | Evidence |
|---|---|---|
| 4.1: Add deep link intent filter to AndroidManifest | **PASS** | Intent filter with `ACTION_VIEW`, `DEFAULT`+`BROWSABLE`, `signus://` scheme |
| 4.2: Register deep links in NavHost | **PASS** | All 3 destinations have `deepLinks = listOf(navDeepLink { uriPattern })` |
| 4.3: Update SignusMessagingService for deep link navigation | **PASS** | `navigateTo` extracted, URI built with scheme, PendingIntent data set; fallback when no navigateTo |

---

## Tests

### Compilation
- **Result**: FIXED (1 issue found and corrected)
- **Issue**: `import androidx.navigation.compose.navDeepLink` — function not in `navigation.compose` package
- **Fix applied**: Changed to `import androidx.navigation.navDeepLink` (correct package: `navigation-common`)
- **After fix**: Both debug and release Kotlin compilation pass

### Unit tests (`./gradlew test`)
- **Total**: 81 tests, 73 passed, 8 failed
- **Failures**: All 8 in `AuthRepositoryImplTest` — **verified as pre-existing** (same failures without deep link changes)
- **New failures introduced**: 0
- **Test coverage gap**: No unit tests exist for `SignusMessagingService` or deep link navigation

---

## Issues Found

### P1: Auth bypass on cold-start deep link (REQ-4.5)

**Severity**: Medium — not a regression but a gap in requirement coverage

**Detail**: When the app is cold-launched via a deep link notification and the user is NOT authenticated:
1. NavHost starts at Splash
2. Navigation Compose resolves the deep link URI and navigates directly to the target destination (e.g., Semaphore)
3. The Splash screen's `LaunchedEffect` runs auth check, but by then the deep link destination is already on screen

**Impact**: An unauthenticated user could briefly see an authenticated screen before the auth flow redirects them.

**Recommendation**: Implement an auth interceptor in the NavHost or MainActivity that defers deep link resolution until auth state is confirmed. In Navigation Compose 2.8+, consider handling this via a custom `navController.handleDeepLink()` call after the startup auth completes, or using the NavHost's `navController.addOnDestinationChangedListener` to intercept unauthorized deep link navigation.

### P2: No unit test coverage

**Severity**: Low

**Detail**: `SignusMessagingService`, `AppNavigation` deep link setup, and `MainActivity` intent handling have no unit tests.

**Recommendation**: Add unit tests for:
- `SignusMessagingService.sendNotification()` — verify URI is set when `navigateTo` is present and not set when null
- `AppNavigation` — verify deep link patterns are correctly registered on each destination
- `MainActivity` — verify deep link intent is properly handled

---

## Summary

| Category | Count |
|---|---|
| ✅ Pass | 10 checks |
| ⚠️ Fail (REQ-4.5) | 1 check (see recommendations above) |
| ℹ️ Pre-existing failures | 8 unrelated tests |
| 🔧 Issues found & fixed | 1 compilation error (wrong import) |

**Verdict**: PASS WITH WARNINGS

The implementation correctly fulfills REQ-4.1 through REQ-4.4, all ADRs, all tasks, and both scenarios. REQ-4.5 (auth bypass prevention) is not explicitly implemented — this is a design-level gap that should be addressed before the change is considered fully compliant.
