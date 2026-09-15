package com.customersupport.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Opens the OEM-specific "Autostart" / "Background activity" settings screen.
 *
 * On Xiaomi, Oppo, Vivo, Huawei, Samsung and similar OEMs, background services
 * are killed unless the user explicitly whitelists the app. There is no public
 * API to change this — the best we can do is deep-link the user to the right
 * screen. On AOSP / devices without such a screen we fall back to app details.
 */
object OemSettingsHelper {

    private const val TAG = "OemSettingsHelper"

    private data class OemIntent(val pkg: String, val cls: String)

    private val candidates: List<OemIntent> by lazy {
        val manufacturer = Build.MANUFACTURER.lowercase()
        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
                manufacturer.contains("poco") -> listOf(
                OemIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                OemIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                OemIntent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            )

            manufacturer.contains("oppo") || manufacturer.contains("realme") -> listOf(
                OemIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                OemIntent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                OemIntent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            )

            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> listOf(
                OemIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                OemIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                OemIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
            )

            manufacturer.contains("samsung") -> listOf(
                OemIntent("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                OemIntent("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )

            manufacturer.contains("oneplus") -> listOf(
                OemIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            )

            manufacturer.contains("asus") -> listOf(
                OemIntent("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings")
            )

            manufacturer.contains("letv") || manufacturer.contains("leeco") -> listOf(
                OemIntent("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
            )

            else -> emptyList()
        }
    }

    /** @return true if an OEM-specific screen was opened. */
    fun openAutoStartSettings(context: Context): Boolean {
        for (candidate in candidates) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(candidate.pkg, candidate.cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (context.packageManager.resolveActivity(intent, 0) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Opened OEM autostart screen: ${candidate.cls}")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "OEM autostart intent failed: ${candidate.cls}", e)
            }
        }
        return openAppDetails(context)
    }

    private fun openAppDetails(context: Context): Boolean {
        return try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            Log.d(TAG, "Opened app details as fallback")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details", e)
            false
        }
    }
}
