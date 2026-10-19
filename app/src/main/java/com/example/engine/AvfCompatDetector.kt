package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

data class DeviceCompatibilityReport(
    val deviceModel: String,
    val manufacturer: String,
    val androidRelease: String,
    val sdkInt: Int,
    val supportedAbis: List<String>,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val isLowRamDevice: Boolean,
    val hasAvfSystemFeature: Boolean,
    val hasKvmDeviceNode: Boolean,
    val isKvmReadableWritable: Boolean,
    val hasManageVmPermission: Boolean,
    val activeBackendMode: String,
    val backendSummary: String,
    val cloningSupportNote: String,
    val limitationsList: List<String>
)

object AvfCompatDetector {

    fun evaluate(context: Context): DeviceCompatibilityReport {
        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = if (memInfo.totalMem > 0) memInfo.totalMem / (1024 * 1024) else 4096L
        val availRamMb = if (memInfo.availMem > 0) memInfo.availMem / (1024 * 1024) else 1536L
        val isLowRam = am?.isLowRamDevice ?: false

        // 1. Check Android Virtualization Framework system feature (Android 13+ / API 33+)
        val hasAvfFeature = pm.hasSystemFeature("android.software.virtualization_framework")

        // 2. Check /dev/kvm hypervisor character device node
        val kvmFile = File("/dev/kvm")
        val hasKvmNode = kvmFile.exists()
        val isKvmAccessible = kvmFile.canRead() && kvmFile.canWrite()

        // 3. Check privileged MANAGE_VIRTUAL_MACHINE permission
        val hasManageVmPerm = context.checkSelfPermission("android.permission.MANAGE_VIRTUAL_MACHINE") ==
            PackageManager.PERMISSION_GRANTED

        val canUseFullAvf = Build.VERSION.SDK_INT >= 33 && hasAvfFeature && hasManageVmPerm

        val activeMode = if (canUseFullAvf) {
            "AVF Hardware Virtualization"
        } else {
            "Limited User-Space Sandbox Mode"
        }

        val summary = if (canUseFullAvf) {
            "Android Virtualization Framework (AVF) and pKVM permissions are granted on this device."
        } else {
            buildString {
                append("Standard unrooted Android restricts hardware hypervisor VMs (android.permission.MANAGE_VIRTUAL_MACHINE is signature|privileged). ")
                if (hasAvfFeature || hasKvmNode) {
                    append("Although hardware virtualization nodes were detected on this ${Build.MODEL}, ")
                } else {
                    append("Hardware AVF / /dev/kvm is not exposed to user-space apps on this ${Build.MODEL}; ")
                }
                append("DroidSandbox is operating in honest Limited User-Space Sandbox Mode using per-profile isolated storage trees and read-only DexClassLoader containers.")
            }
        }

        val cloningNote = "Profile-based App Cloning is enabled: each Testing Profile provisions an independent directory tree (/files/sandbox/profiles/<profileId>/<packageName>/) and separate DexClassLoader instance, allowing multiple instances or versions of the same APK to store state independently without colliding."

        val limitations = listOf(
            "No Kernel/OS Guest Boot: Runs inside DroidSandbox's user-space UID rather than booting a separate Linux kernel or guest Android OS.",
            "System Services & Broadcasts: Imported APKs cannot register host-wide system ContentProviders, foreground services, or device-admin receivers.",
            "Native JNI & UID Boundaries: Apps execute within the sandbox container context; cross-app IPC with host apps is blocked by default to protect the host phone.",
            "Android 14+ Dynamic Code Safety: Extracted DEX payloads are strictly locked read-only (File.setReadOnly()) before loading into DexClassLoader."
        )

        return DeviceCompatibilityReport(
            deviceModel = Build.MODEL ?: "Android Device",
            manufacturer = Build.MANUFACTURER ?: "Generic",
            androidRelease = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            isLowRamDevice = isLowRam,
            hasAvfSystemFeature = hasAvfFeature,
            hasKvmDeviceNode = hasKvmNode,
            isKvmReadableWritable = isKvmAccessible,
            hasManageVmPermission = hasManageVmPerm,
            activeBackendMode = activeMode,
            backendSummary = summary,
            cloningSupportNote = cloningNote,
            limitationsList = limitations
        )
    }
}
