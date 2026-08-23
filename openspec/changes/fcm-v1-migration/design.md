# Design: FCM v1 Migration

## Technical Approach

Rewrite `FcmPushProvider` to use the FCM v1 HTTP API (`/v1/projects/{projectId}/messages:send`) with OAuth2 bearer tokens via `GoogleCredentials`. The `PushProvider` interface, `PushResult`, `FcmErrorClassifier`, and `PartnerPushNotificationService` remain unchanged. Legacy types (`FcmRequest`, `FcmResponse`, `FcmResult`, `FcmNotification`, `checkHttpStatus`, `parseFcmResponse`, `parseFcmHttpResponse`) are deleted after the new implementation is verified.

The Google Auth Library handles the entire OAuth2 lifecycle — JWT assertion, token exchange, and transparent auto-refresh — so the provider never manages tokens manually.

## Architecture Decisions

### Decision: google-auth-library over manual JWT

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `google-auth-library-oauth2-http` | Heavier dependency, but handles token refresh, JWT signing, and retries | **Chosen** |
| Manual JWT + HTTP call to `oauth2.googleapis.com/token` | Less deps, but must manage refresh logic, token expiry, HTTP error handling | Rejected |

**Rationale**: The library's `GoogleCredentials` provides transparent auto-refresh — calling `getAccessToken()` always returns a valid token. Manual JWT would require tracking expiry, re-fetching on 401, and reimplementing Google's token endpoint behavior. The library is the recommended path from Google's FCM docs and is maintained by their team.

### Decision: One request per token (no batching)

| Option | Tradeoff | Decision |
|--------|----------|----------|
| One POST per token | Matches existing `PushProvider` interface, simpler error handling | **Chosen** |
| FCM v1 batch endpoint | Requires changing interface or adding aggregation layer, FCM v1 batch is more complex | Rejected |

**Rationale**: `PartnerPushNotificationService` already iterates tokens and calls `sendPush` per token. The FCM v1 API doesn't support legacy multicast — each request targets exactly one token via `message.token`. Changing the interface would cascade to `PushProvider`, `PushDispatchResult`, and every caller. Not worth the complexity.

### Decision: OAuth2 token lifecycle (GoogleCredentials auto-refresh)

**Choice**: Hold a single `GoogleCredentials` instance in `FcmPushProvider`. Call `getAccessToken()` before each request.

**Alternatives considered**: Cache token externally with refresh logic.

**Rationale**: `GoogleCredentials` internally manages the OAuth2 token lifecycle — it checks expiry, refreshes via JWT assertion when needed, and handles concurrency. Holding a singleton is simpler and safer than manual caching. `getAccessToken()` is cheap when the token is still valid (no network call).

### Decision: Error response parsing strategy

**Choice**: Parse `{"error": {"status": "..."}}` from v1 error responses, pass to `FcmErrorClassifier` unchanged.

**Alternatives considered**: New error classifier for v1, separate HTTP vs. error classification.

**Rationale**: FCM v1 uses the same status strings as legacy (`UNREGISTERED`, `UNAVAILABLE`, `INTERNAL`, etc.). The existing `FcmErrorClassifier.classify(token, errorCode)` works without modification. HTTP-level errors (non-2xx without parseable body) map to `TemporaryFailure` — conservative retry behavior.

### Decision: Legacy code removal after verification

**Choice**: Delete legacy types/functions in a cleanup task after the new implementation is verified.

**Rationale**: Clean boundary — the old tests test `parseFcmResponse` and `checkHttpStatus`, which are removed. Deleting them during the rewrite would break the build. Remove them as a final step when all tests pass and the new code is proven.

## Data Flow

