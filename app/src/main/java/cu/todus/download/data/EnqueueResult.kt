package cu.todus.download.data

/** Resultado de añadir uno o varios enlaces a la cola. [error] es null si se añadió al menos uno. */
data class EnqueueResult(
    val added: Int,
    val duplicates: Int,
    val invalid: Int,
    val error: String? = null
)
