package com.privacyshield.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.privacyshield.remote.model.RemoteCommand
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextPrimary
import com.privacyshield.ui.theme.TextSecondary

@Composable
fun RemotePad(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Navigation row (Home / Back / Menu) ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavButton(
                icon = Icons.Default.Home,
                label = "Home",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.HOME) }
            )
            NavButton(
                icon = Icons.Default.ArrowBack,
                label = "Back",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.BACK) }
            )
            NavButton(
                icon = Icons.Default.MoreVert,
                label = "Menu",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.MENU) }
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── D-pad ─────────────────────────────────────────────────────────
        DPad(enabled = enabled, onCommand = onCommand)

        Spacer(Modifier.height(20.dp))

        // ── Media controls (Rewind / Play-Pause / Fast-Forward) ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MediaButton(
                icon = Icons.Default.FastRewind,
                label = "⏮",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.MEDIA_REWIND) }
            )
            MediaButton(
                icon = Icons.Default.PlayArrow,
                label = "⏯",
                enabled = enabled,
                primary = true,
                onClick = { onCommand(RemoteCommand.MEDIA_PLAY_PAUSE) }
            )
            MediaButton(
                icon = Icons.Default.FastForward,
                label = "⏭",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.MEDIA_FAST_FORWARD) }
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Volume row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VolumeButton(
                icon = Icons.Default.VolumeDown,
                label = "Vol −",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.VOLUME_DOWN) }
            )
            VolumeButton(
                icon = Icons.Default.VolumeMute,
                label = "Mute",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.VOLUME_MUTE) }
            )
            VolumeButton(
                icon = Icons.Default.VolumeUp,
                label = "Vol +",
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.VOLUME_UP) }
            )
        }
    }
}

// ── D-Pad ─────────────────────────────────────────────────────────────────

@Composable
private fun DPad(enabled: Boolean, onCommand: (RemoteCommand) -> Unit) {
    val padSize = 54.dp
    val centerSize = 72.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DPadArrow(
            icon = Icons.Default.KeyboardArrowUp,
            size = padSize,
            enabled = enabled,
            onClick = { onCommand(RemoteCommand.DPAD_UP) }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            DPadArrow(
                icon = Icons.Default.KeyboardArrowLeft,
                size = padSize,
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.DPAD_LEFT) }
            )
            Spacer(Modifier.width(4.dp))
            // OK center button
            Surface(
                shape = CircleShape,
                color = if (enabled) SurfaceVariantDark else SurfaceVariantDark.copy(alpha = 0.5f),
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { if (enabled) onCommand(RemoteCommand.DPAD_CENTER) },
                        modifier = Modifier.size(centerSize)
                    ) {
                        Text(
                            text = "OK",
                            color = if (enabled) CyanAccent else TextSecondary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            DPadArrow(
                icon = Icons.Default.KeyboardArrowRight,
                size = padSize,
                enabled = enabled,
                onClick = { onCommand(RemoteCommand.DPAD_RIGHT) }
            )
        }
        DPadArrow(
            icon = Icons.Default.KeyboardArrowDown,
            size = padSize,
            enabled = enabled,
            onClick = { onCommand(RemoteCommand.DPAD_DOWN) }
        )
    }
}

@Composable
private fun DPadArrow(
    icon: ImageVector,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceVariantDark,
            contentColor = TextPrimary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
    }
}

// ── Nav buttons ───────────────────────────────────────────────────────────

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = { if (enabled) onClick() },
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = SurfaceVariantDark,
                contentColor = TextPrimary
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

// ── Media buttons ─────────────────────────────────────────────────────────

@Composable
private fun MediaButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(if (primary) 60.dp else 52.dp),
        shape = if (primary) CircleShape else RoundedCornerShape(14.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (primary) CyanAccent.copy(alpha = 0.15f) else SurfaceVariantDark,
            contentColor = if (primary) CyanAccent else TextPrimary
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(if (primary) 28.dp else 22.dp))
    }
}

// ── Volume buttons ────────────────────────────────────────────────────────

@Composable
private fun VolumeButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = { if (enabled) onClick() },
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = SurfaceVariantDark,
                contentColor = TextPrimary
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
