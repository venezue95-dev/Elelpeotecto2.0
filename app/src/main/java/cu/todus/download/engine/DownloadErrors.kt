package cu.todus.download.engine

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** El servidor respondió con un código HTTP inesperado. */
class HttpStatusException(val code: Int) : IOException("HTTP $code")

/** El servidor ignoró la cabecera Range (respondió 200 en vez de 206). */
class RangesNotSupportedException : IOException("Ranges not supported")

/** El enlace no pertenece a toDus. */
class NotTodusLinkException : IOException("Not a toDus link")

/** Las descargas no se permiten en este estado de la red (por ejemplo, con una VPN activa). */
class SecurityBlockedException : IOException("Blocked")

object DownloadErrors {

    /**
     * Mensaje seguro para mostrar al usuario.
     * Nunca incluye e.message: las excepciones de red suelen traer la IP y el puerto.
     */
    fun friendlyMessage(e: Throwable): String = when (e) {
        is NotTodusLinkException -> "Solo se admiten enlaces de toDus."
        is SecurityBlockedException -> "Desactiva la VPN para poder descargar."
        is HttpStatusException -> when (e.code) {
            401, 403 -> "El enlace expiró o no tiene permiso de acceso."
            404, 410 -> "El archivo ya no está disponible."
            429 -> "Demasiadas solicitudes. Inténtalo más tarde."
            in 500..599 -> "El servidor no responde (error ${e.code})."
            else -> "El servidor rechazó la solicitud (error ${e.code})."
        }
        is IllegalArgumentException -> "El enlace no es válido."
        is UnknownHostException -> "No hay conexión con el servidor. Revisa tu red."
        is SocketTimeoutException -> "Se agotó el tiempo de espera. Revisa tu señal."
        is ConnectException, is NoRouteToHostException -> "No se pudo conectar. Revisa tu red."
        is SSLException -> "Error de conexión segura."
        is IOException -> if (isNoSpace(e)) "Sin espacio en el almacenamiento." else "Conexión interrumpida."
        else -> "Error inesperado en la descarga."
    }

    /** true si tiene sentido reintentar automáticamente. */
    fun isRetriable(e: Throwable): Boolean = when (e) {
        is NotTodusLinkException, is SecurityBlockedException, is IllegalArgumentException -> false
        is HttpStatusException -> e.code == 408 || e.code == 429 || e.code >= 500
        is IOException -> !isNoSpace(e)
        else -> false
    }

    private fun isNoSpace(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.any {
            val m = it.message
            m != null && (m.contains("ENOSPC") || m.contains("No space left", ignoreCase = true))
        }
}
