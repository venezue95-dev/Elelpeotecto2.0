package cu.todus.download.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import cu.todus.download.data.StoragePaths

/** Permisos de la app y ayudas para pedirlos en cualquier versión de Android. */
object AppPermissions {

    /** Android 11+: acceso a todos los archivos. Android 6-10: WRITE_EXTERNAL_STORAGE. Antes: no hace falta. */
    fun hasStorage(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        else -> true
    }

    /** Android 13+: POST_NOTIFICATIONS. Antes se considera concedido. */
    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /** Pantalla de ajustes "Acceso a todos los archivos" de esta app (Android 11+). */
    fun allFilesAccessIntents(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        appDetailsIntent(context)
    )

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}

object FolderPaths {
    /** Convierte la carpeta elegida con el selector del sistema en una ruta real. */
    fun fromTreeUri(uri: Uri): String? {
        val docId = try {
            DocumentsContract.getTreeDocumentId(uri)
        } catch (_: Exception) {
            return null
        }
        return StoragePaths.documentIdToPath(
            docId,
            primaryRoot = Environment.getExternalStorageDirectory().absolutePath,
            documentsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        )
    }

    fun display(path: String): String =
        StoragePaths.display(path, Environment.getExternalStorageDirectory().absolutePath)
}
