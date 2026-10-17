package cu.todus.download.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import cu.todus.download.data.AppPreferences
import cu.todus.download.data.DownloadItem
import cu.todus.download.data.DownloadRepository
import cu.todus.download.data.formatSpeed
import cu.todus.download.engine.DownloadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var preferences: AppPreferences

    private var isForeground = false
    private var started = false
    private var lastStartId = 0

    // Para detectar cuándo una descarga pasa a COMPLETED / ERROR y avisar al usuario.
    private val lastStatus = HashMap<String, DownloadItem.Status>()
    private var seeded = false

    companion object {
        const val CHANNEL_ID = "todus_downloads_channel"
        const val DONE_CHANNEL_ID = "todus_done_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START_DOWNLOAD = "cu.todus.download.START"
        const val ACTION_PAUSE_DOWNLOAD = "cu.todus.download.PAUSE"
        const val ACTION_CANCEL_DOWNLOAD = "cu.todus.download.CANCEL"
        const val ACTION_PAUSE_ALL = "cu.todus.download.PAUSE_ALL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        private const val WAKELOCK_TIMEOUT_MS = 24 * 60 * 60 * 1000L

        @Volatile private var engineInstance: DownloadEngine? = null
        @Volatile private var repoInstance: DownloadRepository? = null

        fun getRepository(context: Context): DownloadRepository =
            repoInstance ?: synchronized(this) {
                repoInstance ?: DownloadRepository(context.applicationContext).also { repoInstance = it }
            }

        fun getEngine(context: Context): DownloadEngine =
            engineInstance ?: synchronized(this) {
                engineInstance ?: DownloadEngine(
                    context.applicationContext,
                    AppPreferences(context.applicationContext),
                    getRepository(context)
                ).also { engineInstance = it }
            }

        /** Arranca (o reanuda) una descarga con el servicio en primer plano. */
        fun requestStart(context: Context, id: String) {
            val app = context.applicationContext
            val intent = Intent(app, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, id)
            }
            try {
                ContextCompat.startForegroundService(app, intent)
            } catch (_: Exception) {
                // Si el sistema no permite el servicio, la descarga corre igual dentro del proceso.
                getRepository(app).get(id)?.let { getEngine(app).startDownload(it) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(applicationContext)
        createChannels()

        wakeLock = (getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "toDusDownload::DownloadWakeLock")
            ?.apply { setReferenceCounted(false) }

        serviceScope.launch {
            getRepository(applicationContext).downloads.collect { list ->
                trackTransitions(list)
                if (started) refresh(list)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        started = true
        lastStartId = startId

        val engine = getEngine(applicationContext)
        val repo = getRepository(applicationContext)
        val action = intent?.action
        val id = intent?.getStringExtra(EXTRA_DOWNLOAD_ID)

        // startForegroundService exige llamar a startForeground enseguida; si no, la app se cierra.
        if (action == ACTION_START_DOWNLOAD) {
            promoteToForeground(buildOngoingNotification(repo.downloads.value.filter { it.isActive }))
        }

        when (action) {
            ACTION_START_DOWNLOAD -> id?.let { repo.get(it) }?.let { engine.startDownload(it) }
            ACTION_PAUSE_DOWNLOAD -> id?.let { engine.pauseDownload(it) }
            ACTION_CANCEL_DOWNLOAD -> id?.let { engine.cancelDownload(it) }
            ACTION_PAUSE_ALL -> engine.pauseAll()
        }

        refresh(repo.downloads.value)
        return START_NOT_STICKY
    }

    // ───────────────────────── Notificación en curso ─────────────────────────

    private fun refresh(list: List<DownloadItem>) {
        val active = list.filter { it.isActive }
        if (active.isNotEmpty()) {
            if (wakeLock?.isHeld == false) {
                try { wakeLock?.acquire(WAKELOCK_TIMEOUT_MS) } catch (_: Exception) {}
            }
            val notification = buildOngoingNotification(active)
            if (isForeground) {
                getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
            } else {
                promoteToForeground(notification)
            }
        } else {
            if (wakeLock?.isHeld == true) {
                try { wakeLock?.release() } catch (_: Exception) {}
            }
            if (isForeground) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForeground = false
            }
            stopSelf(lastStartId)
        }
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
            isForeground = true
        } catch (_: Exception) {
            // El sistema puede negar el primer plano si la app está en segundo plano.
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildOngoingNotification(active: List<DownloadItem>): Notification {
        val running = active.filter { it.status != DownloadItem.Status.QUEUED }
        val queued = active.size - running.size
        val totalSpeed = running.sumOf { it.speedBps }
        val sized = running.filter { it.totalBytes > 0 }
        val sumTotal = sized.sumOf { it.totalBytes }
        val sumDone = sized.sumOf { it.downloadedBytes.coerceAtMost(it.totalBytes) }
        val percent = if (sumTotal > 0) ((sumDone * 100) / sumTotal).toInt().coerceIn(0, 100) else 0

        val title = when {
            active.isEmpty() -> "Preparando descarga…"
            running.isEmpty() -> "En cola: $queued"
            running.size == 1 && queued == 0 -> running.first().name
            queued > 0 -> "Descargando ${running.size} · $queued en cola"
            else -> "Descargando ${running.size} archivos"
        }
        val text = when {
            active.isEmpty() -> "Iniciando…"
            totalSpeed > 0 -> "${formatSpeed(totalSpeed)}  •  $percent%"
            else -> "Conectando…"
        }

        val pauseAll = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).setAction(ACTION_PAUSE_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, sumTotal <= 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_media_pause, "Pausar todo", pauseAll)
            .build()
    }

    // ───────────────────────── Avisos de fin / error ─────────────────────────

    private fun trackTransitions(list: List<DownloadItem>) {
        if (seeded) {
            for (item in list) {
                val previous = lastStatus[item.id] ?: continue
                if (previous == item.status) continue
                when (item.status) {
                    DownloadItem.Status.COMPLETED -> notifyFinished(item, success = true)
                    DownloadItem.Status.ERROR -> notifyFinished(item, success = false)
                    else -> {}
                }
            }
        }
        lastStatus.clear()
        list.forEach { lastStatus[it.id] = it.status }
        seeded = true
    }

    @SuppressLint("MissingPermission")
    private fun notifyFinished(item: DownloadItem, success: Boolean) {
        if (!preferences.showNotifications) return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(this, DONE_CHANNEL_ID)
            .setSmallIcon(if (success) R.drawable.ic_stat_done else R.drawable.ic_stat_error)
            .setContentTitle(if (success) "Descarga completada" else "Descarga fallida")
            .setContentText(if (success) item.name else "${item.name} · ${item.errorMsg ?: "Error"}")
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            manager.notify((item.id.hashCode() and 0x7fffffff) + 10_000, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Descargas en curso", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progreso de las descargas activas"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(DONE_CHANNEL_ID, "Descargas finalizadas", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos cuando una descarga termina o falla"
            }
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (_: Exception) {}
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
