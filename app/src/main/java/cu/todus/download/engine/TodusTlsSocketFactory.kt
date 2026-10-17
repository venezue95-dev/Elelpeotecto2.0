package cu.todus.download.engine

import cu.todus.download.data.TodusConfig
import android.util.Base64
import java.net.InetAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Socket factory que fuerza el SNI configurado en [TodusConfig] en el handshake TLS.
 * Solo se usa para hosts de toDus: [TodusNetworkClient] rechaza cualquier otro dominio.
 */
internal class TodusTlsSocketFactory(
    private val sniHost: String = TodusConfig.SNI_HOST
) : SSLSocketFactory() {

    val trustManager: X509TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {
            val pins = TodusConfig.PIN_SHA256
            if (pins.isEmpty()) return // sin huellas configuradas: se acepta (comportamiento anterior)
            val leaf = chain?.firstOrNull() ?: throw CertificateException("Sin certificado")
            val digest = MessageDigest.getInstance("SHA-256").digest(leaf.publicKey.encoded)
            val fingerprint = Base64.encodeToString(digest, Base64.NO_WRAP)
            if (fingerprint !in pins) throw CertificateException("Certificado no reconocido")
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val delegate: SSLSocketFactory

    init {
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, arrayOf(trustManager), SecureRandom())
        delegate = sc.socketFactory
    }

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(): Socket = patchSocket(delegate.createSocket())

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket =
        patchSocket(delegate.createSocket(s, sniHost, port, autoClose))

    override fun createSocket(host: String, port: Int): Socket =
        patchSocket(delegate.createSocket(host, port))

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        patchSocket(delegate.createSocket(host, port, localHost, localPort))

    override fun createSocket(host: InetAddress, port: Int): Socket =
        patchSocket(delegate.createSocket(host, port))

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        patchSocket(delegate.createSocket(address, port, localAddress, localPort))

    private fun patchSocket(s: Socket): Socket {
        if (s is SSLSocket) {
            try {
                val params = s.sslParameters
                params.serverNames = listOf(SNIHostName(sniHost))
                s.sslParameters = params
            } catch (_: Exception) {
            }
        }
        return s
    }
}
