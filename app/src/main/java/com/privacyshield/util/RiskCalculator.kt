package com.privacyshield.util

import com.privacyshield.data.model.RiskLevel

object RiskCalculator {

    fun calculate(
        hasDangerousPermissions: Boolean,
        hasAccessibilityService: Boolean,
        hasOverlayPermission: Boolean,
        hasRecordingRisk: Boolean
    ): RiskLevel {
        var score = 0
        // Each category adds one point; accessibility and overlay are weighted higher.
        if (hasDangerousPermissions) score += 1
        if (hasRecordingRisk) score += 1
        if (hasOverlayPermission) score += 1
        if (hasAccessibilityService) score += 2 // Strongest capability — deserves extra weight
        return RiskLevel.fromScore(score)
    }
}
