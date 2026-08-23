# Auditoría Técnica — Sistema de Notificaciones Duo/Signus

Fecha: 2026-06-28

---

## 1. Estado Actual del Backend

### Inicialización del SDK Admin

**NO EXISTE.** El backend NO usa Firebase Admin SDK. No hay `FirebaseApp`, `GoogleCredentials`, ni `FirebaseMessaging` en el código Kotlin.

### Cómo envía FCM actualmente

El backend usa la **API HTTP legacy de FCM** (deprecated desde junio 2024):

```kotlin
// features/notification/providers/FcmPushProvider.kt:18
endpoint = "https://fcm.googleapis.com/fcm/send"
headers.append(HttpHeaders.Authorization, "key=$serverKey")
```

Usa `FCM_SERVER_KEY` desde variable de entorno (`AppConfig.kt:34`). Esta API fue reemplazada por FCM v1 HTTP API.

### Servicio de notificaciones — COMPLETO para status changes

| Componente | Archivo | Responsabilidad | Estado |
|---|---|---|---|
| `NotificationOrchestrator` | `features/notification/NotificationOrchestrator.kt:10` | Coordina routing: intenta WebSocket, si falla → FCM | ✅ Funcional |
| `RealtimeNotificationServiceImpl` | `features/notification/RealtimeNotificationServiceImpl.kt:12` | Gestiona sesiones WebSocket (`ConcurrentHashMap<userId, Set<Session>>`) | ✅ Funcional |
| `PartnerPushNotificationService` | `features/notification/PartnerPushNotificationService.kt:6` | Envía push a todos los dispositivos activos del target | ✅ Funcional |
| `FcmPushProvider` | `features/notification/providers/FcmPushProvider.kt:16` | Transporte FCM via HTTP legacy | ⚠️ Funcional pero deprecated |
| `NotificationSocketRoutes` | `features/notification/NotificationSocketRoutes.kt:12` | WebSocket `/ws` con JWT auth | ✅ Funcional |

### Flujo implementado en backend

```
PATCH /status
  → StatusServiceImpl.updateStatus()           [features/semaphore/StatusServiceImpl.kt:14]
    → semaphoreRepository.updateUserStatus()    [persiste en DB]
    → notificationOrchestrator.notifyPartnerAboutStatusChange()
      → realtimeNotificationService.notifySelfStatusChanged()
      → partnerLookup.findPartnerByUserId()
      → realtimeNotificationService.notifyPartnerStatusChanged()
        → Si deliversRealtime == true → FIN
        → Si deliversRealtime == false → partnerPushNotificationService.notifyUserDevices()
          → deviceTokenLookup.findActiveFcmTokensByUserId()
          → pushProvider.sendPush() por cada token
```

### Almacenamiento del token FCM

| Aspecto | Detalle |
|---|---|
| **Tabla** | `user_device_tokens` (`DeviceTokenTable.kt:5`) |
| **Campos** | id, user_id, device_id, fcm_token, platform, app_version, active, created_at, updated_at, last_registered_at, deactivated_at |
| **Repositorio** | `DeviceTokenRepository` — implementa `DeviceTokenRepositoryPort` + `DeviceTokenLookupPort` |
| **Endpoint PUT** | `/devices/fcm-token` — upsert por userId+deviceId |
| **Endpoint DELETE** | `/devices/fcm-token/{deviceId}` — soft delete (marca active=false) |
| **Endpoint GET** | `/devices/fcm-token` — lista tokens del usuario |
| **Lógica** | Si el mismo fcm_token llega de otro usuario/dispositivo, desactiva la entrada anterior |

---

## 2. Estado Actual de Android

### Componentes de notificación

| Componente | Archivo | Estado |
|---|---|---|
| `SignusMessagingService` | `data/service/SignusMessagingService.kt:24` | ✅ Funcional — recibe push, muestra notificación |
| `DeviceApi` | `data/remote/DeviceApi.kt:10` | ✅ Funcional — PUT/DELETE tokens |
| `UpsertDeviceTokenRequest` | `data/remote/dto/UpsertDeviceTokenRequest.kt:6` | ✅ DTO correcto |
| `UserRepository.registerOrUpdateDeviceToken` | `data/repository/UserRepositoryImpl.kt:137` | ✅ Registra token en backend |
| `UserRepository.syncFcmToken` | `data/repository/UserRepositoryImpl.kt:161` | ✅ Sync al inicio de app |
| `SemaphoreSocket` | `data/remote/socket/SemaphoreSocket.kt:45` | ✅ WebSocket funcional |

