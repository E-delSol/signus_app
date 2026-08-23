# Informe de Validación — Sistema de Notificaciones

Fecha: 2026-06-28

---

## 1. Conclusiones Confirmadas

### Backend — Componentes existentes

| Afirmación | Evidencia | Estado |
|---|---|---|
| Existe `NotificationOrchestrator` | `features/notification/NotificationOrchestrator.kt:10` | ✅ Confirmado |
| Existe `RealtimeNotificationServiceImpl` | `features/notification/RealtimeNotificationServiceImpl.kt:12` | ✅ Confirmado |
| Existe `PartnerPushNotificationService` | `features/notification/PartnerPushNotificationService.kt:6` | ✅ Confirmado |
| Existe `FcmPushProvider` | `features/notification/providers/FcmPushProvider.kt:16` | ✅ Confirmado |
| Existe `DeviceTokenRepository` | `features/devicetoken/DeviceTokenRepository.kt:17` | ✅ Confirmado |
| Tabla `user_device_tokens` | `features/devicetoken/DeviceTokenTable.kt:5` | ✅ Confirmado |
| WebSocket `/ws` con JWT | `features/notification/NotificationSocketRoutes.kt:22` | ✅ Confirmado |
| Backend usa API HTTP legacy de FCM | `features/notification/providers/FcmPushProvider.kt:18` | ✅ Confirmado |
| Android sincroniza token FCM | `data/repository/UserRepositoryImpl.kt:161` (`syncFcmToken`) | ✅ Confirmado |
| Android registra token en backend | `data/remote/DeviceApi.kt:13` (PUT `/devices/fcm-token`) | ✅ Confirmado |
| Logout desactiva token | `data/repository/AuthRepositoryImpl.kt:94-97` | ✅ Confirmado |

### Backend — Flujo de status change

| Paso | Evidencia |
|---|---|
| `PATCH /status` → `StatusServiceImpl.updateStatus()` | `features/semaphore/StatusServiceImpl.kt:14` |
| → `notificationOrchestrator.notifyPartnerAboutStatusChange()` | `features/semaphore/StatusServiceImpl.kt:18` |
| → `realtimeNotificationService.notifySelfStatusChanged()` | `features/notification/NotificationOrchestrator.kt:21` |
| → `realtimeNotificationService.notifyPartnerStatusChanged()` | `features/notification/NotificationOrchestrator.kt:44` |
| → Si falla → `partnerPushNotificationService.notifyUserDevices()` | `features/notification/NotificationOrchestrator.kt:49-54` |

---

## 2. Conclusiones Incorrectas (Corregidas)

### ERROR 1: "Existe un único punto de entrega de eventos"

**La auditoría inicial asumió que el `NotificationOrchestrator` es el único punto de entrada para notificaciones. Esto es FALSO.**

Existen **dos caminos separados**:

| Camino | Quién lo invoca | Eventos | ¿Pasa por orchestrator? |
|---|---|---|---|
| **Camino A** | `StatusServiceImpl` | `PARTNER_STATUS_CHANGED`, `SELF_STATUS_CHANGED` | ✅ Sí |
| **Camino B** | `UserServiceImpl.unlinkCurrentUser()` | `PARTNER_UNLINKED` | ❌ No |

**Evidencia:**

```kotlin
// UserServiceImpl.kt:80 — LLAMA DIRECTAMENTE al servicio de realtime
realtimeNotificationService.notifyPartnerUnlinked(
    targetUserId = partnerId,
    event = PartnerUnlinkedEvent(...)
)
```

**Consecuencia:** El `PARTNER_UNLINKED` **nunca pasa por el orchestrator**, por lo tanto **no tiene fallback a FCM**. Si la pareja está offline cuando se desvincula, **no recibe notificación**.

### ERROR 2: "deliveredRealtime significa que el usuario recibió el evento"

**`deliveredRealtime` NO confirma recepción del cliente.**

```kotlin
// RealtimeNotificationServiceImpl.kt:58-60
val sent = runCatching {
    session.send(Frame.Text(payload))
}.isSuccess
```

`deliveredRealtime = true` significa:
- ✅ La sesión WebSocket existe
- ✅ `session.send()` no lanzó excepción
- ❌ NO significa que el cliente procesó el mensaje
- ❌ NO significa que el cliente confirmó recepción

Es una confirmación de **entrega al buffer del socket**, no de **recepción aplicada**.

### ERROR 3: "El sistema está prácticamente terminado"

**El sistema tiene gaps funcionales reales:**

1. **Unlinking sin FCM fallback** — gap funcional confirmado
2. **Sin `POST_NOTIFICATIONS`** — Android 13+ puede bloquear notificaciones
3. **Sin deep links** — notificaciones no navegan a pantalla específica
4. **Sin limpieza de tokens inválidos** — tokens stale se acumulan
5. **Sin manejo de errores FCM** — `UNREGISTERED`, `INVALID_ARGUMENT` no se procesan

