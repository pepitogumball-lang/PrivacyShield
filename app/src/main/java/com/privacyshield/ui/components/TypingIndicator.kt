package com.privacyshield.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun TypingIndicator(
    personaName: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                color = SurfaceVariantDark
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedDots()
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$personaName is typing…",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AnimatedDots() {
    var activeDot by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            activeDot = (activeDot + 1) % 3
        }
    }

    repeat(3) { i ->
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    CyanAccent.copy(alpha = if (i == activeDot) 0.9f else 0.25f),
                    CircleShape
                )
        )
    }
}
