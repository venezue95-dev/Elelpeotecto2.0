package cu.todus.download.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DownloadRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val file = File(context.filesDir, "downloads_cache.json")

    // Las peticiones de guardado se funden en una sola: siempre se escribe el estado más reciente.
    private val saveSignal = Channel<Unit>(Channel.CONFLATED)

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    init {
        loadFromDisk()
        scope.launch {
            for (ignored in saveSignal) {
                writeToDisk(_downloads.value)
                delay(SAVE_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 1500L
    }

    private fun loadFromDisk() {
        if (!file.exists()) return
        try {
            val array = JSONArray(file.readText())
            val list = mutableListOf<DownloadItem>()
            for (i in 0 until array.length()) {
                try {
                    val obj = array.getJSONObject(i)
                    val status = try {
                        DownloadItem.Status.valueOf(obj.optString("status", "PAUSED"))
                    } catch (_: Exception) {
                        DownloadItem.Status.PAUSED
                    }
                    // Tras reiniciar la app no queda ninguna descarga corriendo.
                    val restored = when (status) {
                        DownloadItem.Status.QUEUED,
                        DownloadItem.Status.CONNECTING,
                        DownloadItem.Status.DOWNLOADING -> DownloadItem.Status.PAUSED
                        else -> status
                    }
                    list.add(
                        DownloadItem(
                            id = obj.getString("id"),
                            url = obj.getString("url"),
                            name = obj.getString("name"),
                            totalBytes = obj.optLong("totalBytes", 0L),
                            downloadedBytes = obj.optLong("downloadedBytes", 0L),
                            speedBps = 0L,
                            status = restored,
                            threads = obj.optInt("threads", 4),
                            destinationPath = obj.optString("destinationPath", ""),
                            errorMsg = if (obj.has("errorMsg")) obj.getString("errorMsg") else null,
                            addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                            completedTimestamp = obj.optLong("completedTimestamp", 0L),
                            mimeType = obj.optString("mimeType", ""),
                            nameIsCustom = obj.optBoolean("nameIsCustom", false)
                        )
                    )
                } catch (_: Exception) {
                    // Un registro dañado no debe borrar todo el historial.
                }
            }
            _downloads.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeToDisk(list: List<DownloadItem>) {
        try {
            val array = JSONArray()
            for (item in list) {
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("url", item.url)
                        put("name", item.name)
                        put("totalBytes", item.totalBytes)
                        put("downloadedBytes", item.downloadedBytes)
                        put("status", item.status.name)
                        put("threads", item.threads)
                        put("destinationPath", item.destinationPath)
                        item.errorMsg?.let { put("errorMsg", it) }
                        put("addedTimestamp", item.addedTimestamp)
                        put("completedTimestamp", item.completedTimestamp)
                        put("mimeType", item.mimeType)
                        put("nameIsCustom", item.nameIsCustom)
                    }
                )
            }
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(array.toString())
            if (!tempFile.renameTo(file)) {
                file.writeText(array.toString())
                tempFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestSave() {
        saveSignal.trySend(Unit)
    }

    fun get(id: String): DownloadItem? = _downloads.value.firstOrNull { it.id == id }

    fun addItem(item: DownloadItem) {
        _downloads.update { listOf(item) + it }
        requestSave()
    }

    /** Modifica un elemento de forma atómica. Devuelve el nuevo valor o null si ya no existe. */
    fun update(id: String, transform: (DownloadItem) -> DownloadItem): DownloadItem? {
        var result: DownloadItem? = null
        _downloads.update { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) {
                result = null
                list
            } else {
                val updated = transform(list[index])
                result = updated
                list.toMutableList().also { it[index] = updated }
            }
        }
        if (result != null) requestSave()
        return result
    }

    /** Actualiza solo el progreso, y solo si el elemento sigue descargando (no pisa pausas ni cancelaciones). */
    fun updateProgress(id: String, downloaded: Long, total: Long, speed: Long) {
        _downloads.update { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index < 0 || list[index].status != DownloadItem.Status.DOWNLOADING) {
                list
            } else {
                val current = list[index]
                list.toMutableList().also {
                    it[index] = current.copy(
                        downloadedBytes = downloaded,
                        totalBytes = if (total > 0) total else current.totalBytes,
                        speedBps = speed
                    )
                }
            }
        }
        requestSave()
    }

    fun removeItem(id: String) {
        _downloads.update { list -> list.filterNot { it.id == id } }
        requestSave()
    }

    fun clearCompleted() {
        _downloads.update { list -> list.filter { it.status != DownloadItem.Status.COMPLETED } }
        requestSave()
    }
}
