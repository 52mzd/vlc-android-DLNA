/*****************************************************************************
 * DmrBootReceiver.kt
 *****************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.dmr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.videolan.tools.Settings

/**
 * Starts the DLNA receiver after device boot so the TV stays discoverable
 * without opening VLC (like a built-in TV DLNA renderer).
 *
 * Starting a foreground service from BOOT_COMPLETED is exempt from the
 * Android 12+ background-start restriction. If the network is not ready yet,
 * the START_STICKY service retries once connectivity is available.
 */
class DmrBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Settings.getInstance(context).getBoolean(DmrService.KEY_DLNA_RECEIVER, true)) return
        Log.i(TAG, "BOOT_COMPLETED: starting DLNA receiver")
        try {
            context.startForegroundService(Intent(context, DmrService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start DmrService on boot", e)
        }
    }

    companion object {
        private const val TAG = "VLC/DmrService"
    }
}
