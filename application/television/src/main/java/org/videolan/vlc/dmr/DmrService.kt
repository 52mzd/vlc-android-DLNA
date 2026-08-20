/*****************************************************************************
 * DmrService.kt
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

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.android.AndroidUpnpService
import org.jupnp.android.AndroidUpnpServiceImpl
import org.jupnp.model.DefaultServiceManager
import org.jupnp.model.meta.DeviceDetails
import org.jupnp.model.meta.DeviceIdentity
import org.jupnp.model.meta.LocalDevice
import org.jupnp.model.meta.LocalService
import org.jupnp.model.meta.ManufacturerDetails
import org.jupnp.model.meta.ModelDetails
import org.jupnp.model.types.DLNACaps
import org.jupnp.model.types.DLNADoc
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDN
import org.videolan.resources.AppContextProvider
import org.videolan.television.ui.DmrReceiverActivity
import org.videolan.tools.Settings
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.gui.helpers.MISC_CHANNEL_ID
import org.videolan.vlc.gui.helpers.NotificationHelper
import java.util.UUID

/**
 * Foreground service hosting the DLNA Media Renderer.
 *
 * Binds the jUPnP protocol stack ([AndroidUpnpServiceImpl]), registers this
 * device as a `MediaRenderer:1` on the local network and bridges control
 * commands to [PlaybackService] through [DmrBridge].
 *
 * Started/stopped from the TV settings switch; not started on boot.
 */
class DmrService : Service() {

    private val tag = "VLC/DmrService"

    private var upnpService: AndroidUpnpService? = null
    private var device: LocalDevice? = null
    private var upnpBound = false

    // 跟随前台 mode (dlna_background_receive=OFF): track how many VLC activities are
    // resumed so the renderer only receives while VLC is in the foreground. In this mode
    // the device is only registered while resumedCount>0, so a swiped/Home'd VLC stops
    // receiving (no "no window + receiving" state, hence no BAL exposure). In 后台接收
    // mode (ON) this tracking is ignored — the device stays registered and the resident
    // receiver window keeps a surface resumed.
    private var resumedCount = 0
    private var lifecycleRegistered = false

