package cz.pfspells.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.CharacterTab
import cz.pfspells.Strings

// ---------- pohyb ----------

/** Systémové „omezit pohyb“ – App ho poskytne z [cz.pfspells.data.systemReducedMotion]; true = rovnou konečný stav. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Křivka „dopsání“ (rychlý rozjezd, dlouhé doběhnutí – jako tah perem). */
val WriteEasing: Easing = CubicBezierEasing(.6f, 0f, .3f, 1f)

/**
 * Průběh 0 → 1 animace deníku (dopsání, linka tužkou, zakroužkování…), spuštěný při prvním zobrazení
 * a znovu při změně [key]. Při omezení pohybu je hned 1. Jedna pomůcka pro všechny animace, ať se chovají stejně.
 */
@Composable
fun rememberRevealProgress(
    key: Any?,
    delayMs: Int = 0,
    durationMs: Int = 600,
    easing: Easing = LinearOutSlowInEasing,
): State<Float> {
    val reduce = LocalReduceMotion.current
    // jednou přehraná animace se po návratu (scroll v seznamu, zpět na obrazovku) už neopakuje
    val played = androidx.compose.runtime.saveable.rememberSaveable(key) { mutableStateOf(false) }
    val anim = remember(key) { Animatable(if (reduce || played.value) 1f else 0f) }
    LaunchedEffect(key, reduce) {
        if (reduce || played.value) anim.snapTo(1f) else anim.animateTo(1f, tween(durationMs, delayMs, easing))
        played.value = true
    }
    return anim.asState()
}

