package cu.todus.download.data

/**
 * Parámetros de red fijos de la app.
 * No se muestran en ninguna pantalla ni se pueden cambiar desde la interfaz.
 */
internal object TodusConfig {
    // Valores ofuscados (ver [Obf]); en el APK no aparecen como texto.
    val NATIONAL_IP: String by lazy { Obf.decode(intArrayOf(18, 142, 162, 170, 152, 159, 93, 163, 126, 66, 27, 13, 131, 166)) }
    val SNI_HOST: String by lazy { Obf.decode(intArrayOf(80, 136, 190, 240, 197, 203, 31, 254, 98, 17, 88)) }
    val USER_AGENT: String by lazy { Obf.decode(intArrayOf(87, 212, 212, 241, 217, 143, 46, 226, 59, 28, 65, 76, 218, 244)) }

    /**
     * Huellas SHA-256 (en Base64) de la clave pública del certificado de toDus. Con al menos una,
     * la app SOLO acepta un servidor que presente esa clave, y ninguna herramienta que intercepte
     * el tráfico (HTTP Canary, Charles, mitmproxy…) puede leer la comunicación aunque instale su certificado.
     * Vacía = se acepta cualquier certificado (comportamiento anterior).
     * Cómo obtenerla: ver README_SEGURIDAD.md.
     */
    val PIN_SHA256: List<String> = emptyList()

    /** Bloquea las descargas mientras haya una VPN activa (HTTP Canary y similares funcionan como VPN local). */
    const val BLOCK_VPN = true

    /** true si [host] es todus.cu o cualquier subdominio suyo. */
    fun isTodusHost(host: String?): Boolean {
        val h = host?.trim()?.trimEnd('.')?.lowercase() ?: return false
        return h == "todus.cu" || h.endsWith(".todus.cu")
    }
}

/** Validación y normalización de los enlaces que pega o comparte el usuario. */
object TodusUrl {

    sealed interface Result {
        data class Ok(val url: String) : Result
        data class Invalid(val message: String) : Result
    }

    private val urlInText = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    private val authority = Regex("^(https?)://([^/?#]+)", RegexOption.IGNORE_CASE)

    fun parse(input: String): Result {
        var s = input.trim()
        if (s.isEmpty()) return Result.Invalid("Pega un enlace de toDus.")

        // Si el texto compartido trae más cosas, nos quedamos con el primer enlace.
        s = urlInText.find(s)?.value ?: s
        s = s.trimEnd('.', ',', ';', '!', '?', ')', ']', '}', '>', '"', '\'')
        if (!s.contains("://")) s = "https://$s"

        val match = authority.find(s) ?: return Result.Invalid("El enlace no es válido.")
        // Se descarta usuario@ y :puerto para comparar solo el host real.
        val host = match.groupValues[2].substringAfterLast('@').substringBefore(':')
        if (host.isBlank()) return Result.Invalid("El enlace no es válido.")
        if (!TodusConfig.isTodusHost(host)) return Result.Invalid("Solo se admiten enlaces de toDus.")

        // Se devuelve el texto original: una URL firmada no debe re-codificarse.
        return Result.Ok(s)
    }

    /**
     * Extrae todos los enlaces de un texto (uno por línea, separados por espacios, pegados desde
     * un chat…). Los repetidos se descartan; los inválidos se devuelven como [Result.Invalid].
     */
    fun parseAll(input: String): List<Result> {
        if (input.isBlank()) return emptyList()
        val candidates = urlInText.findAll(input).map { it.value }.toList()
            .ifEmpty { input.split(Regex("\\s+")).filter { it.isNotBlank() } }
        val seen = HashSet<String>()
        val out = ArrayList<Result>()
        for (candidate in candidates) {
            val result = parse(candidate)
            if (result is Result.Ok) {
                if (seen.add(result.url)) out.add(result)
            } else {
                out.add(result)
            }
        }
        return out
    }
}
