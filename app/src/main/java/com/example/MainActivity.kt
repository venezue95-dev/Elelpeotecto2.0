package com.example

import android.Manifest
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.todus.download.data.AppPreferences
import cu.todus.download.data.DownloadItem
import cu.todus.download.data.DownloadRepository
import cu.todus.download.data.EnqueueResult
import cu.todus.download.data.TodusUrl
import cu.todus.download.engine.DownloadEngine
import cu.todus.download.engine.FileNames
import cu.todus.download.service.DownloadService
import cu.todus.download.ui.components.AddUrlDialog
import cu.todus.download.ui.components.ColorPaletteDialog
import cu.todus.download.ui.components.ConfirmDialog
import cu.todus.download.ui.components.FloatingBottomBar
import cu.todus.download.ui.components.LocalToast
import cu.todus.download.ui.components.NavBarItem
import cu.todus.download.ui.components.ToastHost
import cu.todus.download.ui.components.ToastState
import cu.todus.download.ui.components.ToastType
import cu.todus.download.ui.icons.AppIcons
import cu.todus.download.ui.screens.DownloadsScreen
import cu.todus.download.ui.screens.HistoryScreen
import cu.todus.download.ui.screens.PermissionScreen
import cu.todus.download.ui.screens.SettingsScreen
import cu.todus.download.ui.theme.AppTheme
import cu.todus.download.ui.theme.TodusDownloadTheme
import cu.todus.download.util.AppPermissions
import cu.todus.download.util.FolderPaths
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.absoluteValue