/** Nakreslí jen první část cesty (0 → 1) – tah perem / tužkou. */
fun DrawScope.drawPathProgress(
    path: Path,
    progress: Float,
    color: Color,
    width: Dp,
) {
    if (progress <= 0f) return
    val stroke = Stroke(width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (progress >= 1f) {
        drawPath(path, color, style = stroke)
        return
    }
    val measure = PathMeasure()
    measure.setPath(path, false)
    val dst = Path()
    measure.getSegment(0f, measure.length * progress, dst, true)
    drawPath(dst, color, style = stroke)
}

/**
 * Otočení stránky (přepnutí záložky, otevření detailu): stránka se natočí kolem levého okraje z −16° do roviny
 * a zprůhlednění zmizí. 550 ms ease-out, při omezení pohybu nic.
 */
@Composable
fun Modifier.pageTurn(key: Any?): Modifier {
    val p = rememberRevealProgress(key, durationMs = 550, easing = FastOutSlowInEasing)
    return graphicsLayer {
        val v = p.value
        rotationY = -16f * (1f - v)
        alpha = v
        transformOrigin = TransformOrigin(0f, .5f)
        cameraDistance = 12f * density
    }
}

// ---------- záložky rozdělovníku ----------

/** Tón záložky – text na něm je vždy `ink`. */
fun CharacterTab.tint(): Color = when (this) {
    CharacterTab.CHARACTER -> Nb.tabCharacter
    CharacterTab.COMBAT -> Nb.tabCombat
    CharacterTab.SPELLS -> Nb.tabSpells
    CharacterTab.INVENTORY -> Nb.tabInventory
    CharacterTab.COMPANIONS -> Nb.tabCompanions
}

fun Strings.tabLabel(t: CharacterTab): String = when (t) {
    CharacterTab.CHARACTER -> tabCharacter
    CharacterTab.COMBAT -> tabCombat
    CharacterTab.SPELLS -> tabSpells
    CharacterTab.INVENTORY -> tabInventory
    CharacterTab.COMPANIONS -> tabCompanions
}

/** Ručně tažené podtržení vybrané záložky (vlnka 54 × 7 dp). */
private val TAB_UNDERLINE = androidx.compose.ui.graphics.vector.PathParser()
    .parsePathString("M2 4 C 16 1, 30 6, 52 3").toPath()

@Composable
private fun PencilUnderline(key: Any?, modifier: Modifier = Modifier) {
    val p = rememberRevealProgress(key, durationMs = 500)
    Canvas(modifier.size(54.dp, 7.dp)) {
        val k = size.width / 54f
        val path = Path().apply { addPath(TAB_UNDERLINE); }
        val scaled = Path().apply {
            addPath(path)
            transform(androidx.compose.ui.graphics.Matrix().apply { scale(k, k) })
        }
        drawPathProgress(scaled, p.value, Nb.accent, 2.2.dp)
    }
}

/**
 * Mobil: řada 4 záložek rozdělovníku pod hlavičkou. Vybraná je vyšší (44 dp), tučná a podtržená ručně taženou
 * linkou; pod řadou linka 3 dp v tónu vybrané záložky.
 */
@Composable
fun NbIndexTabs(
    s: Strings,
    selected: CharacterTab,
    onSelect: (CharacterTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<CharacterTab> = CharacterTab.entries,
) {
    // pět záložek na 360–390 dp: menší písmo, ať se „Inventář“ / „Mazlíčci“ vejdou
    val fontSize = if (tabs.size > 4) 14.sp else 15.sp
    val fonts = LocalNbFonts.current
    val tabShape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
    Column(modifier.fillMaxWidth().semantics { contentDescription = s.sectionsLabel }) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEach { t ->
                val sel = t == selected
                Box(
                    Modifier.weight(1f).height(if (sel) 44.dp else 38.dp).clip(tabShape).background(t.tint())
                        .selectable(selected = sel, role = Role.Tab) { onSelect(t) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        s.tabLabel(t),
                        Modifier.padding(horizontal = 2.dp),
                        fontFamily = fonts.sans, fontSize = fontSize,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                    if (sel) PencilUnderline(t, Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(selected.tint()))
    }
}

/**
 * Desktop: stejné záložky svisle na pravém okraji hlavního sloupce (104 dp vysoké, 36 dp široké, vybraná 44 dp,
 * zaoblené rohy vlevo, text otočený).
 */
@Composable
fun NbIndexTabsVertical(
    s: Strings,
    selected: CharacterTab,
    onSelect: (CharacterTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<CharacterTab> = CharacterTab.entries,
) {
    val fonts = LocalNbFonts.current
    val tabShape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)
    Column(
        modifier.width(44.dp).selectableGroup().semantics { contentDescription = s.sectionsLabel },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        tabs.forEach { t ->
            val sel = t == selected
            Box(
                Modifier.width(if (sel) 44.dp else 36.dp).height(104.dp).clip(tabShape).background(t.tint())
                    .selectable(selected = sel, role = Role.Tab) { onSelect(t) },
                contentAlignment = Alignment.Center,
            ) {
                // text otočený o 90° (čte se zdola nahoru): rozložený vodorovně na výšku záložky, pak natočený
                Text(
                    s.tabLabel(t),
                    Modifier.requiredWidth(100.dp).rotate(-90f),
                    textAlign = TextAlign.Center,
                    fontFamily = fonts.sans, fontSize = 15.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------- přepínač Příprava | Hra ----------

/** Segmentový přepínač dvou režimů (Příprava | Hra) – vybraný olivový. */
@Composable
fun NbModeSwitch(
    s: Strings,
    play: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fonts = LocalNbFonts.current
    Row(modifier.clip(Nb.shape).background(Nb.panel).dashedBorder().selectableGroup()) {
        listOf(false to s.modePrepare, true to s.modePlay).forEach { (isPlay, label) ->
            val sel = isPlay == play
            Box(
                Modifier.height(40.dp).background(if (sel) Nb.accent else Color.Transparent)
                    .selectable(selected = sel, role = Role.RadioButton) { onChange(isPlay) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontFamily = fonts.sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = if (sel) Color.White else Nb.ink, maxLines = 1,
                )
            }
        }
    }
}

/** Poznámka ručním písmem (Caveat) – jen nápověda, nikdy data. */
@Composable
fun HandNote(text: String, modifier: Modifier = Modifier, color: Color = Nb.accent, size: Float = 17f) {
    Text(
        text, modifier,
        fontFamily = LocalNbFonts.current.hand, fontWeight = FontWeight.SemiBold, fontSize = size.sp,
        color = color, lineHeight = (size * 1.05f).sp,
    )
}

/** Řádek ovládání kouzel / schopností: Příprava | Hra, vpravo Odpočinek. */
@Composable
fun ModeBar(s: Strings, play: Boolean, onMode: (Boolean) -> Unit, onRest: () -> Unit, modifier: Modifier = Modifier, extra: @Composable RowScope.() -> Unit = {}) {
    var rested by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier.fillMaxWidth().zIndex(1f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NbModeSwitch(s, play, onMode)
            extra()
            Box(Modifier.weight(1f))
            NbDashedButton(s.rest, { onRest(); rested = true }, icon = NbIcons.Moon)
        }
        // „sloty obnoveny ✓“ – samo zmizí
        NbHandStamp(s.restDone, rested, { rested = false }, Modifier.align(Alignment.BottomEnd).offset(x = (-6).dp, y = 34.dp))
    }
}

/** Linky papíru pro vlastní plochy (stejné jako [ruledPaper], jen bez podkladu). */
fun Modifier.paperLines(step: Dp = 28.dp, first: Dp = 27.dp, color: Color = Nb.line): Modifier = drawBehind {
    val st = step.toPx()
    var y = first.toPx()
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += st
    }
}

// ---------- papírové prvky ----------

/** Cesta ze SVG zápisu (souřadnice viewBoxu) zvětšená na plátno. */
private fun svgPath(d: String, scale: Float): Path {
    val p = androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toPath()
    if (scale != 1f) p.transform(androidx.compose.ui.graphics.Matrix().apply { scale(scale, scale) })
    return p
}

private const val SLOT_RING = "M12 3.5c4.8-.3 8.6 3.4 8.4 8.2-.2 4.6-3.9 8.3-8.6 8.2C7 19.8 3.3 16 3.6 11.4 3.9 7 7.4 3.7 12 3.5z"
private const val CROSS = "M7 7 L17 17 M17 7 L7 17"
private const val TICK = "M4 13 l5 5 L20 6"
private const val X_MARK = "M5 5 L19 19 M19 5 L5 19"
private const val QUESTION = "M8.5 8.5a3.6 3.6 0 0 1 7 1.2c0 2.4-3.4 2.9-3.4 5.3M12 18.6v.2"
private const val BANG = "M12 4.5v9.5M12 18.4v.2"
/** ručně tažená elipsa kolem hodnoty (viewBox 74 × 40), konec lehce přetažený přes začátek */
private const val CIRCLE_AROUND = "M12 9 C 30 0, 66 4, 66 20 C 66 36, 22 38, 8 28 C 0 20, 10 8, 30 6"
private const val PAPERCLIP = "M6 30V8a4 4 0 0 1 8 0v26a6 6 0 0 1-12 0V12"

/**
 * Kroužek jedné připravené kopie kouzla / jednoho použití schopnosti: nepravidelný kruh 26 dp v cíli 36 × 40 dp.
 * Volný: výplň panel, okraj [ring]; použitý: výplň tag + tažený křížek [crossColor].
 */
@Composable
fun NbSlotCircle(
    used: Boolean,
    label: String,
    onClick: () -> Unit,
    ring: Color = Nb.accent,
    crossColor: Color = Nb.ink,
) {
    val p = rememberRevealProgress(used, durationMs = 450)
    Box(
        Modifier.size(36.dp, 40.dp).selectable(selected = used, role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val k = size.width / 24f
            val ringPath = svgPath(SLOT_RING, k)
            drawPath(ringPath, if (used) Nb.tag else Nb.panel)
            drawPath(ringPath, ring, style = Stroke(1.7.dp.toPx()))
            if (used) drawPathProgress(svgPath(CROSS, k), p.value, crossColor, 2.3.dp)
        }
    }
}

/**
 * Řada kroužků (kopie / sloty / použití): ťuk na i-tý – je-li použitý, použito = i, jinak i + 1.
 * Víc než [maxCircles] → stepper.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SlotCircles(
    total: Int,
    used: Int,
    label: String,
    modifier: Modifier = Modifier,
    ring: Color = Nb.accent,
    crossColor: Color = Nb.ink,
    maxCircles: Int = 8,
    onSet: (Int) -> Unit,
) {
    if (total > maxCircles) {
        CountStepper(used.coerceAtMost(total), onSet, min = 0, max = total, size = 32.dp, numberSize = 14.sp)
        return
    }
    androidx.compose.foundation.layout.FlowRow(modifier) {
        for (i in 0 until total) {
            val u = i < used
            NbSlotCircle(u, label, { onSet(if (u) i else i + 1) }, ring, crossColor)
        }
    }
}

/** Ručně tažené přeškrtnutí (2 dp ink, natočené −2°) – všechny kopie kouzla seslané. */
@Composable
fun Modifier.handStrike(active: Boolean, color: Color = Nb.ink): Modifier {
    val p = rememberRevealProgress(active, durationMs = 600)
    if (!active) return this
    return drawWithContent {
        drawContent()
        val pad = 3.dp.toPx()
        val y = size.height * .52f
        val x1 = -pad + (size.width + 2 * pad) * p.value
        rotate(-2f, Offset(size.width / 2, y)) {
            drawLine(color, Offset(-pad, y), Offset(x1, y), 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** Stav prerekvizity / položky v okraji listu: ✓, ✗, ?, ! (kreslené tahem). */
enum class MarginMark { OK, NO, UNKNOWN, WARN }

@Composable
fun NbMarginMark(mark: MarginMark, label: String, modifier: Modifier = Modifier, delayMs: Int = 0) {
    val p = rememberRevealProgress(mark, delayMs = delayMs, durationMs = 450)
    val (d, color, w) = when (mark) {
        MarginMark.OK -> Triple(TICK, Nb.accent, 2.6.dp)
        MarginMark.NO -> Triple(X_MARK, Nb.error, 2.6.dp)
        MarginMark.UNKNOWN -> Triple(QUESTION, Nb.inkMuted, 2.2.dp)
        MarginMark.WARN -> Triple(BANG, Nb.ochreInk, 2.6.dp)
    }
    Canvas(modifier.size(22.dp).semantics { contentDescription = label }) {
        drawPathProgress(svgPath(d, size.width / 24f), p.value, color, w)
    }
}

/**
 * Obsah v okraji listu (vlevo od rezavé linky): prvek s nulovou šířkou, jeho obsah je posunutý o [offset] doleva.
 * Na širokém okně okraj není – pak se obsah ukáže normálně v řádku.
 */
@Composable
fun InMargin(inMargin: Boolean, offset: Dp = 40.dp, content: @Composable () -> Unit) {
    if (!inMargin) {
        Box(Modifier.padding(end = 8.dp)) { content() }
        return
    }
    Box(Modifier.width(0.dp).wrapContentWidth(Alignment.Start, unbounded = true)) {
        Box(Modifier.offset(x = -offset)) { content() }
    }
}

/** Ručně tažená elipsa kolem klíčové hodnoty (hlavní atribut kouzelníka…). */
@Composable
fun Modifier.handCircled(active: Boolean, delayMs: Int = 800, color: Color = Nb.accent): Modifier {
    if (!active) return this
    val p = rememberRevealProgress("circled", delayMs = delayMs, durationMs = 500)
    return drawWithContent {
        drawContent()
        // elipsa přesahuje obsah o 8 dp na každou stranu
        val m = 8.dp.toPx()
        val path = svgPath(CIRCLE_AROUND, 1f)
        path.transform(androidx.compose.ui.graphics.Matrix().apply { scale((size.width + 2 * m) / 70f, (size.height + 2 * m) / 38f) })
        translate(-m, -m) { drawPathProgress(path, p.value, color, 1.8.dp) }
    }
}

/**
 * Razítko stavu, který mění pravidla (NAD LIMIT, BEZ VĚDOMÍ, PŘETÍŽENO): mono verzálky, rámeček 2 dp error,
 * natočené −9°; objeví se přitisknutím (zvětšení 2,3 → 1 s odskokem).
 */
@Composable
fun NbStamp(text: String, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(if (reduce) 1f else 2.3f) }
    val p = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        kotlinx.coroutines.coroutineScope {
            launch { p.animateTo(1f, tween(220)) }
            scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = .45f, stiffness = 500f))
        }
    }
    Text(
        text,
        modifier
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                rotationZ = -22f + 13f * p.value
                alpha = p.value
            }
            .clip(RoundedCornerShape(3.dp))
            .background(Nb.paper.copy(alpha = .93f))
            .border(2.dp, Nb.error, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.5.sp,
        color = Nb.error, maxLines = 1,
    )
}

/** Kladné razítko ručním písmem (např. „sloty obnoveny“) – samo zmizí po 1,8 s. */
@Composable
fun NbHandStamp(text: String, visible: Boolean, onHidden: () -> Unit, modifier: Modifier = Modifier) {
    if (!visible) return
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800)
        onHidden()
    }
    Row(
        modifier.graphicsLayer { rotationZ = -3f }.clip(Nb.shape).background(Nb.panel).border(1.5.dp, Nb.accent, Nb.shape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HandNote(text, size = 20f)
        androidx.compose.material3.Icon(NbIcons.Check, null, tint = Nb.accent, modifier = Modifier.size(16.dp))
    }
}

/**
 * Kartotéční lístek: statistiky kouzla / předmětu. Podklad indexCard, linky po 24 dp, horní linka 2 dp indexRule,
 * dvě sponky ze sešívačky přes horní hranu; na širokém okně lehce natočený.
 */
@Composable
fun NbIndexCard(modifier: Modifier = Modifier, tilt: Boolean = false, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(modifier.graphicsLayer { if (tilt) rotationZ = -.6f }) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(Nb.indexCard)
                .paperLines(step = 24.dp, first = 31.dp, color = Nb.accent.copy(alpha = .16f))
                .drawBehind { drawRect(Nb.indexRule, size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())) }
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
            content = content,
        )
        // sponky ze sešívačky přes horní hranu
        Canvas(Modifier.matchParentSize()) {
            val y = 0f
            val w = 22.dp.toPx()
            listOf(size.width * .13f, size.width * .8f).forEach { x ->
                drawLine(Nb.clip, Offset(x, y), Offset(x + w, y), 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

/** Řádek kartotéčního lístku: mono popisek (rust) a hodnota, výška po 24 dp jako linky. */
@Composable
fun IndexCardLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            label.uppercase(), Modifier.width(104.dp).padding(top = 5.dp),
            fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = .9.sp,
            color = Nb.rust, maxLines = 2, lineHeight = 14.sp,
        )
        Text(value, Modifier.weight(1f).padding(top = 1.dp), fontSize = 14.5.sp, lineHeight = 24.sp, color = Nb.ink)
    }
}

/** Zubatý spodní okraj „roztrženého lístku“. */
private val TornShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    val teeth = 14
    val toothH = size.height.coerceAtMost(60f) * 0.14f + 4f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - toothH)
    for (i in teeth downTo 0) {
        val x = size.width * i / teeth
        lineTo(x, if (i % 2 == 0) size.height - toothH else size.height)
    }
    close()
}

/** Roztržený lístek: banner konfliktu / chyby s akcí vpravo. */
@Composable
fun NbTornNote(title: String, text: String?, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(TornShape).background(Nb.noteAlert)
            .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(NbIcons.Warn, null, tint = Nb.error, modifier = Modifier.size(18.dp))
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Nb.error)
            if (action != null) {
                Text(
                    action, Modifier.clip(Nb.shape).clickable(onClick = onAction).padding(horizontal = 4.dp, vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nb.accent,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                )
            }
        }
        if (!text.isNullOrBlank()) Text(text, fontSize = 13.5.sp, lineHeight = 18.sp, color = Nb.ink, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Washi páska přes pravý horní roh – zvýraznění jednoho panelu (nejvýš jedna na obrazovku). */
fun Modifier.tape(): Modifier = drawWithContent {
    drawContent()
    val w = 76.dp.toPx(); val h = 20.dp.toPx()
    val x = size.width - w + 12.dp.toPx(); val y = -9.dp.toPx()
    rotate(6f, Offset(x + w / 2, y + h / 2)) { drawRect(Nb.tape, Offset(x, y), androidx.compose.ui.geometry.Size(w, h)) }
}

/** Sponka přes horní hranu vpravo – připojený záznam (panel class, mazlíček pod pánem). */
fun Modifier.paperclip(right: Dp = 18.dp): Modifier = drawWithContent {
    drawContent()
    val k = 1f * density
    val path = svgPath(PAPERCLIP, k)
    translate(size.width - right.toPx() - 18 * k, -14 * k) {
        drawPath(path, Nb.clip, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Šrafování (−55°, rozteč 5 dp) v obdélníku [width] × výška plátna. */
fun DrawScope.drawHatch(width: Float, color: Color, stroke: Dp = 1.5.dp, gap: Dp = 5.dp) {
    if (width <= 0f) return
    val g = gap.toPx()
    val h = size.height
    val dx = h / kotlin.math.tan(55.0 * kotlin.math.PI / 180).toFloat()
    clipRect(0f, 0f, width, h) {
        var x = -dx
        while (x < width + dx) {
            drawLine(color, Offset(x, h), Offset(x + dx, 0f), stroke.toPx())
            x += g
        }
    }
}

/** Číslo, které se při změně „přepíše“ (staré zmizí, nové se dopíše shora). */
@Composable
fun RewriteText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle,
) {
    if (LocalReduceMotion.current) {
        Text(text, modifier, style = style)
        return
    }
    androidx.compose.animation.AnimatedContent(
        targetState = text, modifier = modifier, label = "rewrite",
        transitionSpec = {
            (androidx.compose.animation.fadeIn(tween(300)) + androidx.compose.animation.slideInVertically(tween(300)) { it / 6 }) togetherWith
                androidx.compose.animation.fadeOut(tween(150))
        },
    ) { t -> Text(t, style = style) }
}

/** Šířka okraje listu na mobilu – obsah začíná za rezavou linkou. */
val MarginStart = 46.dp

/**
 * Okraj sešitového listu (mobil): rezavá linka 1,5 dp na x = 32 dp a druhá 1 dp na x = 36 dp, tři „díry“ po kroužkové
 * vazbě na x = 9 dp. Obsah se odsazuje o [MarginStart].
 */
fun Modifier.notebookMargin(): Modifier = drawBehind {
    val x1 = 32.dp.toPx(); val x2 = 36.dp.toPx()
    drawLine(Nb.rust.copy(alpha = .53f), Offset(x1, 0f), Offset(x1, size.height), 1.5.dp.toPx())
    drawLine(Nb.rust.copy(alpha = .27f), Offset(x2, 0f), Offset(x2, size.height), 1.dp.toPx())
    val r = 6.5.dp.toPx()
    listOf(150.dp, 380.dp, 610.dp).map { it.toPx() }.filter { it + r < size.height }.forEach { y ->
        val c = Offset(9.dp.toPx() + r, y)
        drawCircle(Color(0xFFCDBB96), r, c)
        drawCircle(Nb.ink.copy(alpha = .22f), r, c, style = Stroke(1.dp.toPx()))
    }
}

/** Souhrn postavy jako tabulka buněk s čárkovanými předěly (ÚR., ZÚB, ODOL, REFL, VŮLE). */
@Composable
fun StatsTable(cells: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val fonts = LocalNbFonts.current
    Row(modifier.fillMaxWidth().clip(Nb.shape).background(Nb.panel).dashedBorder()) {
        cells.forEachIndexed { i, (label, value) ->
            Column(
                Modifier.weight(1f).then(if (i > 0) Modifier.dashedEdge(Side.Start, Color(0xFFC4B08C)) else Modifier).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MonoLabel(label, color = Nb.rust, size = 10.5.sp)
                RewriteText(value, style = androidx.compose.ui.text.TextStyle(fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Nb.ink))
            }
        }
    }
}

/** Čárkovaný box s ruční poznámkou (volná místa ve spell levelu, prázdné sloty). */
@Composable
fun DashedNoteBox(text: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 40.dp).clip(Nb.shape).dashedBorder()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) { HandNote(text, color = Nb.inkMuted, size = 19f) }
}


/**
 * Postupné objevení řádku / dlaždice: průhlednost 0 → 1 a posun 8 dp → 0, zpoždění podle pořadí
 * (rozestup 60 ms, nejvýš 10 kroků).
 */
@Composable
fun Modifier.staggerIn(index: Int, baseDelayMs: Int = 120): Modifier {
    val p = rememberRevealProgress("stagger", delayMs = baseDelayMs + index.coerceIn(0, 10) * 60, durationMs = 450)
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 8.dp.toPx()
    }
}

/** Nadpis se „dopíše“ zleva doprava (ořez obsahu podle průběhu). */
@Composable
fun Modifier.writeIn(delayMs: Int = 0): Modifier {
    val p = rememberRevealProgress("write", delayMs = delayMs, durationMs = 700, easing = WriteEasing)
    return drawWithContent {
        clipRect(right = size.width * p.value) { this@drawWithContent.drawContent() }
    }
}

/** Čára se natáhne zleva doprava (scaleX 0 → 1 od levého okraje). */
@Composable
fun Modifier.lineIn(delayMs: Int = 200): Modifier {
    val p = rememberRevealProgress("line", delayMs = delayMs, durationMs = 600)
    return graphicsLayer {
        scaleX = p.value
        transformOrigin = TransformOrigin(0f, .5f)
    }
}

/** Náčrtky tužkou pro prázdné stavy (viewBox 80 × 60). */
enum class Sketch(val d: String) {
    BOOK("M10 14c10-4 20-4 30 2c10-6 20-6 30-2v34c-10-4-20-4-30 2c-10-6-20-6-30-2z M40 16v34 M16 24c6-2 12-2 18 1 M46 25c6-3 12-3 18-1"),
    BAG("M20 24h40l5 28H15z M30 24c0-9 4-13 10-13s10 4 10 13 M27 36h26"),
    PAW("M40 32c-9 0-16 9-16 15 0 5 6 7 16 7s16-2 16-7c0-6-7-15-16-15z M22 30a4 5 0 1 0 8 0a4 5 0 1 0 -8 0 M31 20a4 5 0 1 0 8 0a4 5 0 1 0 -8 0 M41 20a4 5 0 1 0 8 0a4 5 0 1 0 -8 0 M50 30a4 5 0 1 0 8 0a4 5 0 1 0 -8 0"),
    QUILL("M62 8c-15 4-27 16-33 34l-4 10 8-6c16-4 27-16 29-38z M25 52l-7 6 M34 38c6-8 14-16 24-24"),
}

/**
 * Prázdný stav: náčrtek tužkou (kreslí se), nadpis ručním písmem 30 sp, jedna věta, nejvýš jedno plné tlačítko.
 */
@Composable
fun NbEmptyState(
    sketch: Sketch,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: Pair<String, () -> Unit>? = null,
) {
    val p = rememberRevealProgress(sketch, delayMs = 150, durationMs = 900, easing = WriteEasing)
    Column(
        modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(96.dp, 72.dp)) {
            drawPathProgress(svgPath(sketch.d, size.width / 80f), p.value, Nb.inkMuted, 1.8.dp)
        }
        HandNote(title, color = Nb.ink, size = 30f)
        Text(text, fontSize = 14.5.sp, lineHeight = 21.sp, color = Nb.inkMuted, textAlign = TextAlign.Center)
        if (action != null) NbPrimaryButton(action.first, action.second, Modifier.padding(top = 8.dp))
    }
}


/** Hvězda oblíbených: při zapnutí „poskočí“ (.4 → 1,25 → 1, 350 ms); při vypnutí nic. */
@Composable
fun Modifier.starPop(on: Boolean): Modifier {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(on) {
        if (first) { first = false; return@LaunchedEffect }
        if (on && !reduce) {
            scale.snapTo(.4f)
            scale.animateTo(1.25f, tween(210))
            scale.animateTo(1f, tween(140))
        }
    }
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
