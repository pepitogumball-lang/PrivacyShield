package com.privacyshield.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.privacyshield.remote.viewmodel.RemoteConnectionState
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.RiskHigh
import com.privacyshield.ui.theme.RiskMedium
import com.privacyshield.ui.theme.TextSecondary

@Composable
fun ConnectionStatusChip(state: RemoteConnectionState, modifier: Modifier = Modifier) {
    val (label, tint) = when (state) {
        is RemoteConnectionState.Connected -> "Connected" to CyanAccent
        is RemoteConnectionState.Connecting -> "Connecting…" to RiskMedium
        is RemoteConnectionState.PairingRequired -> "Pairing" to RiskMedium
        is RemoteConnectionState.Disconnected -> "Disconnected" to TextSecondary
        is RemoteConnectionState.Error -> "Error" to RiskHigh
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(tint, CircleShape)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                color = tint,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Overload that accepts a plain String + Color for ad-hoc use. */
@Composable
fun StatusChip(label: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(tint, CircleShape)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                color = tint,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