enum class AppTab(val title: String, val icon: ImageVector) {
    DOWNLOADS("Descargas", AppIcons.Download),
    HISTORY("Historial", AppIcons.Library),
    SETTINGS("Ajustes", AppIcons.Sliders)
}

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    private lateinit var repository: DownloadRepository
    private lateinit var engine: DownloadEngine
    private val toasts = ToastState()

    private var sharedTextFromIntent by mutableStateOf<String?>(null)
    private var themeMode by mutableIntStateOf(AppPreferences.THEME_SYSTEM)
    private var accentIndex by mutableIntStateOf(AppPreferences.DEFAULT_ACCENT)
    private var downloadDir by mutableStateOf("")
    private var maxConcurrent by mutableIntStateOf(AppPreferences.DEFAULT_MAX_CONCURRENT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = AppPreferences(this)
        repository = DownloadService.getRepository(this)
        engine = DownloadService.getEngine(this)
        themeMode = preferences.themeMode
        accentIndex = preferences.accentIndex
        downloadDir = preferences.downloadDir
        maxConcurrent = preferences.maxConcurrent

        handleIncomingIntent(intent)

        setContent {
            val dark = when (themeMode) {
                AppPreferences.THEME_LIGHT -> false
                AppPreferences.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            // Iconos de las barras del sistema según el tema que se está viendo.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            TodusDownloadTheme(darkTheme = dark, accentIndex = accentIndex) {
                CompositionLocalProvider(LocalToast provides toasts) {
                    Box(Modifier.fillMaxSize().background(AppTheme.colors.bg)) {
                        AppRoot()
                        ToastHost(toasts, Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            // Se pasa el texto completo: puede traer varios enlaces.
            if (!text.isNullOrBlank()) sharedTextFromIntent = text.trim()
        }
    }

    // ───────────────────────── Acciones sobre archivos ─────────────────────────

    private fun fileUriAndMime(item: DownloadItem): Pair<Uri, String>? {
        val file = File(item.destinationPath)
        if (item.destinationPath.isBlank() || !file.exists()) {
            toasts.show("El archivo ya no existe en el almacenamiento", ToastType.ERROR)
            return null
        }
        return try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            uri to mime
        } catch (_: Exception) {
            toasts.show("No se puede acceder al archivo", ToastType.ERROR)
            null
        }
    }

    private fun openFile(item: DownloadItem) {
        val (uri, mime) = fileUriAndMime(item) ?: return
        try {
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(openIntent, "Abrir con"))
        } catch (_: Exception) {
            toasts.show("No hay aplicación compatible para abrir este archivo", ToastType.WARNING)
        }
    }

    private fun shareFile(item: DownloadItem) {
        val (uri, mime) = fileUriAndMime(item) ?: return
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir archivo"))
        } catch (_: Exception) {
            toasts.show("Error al compartir el archivo", ToastType.ERROR)
        }
    }

    private fun removeFromHistory(item: DownloadItem, deleteFile: Boolean) {
        if (deleteFile && item.destinationPath.isNotBlank()) {
            try { File(item.destinationPath).delete() } catch (_: Exception) {}
            try {
                MediaScannerConnection.scanFile(this, arrayOf(item.destinationPath), null, null)
            } catch (_: Exception) {}
        }
        repository.removeItem(item.id)
        toasts.show(if (deleteFile) "Archivo eliminado" else "Quitado del historial", ToastType.SUCCESS)
    }

    // ───────────────────────── Descargas ─────────────────────────

    /** Añade todos los enlaces de toDus que haya en [rawText]. */
    private fun enqueueMany(rawText: String, customName: String, threads: Int): EnqueueResult {
        val results = TodusUrl.parseAll(rawText)
        if (results.isEmpty()) return EnqueueResult(0, 0, 0, "Pega al menos un enlace de toDus.")

        val valid = results.filterIsInstance<TodusUrl.Result.Ok>()
        val invalid = results.size - valid.size
        if (valid.isEmpty()) {
            val message = (results.first() as TodusUrl.Result.Invalid).message
            return EnqueueResult(0, 0, invalid, message)
        }

        val existing = repository.downloads.value
            .filter { it.status != DownloadItem.Status.COMPLETED && it.status != DownloadItem.Status.CANCELLED }
            .map { it.url }
            .toHashSet()

        // El nombre personalizado solo se aplica cuando hay un único enlace.
        val custom = if (valid.size == 1) FileNames.sanitize(customName) else ""
        var added = 0
        var duplicates = 0
        for (ok in valid) {
            if (ok.url in existing) {
                duplicates++
                continue
            }
            val item = DownloadItem(
                id = UUID.randomUUID().toString(),
                url = ok.url,
                name = custom.ifBlank { FileNames.fromUrl(ok.url).ifBlank { "todus_${System.currentTimeMillis()}.bin" } },
                nameIsCustom = custom.isNotBlank(),
                threads = threads,
                status = DownloadItem.Status.QUEUED
            )
            repository.addItem(item)
            DownloadService.requestStart(this, item.id)
            existing.add(ok.url)
            added++
        }

        val error = if (added == 0) "Estos enlaces ya están en la cola." else null
        return EnqueueResult(added, duplicates, invalid, error)
    }

    private fun announceEnqueue(result: EnqueueResult) {
        if (result.added <= 0) return
        val base = if (result.added == 1) "1 descarga añadida" else "${result.added} descargas añadidas"
        val extras = buildList {
            if (result.duplicates > 0) add("${result.duplicates} repetida${if (result.duplicates > 1) "s" else ""}")
            if (result.invalid > 0) add("${result.invalid} no válida${if (result.invalid > 1) "s" else ""}")
        }
        val type = if (extras.isEmpty()) ToastType.SUCCESS else ToastType.WARNING
        toasts.show(if (extras.isEmpty()) base else "$base · ${extras.joinToString(", ")}", type)
    }

    private fun toggleDownload(item: DownloadItem) {
        when (item.status) {
            DownloadItem.Status.DOWNLOADING, DownloadItem.Status.CONNECTING, DownloadItem.Status.QUEUED ->
                engine.pauseDownload(item.id)
            else -> DownloadService.requestStart(this, item.id)
        }
    }

    private fun resumeAll() {
        val stopped = repository.downloads.value.filter {
            it.status == DownloadItem.Status.PAUSED || it.status == DownloadItem.Status.ERROR
        }
        stopped.forEach { DownloadService.requestStart(this, it.id) }
        if (stopped.isNotEmpty()) toasts.show("Reanudando ${stopped.size} descarga${if (stopped.size > 1) "s" else ""}", ToastType.INFO)
    }

    // ───────────────────────── Carpeta y permisos ─────────────────────────

    private fun onFolderPicked(uri: Uri?) {
        if (uri == null) return
        val path = FolderPaths.fromTreeUri(uri)
        if (path == null) {
            toasts.show("Esa carpeta no es compatible. Elige una del almacenamiento del teléfono.", ToastType.ERROR)
            return
        }
        val folder = File(path)
        val writable = try {
            if (!folder.exists()) folder.mkdirs()
            folder.isDirectory && folder.canWrite()
        } catch (_: SecurityException) {
            false
        }
        if (!writable) {
            toasts.show("No se puede escribir ahí. Concede el acceso al almacenamiento en Ajustes.", ToastType.ERROR)
            return
        }
        preferences.downloadDir = path
        downloadDir = path
        toasts.show("Carpeta guardada: ${FolderPaths.display(path)}", ToastType.SUCCESS)
    }

    private fun openStorageSettings() {
        for (intent in AppPermissions.allFilesAccessIntents(this)) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }

    // ───────────────────────── Interfaz ─────────────────────────

    @Composable
    private fun AppRoot() {
        var storageOk by remember { mutableStateOf(AppPermissions.hasStorage(this)) }
        var notificationsOk by remember { mutableStateOf(AppPermissions.hasNotifications(this)) }
        var skipped by rememberSaveable { mutableStateOf(false) }
        var notificationAsks by rememberSaveable { mutableIntStateOf(0) }
        var gateWasShown by rememberSaveable { mutableStateOf(false) }

        // Al volver de Ajustes se comprueba de nuevo.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            storageOk = AppPermissions.hasStorage(this@MainActivity)
            notificationsOk = AppPermissions.hasNotifications(this@MainActivity)
        }

        val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            storageOk = AppPermissions.hasStorage(this@MainActivity)
        }
        val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationsOk = AppPermissions.hasNotifications(this@MainActivity)
        }

        val requestStorage: () -> Unit = {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> openStorageSettings()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else -> Unit
            }
        }
        val requestNotifications: () -> Unit = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Tras dos rechazos el sistema ya no muestra el diálogo: se lleva al usuario a Ajustes.
                if (notificationAsks >= 2) {
                    try { startActivity(AppPermissions.notificationSettingsIntent(this@MainActivity)) } catch (_: Exception) {}
                } else {
                    notificationAsks++
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        val allGranted = storageOk && notificationsOk
        val showGate = !allGranted && !skipped
        if (showGate) gateWasShown = true

        LaunchedEffect(allGranted) {
            if (allGranted && gateWasShown) {
                toasts.show("Permisos concedidos", ToastType.SUCCESS)
                gateWasShown = false
            }
        }

        Crossfade(targetState = showGate, animationSpec = tween(350), label = "gate") { gate ->
            if (gate) {
                PermissionScreen(
                    storageGranted = storageOk,
                    notificationsGranted = notificationsOk,
                    onRequestStorage = requestStorage,
                    onRequestNotifications = requestNotifications,
                    onSkip = { skipped = true }
                )
            } else {
                MainContent(storageOk = storageOk, onFixStorage = requestStorage)
            }
        }
    }

    @Composable
    private fun MainContent(storageOk: Boolean, onFixStorage: () -> Unit) {
        val c = AppTheme.colors
        val pagerState = rememberPagerState(initialPage = 0) { AppTab.entries.size }
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current

        var showAddDialog by rememberSaveable { mutableStateOf(false) }
        var initialDialogText by rememberSaveable { mutableStateOf("") }
        var showPalette by rememberSaveable { mutableStateOf(false) }
        var pendingCancel by remember { mutableStateOf<DownloadItem?>(null) }
        var pendingDelete by remember { mutableStateOf<DownloadItem?>(null) }
        var confirmClearAll by remember { mutableStateOf(false) }

        val downloads by repository.downloads.collectAsStateWithLifecycle()
        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { onFolderPicked(it) }

        // Texto recibido desde otra app (Telegram, navegador…)
        LaunchedEffect(sharedTextFromIntent) {
            sharedTextFromIntent?.let {
                initialDialogText = it
                showAddDialog = true
                sharedTextFromIntent = null
            }
        }

        // Atrás desde otra pestaña vuelve a Descargas.
        BackHandler(enabled = pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        }

        Scaffold(
            containerColor = c.bg,
            bottomBar = {
                FloatingBottomBar(
                    items = AppTab.entries.map { NavBarItem(it.title, it.icon) },
                    position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                    onSelected = { index ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) { page ->
                // La página que se aleja se encoge y se desvanece un poco.
                val pageModifier = Modifier.graphicsLayer {
                    val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .absoluteValue.coerceIn(0f, 1f)
                    alpha = 1f - offset * 0.5f
                    val scale = 1f - offset * 0.06f
                    scaleX = scale
                    scaleY = scale
                }
                when (AppTab.entries[page]) {
                    AppTab.DOWNLOADS -> DownloadsScreen(
                        downloads = downloads,
                        maxConcurrent = maxConcurrent,
                        onOpenAddDialog = {
                            initialDialogText = ""
                            showAddDialog = true
                        },
                        onPauseResume = { toggleDownload(it) },
                        onCancel = { pendingCancel = it },
                        onOpen = { openFile(it) },
                        onShare = { shareFile(it) },
                        onPauseAll = { engine.pauseAll() },
                        onResumeAll = { resumeAll() },
                        modifier = pageModifier
                    )

                    AppTab.HISTORY -> HistoryScreen(
                        downloads = downloads,
                        onOpen = { openFile(it) },
                        onShare = { shareFile(it) },
                        onDelete = { pendingDelete = it },
                        onClearAll = { confirmClearAll = true },
                        modifier = pageModifier
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        preferences = preferences,
                        onMaxConcurrentChange = { maxConcurrent = it },
                        themeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            preferences.themeMode = it
                        },
                        accentIndex = accentIndex,
                        onChooseAccent = { showPalette = true },
                        downloadDirText = FolderPaths.display(downloadDir),
                        isDefaultDir = downloadDir == AppPreferences.defaultDownloadDir(),
                        onPickFolder = { folderPicker.launch(null) },
                        onResetFolder = {
                            preferences.resetDownloadDir()
                            downloadDir = preferences.downloadDir
                            toasts.show("Carpeta por defecto restaurada", ToastType.INFO)
                        },
                        storageGranted = storageOk,
                        onFixStorage = onFixStorage,
                        versionName = BuildConfig.VERSION_NAME,
                        modifier = pageModifier
                    )
                }
            }
        }

        if (showAddDialog) {
            AddUrlDialog(
                initialText = initialDialogText,
                defaultThreads = preferences.defaultThreads,
                maxConcurrent = maxConcurrent,
                onDismiss = { showAddDialog = false },
                onConfirm = { text, name, threads ->
                    val result = enqueueMany(text, name, threads)
                    if (result.added > 0) {
                        announceEnqueue(result)
                        scope.launch { pagerState.animateScrollToPage(AppTab.DOWNLOADS.ordinal) }
                    }
                    result
                }
            )
        }

        if (showPalette) {
            ColorPaletteDialog(
                selectedIndex = accentIndex,
                onSelect = {
                    accentIndex = it
                    preferences.accentIndex = it
                },
                onDismiss = { showPalette = false }
            )
        }

        pendingCancel?.let { item ->
            ConfirmDialog(
                title = "¿Cancelar descarga?",
                message = "Se quitará \"${item.name}\" de la cola y se borrará lo descargado hasta ahora.",
                confirmLabel = "Cancelar descarga",
                onConfirm = {
                    engine.cancelDownload(item.id)
                    pendingCancel = null
                    toasts.show("Descarga cancelada", ToastType.INFO)
                },
                dismissLabel = "Seguir",
                onDismissClick = { pendingCancel = null },
                onDismissRequest = { pendingCancel = null },
                destructive = true
            )
        }

        pendingDelete?.let { item ->
            ConfirmDialog(
                title = "Eliminar del historial",
                message = "\"${item.name}\"\n\n¿Quieres borrar también el archivo del teléfono?",
                confirmLabel = "Eliminar archivo",
                onConfirm = {
                    removeFromHistory(item, deleteFile = true)
                    pendingDelete = null
                },
                dismissLabel = "Solo quitar de la lista",
                onDismissClick = {
                    removeFromHistory(item, deleteFile = false)
                    pendingDelete = null
                },
                onDismissRequest = { pendingDelete = null },
                destructive = true
            )
        }

        if (confirmClearAll) {
            ConfirmDialog(
                title = "¿Limpiar el historial?",
                message = "Se quitarán todas las entradas. Los archivos descargados no se borrarán.",
                confirmLabel = "Limpiar",
                onConfirm = {
                    repository.clearCompleted()
                    confirmClearAll = false
                    toasts.show("Historial limpio", ToastType.SUCCESS)
                },
                dismissLabel = "Cancelar",
                onDismissClick = { confirmClearAll = false },
                onDismissRequest = { confirmClearAll = false }
            )
        }
    }
}
