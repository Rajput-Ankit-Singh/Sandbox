package com.example.data

import android.content.Context
import android.net.Uri
import com.example.engine.ApkParserEngine
import com.example.engine.AvfCompatDetector
import com.example.engine.DeviceCompatibilityReport
import com.example.engine.ParsedApkMetadata
import com.example.engine.SampleApkPreset
import com.example.engine.SandboxExecutionResult
import com.example.engine.SandboxInstallProgress
import com.example.engine.SandboxRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class StorageSummary(
    val totalSandboxBytes: Long,
    val totalApkBytes: Long,
    val totalIsolatedDataBytes: Long,
    val stagingCacheBytes: Long
)

class SandboxRepository(
    private val context: Context,
    private val dao: SandboxDao
) {
    val allProfiles: Flow<List<SandboxProfileEntity>> = dao.getAllProfiles()
    val allInstalledApps: Flow<List<InstalledSandboxAppEntity>> = dao.getAllInstalledApps()

    fun getInstalledAppsForProfile(profileId: String): Flow<List<InstalledSandboxAppEntity>> =
        dao.getInstalledAppsForProfile(profileId)

    fun getLogsForProfile(profileId: String): Flow<List<SandboxLogEntity>> =
        dao.getLogsForProfile(profileId)

    suspend fun ensureDefaultProfiles() = withContext(Dispatchers.IO) {
        val defaultMain = dao.getProfileById("profile_default")
        if (defaultMain == null) {
            dao.insertProfile(
                SandboxProfileEntity(
                    profileId = "profile_default",
                    name = "Default QA",
                    description = "Primary isolated user-space container profile for standard APK testing.",
                    isDefault = true
                )
            )
            SandboxRuntimeEngine.getProfileRootDir(context, "profile_default")
        }
        val cloneProfile = dao.getProfileById("profile_clone_b")
        if (cloneProfile == null) {
            dao.insertProfile(
                SandboxProfileEntity(
                    profileId = "profile_clone_b",
                    name = "Clone Instance B",
                    description = "Secondary isolated profile for testing parallel app cloning and multi-version state.",
                    isDefault = false
                )
            )
            SandboxRuntimeEngine.getProfileRootDir(context, "profile_clone_b")
        }
    }

    suspend fun createProfile(name: String, description: String): SandboxProfileEntity = withContext(Dispatchers.IO) {
        val cleanSlug = name.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16).ifEmpty { "custom" }
        val profileId = "profile_${cleanSlug}_${System.currentTimeMillis() % 10000}"
        val entity = SandboxProfileEntity(
            profileId = profileId,
            name = name.trim(),
            description = description.trim().ifEmpty { "Isolated testing profile container" },
            isDefault = false
        )
        SandboxRuntimeEngine.getProfileRootDir(context, profileId)
        dao.insertProfile(entity)
        dao.insertLog(
            SandboxLogEntity(
                profileId = profileId,
                packageName = "system.sandbox",
                level = "INFO",
                tag = "PROFILE_MGR",
                message = "Created isolated testing profile '${entity.name}' ($profileId)"
            )
        )
        entity
    }

    suspend fun resetProfile(profileId: String) = withContext(Dispatchers.IO) {
        SandboxRuntimeEngine.purgeProfileDirectory(context, profileId)
        SandboxRuntimeEngine.getProfileRootDir(context, profileId)
        dao.deleteAllAppsInProfile(profileId)
        dao.clearLogsForProfile(profileId)
        dao.insertLog(
            SandboxLogEntity(
                profileId = profileId,
                packageName = "system.sandbox",
                level = "WARN",
                tag = "PROFILE_RESET",
                message = "Profile '$profileId' reset clean. All installed sandbox APKs and isolated data directories wiped."
            )
        )
    }

    suspend fun deleteProfile(profileId: String) = withContext(Dispatchers.IO) {
        val profile = dao.getProfileById(profileId) ?: return@withContext
        if (profile.isDefault) return@withContext
        SandboxRuntimeEngine.purgeProfileDirectory(context, profileId)
        dao.deleteAllAppsInProfile(profileId)
        dao.clearLogsForProfile(profileId)
        dao.deleteProfile(profileId)
    }

    suspend fun parseExternalApkUri(uri: Uri, fileName: String?): Result<ParsedApkMetadata> =
        withContext(Dispatchers.IO) {
            ApkParserEngine.importFromUri(context, uri, fileName)
        }

    suspend fun generateAndParseSampleApk(preset: SampleApkPreset): Result<ParsedApkMetadata> =
        withContext(Dispatchers.IO) {
            ApkParserEngine.generateSampleApk(context, preset)
        }

    suspend fun installParsedApk(
        metadata: ParsedApkMetadata,
        targetProfileId: String,
        onProgress: suspend (SandboxInstallProgress) -> Unit
    ): Result<InstalledSandboxAppEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getInstalledApp(targetProfileId, metadata.packageName)
        val result = SandboxRuntimeEngine.installApkIntoProfile(
            context = context,
            metadata = metadata,
            profileId = targetProfileId,
            existingApp = existing,
            onProgress = onProgress
        )

        result.onSuccess { entity ->
            val insertedId = dao.insertOrUpdateApp(entity)
            val actionLabel = if (existing != null) "Upgraded" else "Installed"
            dao.insertLog(
                SandboxLogEntity(
                    profileId = targetProfileId,
                    packageName = entity.packageName,
                    level = "INFO",
                    tag = "PKG_INSTALLER",
                    message = "$actionLabel ${entity.appName} v${entity.versionName} (${entity.versionCode}) into profile '$targetProfileId' [id=$insertedId]"
                )
            )
        }.onFailure { err ->
            dao.insertLog(
                SandboxLogEntity(
                    profileId = targetProfileId,
                    packageName = metadata.packageName,
                    level = "ERROR",
                    tag = "PKG_INSTALLER",
                    message = "Failed to install ${metadata.appName} (${metadata.versionName}): ${err.message}",
                    stackTrace = err.stackTraceToString()
                )
            )
        }
        result
    }

    suspend fun launchSandboxApp(
        app: InstalledSandboxAppEntity,
        customKey: String? = null,
        customValue: String? = null,
        forceFault: Boolean = false
    ): SandboxExecutionResult = withContext(Dispatchers.IO) {
        val execResult = SandboxRuntimeEngine.launchInSandbox(
            context = context,
            app = app,
            customUserInputKey = customKey,
            customUserInputValue = customValue,
            forceFaultInjection = forceFault
        )

        val updatedApp = app.copy(
            lastLaunchedAt = System.currentTimeMillis(),
            launchCount = app.launchCount + 1,
            dataSizeBytes = execResult.updatedDataSizeBytes,
            lastStartupMs = execResult.startupTimeMs,
            lastRamDeltaKb = execResult.ramDeltaKb,
            lastCpuTimeMs = execResult.cpuTimeMs,
            lastStatus = if (execResult.success) "RUNNING_VERIFIED" else "CRASH_TRAPPED"
        )
        dao.updateApp(updatedApp)

        if (execResult.success) {
            dao.insertLog(
                SandboxLogEntity(
                    profileId = app.profileId,
                    packageName = app.packageName,
                    level = "INFO",
                    tag = "SANDBOX_RUNNER",
                    message = "Launched ${app.appName} v${app.versionName} in ${execResult.startupTimeMs}ms | CPU: ${execResult.cpuTimeMs}ms | RAM Delta: +${execResult.ramDeltaKb}KB"
                )
            )
        } else {
            dao.insertLog(
                SandboxLogEntity(
                    profileId = app.profileId,
                    packageName = app.packageName,
                    level = "CRASH",
                    tag = "SANDBOX_CRASH_HANDLER",
                    message = execResult.errorSummary ?: "Uncaught exception in sandbox container",
                    stackTrace = execResult.stackTrace
                )
            )
        }

        execResult
    }

    suspend fun clearSandboxAppData(app: InstalledSandboxAppEntity): InstalledSandboxAppEntity =
        withContext(Dispatchers.IO) {
            val newSize = SandboxRuntimeEngine.clearAppData(app)
            val updated = app.copy(
                dataSizeBytes = newSize,
                launchCount = 0,
                lastStatus = "DATA_CLEARED"
            )
            dao.updateApp(updated)
            dao.insertLog(
                SandboxLogEntity(
                    profileId = app.profileId,
                    packageName = app.packageName,
                    level = "WARN",
                    tag = "STORAGE_MGR",
                    message = "Cleared isolated app data & SharedPreferences for ${app.appName} in profile '${app.profileId}'."
                )
            )
            updated
        }

    suspend fun uninstallSandboxApp(app: InstalledSandboxAppEntity) = withContext(Dispatchers.IO) {
        SandboxRuntimeEngine.uninstallAppContainer(context, app.profileId, app.packageName)
        dao.deleteAppById(app.id)
        dao.insertLog(
            SandboxLogEntity(
                profileId = app.profileId,
                packageName = app.packageName,
                level = "INFO",
                tag = "PKG_UNINSTALLER",
                message = "Uninstalled ${app.appName} (${app.packageName}) and purged container directory from profile '${app.profileId}'."
            )
        )
    }

    suspend fun clearLogsForProfile(profileId: String) = withContext(Dispatchers.IO) {
        dao.clearLogsForProfile(profileId)
    }

    suspend fun clearStagingCache() = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "apk_staging")
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
        }
    }

    suspend fun computeStorageSummary(apps: List<InstalledSandboxAppEntity>): StorageSummary =
        withContext(Dispatchers.IO) {
            val root = SandboxRuntimeEngine.getSandboxRootDir(context)
            val totalSandbox = SandboxRuntimeEngine.computeDirectorySize(root)
            val totalApk = apps.sumOf { it.apkSizeBytes }
            val totalData = apps.sumOf { it.dataSizeBytes }
            val stagingDir = File(context.cacheDir, "apk_staging")
            val stagingBytes = SandboxRuntimeEngine.computeDirectorySize(stagingDir)
            StorageSummary(
                totalSandboxBytes = totalSandbox,
                totalApkBytes = totalApk,
                totalIsolatedDataBytes = totalData,
                stagingCacheBytes = stagingBytes
            )
        }

    suspend fun evaluateCompatibility(): DeviceCompatibilityReport = withContext(Dispatchers.IO) {
        AvfCompatDetector.evaluate(context)
    }
}
