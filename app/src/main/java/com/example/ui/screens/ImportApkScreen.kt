package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SandboxProfileEntity
import com.example.engine.ParsedApkMetadata
import com.example.engine.SampleApkPreset
import com.example.engine.SandboxInstallProgress
import com.example.ui.components.AppMonogramIcon
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun ImportApkScreen(
    profiles: List<SandboxProfileEntity>,
    selectedProfileIds: Set<String>,
    samplePresets: List<SampleApkPreset>,
    inspectedApk: ParsedApkMetadata?,
    isParsingApk: Boolean,
    installProgress: SandboxInstallProgress?,
    installErrorMessage: String?,
    installSuccessMessage: String?,
    onExternalApkSelected: (Uri, String?) -> Unit,
    onGenerateSampleApk: (SampleApkPreset) -> Unit,
    onToggleTargetProfile: (String) -> Unit,
    onInstallApk: () -> Unit,
    onNavigateToTesting: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val context = LocalContext.current
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName: String? = null
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) fileName = cursor.getString(idx)
                    }
                }
            }
            onExternalApkSelected(uri, fileName)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("import_apk_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Pick APK from Device Storage
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Import APK from Local Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select any .apk archive from your device storage using Android's zero-permission document picker. DroidSandbox treats all imported APKs as untrusted and inspects headers before installation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            documentLauncher.launch(
                                arrayOf(
                                    "application/vnd.android.package-archive",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_local_apk_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select .APK File from Storage")
                    }
                }
            }
        }

        // Section 2: Pre-Install APK Inspector & Profile Cloning Target
        if (isParsingApk) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Parsing APK Archive & Verifying DEX Headers...", style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (installErrorMessage != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("install_error_banner"),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Installation Error",
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Installation / Verification Failed",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = installErrorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                    }
                }
            }
        }

        if (installSuccessMessage != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("install_success_banner"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Text(
                                text = installSuccessMessage,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = onNavigateToTesting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("post_install_launch_test_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Testing & Launch Now")
                        }
                    }
                }
            }
        }

        if (inspectedApk != null) {
            item {
                ApkInspectorCard(
                    apk = inspectedApk,
                    profiles = profiles,
                    selectedProfileIds = selectedProfileIds,
                    installProgress = installProgress,
                    onToggleProfile = onToggleTargetProfile,
                    onInstallClick = onInstallApk
                )
            }
        }

        // Section 3: Built-in Real Sample APK Generator
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Built-in Real APK Test Generator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate real, ZIP-aligned .apk binaries with valid Dalvik DEX headers on-device to test version upgrades, profile cloning, and crash trapping without needing external files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(samplePresets, key = { it.id }) { preset ->
            SamplePresetCard(
                preset = preset,
                onGenerateClick = { onGenerateSampleApk(preset) }
            )
        }
    }
}

@Composable
private fun ApkInspectorCard(
    apk: ParsedApkMetadata,
    profiles: List<SandboxProfileEntity>,
    selectedProfileIds: Set<String>,
    installProgress: SandboxInstallProgress?,
    onToggleProfile: (String) -> Unit,
    onInstallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apk_inspector_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppMonogramIcon(appName = apk.appName, behaviorType = apk.sampleBehaviorType)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "APK Binary Inspector",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = apk.appName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = apk.packageName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Metadata key-value grid
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InspectorRow("File Name", apk.fileName)
                    InspectorRow("Version", "v${apk.versionName} (Code: ${apk.versionCode})")
                    InspectorRow("Archive Size", "${formatBytes(apk.fileSizeBytes)} (${apk.archiveEntriesCount} ZIP entries)")
                    InspectorRow("SDK Requirements", "minSdk ${apk.minSdk} • targetSdk ${apk.targetSdk}")
                    InspectorRow("DEX Bytecode", if (apk.hasValidDex) "Valid classes.dex Verified" else "Missing classes.dex")
                    InspectorRow("Entry Activity", apk.entryClassName)
                    InspectorRow("SHA-256 Digest", apk.sha256Signature.take(24) + "…")
                }
            }

            // Permissions Inspection
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Requested Permissions (${apk.permissions.size})",
                    style = MaterialTheme.typography.labelLarge
                )
                if (apk.permissions.isEmpty()) {
                    Text(
                        text = "Zero host permissions requested (Safe Sandbox Package)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    apk.permissions.forEach { perm ->
                        val isDangerous = perm in apk.dangerousPermissions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDangerous) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDangerous) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = perm + if (isDangerous) " (Isolated / Trapped)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDangerous) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Target Profile Selection (Multi-profile App Cloning support)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Target Testing Profiles (Select multiple to Clone instance)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profiles.forEach { profile ->
                        val isSelected = selectedProfileIds.contains(profile.profileId)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleProfile(profile.profileId) },
                            label = { Text(profile.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("target_profile_chip_${profile.profileId}")
                        )
                    }
                }
            }

            AnimatedVisibility(visible = installProgress != null) {
                installProgress?.let { prog ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = prog.stepTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${prog.stepIndex}/${prog.totalSteps}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        LinearProgressIndicator(
                            progress = { prog.fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Button(
                onClick = onInstallClick,
                enabled = installProgress == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("install_inspected_apk_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.InstallMobile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val label = if (selectedProfileIds.size > 1) {
                    "Install & Clone into ${selectedProfileIds.size} Profiles"
                } else {
                    "Install into Selected Sandbox Profile"
                }
                Text(label)
            }
        }
    }
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SamplePresetCard(
    preset: SampleApkPreset,
    onGenerateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sample_preset_card_${preset.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppMonogramIcon(
                    appName = preset.appName,
                    behaviorType = preset.behaviorType,
                    modifier = Modifier.size(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${preset.packageName} • v${preset.versionName} (${preset.versionCode}) • minSdk ${preset.minSdk}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onGenerateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_sample_btn_${preset.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build & Inspect .APK Binary")
            }
        }
    }
}
