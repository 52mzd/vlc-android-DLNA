/*****************************************************************************
 * DmrReceiverActivity.kt
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
package org.videolan.television.ui

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.resources.util.DmrReceiverWindow
import org.videolan.television.R
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.dmr.DmrService

/**
 * Resident receiver window for the 后台接收 (background receive) DLNA mode.
 *
 * Holds the vout surface for media played through the service layer, so a DLNA
 * push always has a visible surface even when no other VLC activity is alive
 * (solving Android's background-activity-start restriction). It stays *resumed*;
 * it never backgrounds itself.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class DmrReceiverActivity : AppCompatActivity(), PlaybackService.Callback {

    private var service: PlaybackService? = null
    private var surfacesAttached = false

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DmrService.ACTION_RECEIVER_STOP) {
                detachSurface()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dmr_receiver)
        registerReceiver(stopReceiver, IntentFilter(DmrService.ACTION_RECEIVER_STOP))

        lifecycleScope.launch {
            PlaybackService.serviceFlow.collect { onServiceChanged(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        // The window is launched into its own task (NEW_TASK) and only stays resumed
        // while it is on top, so it never hijacks a player resumed in another task.
        attachSurface()
    }

    override fun onStop() {
        // Release the vout surface when this window is no longer visible, so a foreground
        // player can take it over (and re-attach cleanly when this window returns).
        detachSurface()
        super.onStop()
    }

    override fun onDestroy() {
        detachSurface()
        try {
            unregisterReceiver(stopReceiver)
        } catch (ignored: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    private fun onServiceChanged(it: PlaybackService?) {
        if (it != null) {
            it.addCallback(this)
            service = it
            updateNowPlaying()
        } else {
            service?.removeCallback(this)
            service = null
        }
    }

    private fun attachSurface() {
        // Precedence guard: only attach the vout surface while this window is actually
        // visible (resumed). A Vout event arriving while the window is covered by another
        // VLC activity must NOT steal the surface from that foreground player.
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val svc = service ?: return
        if (surfacesAttached) return
        val vlcVout = svc.vout ?: return
        vlcVout.setVideoView(findViewById<SurfaceView>(R.id.dmr_receiver_surface))
        vlcVout.attachViews()
        surfacesAttached = true
    }

    private fun detachSurface() {
        if (!surfacesAttached) return
        service?.vout?.detachViews()
        surfacesAttached = false
    }

    private fun updateNowPlaying() {
        val title = service?.currentMediaWrapper?.title ?: getString(R.string.dmr_receiver_window_title)
        findViewById<TextView>(R.id.dmr_receiver_title).text = title
    }

    override fun update() = updateNowPlaying()

    override fun onMediaEvent(event: IMedia.Event) = Unit

    override fun onMediaPlayerEvent(event: MediaPlayer.Event) {
        if (event.type == MediaPlayer.Event.Vout && event.voutCount > 0) {
            attachSurface()
        }
        updateNowPlaying()
    }

    companion object {
        // Delegate to the cross-module helper (single source of truth for the launch
        // flags + no-hijack guard + stop action). See DmrReceiverWindow for the contract.
        fun start(context: Context, force: Boolean = false) = DmrReceiverWindow.start(context, force)

        fun stop(context: Context) = DmrReceiverWindow.stop(context)
    }
}