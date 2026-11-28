package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.resources.Res
import cz.pfspells.resources.alegreya_bold
import cz.pfspells.resources.alegreya_bold_italic
import cz.pfspells.resources.alegreya_sans_bold
import cz.pfspells.resources.alegreya_sans_medium
import cz.pfspells.resources.alegreya_sans_regular
import cz.pfspells.resources.caveat_bold
import cz.pfspells.resources.caveat_semibold
import cz.pfspells.resources.plex_mono_medium
import cz.pfspells.resources.plex_mono_regular
import cz.pfspells.resources.plex_mono_semibold
import org.jetbrains.compose.resources.Font

/** Barvy designu „Linkovaný zápisník“. */
object Nb {
    val paper = Color(0xFFE6D8BF)
    val panel = Color(0xFFF2E8D5)
    val inset = Color(0xFFEADCC2)
    val tag = Color(0xFFDCCBAA)
    val ink = Color(0xFF2E2418)
    val inkMuted = Color(0xFF5E4D38)
    val placeholder = Color(0xFF6E5B45)
    val accent = Color(0xFF4E5D2B)
    val rust = Color(0xFF8A4B2A)
    /** střední zatížení */
    val ochre = Color(0xFF9A6B14)
    val dash = Color(0xFF9C8662)
    val rule = Color(0xFFA8916A)
    val ruleSoft = Color(0x73A8916A)
    val line = Color(0x214E5D2B)
    val error = Color(0xFF9A2A1A)

    // „Deník dobrodruha“: tóny záložek rozdělovníku (text na nich vždy ink)
    val tabCharacter = Color(0xFFCFD4B2)
    val tabCombat = Color(0xFFE3C3AE)
    val tabSpells = Color(0xFFE8D39F)
    val tabInventory = Color(0xFFD9C9A6)
    /** záložka Mazlíčci – šedozelená (odstín tyrkysové z ikony appky) */
    val tabCompanions = Color(0xFFC3D1CC)
    /** text varování – `ochre` na drobný text nestačí (4,8 : 1 na paper) */
    val ochreInk = Color(0xFF7A5410)
    /** kartotéční lístek (statistiky kouzla / featu / předmětu) */
    val indexCard = Color(0xFFFAF4E6)
    /** horní linka kartotéčního lístku */
    val indexRule = Color(0xFFB5523B)
    /** banner konfliktu („roztržený lístek“) */
    val noteAlert = Color(0xFFF5E4D8)
    /** washi páska (jen dekorace, nejvýš jedna na obrazovku) */
    val tape = Color(0xCCB9C29A)
    /** sponka, sponky ze sešívačky */
    val clip = Color(0xFF7D6F5C)

    val shape = RoundedCornerShape(4.dp)
}

@Immutable
data class NbFonts(
    val serif: FontFamily,
    val sans: FontFamily,
    val mono: FontFamily,
    /** ruční písmo (Caveat) – jen poznámky a nápověda, nikdy data, čísla, tlačítka ani popisky polí */
    val hand: FontFamily,
)

val LocalNbFonts = staticCompositionLocalOf { NbFonts(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Monospace, FontFamily.Cursive) }

@Composable
fun PfTheme(content: @Composable () -> Unit) {
    val fonts = NbFonts(
        serif = FontFamily(
            Font(Res.font.alegreya_bold, FontWeight.Bold),
            Font(Res.font.alegreya_bold_italic, FontWeight.Bold, FontStyle.Italic),
        ),
        sans = FontFamily(
            Font(Res.font.alegreya_sans_regular, FontWeight.Normal),
            Font(Res.font.alegreya_sans_medium, FontWeight.Medium),
            Font(Res.font.alegreya_sans_bold, FontWeight.SemiBold),
            Font(Res.font.alegreya_sans_bold, FontWeight.Bold),
        ),
        mono = FontFamily(
            Font(Res.font.plex_mono_regular, FontWeight.Normal),
            Font(Res.font.plex_mono_medium, FontWeight.Medium),
            Font(Res.font.plex_mono_semibold, FontWeight.SemiBold),
        ),
        hand = FontFamily(
            Font(Res.font.caveat_semibold, FontWeight.SemiBold),
            Font(Res.font.caveat_bold, FontWeight.Bold),
        ),
    )
    val base = Typography()
    fun TextStyle.sans() = copy(fontFamily = fonts.sans)
    fun TextStyle.serif() = copy(fontFamily = fonts.serif, fontWeight = FontWeight.Bold)
    val typography = Typography(
        displayLarge = base.displayLarge.serif(),
        displayMedium = base.displayMedium.serif(),
        displaySmall = base.displaySmall.serif(),
        headlineLarge = base.headlineLarge.serif(),
        headlineMedium = base.headlineMedium.serif(),
        headlineSmall = base.headlineSmall.serif().copy(fontSize = 24.sp),
        titleLarge = base.titleLarge.serif(),
        titleMedium = base.titleMedium.sans().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.sans().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.sans().copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.sans().copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = base.bodySmall.sans().copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.sans().copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.sans(),
        labelSmall = base.labelSmall.sans(),
    )
    val colors = lightColorScheme(
        primary = Nb.accent, onPrimary = Color.White,
        primaryContainer = Nb.inset, onPrimaryContainer = Nb.ink,
        secondary = Nb.rust, onSecondary = Color.White,
        secondaryContainer = Nb.inset, onSecondaryContainer = Nb.ink,
        tertiary = Nb.accent, onTertiary = Color.White,
        background = Nb.paper, onBackground = Nb.ink,
        surface = Nb.paper, onSurface = Nb.ink,
        surfaceVariant = Nb.tag, onSurfaceVariant = Nb.inkMuted,
        surfaceContainerLowest = Nb.panel, surfaceContainerLow = Nb.panel,
        surfaceContainer = Nb.panel, surfaceContainerHigh = Nb.panel, surfaceContainerHighest = Nb.inset,
        surfaceTint = Color.Transparent,
        outline = Nb.dash, outlineVariant = Nb.rule,
        error = Nb.error, onError = Color.White,
    )
    val shapes = Shapes(
        extraSmall = Nb.shape, small = Nb.shape, medium = Nb.shape, large = Nb.shape, extraLarge = Nb.shape,
    )
    CompositionLocalProvider(LocalNbFonts provides fonts) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
    }
}

