package cu.todus.download.engine

import cu.todus.download.data.TodusConfig
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

/**
 * Cliente HTTP único de la app. Se construye una sola vez y se reutiliza
 * (antes se creaba uno nuevo en cada llamada, con su propio pool de conexiones).
 *
 * La IP, el SNI y el User-Agent salen de [TodusConfig]; la interfaz no los expone.
 */
internal class TodusNetworkClient {

    val client: OkHttpClient by lazy { buildClient() }

    private fun buildClient(): OkHttpClient {
        val socketFactory = TodusTlsSocketFactory(TodusConfig.SNI_HOST)

        val dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                // Solo hosts de toDus. Cualquier otro dominio (por ejemplo, una redirección
                // externa) falla en vez de salir por la red normal gastando datos.
                if (!TodusConfig.isTodusHost(hostname)) throw UnknownHostException(hostname)
                return listOf(InetAddress.getByName(TodusConfig.NATIONAL_IP))
            }
        }

        val headers = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Host", TodusConfig.SNI_HOST)
                    .header("User-Agent", TodusConfig.USER_AGENT)
                    .header("Accept-Encoding", "identity")
                    .build()
            )
        }

        return OkHttpClient.Builder()
            .dns(dns)
            // Ignora el proxy configurado en el sistema (Wi-Fi) para que ninguna herramienta lo use para interceptar.
            .proxy(java.net.Proxy.NO_PROXY)
            .sslSocketFactory(socketFactory, socketFactory.trustManager)
            .hostnameVerifier(HostnameVerifier { host, _ -> TodusConfig.isTodusHost(host) })
            .addInterceptor(headers)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
