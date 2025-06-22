// functions/index.js

const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Inicializa el SDK de Firebase Admin para interactuar con Firebase
admin.initializeApp();

/**
 * Escucha cambios en el nodo de humedad del suelo y envía una notificación
 * si el valor cae por debajo de un umbral.
 */
exports.checkHumedadBaja = functions.database.ref("/mediciones/humedad_suelo/{timestamp}")
    .onWrite(async (change, context) => {
        // Obtenemos el nuevo valor de la humedad del suelo
        const humedadActual = change.after.val();

        // Define tu umbral de humedad baja
        const UMBRAL_HUMEDAD_BAJA = 30; // Por ejemplo, 30%

        // Si el valor es null (el nodo fue eliminado) o no es un número, ignorar
        if (humedadActual === null || typeof humedadActual !== "number") {
            console.log("Valor de humedad no válido o nodo eliminado. Ignorando.");
            return null;
        }

        // Verifica si la humedad está por debajo del umbral
        if (humedadActual < UMBRAL_HUMEDAD_BAJA) {
            console.log(`¡Alerta! Humedad del suelo baja: ${humedadActual}%`);

            // 1. Obtener todos los tokens de FCM de los usuarios registrados
            // Asume que los tokens están guardados bajo /users/{userId}/fcmToken
            const usersSnapshot = await admin.database().ref("/users").once("value");
            const tokens = [];

            usersSnapshot.forEach((userSnapshot) => {
                const fcmToken = userSnapshot.child("fcmToken").val();
                if (fcmToken) {
                    tokens.push(fcmToken);
                }
            });

            // Si no hay tokens registrados, no hay a quién enviar
            if (tokens.length === 0) {
                console.log("No hay tokens de FCM registrados para enviar notificaciones.");
                return null;
            }

            // 2. Preparar el mensaje de notificación
            const payload = {
                notification: {
                    title: "🌱 ¡Alerta de Humedad Baja!",
                    body: `La humedad de tu suelo es del ${humedadActual}%, ¡necesita riego!`,
                },
                data: { // Datos personalizados para tu app (opcional)
                    sensorType: "humedad_suelo",
                    value: humedadActual.toString(),
                    threshold: UMBRAL_HUMEDAD_BAJA.toString(),
                    action: "open_humidity_chart", // Podrías usar esto en tu app para abrir el gráfico de humedad
                },
            };

            // 3. Enviar la notificación a todos los tokens y manejar errores
            try {
                const response = await admin.messaging().sendToDevice(tokens, payload);
                console.log("Notificaciones de humedad enviadas con éxito:", response);

                // Procesar los resultados para eliminar tokens inválidos
                const tokensToRemove = [];
                response.results.forEach((result, index) => {
                    const error = result.error;
                    if (error) {
                        console.error("Fallo al enviar a token:", tokens[index], error);
                        // Identificar los errores que indican un token inválido
                        if (error.code === "messaging/invalid-registration-token" ||
                            error.code === "messaging/registration-token-not-registered" ||
                            error.code === "messaging/unregistered") {
                            tokensToRemove.push(tokens[index]);
                        }
                    }
                });

                // Eliminar los tokens inválidos de tu base de datos
                const deletePromises = tokensToRemove.map((token) => {
                    // Busca el usuario que tiene este token y lo elimina
                    return admin.database().ref("/users")
                        .orderByChild("fcmToken").equalTo(token).once("child_added")
                        .then((snapshot) => {
                            if (snapshot.exists()) {
                                console.log(`Eliminando token inválido para UID: ${snapshot.key}`);
                                return snapshot.ref.remove();
                            }
                            return null;
                        })
                        .catch((error) => {
                            console.error("Error al eliminar token inválido de la base de datos:", error);
                        });
                });

                await Promise.all(deletePromises);
                if (tokensToRemove.length > 0) {
                    console.log(`Se eliminaron ${tokensToRemove.length} tokens inválidos de la base de datos.`);
                }
            } catch (error) {
                console.error("Error al enviar notificaciones de humedad:", error);
            }
        } else {
            console.log(`Humedad del suelo normal: ${humedadActual}%`);
        }

        return null;
    });

/**
 * Escucha cambios en el nodo de temperatura ambiental y envía una notificación
 * si el valor sube por encima de un umbral.
 */
exports.checkTemperaturaAlta = functions.database.ref("/mediciones/temperatura_ambiental/{timestamp}")
    .onWrite(async (change, context) => {
        const temperaturaActual = change.after.val();
        const UMBRAL_TEMPERATURA_ALTA = 35; // Por ejemplo, 35°C

        if (temperaturaActual === null || typeof temperaturaActual !== "number") {
            console.log("Valor de temperatura no válido o nodo eliminado. Ignorando.");
            return null;
        }

        if (temperaturaActual > UMBRAL_TEMPERATURA_ALTA) {
            console.log(`¡Alerta! Temperatura ambiental alta: ${temperaturaActual}°C`);

            const usersSnapshot = await admin.database().ref("/users").once("value");
            const tokens = [];
            usersSnapshot.forEach((userSnapshot) => {
                const fcmToken = userSnapshot.child("fcmToken").val();
                if (fcmToken) {
                    tokens.push(fcmToken);
                }
            });

            if (tokens.length === 0) {
                console.log("No hay tokens de FCM registrados para enviar notificaciones de temperatura.");
                return null;
            }

            const payload = {
                notification: {
                    title: "☀️ ¡Alerta de Temperatura Alta!",
                    body: `La temperatura ambiental es de ${temperaturaActual}°C, ¡demasiado alta para tu cultivo!`,
                },
                data: {
                    sensorType: "temperatura_ambiental",
                    value: temperaturaActual.toString(),
                    threshold: UMBRAL_TEMPERATURA_ALTA.toString(),
                    action: "open_temperature_chart",
                },
            };

            try {
                const response = await admin.messaging().sendToDevice(tokens, payload);
                console.log("Notificaciones de temperatura alta enviadas:", response);

                // Procesar los resultados para eliminar tokens inválidos (igual que en humedad)
                const tokensToRemove = [];
                response.results.forEach((result, index) => {
                    const error = result.error;
                    if (error) {
                        console.error("Fallo al enviar a token:", tokens[index], error);
                        if (error.code === "messaging/invalid-registration-token" ||
                            error.code === "messaging/registration-token-not-registered" ||
                            error.code === "messaging/unregistered") {
                            tokensToRemove.push(tokens[index]);
                        }
                    }
                });

                const deletePromises = tokensToRemove.map((token) => {
                    return admin.database().ref("/users")
                        .orderByChild("fcmToken").equalTo(token).once("child_added")
                        .then((snapshot) => {
                            if (snapshot.exists()) {
                                console.log(`Eliminando token inválido para UID: ${snapshot.key}`);
                                return snapshot.ref.remove();
                            }
                            return null;
                        })
                        .catch((error) => {
                            console.error("Error al eliminar token inválido de la base de datos:", error);
                        });
                });

                await Promise.all(deletePromises);
                if (tokensToRemove.length > 0) {
                    console.log(`Se eliminaron ${tokensToRemove.length} tokens inválidos de la base de datos.`);
                }
            } catch (error) {
                console.error("Error al enviar notificaciones de temperatura:", error);
            }
        } else {
            console.log(`Temperatura ambiental normal: ${temperaturaActual}°C`);
        }

        return null;
    });