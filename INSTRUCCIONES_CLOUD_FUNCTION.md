# Guía para desplegar Cloud Functions en Signus

Para que las notificaciones lleguen al dispositivo de la pareja aunque la app esté cerrada, necesitamos una pequeña función en la "nube" de Firebase. Sigue estos pasos:

## 1. Requisitos Previos
Asegúrate de tener instalado **Node.js** en tu ordenador.
Luego, instala las herramientas de línea de comandos de Firebase:
```bash
npm install -g firebase-tools
```

## 2. Inicializar Firebase en el proyecto
Abre una terminal en la carpeta raíz de este proyecto (`/home/edelsol/AndroidStudioProjects/Duo`) y ejecuta:

```bash
firebase login
firebase init functions
```

Durante la configuración:
1.  Selecciona **"Use an existing project"** y elige tu proyecto de Firebase.
2.  Selecciona **JavaScript** como lenguaje.
3.  Di **"y"** (Yes) si te pregunta por instalar dependencias.

Esto creará una carpeta llamada `functions`.

## 3. Poner el Código
Ve a la carpeta `functions` que se acaba de crear y abre el archivo `index.js`.
Borra todo su contenido y pega el siguiente código:

```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendPartnerStatusNotification = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const oldData = change.before.data();

        // 1. Verificamos si el estado ha cambiado
        if (newData.status === oldData.status) {
            return null;
        }

        const partnerId = newData.partnerId;
        if (!partnerId) {
            console.log("Usuario sin pareja");
            return null;
        }

        // 2. Buscamos a la pareja para obtener su token
        const partnerDoc = await admin.firestore().collection("users").doc(partnerId).get();
        
        if (!partnerDoc.exists) return null;

        const partnerToken = partnerDoc.data().fcmToken;
        if (!partnerToken) {
            console.log("La pareja no tiene token FCM");
            return null;
        }

        // 3. Preparamos el mensaje
        let title = "Signus";
        let body = "El estado de tu pareja ha cambiado.";
        
        if (newData.status === "AVAILABLE") {
            title = "¡Estás Disponible!";
            body = "Tu pareja ahora está receptiva 🟢";
        } else if (newData.status === "BUSY") {
            title = "Ocupado";
            body = "Tu pareja necesita espacio 🔴";
        }

        // 4. Configuración para ALTA PRIORIDAD (Suena aunque esté bloqueado)
        const message = {
            token: partnerToken,
            notification: {
                title: title,
                body: body
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "signus_channel", // Coincide con tu app Android
                    priority: "high",
                    defaultSound: true
                }
            },
            data: {
                status: newData.status
            }
        };

        // 5. Enviar
        return admin.messaging().send(message)
            .then(() => console.log("Notificación enviada"))
            .catch(error => console.error("Error:", error));
    });
```

## 4. Desplegar
Vuelve a la terminal (en la raíz del proyecto o en la carpeta functions) y ejecuta:

```bash
firebase deploy --only functions
```

¡Listo! Una vez desplegado, cuando cambies el estado en la app, Firebase detectará el cambio y enviará la notificación al otro móvil en cuestión de segundos.