```
┌──────────────────────────────┐
│ PartnerPushNotificationService│
│  (unchanged — iterates tokens)│
└──────────┬───────────────────┘
           │ sendPush(token, title, body)
           ▼
┌──────────────────────────────┐
│      FcmPushProvider         │
│  (rewritten for v1)          │
│                              │
│  1. credentials.getAccessToken()  │
│  2. Build v1 message payload │
│  3. POST /v1/projects/{id}/  │
│        messages:send         │
│     Authorization: Bearer XX │
│  4. Parse response           │
└──────┬──────────────┬────────┘
       │              │
       ▼              ▼
   HTTP 200       HTTP error
   {"name":...}   {"error":{"status":"..."}}
       │              │
       ▼              ▼
   PushResult     FcmErrorClassifier
   .Success       .classify(token, status)
                       │
                       ▼
                  PushResult.{Permanent,Temporary}Failure
```

### OAuth2 Token Flow (auto-refresh)

```
FcmPushProvider                 GoogleCredentials             Google OAuth2
      │                               │                          │
      │ getAccessToken()              │                          │
      │──────────────────────────────►│                          │
      │                               │                          │
      │       ╔═══════════════════════╗                          │
      │       ║ Token valid + not     ║─── return cached ───────►│
      │       ║ expired?              ║◄─────────────────────────│
      │       ╚═══════════════════════╝                          │
      │       ║ NO — create JWT      ║                          │
      │       ║ assertion, POST to   ║                          │
      │       ║ oauth2.googleapis.com║                          │
      │       ║ /token               ║                          │
      │◄─────────────────────────────║── new access token ──────│
      │       ╚═══════════════════════╝                          │
```

### Error: UNREGISTERED Token

```
PartnerPushNotificationService      FcmPushProvider         DeviceTokenRepository
      │                                   │                        │
      │ sendPush(token, ...)              │                        │
      │──────────────────────────────────►│                        │
      │                                   │ POST /v1/...           │
      │                                   │──────────────────────► │
      │                                   │◄── 404 UNREGISTERED ── │
      │                                   │                        │
      │◄── PermanentFailure("unregistered")│                       │
      │                                   │                        │
      │ deactivateByFcmToken(token, reason)                        │
      │──────────────────────────────────────────────────────────►│
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `gradle/libs.versions.toml` | Modify | Add `google-auth-version` and `google-auth-library-oauth2-http` library entry |
| `build.gradle.kts` | Modify | Add `google-auth-library-oauth2-http` implementation dependency |
| `src/main/kotlin/core/config/AppConfig.kt` | Modify | `FcmConfig(serverKey)` → `FcmConfig(projectId, serviceAccountJson)`; update `loadConfig()` for `FCM_PROJECT_ID` + `FCM_SERVICE_ACCOUNT_JSON` env vars |
| `src/main/kotlin/features/notification/providers/FcmPushProvider.kt` | Modify | Rewrite: v1 endpoint, `GoogleCredentials`, v1 payload, new response parsing. Remove `FcmRequest`, `FcmNotification`, `FcmResponse`, `FcmResult`, `checkHttpStatus`, `parseFcmResponse`, `parseFcmHttpResponse`. |
| `src/main/kotlin/core/di/KoinModules.kt` | Modify | Wire `FcmPushProvider` with `FcmConfig` (projectId, serviceAccountJson) and shared `HttpClient` |
| `src/test/kotlin/features/notification/FcmPushProviderTest.kt` | Modify | Rewrite: test v1 response parsing, error classification via `FcmErrorClassifier`, token acquisition failure, network errors |
| `src/main/kotlin/features/notification/FcmErrorClassifier.kt` | None | No changes needed — already classifies v1 error status strings |

## Interfaces / Contracts

### FcmConfig (updated)

```kotlin
data class FcmConfig(
    val projectId: String,
    val serviceAccountJson: String
)
```

### FCM v1 Message Payload

```kotlin
@Serializable
internal data class FcmV1Request(
    val message: FcmV1Message
)

@Serializable
internal data class FcmV1Message(
    val token: String,
    val notification: FcmV1Notification? = null,
    val data: Map<String, String>? = null
)

@Serializable
internal data class FcmV1Notification(
    val title: String,
    val body: String
)
```

### FCM v1 Success Response

```kotlin
@Serializable
internal data class FcmV1Response(
    @SerialName("name") val name: String? = null
)
```

### FCM v1 Error Response

```kotlin
@Serializable
internal data class FcmV1ErrorBody(
    val error: FcmV1Error
)

