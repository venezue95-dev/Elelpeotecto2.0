package cu.todus.download.engine

import java.io.File
import java.net.URLDecoder

/** Utilidades puras para nombres de archivo (sin dependencias de Android). */
object FileNames {

    private val illegalChars = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private const val MAX_BYTES = 200 // deja margen para ".tdpart.meta" y " (n)"

    /** Quita rutas, caracteres prohibidos y limita la longitud. Devuelve "" si no queda nada útil. */
    fun sanitize(raw: String): String {
        var name = raw.substringAfterLast('/').substringAfterLast('\\')
        name = illegalChars.replace(name, "_").trim().trim('.').trim()
        if (name.isEmpty()) return ""
        return limitBytes(name)
    }

    /** Nombre derivado de la URL (último segmento del path) o "" si no hay. */
    fun fromUrl(url: String): String {
        val path = url.substringBefore('#').substringBefore('?')
        val raw = path.substringAfterLast('/')
        if (raw.isBlank()) return ""
        return sanitize(decode(raw))
    }

    /** Nombre indicado en la cabecera Content-Disposition o "" si no viene. */
    fun fromDisposition(disposition: String): String {
        val utf8 = Regex("filename\\*=(?:UTF-8''|utf-8'')([^;]+)").find(disposition)
        if (utf8 != null) {
            val value = utf8.groupValues[1].trim().trim('"', '\'')
            return sanitize(decode(value))
        }
        val plain = Regex("filename=\"?([^\";]+)\"?").find(disposition)
        return sanitize(plain?.groupValues?.get(1)?.trim()?.trim('"', '\'') ?: "")
    }

    /** Devuelve [name] o "nombre (1).ext", "nombre (2).ext"… si ya existe (o existe su parcial). */
    fun unique(dir: File, name: String): String {
        fun taken(n: String) = File(dir, n).exists() || File(dir, "$n.tdpart").exists()
        if (!taken(name)) return name

        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (i in 1..999) {
            val candidate = "$base ($i)$ext"
            if (!taken(candidate)) return candidate
        }
        return "${base}_${System.currentTimeMillis()}$ext"
    }

    private fun decode(s: String): String = try {
        // '+' en un path es un '+' literal, no un espacio.
        URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
    } catch (_: Exception) {
        s
    }

    private fun limitBytes(name: String): String {
        if (name.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) return name
        val dot = name.lastIndexOf('.')
        val ext = if (dot > 0 && name.length - dot <= 11) name.substring(dot) else ""
        val base = if (ext.isEmpty()) name else name.substring(0, dot)
        val budget = MAX_BYTES - ext.toByteArray(Charsets.UTF_8).size
        val sb = StringBuilder()
        var bytes = 0
        var i = 0
        while (i < base.length) {
            val cp = base.codePointAt(i)
            val chars = Character.toChars(cp)
            val size = String(chars).toByteArray(Charsets.UTF_8).size
            if (bytes + size > budget) break
            sb.append(chars)
            bytes += size
            i += chars.size
        }
        return sb.toString() + ext
    }
}
