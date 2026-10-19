package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.InstalledSandboxAppEntity
import com.example.data.SandboxProfileEntity
import com.example.data.StorageSummary
import com.example.engine.DeviceCompatibilityReport
import com.example.ui.components.MetricBadgeCard
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    profiles: List<SandboxProfileEntity>,
    activeProfileId: String,
    allInstalledApps: List<InstalledSandboxAppEntity>,
    storageSummary: StorageSummary,
    compatibilityReport: DeviceCompatibilityReport?,
    onToggleDarkTheme: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: (String, String) -> Unit,
    onResetProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onClearStagingCache: () -> Unit,
    onClearSingleAppData: (InstalledSandboxAppEntity) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    var newProfileName by rememberSaveable { mutableStateOf("") }
    var newProfileDesc by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Appearance Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Precision Dark Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDarkTheme) "Carbon Slate dark mode active" else "Alabaster light mode active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() },
                        modifier = Modifier.testTag("theme_toggle_switch")
                    )
                }
            }
        }

        // Section 2: Sandbox Storage Manager
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storage_manager_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Sandbox Storage Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadgeCard(
                            label = "Total Sandbox",
                            value = formatBytes(storageSummary.totalSandboxBytes),
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeCard(
                            label = "APK Binaries",
                            value = formatBytes(storageSummary.totalApkBytes),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadgeCard(
                            label = "Isolated App Data",
                            value = formatBytes(storageSummary.totalIsolatedDataBytes),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeCard(
                            label = "Staging Cache",
                            value = formatBytes(storageSummary.stagingCacheBytes),
                            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (allInstalledApps.isNotEmpty()) {
                        Text(
                            text = "Per-App Isolated Storage Breakdown:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        allInstalledApps.forEach { app ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${app.appName} v${app.versionName} (${app.profileId})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "APK: ${formatBytes(app.apkSizeBytes)} • Isolated Data: ${formatBytes(app.dataSizeBytes)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onClearSingleAppData(app) },
                                        modifier = Modifier.height(48.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Clear Data")
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onClearStagingCache,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_staging_cache_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purge Temporary APK Staging Cache")
                    }
                }
            }
        }

        // Section 3: Multiple Testing Profiles & App Cloning Manager
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_manager_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Testing Profiles & App Cloning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Each profile maintains an isolated filesystem root (/files/sandbox/profiles/<id>/) so you can clone the same APK across profiles or test clean-install upgrades.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("New Profile Name (e.g., Staging v2)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_name_input")
                    )
                    OutlinedTextField(
                        value = newProfileDesc,
                        onValueChange = { newProfileDesc = it },
                        label = { Text("Profile Purpose / Notes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_desc_input")
                    )
                    Button(
                        onClick = {
                            if (newProfileName.isNotBlank()) {
                                onCreateProfile(newProfileName, newProfileDesc)
                                newProfileName = ""
                                newProfileDesc = ""
                            }
                        },
                        enabled = newProfileName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_profile_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Isolated Testing Profile")
                    }
                }
            }
        }

        items(profiles, key = { it.profileId }) { profile ->
            val isActive = profile.profileId == activeProfileId
            val appsInProfile = allInstalledApps.count { it.profileId == profile.profileId }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_item_card_${profile.profileId}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name + if (isActive) " (Active)" else "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID: ${profile.profileId} • $appsInProfile Installed Apps",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!profile.isDefault) {
                            IconButton(
                                onClick = { onDeleteProfile(profile.profileId) },
                                modifier = Modifier.testTag("delete_profile_btn_${profile.profileId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete profile ${profile.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Text(
                        text = profile.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isActive) {
                            FilledTonalButton(
                                onClick = { onSelectProfile(profile.profileId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Activate")
                            }
                        }
                        OutlinedButton(
                            onClick = { onResetProfile(profile.profileId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reset_profile_btn_${profile.profileId}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Profile")
                        }
                    }
                }
            }
        }

        // Section 4: Real Hardware & AVF Compatibility Report
        compatibilityReport?.let { report ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("avf_compatibility_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "AVF & Virtualization Compatibility Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = report.backendSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompatRow("Active Sandbox Mode", report.activeBackendMode)
                                CompatRow("Host Device", "${report.manufacturer} ${report.deviceModel}")
                                CompatRow("Android OS", "Android ${report.androidRelease} (API ${report.sdkInt})")
                                CompatRow("CPU ABIs", report.supportedAbis.joinToString(", "))
                                CompatRow("Device Memory", "${report.availableRamMb} MB free / ${report.totalRamMb} MB total")
                                CompatRow("AVF System Feature", if (report.hasAvfSystemFeature) "Present" else "Not Exposed")
                                CompatRow("/dev/kvm Node", if (report.hasKvmDeviceNode) "Detected (R/W: ${report.isKvmReadableWritable})" else "Absent")
                                CompatRow("MANAGE_VIRTUAL_MACHINE", if (report.hasManageVmPermission) "Granted" else "Denied (Privileged Only)")
                            }
                        }

                        Text(
                            text = "App Cloning & Architecture Guarantees:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = report.cloningSupportNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = "Documented Technical Limitations:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        report.limitationsList.forEach { limit ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(14.dp)
                                )
                                Text(
                                    text = limit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.55f)
        )
    }
}
