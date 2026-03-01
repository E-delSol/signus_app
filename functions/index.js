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
      const partnerDoc = await admin.firestore()
          .collection("users")
          .doc(partnerId)
          .get();

      if (!partnerDoc.exists) {
        return null;
      }

      const partnerToken = partnerDoc.data().fcmToken;
      if (!partnerToken) {
        console.log("La pareja no tiene token FCM");
        return null;
      }

      // 3. Preparamos el mensaje
      let title = "Signus";
      let body = "El estado de tu pareja ha cambiado.";

      if (newData.status === "AVAILABLE") {
        title = "¡Tu pareja está Disponible!";
        body = "Es un buen momento para conectar 🟢";
      } else if (newData.status === "BUSY") {
        title = "Tu pareja está Ocupada";
        body = "Mejor espera un poco 🔴";
      }

      // 4. Configuración para ALTA PRIORIDAD
      const message = {
        token: partnerToken,
        notification: {
          title: title,
          body: body,
        },
        android: {
          priority: "high",
          notification: {
            channelId: "signus_channel",
            priority: "high",
            defaultSound: true,
          },
        },
        data: {
          status: newData.status,
        },
      };

      // 5. Enviar
      return admin.messaging().send(message)
          .then(() => {
            console.log("Notificación enviada");
          })
          .catch((error) => {
            console.error("Error:", error);
          });
    });
