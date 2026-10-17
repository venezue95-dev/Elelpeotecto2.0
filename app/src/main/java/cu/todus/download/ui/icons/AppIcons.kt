package cu.todus.download.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Juego de íconos propio de la app: trazo redondeado de 1.9 en una cuadrícula de 24.
 * Se tiñen con el parámetro `tint` de `Icon`, así que siguen el tema y el color de énfasis.
 */
object AppIcons {
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    val Download by lazy { icon("Download", "M12 4v11", "M7 11l5 5 5-5", "M5 20h14") }
    val Library by lazy { icon("Library", "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val Sliders by lazy { icon("Sliders", "M4 7h9", "M17 7h3", "M4 17h3", "M11 17h9", "M13 4v6", "M7 14v6") }
    val Plus by lazy { icon("Plus", "M12 5v14", "M5 12h14") }
    val Pause by lazy { icon("Pause", "M8 5v14", "M16 5v14") }
    val Play by lazy { icon("Play", "M7 4.5v15l12-7.5z") }
    val Close by lazy { icon("Close", "M6 6l12 12", "M18 6L6 18") }
    val Retry by lazy { icon("Retry", "M20 12a8 8 0 1 1-2.4-5.7", "M20 4v5h-5") }
    val Check by lazy { icon("Check", "M5 12.5l4.5 4.5L19 7.5") }
    val Trash by lazy { icon("Trash", "M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13", "M10 11v5", "M14 11v5") }
    val Share by lazy { icon("Share", "M12 15V4", "M8 8l4-4 4 4", "M5 13v6a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-6") }
    val Open by lazy { icon("Open", "M14 4h6v6", "M20 4l-9 9", "M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5") }
    val Link by lazy {
        icon(
            "Link",
            "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1",
            "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"
        )
    }
    val Clipboard by lazy {
        icon("Clipboard", "M9 4h6v3H9z", "M7 5.5H6a1 1 0 0 0-1 1V20a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V6.5a1 1 0 0 0-1-1h-1")
    }
    val Palette by lazy {
        icon(
            "Palette",
            "M12 3a9 9 0 1 0 0 18c1.2 0 2-.8 2-1.8 0-.5-.2-.9-.5-1.2-.3-.3-.5-.7-.5-1.2 0-1 .8-1.8 1.8-1.8H17a4 4 0 0 0 4-4C21 6.4 17 3 12 3z",
            "M7.5 11h.01", "M10 7.5h.01", "M14.5 7.5h.01"
        )
    }
    val Bell by lazy { icon("Bell", "M6 17V11a6 6 0 1 1 12 0v6l2 2H4z", "M10 21h4") }
    val Sun by lazy {
        icon(
            "Sun",
            "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z",
            "M12 2v2", "M12 20v2", "M4.9 4.9l1.4 1.4", "M17.7 17.7l1.4 1.4",
            "M2 12h2", "M20 12h2", "M4.9 19.1l1.4-1.4", "M17.7 6.3l1.4-1.4"
        )
    }
    val Moon by lazy { icon("Moon", "M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z") }
    val Phone by lazy { icon("Phone", "M7 3h10a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M11 18h2") }
    val Info by lazy { icon("Info", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", "M12 11v6", "M12 7.5h.01") }
    val Bolt by lazy { icon("Bolt", "M13 3L5 14h6l-1 7 8-11h-6z") }
    val Search by lazy { icon("Search", "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14z", "M20 20l-4-4") }
    val Queue by lazy { icon("Queue", "M8 6h12", "M8 12h12", "M8 18h12", "M4 6h.01", "M4 12h.01", "M4 18h.01") }
    val Speed by lazy { icon("Speed", "M3.5 17a9 9 0 1 1 17 0", "M12 15l4-5") }
    val Shield by lazy { icon("Shield", "M12 3l8 3v6c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V6z", "M8.5 12l2.5 2.5 4.5-5") }
    val Warning by lazy { icon("Warning", "M12 4l9 16H3z", "M12 10v4", "M12 17h.01") }
    val Film by lazy {
        icon("Film", "M4 5h16v14H4z", "M4 9h3", "M4 15h3", "M17 9h3", "M17 15h3", "M7 5v14", "M17 5v14")
    }
    val Music by lazy {
        icon("Music", "M9 18V6l10-2v12", "M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0z", "M19 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z")
    }
    val Archive by lazy { icon("Archive", "M4 5h16v4H4z", "M5 9v10h14V9", "M10 13h4") }
    val File by lazy { icon("File", "M6 3h8l5 5v13H6z", "M14 3v5h5") }
    val Chevron by lazy { icon("Chevron", "M9 6l6 6-6 6") }
    val Threads by lazy { icon("Threads", "M3 12h4l3-8 4 16 3-8h4") }
    val FolderOpen by lazy {
        icon("FolderOpen", "M3 8V6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v2", "M3 8h18l-2 10a2 2 0 0 1-2 1.6H5A2 2 0 0 1 3 18z")
    }
    val Sweep by lazy { icon("Sweep", "M4 7h16", "M9 7V4h6v3", "M7 7l.8 12h8.4L17 7") }
}
