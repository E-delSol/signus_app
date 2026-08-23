# Revisión Arquitectónica — Sistema de Notificaciones

Fecha: 2026-06-28

---

## 1. Diagnóstico Actual

### Problema Central: Dos caminos de notificación sin punto único de verdad

El sistema actual tiene **dos caminos separados** para enviar notificaciones, sin un único punto de entrada:

```
┌─────────────────────┐          ┌──────────────────────────────────────┐
│ StatusServiceImpl   │ ──────►  │ NotificationOrchestrator             │
│ (PATCH /status)     │          │ → notifyPartnerAboutStatusChange()   │
└─────────────────────┘          │   → notifySelfStatusChanged()        │
                                 │   → notifyPartnerStatusChanged()     │
                                 │   → partnerPushNotificationService   │
                                 └──────────────────────────────────────┘

┌─────────────────────┐          ┌──────────────────────────────────────┐
│ UserServiceImpl     │ ──────►  │ RealtimeNotificationService          │
│ (unlinkCurrentUser) │          │ → notifyPartnerUnlinked()            │
│                     │          │   ❌ Sin FCM fallback                │
└─────────────────────┘          └──────────────────────────────────────┘
```

**Consecuencias:**
- `PARTNER_UNLINKED` no tiene fallback a FCM (gap funcional)
- No hay un único punto para agregar nuevos eventos
- Los tests están acoplados a un servicio específico

---

## 2. Modelo de Eventos

### Eventos actuales

| Evento | Productor | Canal | FCM Fallback |
|---|---|---|---|
| `PARTNER_STATUS_CHANGED` | `StatusServiceImpl` | WebSocket + FCM | ✅ |
| `SELF_STATUS_CHANGED` | `StatusServiceImpl` | WebSocket | ❌ (self) |
| `PARTNER_UNLINKED` | `UserServiceImpl` | WebSocket | ❌ (gap) |

### Eventos futuros planificados

| Evento | Productor | Canal | FCM Fallback |
|---|---|---|---|
| `PARTNER_LINKED` | `UserServiceImpl` | WebSocket | ✅ |
| `PARTNER_DEVICE_CHANGED` | `DeviceTokenRepository` | WebSocket | ❌ (info only) |

### Modelo de payload unificado

Todos los eventos WebSocket comparten esta estructura base:

```json
{
  "type": "<EVENT_TYPE>",
  "timestamp": 1710930000000,
  "<contexto_del_evento>"
}
```

---

## 3. ADRs (Architecture Decision Records)

### ADR-001: Dispatcher Unificado

**Decisión:** Crear `NotificationDispatcher` como reemplazo de `NotificationOrchestrator`.

**Estado:** Propuesto

**Contexto:**
- Existen dos caminos de notificación sin un único punto de entrada
- Agregar nuevos eventos requiere modificar múltiples clases
- `PARTNER_UNLINKED` no tiene fallback a FCM

**Consecuencias:**
- ✅ Único punto de entrada para todos los eventos
- ✅ Fácil de agregar nuevos eventos
- ✅ Fallback a FCM centralizado
- ✅ Tests más fáciles de mantener
- ⚠️ Requiere refactor de `StatusServiceImpl` y `UserServiceImpl`

**Implementación:**

```kotlin
// features/notification/NotificationDispatcher.kt
interface NotificationDispatcher {
    suspend fun dispatch(event: NotificationEvent)
}

sealed class NotificationEvent {
    data class PartnerStatusChanged(
        val partnerId: String,
        val status: String,
        val statusExpiration: Instant?
    ) : NotificationEvent()

    data class SelfStatusChanged(
        val userId: String,
        val status: String,
        val statusExpiration: Instant?
    ) : NotificationEvent()

    data class PartnerUnlinked(
        val partnerId: String
    ) : NotificationEvent()

    data class PartnerLinked(
        val partnerId: String
    ) : NotificationEvent()
}
```

---

### ADR-002: Dispatcher como servicio de dominio

**Decisión:** `NotificationDispatcher` vive en `features/notification/` como servicio de dominio.