---

## 3. Conclusiones No Verificadas

| Afirmación | Problema |
|---|---|
| "Firebase Functions causan doble envío" | **No verificable localmente.** La función escucha `users/{userId}` en Firestore. Si el backend ya no escribe en Firestore (usa su propia BD), la función **nunca se dispara**. Es código muerto, no causante de duplicados. Requiere verificación en Firebase Console. |
| "El backend decide entre WebSocket y FCM" | **Parcialmente correcto.** Solo para `PARTNER_STATUS_CHANGED`. Para `PARTNER_UNLINKED`, el backend **siempre** usa WebSocket, sin fallback. |

---

## 4. Catálogo de Eventos

| Evento | Productor | Consumidor | Canal | ¿Pasa por Orchestrator? | FCM Fallback? |
|---|---|---|---|---|---|
| `PARTNER_STATUS_CHANGED` | `StatusServiceImpl` → `NotificationOrchestrator` | Android `SemaphoreSocket` | WebSocket + FCM | ✅ Sí | ✅ Sí |
| `SELF_STATUS_CHANGED` | `StatusServiceImpl` → `NotificationOrchestrator` | Android `SemaphoreSocket` | WebSocket | ✅ Sí | ❌ No (self) |
| `PARTNER_UNLINKED` | `UserServiceImpl` → `RealtimeNotificationService` | Android `SemaphoreSocket` | WebSocket | ❌ No | ❌ No |
| `PARTNER_LINKED` | **No existe** | N/A | N/A | N/A | N/A |

### Payloads

**`PARTNER_STATUS_CHANGED`** (WebSocket):
```json
{
  "type": "PARTNER_STATUS_CHANGED",
  "partnerId": "user-1",
  "status": "AVAILABLE",
  "statusExpiration": null,
  "timestamp": 1710930000000
}
```

**`SELF_STATUS_CHANGED`** (WebSocket):
```json
{
  "type": "SELF_STATUS_CHANGED",
  "userId": "user-1",
  "status": "BUSY",
  "statusExpiration": null,
  "timestamp": 1710930000000
}
```

**`PARTNER_UNLINKED`** (WebSocket):
```json
{
  "type": "PARTNER_UNLINKED",
  "partnerId": "user-1",
  "timestamp": 1710930000000
}
```

**FCM Push** (solo para `PARTNER_STATUS_CHANGED` cuando offline):
```json
{
  "to": "<fcm_token>",
  "notification": {
    "title": "Estado actualizado",
    "body": "Tu pareja ahora está AVAILABLE"
  },
  "data": {
    "targetUserId": "partner-1"
  }
}
```

---

## 5. Riesgos Reales

### Riesgos Funcionales

| Riesgo | Severidad | Descripción |
|---|---|---|
| **Unlinking sin FCM** | ALTA | Si la pareja está offline y se desvincula, no recibe notificación. El evento se pierde. |
| **Android 13+ bloqueo** | MEDIA | Sin `POST_NOTIFICATIONS`, las notificaciones pueden no mostrarse en Android 13+. |
| **Tokens stale** | MEDIA | No hay limpieza automática de tokens inválidos. Si un usuario desinstala la app sin hacer logout, el token sigue activo en BD. |
| **Login con otro usuario** | BAJA | Al hacer login con otra cuenta, el token FCM del usuario anterior se registra bajo el nuevo userId. El usuario anterior pierde su token. |

### Riesgos Arquitectónicos

| Riesgo | Severidad | Descripción |
|---|---|---|
| **Orchestrator acoplado a status** | MEDIA | `NotificationOrchestrator` solo tiene `notifyPartnerAboutStatusChange()`. No es reutilizable para otros eventos sin modificación. |
| **Dos caminos de notificación** | MEDIA | `StatusServiceImpl` usa orchestrator. `UserServiceImpl` llama directamente a `RealtimeNotificationService`. Inconsistencia arquitectónica. |
| **`deliveredRealtime` no es confiable** | BAJA | El booleano solo confirma que `send()` no falló, no que el cliente recibió el evento. |

### Mejoras Técnicas

| Mejora | Prioridad | Descripción |
|---|---|---|
| **Migrar FcmPushProvider a FCM v1** | Recomendada | API legacy deprecated. No es bloqueante hoy, pero Google puede cortar soporte. |
| **Deep links en notificaciones** | Recomendada | Tocar notificación solo abre MainActivity. |
| **Manejo de data payload** | Recomendada | `SignusMessagingService` ignora campos como `targetUserId`. |
| **Eliminar dependencias Firebase obsoletas** | Baja | `firebase-auth-ktx` y `firebase-firestore-ktx` no se usan en Android. |

