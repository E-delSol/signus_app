# Phase 4 Design: Android Deep Links

## Architecture

### Deep Link Flow
```
FCM message arrives
  → SignusMessagingService.onMessageReceived()
    → Extract `navigateTo` from data payload
    → Build deep link URI: signus://{navigateTo}
    → Create PendingIntent with deep link data
    → Show notification

User taps notification
  → MainActivity opens with deep link intent
  → NavHost resolves deep link via registered patterns
  → Navigation goes to matching composable destination
```

### URI Scheme
- `signus://semaphore` → Semaphore screen
- `signus://settings` → Settings screen
- `signus://pairing` → Pairing screen
- Unknown patterns → ignored, normal startup flow

### Files to modify
| File | Change |
|------|--------|
| `AndroidManifest.xml` | Add intent filter for `signus://` scheme |
| `AppNavigation.kt` | Add `navDeepLink` to Semaphore, Settings, Pairing destinations |
| `SignusMessagingService.kt` | Parse `navigateTo` from data payload, build deep link URI in PendingIntent |

## Key Decisions

### ADR-P4-1: Not using @DeepLink annotation
Use `navDeepLink { uriPattern }` on each composable destination instead of `@DeepLink` on route objects, keeping routes as pure sealed classes without Android dependencies.

### ADR-P4-2: StartDestination unchanged
SplashScreen remains the start destination. Deep links are resolved by the NavHost's deep link system, which can navigate to a matching destination even when starting from Splash.

### ADR-P4-3: Backend responsibility
The backend dispatcher MUST include `navigateTo` in the FCM data payload. This design only handles parsing and navigation — the backend change is tracked separately.