### Flujo actual Android

```
1. App inicia → SemaphoreViewModel.init → userRepository.syncFcmToken()
2. Backend envía push → SignusMessagingService.onMessageReceived()
3. Muestra notificación con NotificationManager
4. Tocar notificación → abre MainActivity (sin deep link)
```

### Dependencias Firebase

```kotlin
// build.gradle.kts
firebase-auth-ktx          // LEGACY — auth ya está en backend
firebase-firestore-ktx     // LEGACY — solo se usa en Firebase Functions
firebase-messaging-ktx     // ACTIVO — FCM
firebase-crashlytics-ktx   // ACTIVO — Crashlytics
```

---

## 3. Carencias Detectadas (Priorizadas)

### Prioridad ALTA

| # | Carencia | Lado | Impacto |
|---|---|---|---|
| 1 | **`FcmPushProvider` usa API HTTP legacy** | Backend | API deprecated, Google puede cortar soporte. Debe migrarse a FCM v1 HTTP API o Firebase Admin SDK |
| 2 | **No hay push fallback para unlinking** | Backend | `UserServiceImpl.unlinkCurrentUser()` solo envía WebSocket, no FCM |
| 3 | **Sin permiso `POST_NOTIFICATIONS`** | Android | En Android 13+ las notificaciones pueden ser bloqueadas silenciosamente |
| 4 | **Firebase Functions legacy activa** | Firebase | `functions/index.js` sigue enviando notificaciones por Firestore triggers — riesgo de doble envío |

### Prioridad MEDIA

| # | Carencia | Lado | Impacto |
|---|---|---|---|
| 5 | **Sin deep links en notificaciones** | Android | Tocar notificación solo abre MainActivity sin navegar a pantalla específica |
| 6 | **Sin manejo de data payload** | Android | `SignusMessagingService` solo extrae title/body, ignora campos como `targetUserId` |
| 7 | **`firebase-auth-ktx` y `firebase-firestore-ktx` obsoletas** | Android | Dependencias muertas que aumentan el APK |

### Prioridad BAJA

| # | Carencia | Lado | Impacto |
|---|---|---|---|
| 8 | **Notification ID siempre es 0** | Android | Todas las notificaciones se apilan |
| 9 | **Sin métricas de entrega** | Backend | No hay trazabilidad de si el push llegó |

---

## 4. Riesgos

| Riesgo | Severidad | Descripción |
|---|---|---|
| **FCM v1 migration** | ALTA | La API legacy `fcm.googleapis.com/fcm/send` fue deprecated. Google puede dejar de aceptar requests. Debe migrarse a FCM v1 HTTP API (`fcm.googleapis.com/v1/projects/{project}/messages:send`) |
| **Doble envío** | ALTA | Si `functions/index.js` sigue activa y el backend también envía, el usuario recibe 2 notificaciones |
| **Sin push para unlinking** | MEDIA | Si la pareja está offline y se desvincula, no recibe notificación |
| **Android 13+ bloqueo** | MEDIA | Sin `POST_NOTIFICATIONS`, las notificaciones pueden no mostrarse |
| **Servidor Key expuesta** | BAJA | `FCM_SERVER_KEY` en variable de entorno — está bien, pero la API legacy es menos segura que Admin SDK con service account |

---

## 5. Arquitectura Propuesta

### Estado actual (confirmado)

```
Backend (Ktor)
  ├── PATCH /status → StatusServiceImpl → NotificationOrchestrator
  │     ├── RealtimeNotificationServiceImpl (WebSocket)
  │     └── PartnerPushNotificationService → FcmPushProvider (HTTP legacy)
  ├── DELETE /partner → UserServiceImpl → RealtimeNotificationService (solo WebSocket)
  └── WebSocket /ws → NotificationSocketRoutes → RealtimeNotificationServiceImpl

Android
  ├── SignusMessagingService ← recibe FCM push
  ├── SemaphoreSocket ← recibe WebSocket events
  └── UserRepository ← coordina sync de tokens
```