/** Linkovaný papír: podklad + vodorovné linky po 28 dp. */
fun Modifier.ruledPaper(color: Color = Nb.paper): Modifier = background(color).drawBehind {
    val step = 28.dp.toPx()
    var y = 27.dp.toPx()
    val stroke = 1.dp.toPx()
    while (y < size.height) {
        drawLine(Nb.line, Offset(0f, y + stroke / 2), Offset(size.width, y + stroke / 2), stroke)
        y += step
    }
}

private fun dashEffect(on: Float, off: Float) = PathEffect.dashPathEffect(floatArrayOf(on, off), 0f)

/** Čárkovaný rámeček kolem prvku. */
fun Modifier.dashedBorder(color: Color = Nb.dash, width: Dp = 1.5.dp, radius: Dp = 4.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = size.copy(width = size.width - w, height = size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = dashEffect(w * 3, w * 2)),
    )
}

enum class Side { Top, Bottom, Start, End }

/** Čárkovaná linka na jedné hraně (oddělovače panelů). */
fun Modifier.dashedEdge(side: Side, color: Color = Nb.dash, width: Dp = 1.5.dp): Modifier = drawBehind {
    val w = width.toPx()
    val effect = dashEffect(w * 3, w * 2)
    val (a, b) = when (side) {
        Side.Top -> Offset(0f, w / 2) to Offset(size.width, w / 2)
        Side.Bottom -> Offset(0f, size.height - w / 2) to Offset(size.width, size.height - w / 2)
        Side.Start -> Offset(w / 2, 0f) to Offset(w / 2, size.height)
        Side.End -> Offset(size.width - w / 2, 0f) to Offset(size.width - w / 2, size.height)
    }
    drawLine(color, a, b, w, pathEffect = effect)
}

/** Plná linka na spodní hraně (řádky seznamu). */
fun Modifier.bottomRule(color: Color = Nb.rule, width: Dp = 1.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawLine(color, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
}

/** Tahové ikony z návrhu (24×24 SVG path). */
object NbIcons {
    private fun stroke(name: String, width: Float, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Back = stroke("back", 2f, "M15 18l-6-6 6-6")
    val Trash = stroke("trash", 1.8f, "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Plus = stroke("plus", 2.2f, "M12 5v14M5 12h14")
    val Search = stroke("search", 2f, "M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0z", "M20 20l-3.5-3.5")
    val Book = stroke("book", 1.8f, "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2V5z", "M4 19a2 2 0 0 1 2-2h13")
    val Close = stroke("close", 2f, "M6 6l12 12M18 6L6 18")
    val Share = stroke("share", 1.8f, "M12 3v12", "M7 8l5-5 5 5", "M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6")
    val Pencil = stroke("pencil", 1.8f, "M4 20h4L19 9l-4-4L4 16v4z", "M13 7l4 4")
    val Menu = stroke("menu", 2.2f, "M12 5.5v.01M12 12v.01M12 18.5v.01")
    val Check = stroke("check", 2.4f, "M5 12.5l4.5 4.5L19 7.5")
    val Warn = stroke("warn", 2f, "M12 4l9 16H3L12 4z", "M12 10v4", "M12 17v.01")
    val Refresh = stroke("refresh", 2f, "M20 11a8 8 0 1 0-2.3 5.7", "M20 4v7h-7")
    val ChevronDown = stroke("chevron-down", 2f, "M6 9l6 6 6-6")
    /** Odpočinek (měsíc) */
    val Moon = stroke("moon", 2f, "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z")

    private const val STAR = "M12 3.5l2.6 5.3 5.8.8-4.2 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z"
    val Star = stroke("star", 1.8f, STAR)
    val StarFilled: ImageVector = ImageVector.Builder("star-filled", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString(STAR).toNodes(),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}
