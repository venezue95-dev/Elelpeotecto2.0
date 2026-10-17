package cu.todus.download

import cu.todus.download.data.StoragePaths
import cu.todus.download.data.TodusConfig
import cu.todus.download.data.TodusUrl
import cu.todus.download.engine.DownloadErrors
import cu.todus.download.engine.FileNames
import cu.todus.download.engine.HttpStatusException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.ConnectException

class CoreLogicTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ── Nombres de archivo ──

    @Test
    fun sanitize_removesPathTraversal() {
        assertEquals("passwd", FileNames.sanitize("../../etc/passwd"))
        assertEquals("", FileNames.sanitize(".."))
        assertEquals("a_b_c_.mp4", FileNames.sanitize("a:b*c?.mp4"))
        assertEquals("video.mp4", FileNames.sanitize("  video.mp4  "))
    }

    @Test
    fun sanitize_limitsLengthKeepingExtension() {
        val result = FileNames.sanitize("a".repeat(400) + ".mp4")
        assertTrue(result.toByteArray().size <= 200)
        assertTrue(result.endsWith(".mp4"))
    }

    @Test
    fun fromUrl_decodesAndStripsQuery() {
        assertEquals("mi video.mp4", FileNames.fromUrl("https://s3.todus.cu/a/mi%20video.mp4?token=1#x"))
        assertEquals("", FileNames.fromUrl("https://s3.todus.cu/"))
    }

    @Test
    fun fromDisposition_parsesBothForms() {
        assertEquals("v.mp4", FileNames.fromDisposition("attachment; filename=\"v.mp4\""))
        assertEquals("ñandú.mp4", FileNames.fromDisposition("attachment; filename*=UTF-8''%C3%B1and%C3%BA.mp4"))
        assertEquals("evil.sh", FileNames.fromDisposition("attachment; filename=\"../../evil.sh\""))
    }

    @Test
    fun unique_addsCounterWhenTaken() {
        val dir = tmp.newFolder()
        assertEquals("a.mp4", FileNames.unique(dir, "a.mp4"))
        File(dir, "a.mp4").writeText("x")
        assertEquals("a (1).mp4", FileNames.unique(dir, "a.mp4"))
        File(dir, "a (1).mp4.tdpart").writeText("x")
        assertEquals("a (2).mp4", FileNames.unique(dir, "a.mp4"))
    }

    // ── Enlaces ──

    @Test
    fun url_acceptsTodusLinks() {
        assertTrue(TodusUrl.parse("https://s3.todus.cu/x/y.mp4?a=1") is TodusUrl.Result.Ok)
        assertTrue(TodusUrl.parse("s3.todus.cu/x/y.mp4") is TodusUrl.Result.Ok)
        val shared = TodusUrl.parse("Mira esto https://s3.todus.cu/x/y.mp4.") as TodusUrl.Result.Ok
        assertEquals("https://s3.todus.cu/x/y.mp4", shared.url)
    }

    @Test
    fun url_rejectsOtherHosts() {
        assertTrue(TodusUrl.parse("https://evil.com/x") is TodusUrl.Result.Invalid)
        assertTrue(TodusUrl.parse("https://s3.todus.cu@evil.com/x") is TodusUrl.Result.Invalid)
        assertTrue(TodusUrl.parse("https://s3.todus.cu.evil.com/x") is TodusUrl.Result.Invalid)
        assertTrue(TodusUrl.parse("https://evil.com/?u=s3.todus.cu") is TodusUrl.Result.Invalid)
        assertTrue(TodusUrl.parse("ftp://s3.todus.cu/x") is TodusUrl.Result.Invalid)
        assertTrue(TodusUrl.parse("   ") is TodusUrl.Result.Invalid)
    }

    // ── Errores ──

    @Test
    fun errors_neverLeakAddresses() {
        val msg = DownloadErrors.friendlyMessage(ConnectException("failed to connect to /10.1.2.3 (port 443)"))
        assertFalse(msg.contains("10.1.2.3"))
        assertFalse(msg.contains("443"))
    }

    @Test
    fun errors_retryRules() {
        assertTrue(DownloadErrors.isRetriable(HttpStatusException(503)))
        assertFalse(DownloadErrors.isRetriable(HttpStatusException(403)))
        assertTrue(DownloadErrors.isRetriable(java.io.IOException("reset")))
        assertFalse(DownloadErrors.isRetriable(java.io.IOException("write failed: ENOSPC (No space left on device)")))
    }

    // ── Varios enlaces a la vez ──

    @Test
    fun parseAll_findsEveryLinkAndDropsDuplicates() {
        val text = """
            Mira esto: https://s3.todus.cu/a/uno.mp4?x=1
            https://s3.todus.cu/a/dos.mp4,
            https://s3.todus.cu/a/uno.mp4?x=1
            https://ejemplo.com/otro.mp4
        """.trimIndent()
        val results = TodusUrl.parseAll(text)
        val ok = results.filterIsInstance<TodusUrl.Result.Ok>().map { it.url }
        assertEquals(listOf("https://s3.todus.cu/a/uno.mp4?x=1", "https://s3.todus.cu/a/dos.mp4"), ok)
        assertEquals(1, results.count { it is TodusUrl.Result.Invalid })
    }

    @Test
    fun parseAll_acceptsLinksWithoutScheme_andIgnoresBlank() {
        assertTrue(TodusUrl.parseAll("   \n ").isEmpty())
        val results = TodusUrl.parseAll("s3.todus.cu/a/uno.mp4\ns3.todus.cu/a/dos.mp4")
        assertEquals(2, results.filterIsInstance<TodusUrl.Result.Ok>().size)
    }

    // ── Carpeta elegida ──

    @Test
    fun documentIdToPath_primaryAndSdCard() {
        val primary = "/storage/emulated/0"
        val docs = "/storage/emulated/0/Documents"
        assertEquals("/storage/emulated/0/Download/Pelis", StoragePaths.documentIdToPath("primary:Download/Pelis", primary, docs))
        assertEquals("/storage/emulated/0", StoragePaths.documentIdToPath("primary:", primary, docs))
        assertEquals("/storage/1A2B-3C4D/Videos", StoragePaths.documentIdToPath("1A2B-3C4D:Videos", primary, docs))
        assertEquals("/storage/emulated/0/Documents/x", StoragePaths.documentIdToPath("home:x", primary, docs))
    }

    @Test
    fun display_shortensKnownRoots() {
        val primary = "/storage/emulated/0"
        assertEquals("Almacenamiento/Download/toDus Download", StoragePaths.display("/storage/emulated/0/Download/toDus Download", primary))
        assertEquals("Almacenamiento interno", StoragePaths.display("/storage/emulated/0/", primary))
        assertEquals("Tarjeta SD/Videos", StoragePaths.display("/storage/1A2B-3C4D/Videos", primary))
    }

    // ── Ofuscación ──

    @Test
    fun config_decodesObfuscatedValues() {
        assertEquals("s3.todus.cu", TodusConfig.SNI_HOST)
        assertEquals("toDus Download", TodusConfig.USER_AGENT)
        assertEquals("152.207.206.86", TodusConfig.NATIONAL_IP)
        assertTrue(TodusConfig.isTodusHost("s3.todus.cu"))
    }
}
