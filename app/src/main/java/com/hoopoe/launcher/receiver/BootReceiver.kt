package com.hoopoe.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hoopoe.launcher.MainActivity
import com.hoopoe.launcher.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Boot receiver for optional automatic activation when vehicle starts / head unit boots.
 * Respects user preference in settings and Android 10+ background activity launch policies.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = SettingsRepository(context)
                    val settings = repo.settingsFlow.first()
                    if (settings.autoLaunchOnBoot) {
                        Log.i("HoopoeBootReceiver", "Auto-launch on boot enabled. Starting HOOPOE Companion...")
                        val launchIntent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }
                        context.startActivity(launchIntent)
                    } else {
                        Log.i("HoopoeBootReceiver", "Boot detected, but auto-launch is disabled in settings.")
                    }
                } catch (e: Exception) {
                    Log.e("HoopoeBootReceiver", "Failed to launch on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
