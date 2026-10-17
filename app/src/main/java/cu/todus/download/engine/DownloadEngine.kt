package cu.todus.download.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import cu.todus.download.data.AppPreferences
import cu.todus.download.data.DownloadItem
import cu.todus.download.data.DownloadRepository
import cu.todus.download.data.TodusConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.math.min

internal data class FileMetadata(
    val fileName: String,
    val contentLength: Long,
    val supportsRanges: Boolean
)

class DownloadEngine(
    private val context: Context,
    private val preferences: AppPreferences,
    private val repository: DownloadRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /** Qué pidió el usuario al cancelar un trabajo: pausar (conservar parcial) o cancelar (descartar). */
    private val stopIntents = ConcurrentHashMap<String, DownloadItem.Status>()
    private val network = TodusNetworkClient()

    /** Cola de espera (orden de llegada) y número de descargas que ocupan un hueco ahora mismo. */
    private val waiting = ConcurrentLinkedQueue<String>()
    private val slots = AtomicInteger(0)

    private companion object {
        const val TAG = "DownloadEngine"
        const val MIN_MULTI_BYTES = 1024L * 1024L
        const val MAX_AUTO_RETRIES = 6
        const val BUFFER_SIZE = 64 * 1024
        const val TICK_MS = 800L
    }

    // ───────────────────────── API pública ─────────────────────────

    fun startDownload(item: DownloadItem) {
        val id = item.id
        val latest = repository.get(id) ?: return
        if (latest.status == DownloadItem.Status.COMPLETED) return

        // LAZY + putIfAbsent: el trabajo se registra ANTES de ejecutarse, así que un fallo
        // inmediato no puede dejar una entrada huérfana ni se lanzan dos trabajos a la vez.
        val job = scope.launch(start = CoroutineStart.LAZY) { runDownload(id) }
        if (activeJobs.putIfAbsent(id, job) != null) {
            job.cancel()
            return
        }
        stopIntents.remove(id)
        job.invokeOnCompletion { activeJobs.remove(id, job) }
        job.start()
    }

    fun pauseDownload(id: String) {
        val job = activeJobs[id]
        if (job != null) {
            stopIntents[id] = DownloadItem.Status.PAUSED
            job.cancel()
        } else {
            repository.update(id) {
                if (it.status == DownloadItem.Status.COMPLETED) it
                else it.copy(status = DownloadItem.Status.PAUSED, speedBps = 0L)
            }
        }
    }

    fun pauseAll() {
        activeJobs.keys.toList().forEach { pauseDownload(it) }
    }

    /** Cancela y elimina la descarga de la lista, borrando lo descargado hasta ahora. */
    fun cancelDownload(id: String) {
        val job = activeJobs[id]
        if (job != null) {
            stopIntents[id] = DownloadItem.Status.CANCELLED
            job.cancel()
        } else {
            discard(id)
        }
    }

    // ───────────────────────── Ciclo de una descarga ─────────────────────────

    /**
     * Pone la descarga en cola, espera su turno (máximo [AppPreferences.maxConcurrent] a la vez,
     * el resto queda en estado QUEUED) y la ejecuta con reintentos.
     */
    private suspend fun runDownload(id: String) {
        repository.update(id) {
            if (it.status == DownloadItem.Status.COMPLETED) it
            else it.copy(status = DownloadItem.Status.QUEUED, errorMsg = null, speedBps = 0L)
        }
        try {
            acquireSlot(id)
        } catch (e: CancellationException) {
            onStopped(id)
            throw e
        }
        try {
            runWithRetries(id)
        } finally {
            slots.decrementAndGet()
        }
    }

    /** Espera su turno respetando el orden de llegada. El límite se lee cada vez: se puede cambiar en caliente. */
    private suspend fun acquireSlot(id: String) {
        waiting.add(id)
        try {
            while (true) {
                if (waiting.peek() == id) {
                    val current = slots.get()
                    if (current < preferences.maxConcurrent && slots.compareAndSet(current, current + 1)) {
                        waiting.remove(id)
                        return
                    }
                }
                delay(250L)
            }
        } catch (e: CancellationException) {
            waiting.remove(id)
            throw e
        }
    }

    private suspend fun runWithRetries(id: String) {
        var attempt = 0
        try {
            while (true) {
                attempt++
                try {
                    transfer(id)
                    return
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Si el fallo vino de pausar/cancelar, esto lanza CancellationException.
                    currentCoroutineContext().ensureActive()
                    Log.w(TAG, "Intento $attempt fallido: ${e.javaClass.simpleName}")

                    val retry = preferences.autoResume &&
                        attempt <= MAX_AUTO_RETRIES &&
                        DownloadErrors.isRetriable(e)
                    if (!retry) {
                        repository.update(id) {
                            it.copy(
                                status = DownloadItem.Status.ERROR,
                                errorMsg = DownloadErrors.friendlyMessage(e),
                                speedBps = 0L
                            )
                        }
                        return
                    }
                    // Reanudación automática: espera y vuelve a intentar desde donde quedó.
                    repository.update(id) {
                        it.copy(status = DownloadItem.Status.CONNECTING, errorMsg = null, speedBps = 0L)
                    }
                    delay(min(30_000L, 2_000L shl (attempt - 1)))
                }
            }
        } catch (e: CancellationException) {
            onStopped(id)
            throw e
        }
    }

    private fun onStopped(id: String) {
        when (stopIntents.remove(id)) {
            DownloadItem.Status.CANCELLED -> discard(id)
            else -> repository.update(id) {
                if (it.status == DownloadItem.Status.COMPLETED) it
                else it.copy(status = DownloadItem.Status.PAUSED, speedBps = 0L)
            }
        }
    }

    private fun discard(id: String) {
        repository.get(id)?.let { deletePartialFiles(it) }
        repository.removeItem(id)
    }

    /** Un intento completo: sondeo, destino, descarga y cierre del archivo. */
    private suspend fun transfer(id: String) {
        var item = repository.update(id) {
            it.copy(status = DownloadItem.Status.CONNECTING, errorMsg = null, speedBps = 0L)
        } ?: throw CancellationException("Descarga eliminada")

        NetworkGuard.check(context)

        val httpUrl = item.url.toHttpUrlOrNull() ?: throw IllegalArgumentException("url")
        if (!TodusConfig.isTodusHost(httpUrl.host)) throw NotTodusLinkException()

        // 1) Metadatos (tamaño, nombre, soporte de rangos)
        val meta = probe(item.url)
        currentCoroutineContext().ensureActive()

        var total = item.totalBytes
        if (total <= 0L && meta != null && meta.contentLength > 0L) total = meta.contentLength

        // 2) Destino
        val dir = destinationFolder()
        val finalFile = resolveFinalFile(item, meta, dir)
        val parent = finalFile.parentFile ?: dir
        val partFile = File(parent, finalFile.name + ".tdpart")
        val metaFile = File(parent, finalFile.name + ".tdpart.meta")

        item = repository.update(id) {
            it.copy(
                name = finalFile.name,
                totalBytes = total,
                destinationPath = finalFile.absolutePath,
                status = DownloadItem.Status.DOWNLOADING
            )
        } ?: throw CancellationException("Descarga eliminada")

        // 3) Descarga
        val threads = item.threads.coerceIn(1, 16)
        val wantMulti = total >= MIN_MULTI_BYTES && threads > 1 && (meta == null || meta.supportsRanges)
        var done = false
        if (wantMulti) {
            try {
                downloadMulti(id, item.url, total, threads, partFile, metaFile)
                done = true
            } catch (_: RangesNotSupportedException) {
                // El servidor no admite rangos: se baja en un solo hilo.
            }
        }
        if (!done) downloadSingle(id, item.url, total, partFile, metaFile)

        // 4) Cierre: el parcial pasa a ser el archivo final
        if (finalFile.exists()) finalFile.delete()
        if (!partFile.renameTo(finalFile)) throw IOException("No se pudo guardar el archivo")
        metaFile.delete()

        try {
            MediaScannerConnection.scanFile(context, arrayOf(finalFile.absolutePath), null, null)
        } catch (_: Exception) {
        }

        val size = finalFile.length()
        repository.update(id) {
            it.copy(
                status = DownloadItem.Status.COMPLETED,
                downloadedBytes = size,
                totalBytes = size,
                speedBps = 0L,
                errorMsg = null,
                destinationPath = finalFile.absolutePath,
                completedTimestamp = System.currentTimeMillis()
            )
        }
    }

    // ───────────────────────── Sondeo de metadatos ─────────────────────────

    private suspend fun probe(url: String): FileMetadata? {
        var length = -1L
        var ranges = false
        var name = FileNames.fromUrl(url)

        // HEAD primero
        try {
            val call = network.client.newCall(Request.Builder().url(url).head().build())
            withCancellableCall(call) {
                call.execute().use { resp ->
                    if (resp.isSuccessful) {
                        length = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                        ranges = resp.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
                        resp.header("Content-Disposition")?.let { d ->
                            FileNames.fromDisposition(d).takeIf { it.isNotBlank() }?.let { name = it }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // Si HEAD no dio tamaño, GET de 1 byte
        if (length <= 0L) {
            try {
                val call = network.client.newCall(
                    Request.Builder().url(url).header("Range", "bytes=0-0").get().build()
                )
                withCancellableCall(call) {
                    call.execute().use { resp ->
                        if (resp.isSuccessful) {
                            val total = resp.header("Content-Range")
                                ?.substringAfterLast('/')?.trim()?.toLongOrNull()
                            if (total != null && total > 0L) {
                                length = total
                                ranges = true
                            } else {
                                length = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                            }
                            resp.header("Content-Disposition")?.let { d ->
                                FileNames.fromDisposition(d).takeIf { it.isNotBlank() }?.let { name = it }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return null
            }
        }
        return FileMetadata(fileName = name, contentLength = length, supportsRanges = ranges)
    }

    // ───────────────────────── Un solo hilo ─────────────────────────

    private suspend fun downloadSingle(
        id: String,
        url: String,
        total: Long,
        part: File,
        metaFile: File
    ): Unit = coroutineScope {
        // Restos de un intento multi-hilo: el parcial está pre-asignado y no es fiable.
        if (metaFile.exists()) {
            metaFile.delete()
            part.delete()
        }
        var existing = if (part.exists()) part.length() else 0L
        if (total > 0L && existing > total) {
            part.delete()
            existing = 0L
        }
        if (total > 0L && existing == total) return@coroutineScope // ya estaba completo

        val counter = AtomicLong(existing)
        val totalRef = AtomicLong(total)
        val ticker = launchTicker(id, counter, totalRef)
        try {
            var finished = false
            for (round in 0..1) {
                val builder = Request.Builder().url(url)
                if (existing > 0L) builder.header("Range", "bytes=$existing-")
                val call = network.client.newCall(builder.build())
                val startAt = existing

                val mustRestart = withCancellableCall(call) {
                    call.execute().use { resp ->
                        val code = resp.code
                        if (code == 416 && startAt > 0L) return@use true
                        if (code != 200 && code != 206) throw HttpStatusException(code)
                        val body = resp.body ?: throw IOException("Cuerpo de respuesta vacío")

                        var append = false
                        if (code == 206 && startAt > 0L) {
                            val rangeStart = resp.header("Content-Range")
                                ?.substringAfter("bytes ", "")?.substringBefore('-')?.trim()?.toLongOrNull()
                            if (rangeStart != startAt) return@use true // el servidor devolvió otro tramo
                            append = true
                        }
                        // Con 200 el servidor ignoró el rango: se reescribe desde cero.
                        val base = if (append) startAt else 0L
                        counter.set(base)
                        val len = body.contentLength()
                        if (totalRef.get() <= 0L && len > 0L) totalRef.set(base + len)

                        FileOutputStream(part, append).use { out ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n == -1) break
                                    out.write(buffer, 0, n)
                                    counter.addAndGet(n.toLong())
                                }
                                out.flush()
                            }
                        }
                        val expected = totalRef.get()
                        if (expected > 0L && counter.get() != expected) {
                            throw IOException("Unexpected end of stream")
                        }
                        false
                    }
                }

                if (!mustRestart) {
                    finished = true
                    break
                }
                part.delete()
                existing = 0L
                counter.set(0L)
            }
            if (!finished) throw IOException("No se pudo reanudar la descarga")
        } finally {
            ticker.cancel()
        }
        Unit
    }

    // ───────────────────────── Varios hilos ─────────────────────────

    private suspend fun downloadMulti(
        id: String,
        url: String,
        total: Long,
        threads: Int,
        part: File,
        metaFile: File
    ): Unit = coroutineScope {
        val chunkSize = total / threads
        val starts = LongArray(threads) { it * chunkSize }
        val ends = LongArray(threads) { if (it == threads - 1) total - 1 else it * chunkSize + chunkSize - 1 }

        // Progreso por tramo: permite reanudar sin volver a bajar lo ya descargado.
        val done = AtomicLongArray(threads)
        if (part.exists() && part.length() == total) {
            readChunkMeta(metaFile, total, threads, starts, ends, done)
        } else {
            metaFile.delete()
        }
        RandomAccessFile(part, "rw").use { if (it.length() != total) it.setLength(total) }

        val counter = AtomicLong((0 until threads).sumOf { done.get(it) })
        val totalRef = AtomicLong(total)
        val ticker = launchTicker(id, counter, totalRef) { writeChunkMeta(metaFile, total, threads, done) }
        try {
            (0 until threads).map { i ->
                launch { downloadChunk(url, part, starts[i], ends[i], i, done, counter) }
            }.joinAll()
        } finally {
            ticker.cancel()
            writeChunkMeta(metaFile, total, threads, done)
        }

        val sum = (0 until threads).sumOf { done.get(it) }
        if (sum != total) throw IOException("Unexpected end of stream")
        Unit
    }

    private suspend fun downloadChunk(
        url: String,
        part: File,
        chunkStart: Long,
        chunkEnd: Long,
        index: Int,
        done: AtomicLongArray,
        counter: AtomicLong
    ) {
        val from = chunkStart + done.get(index)
        if (from > chunkEnd) return // tramo ya completo

        val call = network.client.newCall(
            Request.Builder().url(url).header("Range", "bytes=$from-$chunkEnd").build()
        )
        withCancellableCall(call) {
            call.execute().use { resp ->
                // Con 200 el servidor mandaría el archivo entero a cada hilo y se corrompería.
                if (resp.code == 200) throw RangesNotSupportedException()
                if (resp.code != 206) throw HttpStatusException(resp.code)
                val body = resp.body ?: throw IOException("Cuerpo de respuesta vacío")

                body.byteStream().use { input ->
                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(from)
                        val buffer = ByteArray(BUFFER_SIZE)
                        var left = chunkEnd - from + 1
                        while (left > 0L) {
                            val n = input.read(buffer, 0, min(buffer.size.toLong(), left).toInt())
                            if (n == -1) break
                            raf.write(buffer, 0, n)
                            left -= n
                            done.addAndGet(index, n.toLong())
                            counter.addAndGet(n.toLong())
                        }
                        if (left > 0L) throw IOException("Unexpected end of stream")
                    }
                }
            }
        }
    }

    private fun readChunkMeta(
        metaFile: File,
        total: Long,
        threads: Int,
        starts: LongArray,
        ends: LongArray,
        done: AtomicLongArray
    ) {
        try {
            if (!metaFile.exists()) return
            val lines = metaFile.readLines()
            if (lines.size != threads + 1 || lines[0] != "$total:$threads") return
            val values = LongArray(threads) { lines[it + 1].trim().toLong() }
            for (i in 0 until threads) {
                if (values[i] < 0L || values[i] > ends[i] - starts[i] + 1) return
            }
            for (i in 0 until threads) done.set(i, values[i])
        } catch (_: Exception) {
            for (i in 0 until threads) done.set(i, 0L)
        }
    }

    private fun writeChunkMeta(metaFile: File, total: Long, threads: Int, done: AtomicLongArray) {
        try {
            val sb = StringBuilder("$total:$threads")
            for (i in 0 until threads) sb.append('\n').append(done.get(i))
            metaFile.writeText(sb.toString())
        } catch (_: Exception) {
        }
    }

    // ───────────────────────── Utilidades ─────────────────────────

    /** Publica velocidad y progreso cada [TICK_MS] ms hasta que se cancele. */
    private fun CoroutineScope.launchTicker(
        id: String,
        counter: AtomicLong,
        totalRef: AtomicLong,
        onTick: () -> Unit = {}
    ): Job = launch {
        var last = counter.get()
        var lastTime = System.nanoTime()
        while (isActive) {
            delay(TICK_MS)
            val now = System.nanoTime()
            val current = counter.get()
            val elapsedMs = ((now - lastTime) / 1_000_000L).coerceAtLeast(1L)
            val speed = ((current - last).coerceAtLeast(0L) * 1000L) / elapsedMs
            last = current
            lastTime = now
            onTick()
            repository.updateProgress(id, current, totalRef.get(), speed)
        }
    }

    /**
     * Ejecuta [block] (bloqueante) en IO y, si la corrutina se cancela, cierra la llamada HTTP
     * para que la lectura bloqueada termine al instante (pausar/cancelar ya no espera al timeout).
     */
    private suspend fun <T> withCancellableCall(call: Call, block: () -> T): T = coroutineScope {
        val watcher = launch(Dispatchers.Default) {
            try {
                awaitCancellation()
            } finally {
                call.cancel()
            }
        }
        try {
            withContext(Dispatchers.IO) { block() }
        } finally {
            watcher.cancel()
        }
    }

    private fun resolveFinalFile(item: DownloadItem, meta: FileMetadata?, dir: File): File {
        // Reanudación: si ya hay un parcial de esta descarga, se conserva su nombre y carpeta.
        if (item.destinationPath.isNotBlank()) {
            val previous = File(item.destinationPath)
            val parent = previous.parentFile
            if (parent != null && File(parent, previous.name + ".tdpart").exists()) return previous
        }

        val metaName = meta?.fileName.orEmpty()
        val raw = when {
            item.nameIsCustom && item.name.isNotBlank() -> item.name
            metaName.isNotBlank() -> metaName
            else -> item.name
        }
        val safe = FileNames.sanitize(raw).ifBlank { "todus_${System.currentTimeMillis()}.bin" }
        return File(dir, FileNames.unique(dir, safe))
    }

    /** Carpeta elegida por el usuario; si no se puede escribir en ella, usa la de la app. */
    private fun destinationFolder(): File {
        val target = File(preferences.downloadDir)
        try {
            if (!target.exists()) target.mkdirs()
            if (target.isDirectory && target.canWrite()) return target
        } catch (_: SecurityException) {
        }

        // Alternativa: carpeta propia de la app.
        val appExternal = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (appExternal != null) {
            val appTarget = File(appExternal, AppPreferences.DEFAULT_FOLDER)
            if (!appTarget.exists()) appTarget.mkdirs()
            return appTarget
        }

        val internalTarget = File(context.filesDir, AppPreferences.DEFAULT_FOLDER)
        if (!internalTarget.exists()) internalTarget.mkdirs()
        return internalTarget
    }

    private fun deletePartialFiles(item: DownloadItem) {
        if (item.destinationPath.isBlank()) return
        val file = File(item.destinationPath)
        val parent = file.parentFile ?: return
        File(parent, file.name + ".tdpart").delete()
        File(parent, file.name + ".tdpart.meta").delete()
    }
}
