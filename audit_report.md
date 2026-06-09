# Auditoría de PrivacyShield - Informe de Ingeniería Senior

## Estado Actual
El proyecto tiene una base técnica sólida con **Kotlin, Compose, MVVM y DataStore**. Sin embargo, existe una duplicidad de servicios de monitoreo y algunas funcionalidades que parecen "stubs" o que no están plenamente integradas en la experiencia del usuario de forma honesta.

### Hallazgos Clave:
1.  **Duplicidad de Servicios:** Existen `PrivacyWatchService` (esqueleto) y `ProtectionOrchestratorService` (implementación real). Esto confunde la arquitectura y el propósito de la app.
2.  **Lógica de Protección Real:** `ProtectionOrchestratorService` ya implementa una lógica valiente: usa `AccessibilityService` y `UsageStats` para detectar la app en primer plano y muestra un overlay negro si se detecta riesgo de grabación/captura mientras una app protegida está abierta.
3.  **Honestidad Técnica:** Los comentarios en el código muestran que los desarrolladores son conscientes de las limitaciones de Android (no se puede bloquear llamadas al sistema, no se pueden matar apps sin root), pero la UI aún tiene elementos que podrían ser más claros.
4.  **Permisos:** La app solicita permisos potentes (`QUERY_ALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `BIND_ACCESSIBILITY_SERVICE`). Es crítico que el usuario entienda por qué.
5.  **Código "Muerto" o Innecesario:** Hay carpetas como `remote` (Android TV, IR) y `bothub` que parecen fuera del alcance de una app de "Privacy Shield" o son restos de otro proyecto.

## Plan de Acción

### 1. Limpieza y Consolidación de Arquitectura
*   **Eliminar `PrivacyWatchService`:** Consolidar todo en `ProtectionOrchestratorService`.
*   **Depurar paquetes irrelevantes:** Evaluar si `com.privacyshield.remote` y `com.privacyshield.bothub` pertenecen al producto. Si no aportan a la "privacidad", marcarlos para eliminación o moverlos a un módulo separado (en este caso, los eliminaré para enfocar el producto).

### 2. Refuerzo de la Lógica de Protección
*   **Mejorar `RiskCalculator`:** Hacerlo más granular.
*   **Optimizar `ProtectionOrchestrator`:** Asegurar que el overlay sea reactivo y no consuma batería excesiva.
*   **Validación de Permisos:** Añadir comprobaciones robustas antes de intentar iniciar servicios que requieren permisos especiales.

### 3. Interfaz de Usuario Honesta
*   **Pantalla de Inicio:** Mostrar claramente si la "Protección en Vivo" está activa y qué permisos faltan.
*   **Settings:** Simplificar el flujo de activación de permisos.
*   **Explicaciones:** Añadir tooltips o diálogos que expliquen qué *no* puede hacer la app (ej. "No podemos impedir que Facebook use tu micro, pero te avisamos si intentan capturar tu pantalla mientras usas tu banco").

### 4. Estabilidad y CI
*   **Compilación:** Validar con `./gradlew assembleDebug`.
*   **GitHub Actions:** Asegurar que el CI valide la compilación en cada PR.

### 5. Documentación
*   **README:** Reescribir para reflejar la funcionalidad real y los límites técnicos.