### Lo que falta para completar la migración

```
Backend
  ├── [ ] Migrar FcmPushProvider → FCM v1 HTTP API (o Admin SDK)
  ├── [ ] Agregar push fallback para unlinking
  └── [ ] Desactivar Firebase Functions legacy

Android
  ├── [ ] Agregar permiso POST_NOTIFICATIONS
  ├── [ ] Agregar deep links en notificaciones
  ├── [ ] Manejar data payload (tipo de evento)
  └── [ ] Eliminar dependencias Firebase obsoletas
```

---

## 6. Plan de Implementación

### Fase 1: Desactivar Firebase Functions

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Eliminar el doble envío de notificaciones |
| **Archivos** | `functions/index.js`, `firebase.json` |
| **Acción** | Desactivar `sendPartnerStatusNotification` (no eliminar el proyecto aún) |
| **Criterio** | Solo el backend Ktor envía notificaciones |
| **Riesgo** | ALTO — hacer solo si se confirma que el backend ya envía |

### Fase 2: Migrar FcmPushProvider a FCM v1

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Dejar de usar API deprecated |
| **Archivos** | `features/notification/providers/FcmPushProvider.kt`, `core/config/AppConfig.kt`, `.env` |
| **Acción** | Opción A: migrar a HTTP API v1 con service account. Opción B: usar Firebase Admin SDK |
| **Criterio** | Push se envía correctamente via FCM v1 |
| **Riesgo** | Requiere configurar service account o project ID |

### Fase 3: Push fallback para unlinking

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Notificar al usuario desconectado cuando se desvincula |
| **Archivos** | `features/user/UserServiceImpl.kt`, `features/notification/NotificationOrchestrator.kt` |
| **Acción** | Crear método `notifyPartnerAboutUnlinking` en el orchestrator, usar en `UserServiceImpl.unlinkCurrentUser()` |
| **Criterio** | Si la pareja está offline, recibe push de desvinculación |
| **Riesgo** | Bajo — patrón ya existe |

### Fase 4: Permisos Android 13+

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Notificaciones funcionan en Android 13+ |
| **Archivos** | `AndroidManifest.xml`, `MainActivity.kt` |
| **Acción** | Agregar `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`, solicitar en runtime |
| **Criterio** | Notificaciones aparecen en Android 13+ |
| **Riesgo** | Bajo |

### Fase 5: Deep Links + Data Payload

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Navegar a pantalla específica al tocar notificación |
| **Archivos** | `SignusMessagingService.kt`, `AndroidManifest.xml`, `AppNavigation.kt` |
| **Acción** | Definir scheme URI, procesar data payload, navegar según tipo de evento |
| **Criterio** | Tocar notificación abre la pantalla correcta |
| **Riesgo** | Medio — requiere coordinar contrato con backend |

### Fase 6: Limpieza

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Eliminar código muerto |
| **Archivos** | `app/build.gradle.kts`, `functions/` |
| **Acción** | Eliminar `firebase-auth-ktx`, `firebase-firestore-ktx`, eliminar Firebase Functions |
| **Criterio** | App compila y funciona sin esas dependencias |
| **Riesgo** | Bajo |

---

## Resumen Ejecutivo

| Área | Estado |
|---|---|
| **Backend — Routing WS vs FCM** | ✅ Completo (`NotificationOrchestrator`) |
| **Backend — FCM Provider** | ⚠️ Funcional pero API deprecated |
| **Backend — Push para unlinking** | ❌ Falta |
| **Android — FCM Client** | ✅ Completo |
| **Android — Permisos 13+** | ❌ Falta |
| **Android — Deep Links** | ❌ Falta |
| **Firebase Functions** | ⚠️ Legacy — debe desactivarse |

La arquitectura del backend es sólida y bien diseñada. El `NotificationOrchestrator` con el patrón port/adapter es exactamente lo que se necesita. El trabajo principal es migrar el FCM provider y completar el lado Android.