**Estado:** Propuesto

**Contexto:**
- El orchestrator actual está en `features/notification/`
- Es un servicio que orquesta otros servicios
- No depende de infraestructura específica

**Consecuencias:**
- ✅ Sigue la convención del proyecto
- ✅ Inyectado vía Koin
- ✅ Desacoplado de infraestructura

---

### ADR-003: FCM con payload mínimo

**Decisión:** FCM solo transporta `type` + `targetUserId`. La app sincroniza estado desde el backend.

**Estado:** Propuesto

**Contexto:**
- FCM tiene límite de 4KB
- El estado puede cambiar entre el envío y la entrega
- El backend es la fuente de verdad

**Consecuencias:**
- ✅ Payload pequeño y confiable
- ✅ La app siempre tiene estado fresco
- ⚠️ Requiere un endpoint para sync después de wake-up
- ⚠️ Un paso extra en la app

---

### ADR-004: Self-events bypass dispatcher

**Decisión:** `SELF_STATUS_CHANGED` va directo por WebSocket, sin pasar por el dispatcher.

**Estado:** Propuesto

**Contexto:**
- El usuario siempre tiene una sesión activa cuando cambia su propio estado
- No necesita FCM fallback
- Es una optimización

**Consecuencias:**
- ✅ Un paso menos en el flujo más común
- ✅ El usuario siempre está online cuando self-emite
- ⚠️ Excepción a la regla del dispatcher único

---

### ADR-005: Sin persistencia de eventos

**Decisión:** No persistir eventos. El estado en la BD es la fuente de verdad.

**Estado:** Propuesto

**Contexto:**
- Los eventos son notificaciones, no transacciones
- La app puede sync cuando reconecta
- Simplifica la arquitectura

**Consecuencias:**
- ✅ Menos complejidad
- ✅ No hay cleanup de eventos viejos
- ⚠️ Si el usuario pierde notificaciones, puede sync manualmente

---

## 4. Reglas Arquitectónicas

1. **FCM es solo transporte.** Nunca lleva estado de dominio completo.
2. **Backend es fuente de verdad.** La app nunca decide entre WebSocket y FCM.
3. **Un punto de entrada.** Todos los eventos pasan por `NotificationDispatcher` (excepto self-events).
4. **Eventos son efímeros.** No se persisten. El estado en BD es la fuente de verdad.
5. **Fallback centralizado.** La lógica de "si falla WebSocket, intenta FCM" vive en un solo lugar.
6. **Tokens son responsabilidad del backend.** El backend decide a qué tokens enviar.
7. **FCM payload mínimo.** Solo `type` + `targetUserId`. Nada más.
8. **App sync después de wake-up.** Si recibe FCM, pide estado al backend.
9. **Nuevos eventos son faciles.** Agregar un evento solo requiere: (a) definir el sealed class, (b) implementar la lógica de dispatch.
10. **Tests mockean el dispatcher.** Los productores no necesitan saber de WebSocket o FCM.

---

## 5. Diagrama de Arquitectura Objetivo

