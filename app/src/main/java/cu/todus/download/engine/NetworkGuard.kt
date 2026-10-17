package cu.todus.download.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import cu.todus.download.data.TodusConfig
import java.net.NetworkInterface

/** Comprobaciones previas a descargar. */
internal object NetworkGuard {

    /** Lanza [SecurityBlockedException] si hay una VPN activa (cuando [TodusConfig.BLOCK_VPN] es true). */
    fun check(context: Context) {
        if (TodusConfig.BLOCK_VPN && isVpnActive(context)) throw SecurityBlockedException()
    }

    fun isVpnActive(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                // Cualquier red del sistema que use transporte VPN.
                @Suppress("DEPRECATION")
                val networks = cm.allNetworks
                for (network in networks) {
                    val caps = cm.getNetworkCapabilities(network) ?: continue
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
                }
            }
        } catch (_: Exception) {
        }
        // Respaldo: interfaces de túnel activas.
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            for (iface in interfaces) {
                val name = iface.name.lowercase()
                val tunnel = name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("tap") ||
                    name.startsWith("wg") || name.startsWith("ipsec")
                if (tunnel && iface.isUp) return true
            }
        } catch (_: Exception) {
        }
        return false
    }
}