@Serializable
internal data class FcmV1Error(
    val code: Int = 0,
    val message: String = "",
    val status: String = ""
)
```

### FcmPushProvider (new constructor)

```kotlin
class FcmPushProvider(
    private val projectId: String,
    credentials: GoogleCredentials,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json() }
    }
) : PushProvider {
    private val fcmEndpoint = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
    // ...
}
```

No changes to public `PushProvider` interface or `PushResult`.

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | `parseFcmV1Response()` — success (200 + name), missing name, error status mapping | Pure function tests, no HTTP, like current `parseFcmResponse` tests |
| Unit | `FcmErrorClassifier` — already tested independently, no new tests needed | — |
| Unit | `GoogleCredentials` initialization — valid JSON, invalid JSON, missing scopes | Mock `GoogleCredentials`, test failure path returns `TemporaryFailure` |
| Integration | Full `sendPush` with mocked HTTP — success, UNREGISTERED, UNAVAILABLE, network error | Mock Ktor `HttpClient` via `MockEngine`, assert `PushResult` types |
| Integration | DI wiring — `FcmPushProvider` created via Koin with `FcmConfig` | Koin `checkModules` test |

### Test File Structure

Rewrite `FcmPushProviderTest.kt`:
- Replace all `parseFcmResponse`/`checkHttpStatus` tests with `parseFcmV1Response` tests
- Add `GoogleCredentials` failure test (invalid JSON → `TemporaryFailure`)
- Add HTTP integration tests using Ktor `MockEngine`:
  - 200 + name → `Success`
  - 200 + missing name → `PermanentFailure(parse_error)`
  - 404 + UNREGISTERED → `PermanentFailure(unregistered)`
  - 503 + UNAVAILABLE → `TemporaryFailure`
  - Timeout/network error → `TemporaryFailure`

## Migration / Rollout

1. **Add dependency** — `google-auth-library-oauth2-http` in gradle (no runtime impact)
2. **Update FcmConfig** — change struct, add new env vars (`FCM_PROJECT_ID`, `FCM_SERVICE_ACCOUNT_JSON`), keep `FCM_SERVER_KEY` temporarily for rollback
3. **Rewrite FcmPushProvider** — new implementation lives alongside old imports at first (compile succeeds because old types are package-internal and not referenced by other production files)
4. **Update Koin wiring** — FcmPushProvider now receives `FcmConfig` + `HttpClient`
5. **Rewrite tests** — new v1-centric tests
6. **Delete legacy types** — `FcmRequest`, `FcmNotification`, `FcmResponse`, `FcmResult`, `checkHttpStatus`, `parseFcmResponse`, `parseFcmHttpResponse`
7. **Verify end-to-end** — run all tests, verify push sends in staging

**Rollback**: Revert the FcmPushProvider file, restore FcmConfig to `serverKey`, keep `FCM_SERVER_KEY` env var until migration is confirmed stable.

## Implementation Order

1. `gradle/libs.versions.toml` + `build.gradle.kts` — add dependency
2. `AppConfig.kt` — update `FcmConfig` and `loadConfig()`
3. `FcmPushProvider.kt` — rewrite implementation (keep legacy types until step 6)
4. `KoinModules.kt` — update DI wiring
5. `FcmPushProviderTest.kt` — rewrite tests for v1
6. Delete legacy types/functions from `FcmPushProvider.kt`
7. Remove `FCM_SERVER_KEY` env var from deployment config (operational, after staging verification)

## Open Questions

- [ ] What is the exact latest stable version of `google-auth-library-oauth2-http`? (check Maven Central — suggested: `1.30.0` or newer)
- [ ] Does the app's GCP service account require any additional IAM permissions for FCM v1? (should be `cloudmessaging.messages.create`)
- [ ] Should `GoogleCredentials` be created inside `FcmPushProvider` or be provided as a factory/dependency from DI? (current design: created from `FcmConfig.serviceAccountJson` inside the provider or a factory — single instance)
