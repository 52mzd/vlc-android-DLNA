/*****************************************************************************
 * FontPrewarmer.kt
 *****************************************************************************
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
 ***************************************************************************/

package org.videolan.vlc.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.resources.R
import org.videolan.tools.AppScope
import org.videolan.vlc.PlaybackService
import java.io.File

/**
 * Builds the freetype/fontconfig font database in the background at app start.
 *
 * On low-end devices the first video playback otherwise stalls on a black
 * screen while the spu text renderer scans all system fonts (~45s on an
 * Amlogic S4 TV box, 3-10s on low-end phones): the build happens on the vout
 * creation path and blocks video output. The resulting cache file
 * (<dataDir>/app_vlc/.cache/fontconfig/<hash>-arm.cache-7) persists across
 * processes and app restarts; it only disappears on install or data clear,
 * which is exactly when this prewarm rebuilds it.
 *
 * Idempotent: with the cache present the warm path is a single directory
 * check and no player is ever created. The prewarm itself is a headless
 * dummy-vout playback of a tiny bundled clip (no UI, no sound, no
 * notification); it yields if a real playback is already running.
 */
object FontPrewarmer {

    private const val TAG = "VLC/FontPrewarmer"
    private const val STARTUP_DELAY_MS = 8_000L
    private const val BUILD_TIMEOUT_MS = 90_000L
    private const val POLL_INTERVAL_MS = 1_000L

    fun maybePrewarm(context: Context) {
        val cacheDir = fontCacheDir(context)
        if (isWarm(cacheDir)) return
        AppScope.launch(Dispatchers.IO) {
            delay(STARTUP_DELAY_MS)
            // A real playback in flight builds the cache anyway — don't compete.
            if (PlaybackService.instance != null) return@launch
            Log.i(TAG, "Pre-warming the fontconfig cache (cold font database)")
            try {
                val libvlc = LibVLC(context, arrayListOf("--vout=dummy", "--aout=dummy"))
                val mediaPlayer = MediaPlayer(libvlc)
                context.resources.openRawResourceFd(R.raw.font_prewarm).use { afd ->
                    val media = Media(libvlc, afd)
                    media.addOption(":input-repeat=65535")
                    mediaPlayer.media = media
                    mediaPlayer.play()
                    val start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < BUILD_TIMEOUT_MS) {
                        delay(POLL_INTERVAL_MS)
                        if (isWarm(cacheDir)) {
                            Log.i(TAG, "Fontconfig cache built in ${System.currentTimeMillis() - start}ms")
                            break
                        }
                        // A real playback started — it builds the cache itself; stop competing.
                        if (PlaybackService.instance != null) {
                            Log.i(TAG, "Aborting font prewarm: real playback started")
                            break
                        }
                    }
                    if (!isWarm(cacheDir) && PlaybackService.instance == null)
                        Log.w(TAG, "Fontconfig cache not built within ${BUILD_TIMEOUT_MS}ms; will retry on next app start")
                    mediaPlayer.stop()
                    media.release()
                }
                mediaPlayer.release()
                libvlc.release()
            } catch (e: Exception) {
                Log.w(TAG, "Font prewarm failed; will retry on next app start", e)
            }
        }
    }

    private fun isWarm(cacheDir: File): Boolean =
        cacheDir.listFiles()?.any { it.name.contains(".cache-") } == true

    private fun fontCacheDir(context: Context): File =
        File(File(context.filesDir.parentFile, "app_vlc"), ".cache/fontconfig")
}
