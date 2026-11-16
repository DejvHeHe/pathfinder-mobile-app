package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.data.CasterClass

// ---------- texty ----------

@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color = Nb.inkMuted, size: TextUnit = 11.5.sp) {
    Text(
        text.uppercase(),
        modifier,
        fontFamily = LocalNbFonts.current.mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = size,
        letterSpacing = (size.value * 0.08f).sp,
        color = color,
    )
}

/** „Level 1 ───────“ – kurzívní rezavý nadpis s čárkovanou linkou. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, size: TextUnit = 18.sp) {
    Row(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            fontFamily = LocalNbFonts.current.serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = size,
            color = Nb.rust,
        )
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f).height(2.dp).dashedEdge(Side.Top, Nb.rule))
    }
}

// ---------- tlačítka ----------

@Composable
fun NbIconButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = Nb.ink, size: Dp = 20.dp) {
    IconButton(onClick = onClick, Modifier.size(44.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
fun BackButton(onBack: () -> Unit) = NbIconButton(NbIcons.Back, "Back", onBack, size = 22.dp)

/** Plné olivové tlačítko (hlavní akce). */
@Composable
fun NbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = NbIcons.Plus,
    height: Dp = 44.dp,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .heightIn(min = height)
            .clip(Nb.shape)
            .background(if (enabled) Nb.accent else Nb.rule)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Plovoucí hlavní tlačítko vpravo dole (mobil). */
@Composable
fun NbFab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = NbIcons.Plus) {
    NbPrimaryButton(text, onClick, modifier.shadow(8.dp, Nb.shape), icon = icon, height = 52.dp)
}

/** Papírové tlačítko s čárkovaným okrajem (vedlejší akce). */
@Composable
fun NbDashedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailing: ImageVector? = null,
) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(Nb.shape)
            .background(Nb.panel)
            .dashedBorder()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = Nb.ink, modifier = Modifier.size(18.dp))
        Text(
            text,
            color = if (enabled) Nb.ink else Nb.inkMuted,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) Icon(trailing, null, tint = Nb.inkMuted, modifier = Modifier.size(16.dp))
    }
}

/** Malé čtvercové tlačítko s okrajem (− / + / ×n). */
@Composable
fun NbSquareButton(
    label: String,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    size: Dp = 36.dp,
    content: @Composable () -> Unit = {
        Text(label, fontSize = 18.sp, color = if (enabled) Nb.ink else Nb.rule, textAlign = TextAlign.Center)
    },
) {
    Box(
        Modifier
            .size(size)
            .clip(Nb.shape)
            .background(Nb.inset)
            .border(1.dp, Nb.rule, Nb.shape)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** − číslo + */
@Composable
fun CountStepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 99,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    numberSize: TextUnit = 15.sp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NbSquareButton("−", { onChange(value - 1) }, "Decrease", enabled = value > min, size = size)
        Text(
            "$value",
            Modifier.widthIn(min = 20.dp),
            textAlign = TextAlign.Center,
            fontFamily = LocalNbFonts.current.mono,
            fontWeight = FontWeight.SemiBold,
            fontSize = numberSize,
            color = Nb.ink,
        )
        NbSquareButton("+", { onChange(value + 1) }, "Increase", enabled = value < max, size = size)
    }
}

// ---------- štítky, panely, filtry ----------

@Composable
fun NbTag(text: String, color: Color = Nb.ink, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.clip(Nb.shape).background(Nb.tag).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = LocalNbFonts.current.sans,
        maxLines = 1,
    )
}

/** Panel s čárkovaným okrajem. */
@Composable
fun NbPanel(
    modifier: Modifier = Modifier,
    color: Color = Nb.panel,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.clip(Nb.shape).background(color).dashedBorder().padding(padding), content = content)
}

/** Filtr levelu: vybraný = olivový, ostatní papírové s čárkovaným okrajem. */
@Composable
fun NbFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.height(34.dp).clip(Nb.shape)
    Box(
        (if (selected) base.background(Nb.accent) else base.background(Nb.panel).dashedBorder())
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = LocalNbFonts.current.mono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (selected) Color.White else Nb.ink,
        )
    }
}

@Composable
fun NbSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    background: Color = Nb.panel,
) {
    val fonts = LocalNbFonts.current
    Row(
        modifier.height(44.dp).clip(Nb.shape).background(background).dashedBorder().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(NbIcons.Search, null, tint = Nb.inkMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, color = Nb.placeholder, fontFamily = fonts.sans, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = fonts.sans, fontSize = 15.sp, color = Nb.ink),
                cursorBrush = SolidColor(Nb.accent),
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) NbIconButton(NbIcons.Close, "Clear search", { onValueChange("") }, Nb.inkMuted, 16.dp)
    }
}

/** Jednoduchý dropdown (bez experimentálního ExposedDropdownMenuBox). */
@Composable
fun <T> SimpleDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        NbDashedButton("$label: ${optionLabel(selected)}", { open = true }, trailing = NbIcons.ChevronDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Nb.panel) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(optionLabel(opt), color = Nb.ink) },
                    onClick = { onSelect(opt); open = false },
                )
            }
        }
    }
}

// ---------- kostra obrazovky ----------

/**
 * Mobil: horní lišta (zpět, titul, akce) + obsah + plovoucí tlačítko.
 * Široké okno: velký nadpis s akcemi a hlavním tlačítkem v řádku, bez plovoucího tlačítka.
 */
@Composable
fun NbScreen(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    wide: Boolean,
    modifier: Modifier = Modifier,
    primary: Pair<String, () -> Unit>? = null,
    headerExtra: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val fonts = LocalNbFonts.current
    if (wide) {
        Column(modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onBack != null) BackButton(onBack)
                Column(Modifier.weight(1f)) {
                    Text(title, fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    subtitle?.let { Text(it, fontSize = 15.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp)) }
                }
                headerExtra()
                primary?.let { (text, action) -> NbPrimaryButton(text, action) }
                actions()
            }
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    } else {
        Box(modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (onBack != null) BackButton(onBack) else Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        subtitle?.let { Text(it, fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)) }
                    }
                    actions()
                }
                Column(Modifier.weight(1f).fillMaxWidth(), content = content)
            }
            primary?.let { (text, action) ->
                NbFab(text, action, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp))
            }
        }
    }
}

// ---------- pomocné ----------

fun classLevelSummary(cls: CasterClass?, level: Int): String {
    if (cls == null) return "Unknown class · level $level"
    return "${cls.name} $level · ${spellRangeText(cls, level)}"
}

fun spellRangeText(cls: CasterClass, level: Int): String {
    val max = cls.maxSpellLevel(level)
    val min = if (cls.hasCantrips) 0 else 1
    return when {
        max < 0 -> "no spells yet"
        max == min -> "spell level $max"
        else -> "spell levels $min–$max"
    }
}

fun spellLevelLabel(level: Int): String = if (level == 0) "Cantrips (0)" else "Level $level"

fun copiesCount(n: Int): String = if (n == 1) "1 copy" else "$n copies"

fun spellsCount(n: Int): String = if (n == 1) "1 spell" else "$n spells"
