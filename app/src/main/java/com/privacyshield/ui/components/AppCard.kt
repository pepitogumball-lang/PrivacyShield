package com.privacyshield.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.privacyshield.data.model.InstalledAppInfo
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.OutlineDark
import com.privacyshield.ui.theme.ProtectedBlue
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.SurfaceVariantDark
import com.privacyshield.ui.theme.TextSecondary
import com.privacyshield.util.IconCache

@Composable
fun AppCard(
    app: InstalledAppInfo,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    onClick: () -> Unit = {}
) {
    val borderColor = if (app.isProtected) ProtectedBlue.copy(alpha = 0.6f) else OutlineDark

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isProtected) SurfaceVariantDark else SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (app.isProtected) 1.dp else 0.5.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    AppIconImage(packageName = app.packageName)
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                RiskBadge(riskLevel = app.riskLevel)
            }

            if (app.hasAccessibilityService || app.hasOverlayPermission ||
                app.hasRecordingRisk || app.isProtected
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (app.isProtected) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected",
                            tint = ProtectedBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.hasAccessibilityService) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = "Accessibility",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.hasOverlayPermission) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Overlay",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.hasRecordingRisk) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Recording risk",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf<Bitmap?>(IconCache.getSync(packageName)) }

    LaunchedEffect(packageName) {
        if (bitmap == null) {
            bitmap = IconCache.loadIcon(context, packageName)
        }
    }

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(size.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size((size * 0.75f).dp)
            )
        }
    }
}
