package cu.todus.download.data

import java.io.File

/** Lógica pura de rutas de almacenamiento (sin dependencias de Android, se puede probar en la JVM). */
object StoragePaths {

    /**
     * Convierte el identificador de documento de un árbol SAF ("primary:Download/Pelis",
     * "1A2B-3C4D:Videos") en una ruta de archivo real. Devuelve null si el volumen no es reconocible.
     */
    fun documentIdToPath(docId: String, primaryRoot: String, documentsRoot: String): String? {
        val parts = docId.split(":", limit = 2)
        val volume = parts[0]
        val relative = parts.getOrElse(1) { "" }.trim('/')
        val base = when {
            volume.equals("primary", ignoreCase = true) -> primaryRoot
            volume.equals("home", ignoreCase = true) -> documentsRoot
            volume.isNotEmpty() && volume.all { it.isLetterOrDigit() || it == '-' } -> "/storage/$volume"
            else -> return null
        }
        return if (relative.isEmpty()) File(base).path else File(base, relative).path
    }

    /** Texto corto para mostrar una ruta: "Almacenamiento/Download/toDus Download". */
    fun display(path: String, primaryRoot: String): String {
        val clean = path.trimEnd('/')
        return when {
            clean == primaryRoot -> "Almacenamiento interno"
            clean.startsWith("$primaryRoot/") -> "Almacenamiento/" + clean.removePrefix("$primaryRoot/")
            clean.startsWith("/storage/") -> "Tarjeta SD/" + clean.removePrefix("/storage/").substringAfter('/', "")
            else -> clean
        }
    }
}
