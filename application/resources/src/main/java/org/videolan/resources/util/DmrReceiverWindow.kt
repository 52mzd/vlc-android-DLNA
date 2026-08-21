/*****************************************************************************
 * DmrReceiverWindow.kt
 *
 * Copyright © 2026 VLC authors and VideoLAN
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
 */
package org.videolan.resources.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.videolan.resources.AppContextProvider

/**
 * Cross-module helper to start/stop the DMR resident receiver window
 * ([org.videolan.television.ui.DmrReceiverActivity], television module) from the
 * phone-UI fragment (vlc-android), which cannot depend on television.
 *
 * Referenced by string class name via [ComponentName] (same pattern as
 * `StartActivity` launching RemoteAccessShareActivity and `startRemoteAccess`).
 * The action constant is the single source of truth for the stop signal;
 * `DmrService.ACTION_RECEIVER_STOP` delegates to it.
 */
object DmrReceiverWindow {

    /** Stop signal for the resident receiver window (DmrService.ACTION_RECEIVER_STOP). */
    const val ACTION_RECEIVER_STOP = "org.videolan.vlc.dmr.RECEIVER_STOP"

    private const val RECEIVER_ACTIVITY = "org.videolan.television.ui.DmrReceiverActivity"

    /**
     * Launch the resident receiver window. `force=false` (default) applies no-hijack
     * precedence: skip when another VLC activity is in the foreground, so the window
     * never yanks an active playback away (used by DmrService automatic launches).
     * `force=true` is for user-initiated launches (flipping the 后台接收 switch in
     * settings), where the user explicitly asked for the window.
     */
    fun start(context: Context, force: Boolean = false) {
        if (!force && AppContextProvider.currentActivity != null) return
        context.startActivity(Intent().apply {
            component = ComponentName(context, RECEIVER_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }

    /** Signal the resident receiver window to finish (matches DmrReceiverActivity's receiver). */
    fun stop(context: Context) {
        context.sendBroadcast(Intent(ACTION_RECEIVER_STOP).setPackage(context.packageName))
    }
}