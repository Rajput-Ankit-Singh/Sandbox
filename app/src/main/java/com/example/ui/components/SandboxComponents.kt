package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SandboxProfileEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

@Composable
fun SandboxModeBanner(
    activeMode: String,
    activeProfileName: String,
    onInspectCompatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLimited = activeMode.contains("Limited", ignoreCase = true)
    val containerColor = if (isLimited) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
    }
    val contentColor = if (isLimited) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onInspectCompatClick)
            .testTag("sandbox_mode_banner"),
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isLimited) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isLimited) Icons.Default.WarningAmber else Icons.Default.Security,
                    contentDescription = "Sandbox Isolation Mode",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = activeMode,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                    Text(
                        text = "Active Profile: $activeProfileName • Tap for AVF & device limits",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSelectorBar(
    profiles: List<SandboxProfileEntity>,
    activeProfileId: String,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        profiles.forEach { profile ->
            val selected = profile.profileId == activeProfileId
            FilterChip(
                selected = selected,
                onClick = { onSelectProfile(profile.profileId) },
                label = {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.FolderSpecial,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("profile_chip_${profile.profileId}")
            )
        }
    }
}

@Composable
fun MetricBadgeCard(
    label: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DismissibleStatusToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_toast_banner"),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AppMonogramIcon(
    appName: String,
    behaviorType: String,
    modifier: Modifier = Modifier
) {
    val bgColor = when (behaviorType) {
        "CRASH_TEST" -> MaterialTheme.colorScheme.errorContainer
        "UPGRADE" -> MaterialTheme.colorScheme.secondaryContainer
        "STORAGE_STRESS" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = when (behaviorType) {
        "CRASH_TEST" -> MaterialTheme.colorScheme.onErrorContainer
        "UPGRADE" -> MaterialTheme.colorScheme.onSecondaryContainer
        "STORAGE_STRESS" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val initials = appName.trim().take(2).uppercase().ifEmpty { "AP" }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}
