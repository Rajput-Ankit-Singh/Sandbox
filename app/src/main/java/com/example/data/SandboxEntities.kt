package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sandbox_profiles")
data class SandboxProfileEntity(
    @PrimaryKey val profileId: String,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isolationMode: String = "USER_SPACE_CONTAINER"
)

@Entity(
    tableName = "installed_sandbox_apps",
    indices = [Index(value = ["profileId", "packageName"], unique = true)]
)
data class InstalledSandboxAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val apkFilePath: String,
    val isolatedDataDirPath: String,
    val apkSizeBytes: Long,
    val dataSizeBytes: Long,
    val sha256Signature: String,
    val permissionsCsv: String,
    val entryClassName: String,
    val sampleBehaviorType: String, // "STANDARD", "UPGRADE", "CRASH_TEST", "STORAGE_STRESS", "EXTERNAL_APK"
    val installedAt: Long = System.currentTimeMillis(),
    val lastLaunchedAt: Long? = null,
    val launchCount: Int = 0,
    val lastStartupMs: Long = 0L,
    val lastRamDeltaKb: Long = 0L,
    val lastCpuTimeMs: Long = 0L,
    val lastStatus: String = "INSTALLED"
)

@Entity(tableName = "sandbox_logs")
data class SandboxLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // "INFO", "WARN", "ERROR", "CRASH"
    val tag: String,
    val message: String,
    val stackTrace: String? = null
)
