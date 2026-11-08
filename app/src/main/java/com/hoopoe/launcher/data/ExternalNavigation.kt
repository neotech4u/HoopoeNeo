package com.hoopoe.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hands a destination to a real installed maps provider. HOOPOE does not calculate
 * turn-by-turn road routes itself.
 */
object ExternalNavigation {
    fun openDrivingDirections(
        context: Context,
        destination: String,
        originLatitude: Double? = null,
        originLongitude: Double? = null
    ): Boolean {
        if (destination.isBlank()) return false

        val uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
            .appendQueryParameter("api", "1")
            .apply {
                if (originLatitude != null && originLongitude != null &&
                    originLatitude in -90.0..90.0 && originLongitude in -180.0..180.0
                ) {
                    appendQueryParameter("origin", "$originLatitude,$originLongitude")
                }
            }
            .appendQueryParameter("destination", destination)
            .appendQueryParameter("travelmode", "driving")
            .appendQueryParameter("dir_action", "navigate")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
