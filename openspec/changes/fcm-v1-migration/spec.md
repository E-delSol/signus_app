# FcmPushProvider — FCM v1 Migration

## Purpose

Migrate the backend `FcmPushProvider` from the legacy FCM HTTP API (key-based auth, `/fcm/send`) to the FCM v1 HTTP API (OAuth2 bearer token, `/v1/projects/{id}/messages:send`). The `PushProvider` interface, `PushResult`, and `FcmErrorClassifier` remain unchanged.

## Requirements

### REQ-6.1: FCM v1 Endpoint
FcmPushProvider SHALL send POST requests to `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`.

#### Scenario: Successful delivery
- GIVEN a valid `projectId` in `FcmConfig`
- WHEN FcmPushProvider sends a push notification
- THEN the request URL SHALL include the projectId
- AND the HTTP method SHALL be POST

### REQ-6.2: OAuth2 Token via GoogleCredentials
FcmPushProvider SHALL use `GoogleCredentials` from `google-auth-library-oauth2-http` to obtain a bearer access token.

#### Scenario: Token acquired successfully
- GIVEN valid service account JSON in `FcmConfig`
- WHEN `GoogleCredentials.getAccessToken()` succeeds
- THEN the Authorization header SHALL be `Bearer {access_token}`

#### Scenario: Token acquisition fails
- GIVEN invalid or expired service account JSON
- WHEN `GoogleCredentials.getAccessToken()` throws
- THEN FcmPushProvider SHALL return `PushResult.TemporaryFailure`

### REQ-6.3: One Message Per Request
FcmPushProvider SHALL send exactly one token per HTTP POST request (no batching).

#### Scenario: Single target token
- GIVEN a push request for one token
- WHEN FcmPushProvider builds the request body
- THEN the FCM v1 message SHALL target that single token only
- AND no batch payload SHALL be used

### REQ-6.4: Success Response
On HTTP 200 with a JSON body containing `{"name": "projects/.../messages/..."}`, FcmPushProvider SHALL return `PushResult.Success`.

#### Scenario: 200 with name
- GIVEN FCM v1 responds with HTTP 200 and `{"name": "projects/abc/messages/msg123"}`
- WHEN FcmPushProvider processes the response
- THEN the result SHALL be `PushResult.Success` for the token

#### Scenario: 200 with missing name
- GIVEN FCM v1 responds with HTTP 200 and empty or unexpected body
- WHEN FcmPushProvider processes the response
- THEN the result SHALL be `PushResult.PermanentFailure` with `parse_error`

### REQ-6.5: Error Classification by Status
On HTTP error with `{"error": {"status": "..."}}` in the response body, FcmPushProvider SHALL classify via `FcmErrorClassifier`.

#### Scenario: UNREGISTERED
- GIVEN FCM v1 responds with `{"error": {"status": "UNREGISTERED"}}`
- WHEN FcmPushProvider classifies the error
- THEN the result SHALL be `PushResult.PermanentFailure` with `reason = "unregistered"`

#### Scenario: UNAVAILABLE
- GIVEN FCM v1 responds with `{"error": {"status": "UNAVAILABLE"}}`
- WHEN FcmPushProvider classifies the error
- THEN the result SHALL be `PushResult.TemporaryFailure`

#### Scenario: INTERNAL
- GIVEN FCM v1 responds with `{"error": {"status": "INTERNAL"}}`
- WHEN FcmPushProvider classifies the error
- THEN the result SHALL be `PushResult.TemporaryFailure`

#### Scenario: Unknown status
- GIVEN FCM v1 responds with `{"error": {"status": "SOME_UNKNOWN_CODE"}}`
- WHEN FcmPushProvider classifies the error
- THEN the result SHALL be `PushResult.TemporaryFailure`

### REQ-6.6: Network Error Handling
FcmPushProvider SHALL wrap network-timeout, connection-refused, and transport exceptions as `PushResult.TemporaryFailure`.

#### Scenario: HTTP timeout
- GIVEN the FCM endpoint is unreachable
- WHEN the HTTP client throws a timeout exception
- THEN FcmPushProvider SHALL return `PushResult.TemporaryFailure`

### REQ-6.7: FcmConfig Shape
`FcmConfig` SHALL contain `projectId: String` and `serviceAccountJson: String` (replacing legacy `serverKey: String`).

#### Scenario: Config loaded from env
- GIVEN `FCM_PROJECT_ID` and `FCM_SERVICE_ACCOUNT_JSON` env vars
- WHEN `loadConfig()` runs
- THEN `FcmConfig.projectId` and `FcmConfig.serviceAccountJson` SHALL be populated

### REQ-6.8: Legacy Code Removal
Legacy `FcmPushProvider` HTTP code (`key=` auth, `/fcm/send` endpoint), `FcmResponse`, `FcmResult`, `FcmRequest`, `FcmNotification`, `checkHttpStatus()`, `parseFcmResponse()`, and `parseFcmHttpResponse()` SHALL be removed after migration is verified.

#### Scenario: No legacy types referenced
- GIVEN the migration is verified passing
- WHEN searching the codebase for legacy types
- THEN `FcmResponse`, `FcmResult`, `FcmRequest`, `FcmNotification` SHALL NOT exist
- AND `checkHttpStatus`, `parseFcmResponse`, `parseFcmHttpResponse` SHALL NOT exist

### REQ-6.9: Interface Stability
`PushProvider` interface and `PushResult` sealed class SHALL remain unchanged.

#### Scenario: Provider contract preserved
- GIVEN the existing `PushProvider` interface
- WHEN verifying after migration
- THEN `sendPush(targetUserId, token, title, body)` SHALL return `PushResult`
- AND all existing callers SHALL compile without changes
