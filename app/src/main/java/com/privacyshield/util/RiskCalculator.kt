package com.privacyshield.util

import com.privacyshield.data.model.RiskLevel

object RiskCalculator {
    /**
     * Calcula el nivel de riesgo de una aplicación basándose en sus capacidades técnicas.
     * 
     * Nota: En Android estándar, tener un permiso no significa que la app esté haciendo algo malo,
     * pero representa una superficie de ataque o riesgo de privacidad potencial.
     */
    fun calculate(
        hasDangerousPermissions: Boolean,
        hasAccessibilityService: Boolean,
        hasOverlayPermission: Boolean,
        hasRecordingRisk: Boolean
    ): RiskLevel {
        var score = 0
        
        // El servicio de accesibilidad es el riesgo más alto porque puede leer pantalla y simular toques.
        if (hasAccessibilityService) score += 4
        
        // El riesgo de grabación es crítico para la privacidad de datos sensibles.
        if (hasRecordingRisk) score += 3
        
        // Los permisos de superposición (Overlay) pueden usarse para ataques de tapjacking.
        if (hasOverlayPermission) score += 2
        
        // Permisos peligrosos estándar (Cámara, Micro, Ubicación, etc.)
        if (hasDangerousPermissions) score += 1

        return when {
            score >= 6 -> RiskLevel.CRITICAL
            score >= 4 -> RiskLevel.HIGH
            score >= 2 -> RiskLevel.MEDIUM
            score > 0 -> RiskLevel.LOW
            else -> RiskLevel.LOW
        }
    }
}
