package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.InstalledSandboxAppEntity
import com.example.data.SandboxProfileEntity
import com.example.data.StorageSummary
import com.example.ui.components.AppMonogramIcon
import com.example.ui.components.MetricBadgeCard
import com.example.ui.components.ProfileSelectorBar
import com.example.ui.components.SandboxModeBanner
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun HomeScreen(
    profiles: List<SandboxProfileEntity>,
    activeProfileId: String,
    installedApps: List<InstalledSandboxAppEntity>,
    allAppsCount: Int,
    searchQuery: String,
    activeBackendMode: String,
    storageSummary: StorageSummary,
    onSelectProfile: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLaunchApp: (InstalledSandboxAppEntity) -> Unit,
    onInspectApp: (InstalledSandboxAppEntity) -> Unit,
    onUninstallApp: (InstalledSandboxAppEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile = remember(profiles, activeProfileId) {
        profiles.find { it.profileId == activeProfileId }
    }
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SandboxModeBanner(
                activeMode = activeBackendMode,
                activeProfileName = activeProfile?.name ?: "Default QA",
                onInspectCompatClick = onNavigateToSettings
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Testing Profiles (Isolated Containers)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${profiles.size} Profiles • $allAppsCount Total Instances",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ProfileSelectorBar(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    onSelectProfile = onSelectProfile
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBadgeCard(
                    label = "Profile Apps",
                    value = "${installedApps.size} Installed",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricBadgeCard(
                    label = "Sandbox Storage",
                    value = formatBytes(storageSummary.totalSandboxBytes),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input"),
                placeholder = { Text("Filter installed sandbox packages...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search packages")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (filteredApps.isEmpty()) {
            item {
                EmptySandboxStateCard(
                    profileName = activeProfile?.name ?: "Default QA",
                    hasSearchFilter = searchQuery.isNotBlank(),
                    onNavigateToImport = onNavigateToImport
                )
            }
        } else {
            items(filteredApps, key = { "${it.profileId}_${it.packageName}" }) { app ->
                SandboxInstalledAppCard(
                    app = app,
                    onLaunch = { onLaunchApp(app) },
                    onInspect = { onInspectApp(app) },
                    onUninstall = { onUninstallApp(app) }
                )
            }
        }
    }
}

@Composable
private fun SandboxInstalledAppCard(
    app: InstalledSandboxAppEntity,
    onLaunch: () -> Unit,
    onInspect: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installed_app_card_${app.packageName}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppMonogramIcon(
                    appName = app.appName,
                    behaviorType = app.sampleBehaviorType
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "v${app.versionName} (${app.versionCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.testTag("uninstall_btn_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Uninstall ${app.appName}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Telemetry & Isolation metadata strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataPill("APK: ${formatBytes(app.apkSizeBytes)}")
                MetadataPill("Data: ${formatBytes(app.dataSizeBytes)}")
                MetadataPill("SDK ${app.minSdk}→${app.targetSdk}")
                if (app.launchCount > 0) {
                    MetadataPill("${app.lastStartupMs}ms •Runs: ${app.launchCount}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLaunch,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("launch_btn_${app.packageName}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch in Sandbox")
                }

                FilledTonalButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("inspect_btn_${app.packageName}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test & Logs")
                }
            }
        }
    }
}

@Composable
private fun MetadataPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptySandboxStateCard(
    profileName: String,
    hasSearchFilter: Boolean,
    onNavigateToImport: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_sandbox_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom vector blueprint illustration on Canvas
            Box(
                modifier = Modifier.size(88.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.12f),
                        radius = w * 0.46f
                    )
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.25f),
                        radius = w * 0.32f
                    )
                    // Isometric container lines
                    drawLine(
                        color = primaryColor,
                        start = Offset(w * 0.5f, h * 0.2f),
                        end = Offset(w * 0.78f, h * 0.36f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(w * 0.5f, h * 0.2f),
                        end = Offset(w * 0.22f, h * 0.36f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = secondaryColor,
                        start = Offset(w * 0.22f, h * 0.36f),
                        end = Offset(w * 0.5f, h * 0.52f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = secondaryColor,
                        start = Offset(w * 0.78f, h * 0.36f),
                        end = Offset(w * 0.5f, h * 0.52f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = if (hasSearchFilter) "No Matching Packages" else "No APKs Installed in '$profileName'",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (hasSearchFilter) {
                    "Try clearing your search filter or switching to another testing profile."
                } else {
                    "Import an .apk file from local storage or generate a real sample test APK to inspect headers, isolate app data, and benchmark runtime execution."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onNavigateToImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("empty_state_import_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import or Generate Sample APK")
            }
        }
    }
}
