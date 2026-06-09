# PrivacyShield — Android Privacy Audit & Monitoring

PrivacyShield es una herramienta de auditoría de seguridad para Android diseñada para identificar aplicaciones con capacidades intrusivas y proporcionar una capa de monitoreo reactivo contra la captura de pantalla no autorizada.

## 🛡️ Lo que PrivacyShield HACE por ti
*   **Auditoría de Riesgos:** Escanea todas las aplicaciones instaladas y las clasifica según su potencial de riesgo (Servicios de Accesibilidad, Superposición de Pantalla, Grabación de Audio/Video y Permisos Peligrosos).
*   **Monitoreo en Vivo:** Utiliza un servicio en primer plano que detecta cuándo una aplicación marcada como "Protegida" está abierta simultáneamente con una aplicación sospechosa o una herramienta de captura de pantalla.
*   **Bloqueo de Pantalla (Overlay):** Si se detecta un riesgo de captura mientras usas una app protegida, PrivacyShield despliega un protector negro sobre la pantalla para evitar la filtración de datos sensibles.
*   **100% Offline:** Todo el procesamiento ocurre localmente. Sin telemetría, sin analíticas, sin permisos de internet para el núcleo de privacidad.

## ⚠️ Límites Técnicos (Honestidad Android)
Debido a las restricciones de seguridad de Android estándar (sin Root), PrivacyShield tiene los siguientes límites:
1.  **No puede "Matar" otras Apps:** Android impide que una app cierre a otra. Nuestra protección es visual (overlay) para proteger tus datos, no un cierre forzado del proceso atacante.
2.  **Detección Heurística:** La detección de captura de pantalla se basa en cambios en las rutas de medios y estados de visualización. Puede no detectar herramientas de captura extremadamente avanzadas o de sistema.
3.  **Dependencia de Permisos:** Para funcionar correctamente, requiere que el usuario conceda manualmente:
    *   **Servicio de Accesibilidad:** Para detectar qué app está en primer plano.
    *   **Acceso a Uso:** Como respaldo para la detección de aplicaciones.
    *   **Mostrar sobre otras apps:** Para poder desplegar el protector de pantalla.

## 🛠️ Stack Tecnológico
*   **Lenguaje:** Kotlin
*   **UI:** Jetpack Compose + Material 3
*   **Arquitectura:** MVVM + StateFlow
*   **Persistencia:** DataStore Preferences
*   **Mínimo SDK:** 26 (Android 8.0)
*   **Target SDK:** 34 (Android 14)

## 🚀 Compilación
### Requisitos
*   JDK 17
*   Android SDK (Platform 34)

### Comando
```bash
./gradlew assembleDebug
```
El APK resultante se encontrará en: `app/build/outputs/apk/debug/app-debug.apk`

## 📄 Licencia
MIT — Ver archivo [LICENSE](LICENSE) para más detalles.