### Deuda Técnica

| Deuda | Descripción |
|---|---|
| **Sin limpieza de tokens** | No hay job periódico que desactive tokens de dispositivos desinstalados. |
| **Sin manejo de errores FCM** | `FcmPushProvider` no distingue `UNREGISTERED`, `INVALID_ARGUMENT`, etc. |
| **Firebase Functions muertas** | `functions/index.js` probablemente nunca se dispara (backend no escribe en Firestore). |
| **Notification ID = 0** | Todas las notificaciones se apilan en Android. |

---

## 6. Validación de Firebase Functions

**La función `sendPartnerStatusNotification` escucha cambios en `users/{userId}` en Firestore.**

El backend Ktor **NO escribe en Firestore** — usa su propia BD PostgreSQL con Exposed.

**Conclusión:** La función **nunca se dispara** con el flujo actual. Es código muerto. No causa doble envío.

**Verificación necesaria:** Confirmar en Firebase Console que la función está desplegada y si ha recibido invocaciones recientes.

---

## 7. Ciclo de Vida del Token FCM en Android

| Evento | Acción | Evidencia |
|---|---|---|
| **App inicia (SemaphoreViewModel.init)** | `userRepository.syncFcmToken()` | `presentation/semaphore/SemaphoreViewModel.kt:43` |
| **Login** | `userRepository.syncFcmToken()` | `data/repository/AuthRepositoryImpl.kt:48` |
| **Register** | `userRepository.syncFcmToken()` | `data/repository/AuthRepositoryImpl.kt:78` |
| **Firebase rota token** | `onNewToken()` → `registerOrUpdateDeviceToken()` | `data/service/SignusMessagingService.kt:29-34` |
| **Logout** | `deactivateDeviceToken(deviceId)` | `data/repository/AuthRepositoryImpl.kt:94-97` |

### Gaps en el ciclo de vida

| Gap | Impacto |
|---|---|
| **Login con otro usuario** | El token FCM del usuario anterior se registra bajo el nuevo userId. El usuario anterior pierde su token. |
| **Logout falla silenciosamente** | `deactivateDeviceToken` está envuelto en `runCatching`. Si falla, el token queda activo en BD. |
| **Sin desactivación en logout forzado** | Si la app se fuerza a cerrar, el token no se desactiva. |

---

## 8. Recomendaciones para el SDD

### Partes suficientemente claras para SDD

1. **Flujo de status change** — Completamente verificado, con tests
2. **Almacenamiento de tokens** — Tabla, repositorio, endpoints, todo claro
3. **WebSocket client Android** — Funcional, parsea los 3 eventos
4. **Ciclo de vida básico del token** — Login, register, logout, onNewToken

### Aspectos que requieren decisión antes del SDD

| Aspecto | Pregunta |
|---|---|
| **Unlinking + FCM** | ¿Se implementa push fallback para `PARTNER_UNLINKED`? Si sí, ¿debe pasar por el orchestrator o crear un nuevo orchestrator? |
| **Orchestrator extensible** | ¿Se generaliza el orchestrator para soportar múltiples eventos o se mantiene específico para status? |
| **Firebase Functions** | ¿Se eliminan o se mantienen como backup? |
| **FCM v1 migration** | ¿Se hace ahora o como mejora posterior? |
| **Deep links** | ¿Qué pantalla se abre al tocar una notificación de status change? ¿Qué pasa con unlinking? |
| **Limpieza de tokens** | ¿Se implementa job periódico en backend o se maneja bajo demanda? |

---

## 9. Prioridades Revisadas

El orden de la auditoría inicial era:

```
1. Desactivar Firebase Functions
2. Migrar FcmPushProvider a FCM v1
3. Push fallback para unlinking
4. Permisos Android 13+
5. Deep Links + Data Payload
6. Limpieza
```

### Orden recomendado (corregido)

```
1. Permisos Android 13+           [Independent, quick win, high impact]
2. Push fallback para unlinking   [Functional gap, high impact]
3. Deep Links + Data Payload      [UX improvement, medium impact]
4. Limpieza de tokens             [Technical debt, medium impact]
5. Migrar FcmPushProvider a FCM v1 [Infrastructure, recommended but not blocking]
6. Desactivar Firebase Functions   [After confirming backend handles all cases]
```

**Justificación:**
- Los permisos de Android 13+ son rápidos y eliminan un riesgo real de que no se muestren notificaciones
- El fallback de unlinking es un gap funcional que debe cerrarse antes de pulir
- La migración a FCM v1 es recomendada pero no bloqueante — la API legacy sigue funcionando
- Las Firebase Functions son código muerto actualmente, no causan problemas inmediatos