    private val lifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            resumedCount++
            // 后台接收 keeps the device registered regardless of foreground; only
            // re-evaluate in 跟随前台 mode.
            if (followsForeground()) registerDeviceIfForeground()
        }

        override fun onActivityPaused(activity: Activity) {
            resumedCount--
            if (followsForeground() && resumedCount <= 0) unregisterDevice()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            upnpService = binder as AndroidUpnpService
            registerDeviceIfForeground()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // jUPnP service died, rebind on next start command
            upnpService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.createNotificationChannels(this)
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(tag, "Failed to start foreground notification", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Track VLC foreground for 跟随前台 mode. Registered once (idempotent) on the app
        // Application; kept in onStartCommand so a START_STICKY restart re-registers it.
        if (!lifecycleRegistered) {
            (applicationContext as? Application)?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            lifecycleRegistered = true
            // Seed the counter: the callback only fires for future resume/pause events, so
            // if an activity is already resumed (e.g. the user toggles the switch from
            // settings) account for it here or the renderer would never register.
            if (AppContextProvider.currentActivity != null) resumedCount++
        }
        if (!upnpBound) bindUpnpService()
        // Make sure the playback service is up so that a pushed URI can play immediately
        try {
            PlaybackService.start(this)
        } catch (e: Exception) {
            Log.w(tag, "Failed to start PlaybackService", e)
        }
        // 后台接收 mode: keep a resident receiver window resumed so a push always has a
        // surface. Kept in onStartCommand (not onCreate) so a START_STICKY restart
        // re-establishes the resumed window.
        if (Settings.getInstance(this).getBoolean(KEY_DLNA_BACKGROUND_RECEIVE, false)) {
            DmrReceiverActivity.start(this)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (lifecycleRegistered) {
            (applicationContext as? Application)?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            lifecycleRegistered = false
        }
        unregisterDevice()
        upnpService = null
        // Signal the resident receiver window to finish when DMR is switched off.
        sendBroadcast(Intent(ACTION_RECEIVER_STOP).setPackage(packageName))
        if (upnpBound) {
            upnpBound = false
            try {
                unbindService(serviceConnection)
            } catch (ignored: IllegalArgumentException) {
            }
        }
        super.onDestroy()
    }

    private fun bindUpnpService() {
        upnpBound = true
        try {
            bindService(Intent(this, AndroidUpnpServiceImpl::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(tag, "Failed to bind UPnP service", e)
            upnpBound = false
        }
    }

    /** Register the renderer only when VLC should be receiving: in 跟随前台 mode the
     *  device must be skipped while no VLC activity is resumed (else it would accept
     *  pushes with no window — the exact BAL bug this feature fixes). */
    private fun registerDeviceIfForeground() {
        if (followsForeground() && resumedCount <= 0) return
        registerDevice()
    }

    /** 跟随前台 mode = the 后台接收 switch is OFF. In that mode the renderer follows
     *  VLC's foreground state. In 后台接收 mode it stays registered regardless. */
    private fun followsForeground() = !Settings.getInstance(this).getBoolean(KEY_DLNA_BACKGROUND_RECEIVE, false)

    private fun unregisterDevice() {
        val service = upnpService ?: run { device = null; return }
        if (device != null) {
            try {
                service.getRegistry().removeDevice(device)
                Log.i(tag, "DLNA renderer unregistered (VLC left foreground)")
            } catch (e: Exception) {
                Log.w(tag, "Failed to unregister DLNA renderer", e)
            }
        }
        device = null
    }

    private fun registerDevice() {
        val service = upnpService ?: return
        if (device != null) return
        try {
            val details = DeviceDetails(
                    getFriendlyName(),
                    ManufacturerDetails("VideoLAN"),
                    ModelDetails("VLC for Android", "DLNA Media Renderer", BuildConfig.VLC_VERSION_NAME),
                    null,
                    arrayOf(DLNADoc("DMR", "1.50")),
                    DLNACaps(arrayOf("playcontainer-0-0", "av-1-0"))
            )
            device = LocalDevice(
                    DeviceIdentity(UDN(getOrCreateUdn())),
                    UDADeviceType("MediaRenderer", 1),
                    details,
                    arrayOf(
                            bindService(AVTransportService::class.java),
                            bindService(RenderingControlService::class.java),
                            bindService(ConnectionManagerService::class.java)
                    )
            )
            service.getRegistry().addDevice(device)
            Log.i(tag, "DLNA renderer registered as ${Build.MODEL}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register DLNA renderer", e)
            device = null
        }
    }

    /**
     * Reads a service implementation's annotations and binds it to its manager.
     * Without [org.jupnp.model.LocalService.setManager] jUPnP cannot invoke actions
     * (every SOAP action would fail with ACTION_FAILED / HTTP 500).
     */
    private fun <T : Any> bindService(clazz: Class<T>): LocalService<T> {
        @Suppress("UNCHECKED_CAST")
        val service: LocalService<T> = AnnotationLocalServiceBinder().read(clazz) as LocalService<T>
        service.setManager(DefaultServiceManager(service, clazz))
        return service
    }

    /**
     * Custom device name from settings, falling back to "VLC (<model>)" when empty.
     */
    private fun getFriendlyName(): String {
        val custom = Settings.getInstance(this).getString(KEY_DLNA_NAME, null)?.takeIf { it.isNotBlank() }
        return custom ?: getString(R.string.dmr_device_name, Build.MODEL)
    }

    private fun getOrCreateUdn(): String {
        val settings = Settings.getInstance(this)
        settings.getString(KEY_DLNA_UDN, null)?.let { return it }
        val udn = "uuid:" + UUID.randomUUID()
        settings.edit().putString(KEY_DLNA_UDN, udn).apply()
        return udn
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, StartActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MISC_CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_notif_remote_access)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getString(R.string.dmr_enabled_title))
                .setContentText(getString(R.string.dmr_enabled_summary))
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .build()
    }

    companion object {
        const val KEY_DLNA_RECEIVER = "dlna_receiver"
        const val KEY_DLNA_NAME = "dlna_receiver_name"
        const val KEY_DLNA_UDN = "dlna_udn"
        const val KEY_DLNA_BACKGROUND_RECEIVE = "dlna_background_receive"

        /** Broadcast action signalling the resident receiver window to finish. */
        const val ACTION_RECEIVER_STOP = "org.videolan.vlc.dmr.RECEIVER_STOP"
        private const val NOTIFICATION_ID = 0x4D52 // "MR"
    }
}
