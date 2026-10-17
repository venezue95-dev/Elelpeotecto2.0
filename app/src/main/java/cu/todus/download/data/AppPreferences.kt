package cu.todus.download.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.File

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("todus_download_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_THREADS = 4
        const val DEFAULT_FOLDER = "toDus Download"

        const val DEFAULT_MAX_CONCURRENT = 3
        const val MAX_CONCURRENT_LIMIT = 5

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        /** Posición del cian en la paleta de 100 colores. */
        const val DEFAULT_ACCENT = 5
        const val ACCENT_COUNT = 100

        fun defaultDownloadDir(): String =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_FOLDER).absolutePath
    }

    init {
        migrateLegacy()
    }

    private fun migrateLegacy() {
        val editor = prefs.edit()
        var changed = false

        // Valores que versiones anteriores guardaban en el teléfono.
        for (key in listOf("national_ip", "sni_host", "user_agent", "dynamic_colors")) {
            if (prefs.contains(key)) { editor.remove(key); changed = true }
        }
        // El tema AMOLED desaparece: quien lo usaba pasa a tema oscuro.
        if (prefs.contains("amoled_theme")) {
            if (prefs.getBoolean("amoled_theme", false) && !prefs.contains("theme_mode")) {
                editor.putInt("theme_mode", THEME_DARK)
            }
            editor.remove("amoled_theme"); changed = true
        }
        // La subcarpeta se convierte en una ruta completa elegible.
        if (prefs.contains("sub_folder")) {
            if (!prefs.contains("download_dir")) {
                val sub = prefs.getString("sub_folder", DEFAULT_FOLDER).orEmpty().ifBlank { DEFAULT_FOLDER }
                val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), sub).absolutePath
                editor.putString("download_dir", path)
            }
            editor.remove("sub_folder"); changed = true
        }
        if (changed) editor.apply()
    }

    var defaultThreads: Int
        get() = prefs.getInt("default_threads", DEFAULT_THREADS).coerceIn(1, 16)
        set(value) = prefs.edit().putInt("default_threads", value.coerceIn(1, 16)).apply()

    /** Cuántas descargas corren a la vez (1..5). Las demás esperan en cola. */
    var maxConcurrent: Int
        get() = prefs.getInt("max_concurrent", DEFAULT_MAX_CONCURRENT).coerceIn(1, MAX_CONCURRENT_LIMIT)
        set(value) = prefs.edit().putInt("max_concurrent", value.coerceIn(1, MAX_CONCURRENT_LIMIT)).apply()

    /** Ruta absoluta de la carpeta de destino. */
    var downloadDir: String
        get() = prefs.getString("download_dir", null)?.takeIf { it.isNotBlank() } ?: defaultDownloadDir()
        set(value) = prefs.edit().putString("download_dir", value).apply()

    fun resetDownloadDir() = prefs.edit().remove("download_dir").apply()

    var autoResume: Boolean
        get() = prefs.getBoolean("auto_resume", true)
        set(value) = prefs.edit().putBoolean("auto_resume", value).apply()

    var showNotifications: Boolean
        get() = prefs.getBoolean("show_notifications", true)
        set(value) = prefs.edit().putBoolean("show_notifications", value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", THEME_SYSTEM).coerceIn(THEME_SYSTEM, THEME_DARK)
        set(value) = prefs.edit().putInt("theme_mode", value.coerceIn(THEME_SYSTEM, THEME_DARK)).apply()

    var accentIndex: Int
        get() = prefs.getInt("accent_index", DEFAULT_ACCENT).coerceIn(0, ACCENT_COUNT - 1)
        set(value) = prefs.edit().putInt("accent_index", value.coerceIn(0, ACCENT_COUNT - 1)).apply()
}
