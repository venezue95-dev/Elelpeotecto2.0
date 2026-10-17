package cu.todus.download.data

import java.util.Locale

data class DownloadItem(
    val id: String,
    val url: String,
    val name: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBps: Long = 0L,
    val status: Status = Status.QUEUED,
    val threads: Int = 4,
    val progress: Float = 0f,
    val destinationPath: String = "",
    val errorMsg: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long = 0L,
    val mimeType: String = "",
    /** true si el usuario escribió el nombre; si no, el motor usa el que indique el servidor. */
    val nameIsCustom: Boolean = false
) {
    enum class Status {
        QUEUED,
        CONNECTING,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        ERROR,
        CANCELLED
    }

    val isFinished: Boolean
        get() = status == Status.COMPLETED || status == Status.ERROR || status == Status.CANCELLED

    /** En cola, conectando o descargando (necesita el servicio en primer plano). */
    val isActive: Boolean
        get() = status == Status.QUEUED || status == Status.CONNECTING || status == Status.DOWNLOADING

    val percentage: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    fun humanSpeed(): String = formatSpeed(speedBps)

    fun humanSize(bytes: Long = totalBytes): String = formatSize(bytes)

    fun etaString(): String {
        if (status != Status.DOWNLOADING || speedBps <= 0L || totalBytes <= 0L) return ""
        val remainingBytes = totalBytes - downloadedBytes
        if (remainingBytes <= 0) return "Completando..."
        val remainingSeconds = remainingBytes / speedBps
        return when {
            remainingSeconds >= 3600 -> {
                val hours = remainingSeconds / 3600
                val mins = (remainingSeconds % 3600) / 60
                "${hours}h ${mins}m"
            }
            remainingSeconds >= 60 -> {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                "${mins}m ${secs}s"
            }
            else -> "${remainingSeconds}s"
        }
    }

    val fileExtension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isVideo: Boolean
        get() = fileExtension in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp")

    val isAudio: Boolean
        get() = fileExtension in listOf("mp3", "m4a", "flac", "ogg", "wav", "aac", "opus")

    val isDocument: Boolean
        get() = fileExtension in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub")

    val isArchive: Boolean
        get() = fileExtension in listOf("zip", "rar", "7z", "tar", "gz", "apk")
}

fun formatSpeed(bytesPerSecond: Long): String {
    val speed = bytesPerSecond.toDouble()
    return when {
        speed >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB/s", speed / (1024 * 1024))
        speed >= 1024 -> String.format(Locale.US, "%.1f KB/s", speed / 1024)
        else -> "$bytesPerSecond B/s"
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val b = bytes.toDouble()
    return when {
        b >= 1024.0 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", b / (1024.0 * 1024 * 1024))
        b >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", b / (1024 * 1024))
        b >= 1024 -> String.format(Locale.US, "%.1f KB", b / 1024)
        else -> "$bytes B"
    }
}
