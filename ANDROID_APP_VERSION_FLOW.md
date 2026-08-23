# Android app version enforcement flow

This document explains how the Android app should work with the backend app-version enforcement that is already implemented in `signus_back`.

## Short answer

Yes: the Android app sends its current version to the backend using the `X-App-Version` header.

The backend checks that value against the minimum supported version configured on the server.

- If the version is supported, the app continues normally.
- If the version is too old, the backend rejects the request and the app should block access and open Google Play so the user can update.

## Current backend contract

| Topic | Decision |
|-------|----------|
| Header name | `X-App-Version` |
| Version format | `major`, `major.minor`, or `major.minor.patch` |
| Valid examples | `2`, `2.3`, `2.3.1` |
| Invalid examples | `2.beta`, `v2.3.1`, `1.2.3.4` |
| Minimum version source | `MIN_SUPPORTED_APP_VERSION` on backend environment |
| HTTP unsupported response | `426 Upgrade Required` |
| HTTP invalid format response | `400 Bad Request` |
| WebSocket unsupported behavior | close with `VIOLATED_POLICY` |

## Recommended Android startup flow

## Quick path

1. Start the app and read the installed app version.
2. Send that version in the `X-App-Version` header on the first backend request made during splash.
3. Inspect the response:
   - continue if the request is accepted
   - force update if the backend returns `426`
   - treat `400` as a client integration error and handle safely

## Important note

The backend currently does **not** expose a dedicated `/version-check` endpoint.

That means Android should use one of these approaches:

1. **Preferred now**: include `X-App-Version` on the first real API request already performed during splash.
2. **Possible later**: add a dedicated lightweight endpoint if the team wants an explicit version check route.

So the implemented backend already supports the version decision, but the Android app must integrate it into its startup request flow.

## HTTP behavior the Android app must handle

### 1. Supported version

The backend allows the request to continue.

Result for Android:
- keep normal app flow
- continue splash initialization
- navigate to login or home as usual

### 2. Unsupported version

The backend returns:

- status: `426 Upgrade Required`
- body:

```json
{
  "error": "App version is no longer supported. Minimum supported version is 2.0.0"
}
```

Result for Android:
- stop normal navigation
- show update UI if desired
- open Google Play
- do not allow access until the app is updated

### 3. Missing version header

The backend returns:

- status: `426 Upgrade Required`
- body:

```json
{
  "error": "App version header X-App-Version is required"
}
```

Result for Android:
- this is an integration bug in the app
- do not treat it as a server outage
- log it and fail safely

### 4. Invalid version format

The backend returns:

- status: `400 Bad Request`
- body:

```json
{
  "error": "Invalid app version format"
}
```

Result for Android:
- this means the app is sending the version in the wrong format
- fix the Android header formatting
- do not treat it as a valid “force update” signal

## End-to-end sequence

```text
Android app starts
  -> Splash begins
  -> App reads installed versionName
  -> App sends request with header X-App-Version: <versionName>
  -> Backend compares against MIN_SUPPORTED_APP_VERSION
      -> If supported: request continues
      -> If unsupported: backend returns 426
  -> Android receives result
      -> Supported: continue navigation
      -> Unsupported: open Google Play and block access
```

## Android responsibilities

| Area | Android app must do |
|------|---------------------|
| Read installed version | Get the current app version from package info |
| Send header | Add `X-App-Version` to all backend requests |
| Splash decision | Interpret `426` as force-update required |
| User experience | Block access until updated |
| Store redirect | Open Google Play for this app |
| Error handling | Distinguish unsupported version from malformed header bugs |

## Backend responsibilities

| Area | Backend already does |
|------|----------------------|
| Version policy | Enforces global minimum supported version |
| Comparison | Compares semantic numeric versions |
| HTTP enforcement | Rejects outdated HTTP clients |
| WebSocket enforcement | Rejects outdated `/ws` connections |
| Configurability | Uses `MIN_SUPPORTED_APP_VERSION` from environment |

## Recommended Android decision rules

Use the following rules in splash:

- **2xx / normal business response**: continue normally
- **426 Upgrade Required**:
  - if the message says the app version is unsupported, force update
  - if the message says the header is missing, log integration issue and fail safely
- **400 Bad Request** with `Invalid app version format`:
  - treat as Android integration bug
  - log and fail safely
- **network/server errors**:
  - handle with your existing offline or retry strategy
  - do not automatically redirect to Google Play

## Google Play redirect behavior

When the backend says the app version is unsupported, the Android app should:

1. stop the normal splash flow
2. show a non-dismissible update message or update screen
3. open the Play Store page for the app
4. prevent the user from entering the app until the installed version satisfies the backend minimum

## Example request

```http
GET /some-startup-request HTTP/1.1
Host: api.example.com
X-App-Version: 2.1.0
Authorization: Bearer <token-if-needed>
```

## Example supported case

```text
Request accepted
-> Android continues normal startup
```

## Example unsupported case

```text
HTTP 426
{"error":"App version is no longer supported. Minimum supported version is 2.0.0"}
-> Android opens Google Play and blocks access
```

## Suggested implementation checklist for Android

- [ ] Read `versionName` from the installed Android app
- [ ] Send `X-App-Version` on the first request made during splash
- [ ] Add `X-App-Version` to the shared network layer for all future requests
- [ ] Detect `426 Upgrade Required`
- [ ] Detect the unsupported-version error message
- [ ] Redirect to Google Play
- [ ] Prevent access to the app until update is completed
- [ ] Log invalid-format and missing-header cases as integration issues
- [ ] Apply the same header to WebSocket connection requests if the app uses `/ws`

## What is true today

The current backend implementation already supports the force-update decision.

What still belongs to Android is:
- sending the header
- handling the response in splash
- redirecting to Google Play
- blocking app usage until updated

## If the team wants an even cleaner mobile flow later

A future improvement could be adding a dedicated endpoint such as `/app/version-check`.

That is **not required** for the current solution to work, but it may make the Android startup flow easier to reason about.
