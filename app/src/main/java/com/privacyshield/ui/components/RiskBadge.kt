package com.privacyshield.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.privacyshield.data.model.RiskLevel
import com.privacyshield.ui.theme.RiskCritical
import com.privacyshield.ui.theme.RiskHigh
import com.privacyshield.ui.theme.RiskLow
import com.privacyshield.ui.theme.RiskMedium

@Composable
fun RiskBadge(riskLevel: RiskLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = riskLevel.badgeColors()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = riskLevel.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

private fun RiskLevel.badgeColors(): Pair<Color, Color> = when (this) {
    RiskLevel.LOW -> RiskLow to RiskLow
    RiskLevel.MEDIUM -> RiskMedium to RiskMedium
    RiskLevel.HIGH -> RiskHigh to RiskHigh
    RiskLevel.CRITICAL -> RiskCritical to RiskCritical
}