```
┌─────────────────────────────────────────────────────────────┐
│                    Backend (Ktor)                            │
│                                                              │
│  ┌──────────────┐    ┌─────────────────────────────────────┐ │
│  │ UserService  │    │ NotificationDispatcher              │ │
│  │ StatusService│───►│ (servicio de dominio)               │ │
│  └──────────────┘    │                                     │ │
│                      │  ┌─────────────────────────────┐    │ │
│                      │  │ RealtimeNotificationService  │    │ │
│                      │  │ (WebSocket sessions)         │    │ │
│                      │  └─────────────────────────────┘    │ │
│                      │                                     │ │
│                      │  ┌─────────────────────────────┐    │ │
│                      │  │ PartnerPushNotificationService│   │ │
│                      │  │ (FCM delivery)               │    │ │
│                      │  └─────────────────────────────┘    │ │
│                      └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
              WebSocket             FCM Push
                    │                   │
┌───────────────────┴───────────────────┴──────────────────────┐
│                    Android App                                │
│                                                               │
│  ┌──────────────┐    ┌─────────────────────────────────────┐  │
│  │ Semaphore    │    │ SignusMessagingService              │  │
│  │ Socket       │◄───│ (FCM → sync from backend)           │  │
│  └──────────────┘    └─────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

---

## 6. Backlog SDD

### Fase 1: Backend — Dispatcher Unificado
- [ ] 1.1 Crear `NotificationDispatcher` interface + sealed class `NotificationEvent`
- [ ] 1.2 Implementar `NotificationDispatcherImpl` con lógica de dispatch
- [ ] 1.3 Crear adapter para `RealtimeNotificationService`
- [ ] 1.4 Crear adapter para `PartnerPushNotificationService`
- [ ] 1.5 Actualizar `StatusServiceImpl` para usar dispatcher
- [ ] 1.6 Actualizar `UserServiceImpl` para usar dispatcher
- [ ] 1.7 Actualizar Koin modules
- [ ] 1.8 Crear tests unitarios para dispatcher
- [ ] 1.9 Eliminar `NotificationOrchestrator` (migrado a dispatcher)

### Fase 2: Backend — FCM Fallback para Unlinking
- [ ] 2.1 Agregar fallback FCM en dispatcher para `PartnerUnlinked`
- [ ] 2.2 Crear adapter para `DeviceTokenLookupPort`
- [ ] 2.3 Crear adapter para `PartnerLookupPort`
- [ ] 2.4 Crear tests para fallback de unlinking

### Fase 3: Android — Permisos
- [ ] 3.1 Agregar `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`
- [ ] 3.2 Solicitar permiso en runtime (Android 13+)
- [ ] 3.3 Manejar caso de permiso denegado

### Fase 4: Android — Deep Links
- [ ] 4.1 Definir URI scheme para notificaciones
- [ ] 4.2 Configurar intent filter en AndroidManifest
- [ ] 4.3 Implementar navegación desde notificación
- [ ] 4.4 Agregar data payload en FCM para context

### Fase 5: Backend — Limpieza de Tokens
- [ ] 5.1 Crear job periódico para limpiar tokens stale
- [ ] 5.2 Manejar errores FCM (`UNREGISTERED`, `INVALID_ARGUMENT`)
- [ ] 5.3 Notificar a la app cuando token es inválido

### Fase 6: Backend — Migración FCM v1
- [ ] 6.1 Migrar `FcmPushProvider` de HTTP legacy a FCM v1
- [ ] 6.2 Usar OAuth2 en vez de server key
- [ ] 6.3 Actualizar tests
- [ ] 6.4 Eliminar código legacy

---

## 7. Orden de Ejecución Recomendado

```
Fase 1 (Dispatcher) ──► Fase 2 (FCM Fallback) ──► Fase 5 (Limpieza) ──► Fase 6 (FCM v1)
                                                         │
Fase 3 (Permisos) ──► Fase 4 (Deep Links) ───────────────┘
(independiente)        (depende de FCM data payload)
```

**Justificación:**
- Fase 1 es la base para todo lo demás
- Fase 2 cierra el gap funcional de unlinking
- Fase 3 es independiente y de alto impacto
- Fase 4 requiere FCM data payload (Fase 2)
- Fase 5 y 6 son mejoras técnicas, no bloqueantes

---

## 8. Decisiones Pendientes

| Decisión | Pregunta | Impacto |
|---|---|---|
| **Deep links** | ¿Qué pantalla se abre al tocar una notificación? | Navegación |
| **Limpieza de tokens** | ¿Job periódico o bajo demanda? | Performance |
| **Firebase Functions** | ¿Eliminar o mantener como backup? | Mantenimiento |
| **FCM v1** | ¿Ahora o como mejora posterior? | Infraestructura |

---

## 9. Referencias

- Auditoría: `NOTIFICATION_AUDIT.md`
- Validación: `NOTIFICATION_VALIDATION.md`
- Arquitectura backend: `ARCHITECTURE.md`
- Plan de notificaciones: `REALTIME_NOTIFICATIONS_PLAN.md`
