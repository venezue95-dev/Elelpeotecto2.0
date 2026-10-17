package cu.todus.download.data

/**
 * Evita que los parámetros de red aparezcan como texto legible en el APK (herramientas como
 * `strings`, buscadores de texto o decompiladores básicos). Es ofuscación, no cifrado fuerte:
 * quien ejecute la app con herramientas de depuración todavía puede observar los valores en memoria.
 */
internal object Obf {
    private val key = intArrayOf(35, 187, 144, 132, 170, 175, 106, 141, 76, 114, 45)

    fun decode(data: IntArray): String {
        val out = ByteArray(data.size) { i -> (data[i] xor key[i % key.size]).toByte() }
        return String(out, Charsets.UTF_8)
    }
}
