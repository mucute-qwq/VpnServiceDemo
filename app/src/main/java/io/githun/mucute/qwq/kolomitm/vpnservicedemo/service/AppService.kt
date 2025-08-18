package io.githun.mucute.qwq.kolomitm.vpnservicedemo.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import engine.Engine
import engine.Key
import io.githun.mucute.qwq.kolomitm.vpnservicedemo.MainActivity
import io.githun.mucute.qwq.kolomitm.vpnservicedemo.R

class AppService : VpnService() {

    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        if (notificationManagerCompat.getNotificationChannelCompat(NOTIFICATION_CHANNEL_ID) == null) {
            notificationManagerCompat.createNotificationChannel(
                NotificationChannelCompat.Builder(
                    NOTIFICATION_CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                )
                    .setName(getString(R.string.app_name))
                    .build()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopKoloMITM()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        when (intent.action) {

            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startKoloMITM()
            }

            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                stopKoloMITM()
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.routing_packets))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop),
                createStopPendingIntent()
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                action = Intent.ACTION_MAIN
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createStopPendingIntent(): PendingIntent {
        return PendingIntent.getForegroundService(
            this,
            1,
            Intent(ACTION_STOP).also { it.`package` = packageName },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun startKoloMITM() {
        state = State.Active

        val builder = Builder()
        builder.setBlocking(true)
        builder.setSession(getString(R.string.app_name))
        builder.addAllowedApplication("com.android.chrome") // Your target application here
        builder.addDnsServer("8.8.8.8")

        // Add IPv4 support
        builder.addAddress(PRIVATE_VLAN4_CLIENT, 30)
        builder.addRoute("0.0.0.0", 0)

        // Add IPv6 support
        builder.addAddress(PRIVATE_VLAN6_CLIENT, 126)
        builder.addRoute("::", 0)

        // Establish TUN session
        parcelFileDescriptor = builder.establish() ?: error("Cannot establish VPN")

        val key = Key()
        key.mark = 0
        key.mtu = 0
        key.device = "fd://${parcelFileDescriptor!!.fd}"
        key.setInterface("")
        key.logLevel = "debug"
        key.proxy = "direct://" // If you want to use proxy, replace it with -proxy socks5://<username>:<password>@server_host:port
        key.restAPI = ""
        key.tcpSendBufferSize = ""
        key.tcpReceiveBufferSize = ""
        key.tcpModerateReceiveBuffer = false

        Engine.insert(key)

        // Start routing
        Engine.start()
    }

    private fun stopKoloMITM() {
        state = State.Inactive

        // I don't recommend to call Engine.stop() because it will crash our process
        // Just close the ParcelFileDescriptor to release sources
        parcelFileDescriptor?.close()
        parcelFileDescriptor = null
    }

    enum class State {

        Active, Inactive, Loading

    }

    companion object {

        private const val NOTIFICATION_CHANNEL_ID = "appservice_channel"

        private const val NOTIFICATION_ID = 120

        const val ACTION_START =
            "io.githun.mucute.qwq.kolomitm.vpnservicedemo.service.AppService.start"

        const val ACTION_STOP =
            "io.githun.mucute.qwq.kolomitm.vpnservicedemo.service.AppService.stop"

        var state by mutableStateOf(State.Inactive)
            private set

        const val PRIVATE_VLAN4_CLIENT = "10.13.37.1"

        const val PRIVATE_VLAN6_CLIENT = "1337::1"

    }

}