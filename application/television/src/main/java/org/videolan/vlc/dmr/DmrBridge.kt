/*****************************************************************************
 * DmrBridge.kt
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

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.resources.AppContextProvider
import org.videolan.tools.AppScope
import org.videolan.tools.POSITION_IN_MEDIA
import org.videolan.tools.POSITION_IN_SONG
import org.videolan.tools.Settings
import org.videolan.tools.VIDEO_RESUME_TIME
import org.videolan.tools.VIDEO_RESUME_URI
import org.videolan.vlc.PlaybackService
import java.util.Locale

/**
 * Bridges UPnP SOAP actions to the [PlaybackService].
 *
 * jUPnP invokes the annotated action methods on its internal stream threads,
 * so mutating operations are dispatched to the main thread, while read-only
 * queries are executed directly (libvlc is thread-safe).
 */
object DmrBridge {

    private const val TAG = "VLC/DmrBridge"

    private val handler = Handler(Looper.getMainLooper())

    private val service: PlaybackService?
        get() = PlaybackService.instance

    /**
     * Loads a pushed URL and starts playback immediately (replacing current
     * content), matching what DLNA control points expect from this renderer.
     *
     * NOTE: do not pause after load (MEDIA_PAUSED / :start-paused): the common
     * control points assume playback starts automatically after
     * SetAVTransportURI, and if the renderer stays paused they conclude the
     * session ended and stop serving the stream (which then starves VLC's
     * buffer).
     */
    fun load(url: String) = dispatch {
        Log.i(TAG, "load: $url")
        val mediaWrapper = MLServiceLocator.getAbstractMediaWrapper(Uri.parse(url))
        val current = service
        if (current != null) {
            clearResumeState(current)
            try {
                current.playlistManager.forceStartFromBeginning = true
            } catch (ignored: Exception) {
            }
            current.load(listOf(mediaWrapper), 0)
        } else {
            // PlaybackService not running yet: start it and wait for the instance
            PlaybackService.start(AppContextProvider.appContext)
            AppScope.launch {
                PlaybackService.serviceFlow.first { it != null }
                service?.let {
                    try {
                        it.playlistManager.forceStartFromBeginning = true
                    } catch (ignored: Exception) {
                    }
                    clearResumeState(it)
                }
                service?.load(listOf(mediaWrapper), 0)
            }
        }
    }

    fun play() = dispatch { service?.play() }

    fun pause() = dispatch { service?.pause() }

    fun stop() = dispatch { service?.stop() }

    fun seek(millis: Long) = dispatch {
        service?.playlistManager?.player?.setTime(millis)
    }

    fun setVolume(volume: Int) = dispatch { service?.setVolume(volume.coerceIn(0, 100)) }

    fun setMute(mute: Boolean) = dispatch {
        val player = service?.playlistManager?.player ?: return@dispatch
        if (mute) {
            if (player.getVolume() > 0) lastVolume = player.getVolume()
            player.setVolume(0)
        } else {
            player.setVolume(lastVolume)
        }
    }

    /**
     * A DLNA push always starts a brand-new playback from the beginning.
     * VLC's resume logic would otherwise seek to the previous position stored in
     * [org.videolan.vlc.media.PlaylistManager.savedTime] (left over from a restored
     * playlist) or in the saved settings, causing a fresh push to start mid-file
     * (or stall when the stale position is out of range).
     */
    private fun clearResumeState(current: PlaybackService) {
        try {
            current.playlistManager.savedTime = 0L
        } catch (ignored: Exception) {
        }
        try {
            Settings.getInstance(AppContextProvider.appContext).edit()
                    .putLong(POSITION_IN_MEDIA, -1L)
                    .putLong(POSITION_IN_SONG, -1L)
                    .putLong(VIDEO_RESUME_TIME, -1L)
                    .remove(VIDEO_RESUME_URI)
                    .apply()
        } catch (ignored: Exception) {
        }
    }

    /** Last non-zero volume, used to restore volume when unmuting. */
    @Volatile
    private var lastVolume: Int = 100

    // ---- Read-only queries (called on UPnP stream threads) ----

    fun getPositionMillis(): Long = service?.getTime() ?: 0L

    fun getDurationMillis(): Long = service?.playlistManager?.player?.getLength() ?: 0L

    fun getVolume(): Int {
        val volume = service?.playlistManager?.player?.getVolume() ?: 100
        if (volume > 0) lastVolume = volume
        return volume
    }

    fun isPlaying(): Boolean = service?.playlistManager?.player?.isPlaying() ?: false

    // ---- UPnP time format helpers ("H:MM:SS" per UPnP AV spec) ----

    fun formatUpnpTime(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    }

    fun parseUpnpTime(time: String): Long {
        val parts = time.trim().split(':')
        if (parts.isEmpty()) return 0L
        var millis = 0L
        try {
            for (part in parts) {
                millis = millis * 60L + part.toLong()
            }
        } catch (ignored: NumberFormatException) {
            return 0L
        }
        return millis * 1000L
    }

    private fun dispatch(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else handler.post(block)
    }
}
