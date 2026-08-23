# Tasks: FCM v1 Migration

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~580 (across 3 PRs) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Deps + Config + v1 Models + parse tests | PR 1 | base = `feature/fcm-v1-migration`; ~120 lines |
| 2 | Provider rewrite + DI + integration tests | PR 2 | base = PR 1 branch; ~200 lines |
| 3 | Legacy cleanup + test rewrite + verify | PR 3 | base = PR 2 branch; ~50 lines net delete |

## Phase 1: Foundation (PR 1)

- [x] **T6.1** Add `google-auth-library-oauth2-http` to `gradle/libs.versions.toml` (version + library entry) and `build.gradle.kts` (implementation dep). No test needed. Small.
- [x] **T6.2** Update `FcmConfig(projectId, serviceAccountJson)` in `AppConfig.kt`; add `FCM_PROJECT_ID`, `FCM_SERVICE_ACCOUNT_JSON` env vars to `loadConfig()`. Test: config loads from env vars correctly. Small.
- [x] **T6.3a RED** Write `parseFcmV1Response()` unit tests: 200+name→Success, 200+missing name→PermanentFailure(parse_error), error status→FcmErrorClassifier. Small.
- [x] **T6.3b GREEN** Implement `parseFcmV1Response()` and v1 response models (`FcmV1Request`, `FcmV1Message`, `FcmV1Notification`, `FcmV1Response`, `FcmV1ErrorBody`, `FcmV1Error`) in `FcmPushProvider.kt`. Small.

## Phase 2: Provider + DI (PR 2)

- [x] **T6.4a RED** Write integration tests for `FcmPushProvider.sendPush` with Ktor `MockEngine`: 200+name→Success, 404+UNREGISTERED→PermanentFailure, 503+UNAVAILABLE→TemporaryFailure, timeout→TemporaryFailure, 200+missing name→PermanentFailure(parse_error). Medium.
- [x] **T6.4b GREEN** Rewrite `FcmPushProvider`: constructor takes `projectId: String`, `GoogleCredentials`, `HttpClient`; `sendPush` calls `credentials.getAccessToken()`, builds v1 payload via `FcmV1Request`, POSTs to `/v1/projects/{id}/messages:send`, parses response. Medium.
- [x] **T6.5** Update `KoinModules.kt` wiring: `FcmPushProvider(projectId = get<FcmConfig>().projectId, credentials = GoogleCredentials.fromStream(...), client = get())`. Test: Koin `checkModules()` passes. Small.

## Phase 3: Cleanup (PR 3)

- [x] **T6.6** Remove legacy types from `FcmPushProvider.kt`: `FcmRequest`, `FcmNotification`, `FcmResponse`, `FcmResult`, `checkHttpStatus()`, `parseFcmResponse()`, `parseFcmHttpResponse()`. Small.
- [x] **T6.7** Rewrite `FcmPushProviderTest.kt`: remove legacy `parseFcmResponse` + `checkHttpStatus` tests; keep only v1 parse tests + MockEngine integration tests. Verify `PartnerPushNotificationService` + `PushDispatchResult` compile unchanged. Small.

## Dependency Graph

```
T6.1 ──► T6.2 ──► T6.3a ──► T6.3b ──► T6.4a ──► T6.4b ──► T6.5 ──► T6.6 ──► T6.7
                                                                                 ▲
                                                                                 │
                                                    PartnerPushNotificationService
                                                    (unchanged — verify compiles)
```

## Implementation Order

1. **PR 1 (Phase 1)**: Dep + Config + v1 models + parse function + parse tests. Each step compiles and tests pass independently. Base: `feature/fcm-v1-migration`.
2. **PR 2 (Phase 2)**: Integration tests first (RED), then provider rewrite (GREEN), then DI wiring. Base: PR 1 branch.
3. **PR 3 (Phase 3)**: Legacy cleanup + test rewrite. Base: PR 2 branch. Feature branch merges to main when verified.
