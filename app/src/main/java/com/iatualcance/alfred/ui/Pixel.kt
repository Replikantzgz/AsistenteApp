package com.iatualcance.alfred.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Forma "pixel": esquinas cortadas en escalón, sin curvas. */
val PxShape = CutCornerShape(6.dp)
val PxLine = Color(0xFF4A4590)
val GoldDark = Color(0xFF8A6D12)

/** Borde duro de 2 dp, como los marcos de un juego de 16 bits. */
fun Modifier.pxBorder(color: Color = PxLine) = this.border(2.dp, color, PxShape)

// --------------------------------------------------------------------------- Sprites
private val PALETTE = mapOf(
    'k' to Color(0xFF0D0A25), 'w' to Color(0xFFF1EFFF), 'g' to Color(0xFFD9B54A), 'y' to Color(0xFFFFE0B0),
    'b' to Color(0xFF5B9BFF), 'r' to Color(0xFFE5534B), 'd' to Color(0xFF8A6D12), 'n' to Color(0xFF2B2760),
    'e' to Color(0xFF3FBF7F),
)

object Sprites {
    val MAIL = listOf(
        "............",
        "kkkkkkkkkkkk",
        "kwwwwwwwwwwk",
        "kkwwwwwwwwkk",
        "kwkwwwwwwkwk",
        "kwwkwwwwkwwk",
        "kwwwkkkkwwwk",
        "kwwwwwwwwwwk",
        "kwwwwwwwwwwk",
        "kkkkkkkkkkkk",
        "............",
        "............",
    )
    val CALENDAR = listOf(
        "..k......k..",
        ".kkkkkkkkkk.",
        ".krrrrrrrrk.",
        ".krrrrrrrrk.",
        ".kkkkkkkkkk.",
        ".kwwwwwwwwk.",
        ".kwbwwbwwwk.",
        ".kwwwwwwwwk.",
        ".kwbwwgggwk.",
        ".kwwwwwwwwk.",
        ".kkkkkkkkkk.",
        "............",
    )
    val NOTES = listOf(
        ".kkkkkkk....",
        ".kwwwwwkk...",
        ".kwwwwwkwk..",
        ".kwwwwwkkkk.",
        ".kwbbbbwwwk.",
        ".kwwwwwwwwk.",
        ".kwbbbbbwwk.",
        ".kwwwwwwwwk.",
        ".kwbbbwwwwk.",
        ".kwwwwwwwwk.",
        ".kkkkkkkkkk.",
        "............",
    )
    val CONTACTS = listOf(
        "............",
        "....kkkk....",
        "...kyyyyk...",
        "...kyyyyk...",
        "...kyyyyk...",
        "....kkkk....",
        "..kkkkkkkk..",
        ".kbbbbbbbbk.",
        ".kbbbbbbbbk.",
        ".kbbbbbbbbk.",
        ".kkkkkkkkkk.",
        "............",
    )
    val ALFRED = listOf(
        "................",
        ".....kkkkkk.....",
        "....kggggggk....",
        "...kggggggggk...",
        "...kyyyyyyyyk...",
        "...kykkyykkyk...",
        "...kyyyyyyyyk...",
        "...kyykkkkyyk...",
        "....kyyyyyyk....",
        ".....kkkkkk.....",
        "...kwrrwwrrwk...",
        "..kkwrrrrrrwkk..",
        ".kbbkwwrrwwkbbk.",
        ".kbbbkwwwwkbbbk.",
        ".kbbbbkwwkbbbbk.",
        ".kkkkkkkkkkkkkk.",
    )
}

@Composable
fun PixelSprite(rows: List<String>, size: Dp, modifier: Modifier = Modifier, description: String? = null) {
    val h = rows.size
    val w = rows.maxOf { it.length }
    Canvas(
        modifier.size(size).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        val cell = minOf(this.size.width / w, this.size.height / h)
        val ox = (this.size.width - cell * w) / 2
        val oy = (this.size.height - cell * h) / 2
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                PALETTE[ch]?.let { c ->
                    drawRect(c, Offset(ox + x * cell, oy + y * cell), Size(cell + 0.6f, cell + 0.6f))
                }
            }
        }
    }
}

// --------------------------------------------------------------------------- Barra inferior
private data class NavItem(val tab: Tab, val label: String, val sprite: List<String>)

private val sideLeft = listOf(NavItem(Tab.MAIL, "CORREO", Sprites.MAIL), NavItem(Tab.AGENDA, "AGENDA", Sprites.CALENDAR))
private val sideRight = listOf(NavItem(Tab.NOTES, "NOTAS", Sprites.NOTES), NavItem(Tab.CONTACTS, "CONTACTOS", Sprites.CONTACTS))

/**
 * Cinco iconos: Correo · Agenda · ALFRED (grande, centro) · Notas · Contactos.
 * El botón central es el asistente, que puede manejar todas las demás pantallas.
 */
@Composable
fun PixelNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().background(Color.Transparent).navigationBarsPadding().height(92.dp)) {
        // Barra
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp).background(NavySurface)) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(GoldDark))
        }
        Row(Modifier.fillMaxWidth().height(92.dp)) {
            sideLeft.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
            CenterItem(selected == Tab.CHAT) { onSelect(Tab.CHAT) }
            sideRight.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
        }
    }
}

@Composable
private fun RowScope.SideItem(item: NavItem, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(92.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 30.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(38.dp).background(if (active) Color(0xFF4A3C0C) else NavyHigh, PxShape)
                .border(2.dp, if (active) Gold else PxLine, PxShape),
            contentAlignment = Alignment.Center,
        ) { PixelSprite(item.sprite, 26.dp, description = item.label) }
        Text(
            item.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center,
            color = if (active) Gold else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun RowScope.CenterItem(active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1.25f).height(92.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(66.dp).background(if (active) Gold else Color(0xFF2B2760), CutCornerShape(10.dp))
                .border(3.dp, if (active) Color(0xFFFFE9A6) else Gold, CutCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { PixelSprite(Sprites.ALFRED, 52.dp, description = "Alfred, asistente") }
        Text("ALFRED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gold, modifier = Modifier.padding(top = 2.dp))
    }
}

// --------------------------------------------------------------------------- Barra superior
/** Misma firma que TopAppBar, con título en fuente pixel y filete dorado. */
@Composable
fun PixelBar(
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(NavySurface).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            navigationIcon()
            Box(Modifier.weight(1f).padding(start = 8.dp)) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Gold) {
                    androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
                }
            }
            actions()
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(GoldDark))
    }
}

/** Fondo con una rejilla de puntos tenue, como papel cuadriculado de píxeles. */
fun Modifier.pixelGrid(): Modifier = this.drawBehind {
    val step = 24.dp.toPx()
    val dot = 2.dp.toPx()
    val c = Color(0x14FFFFFF)
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) { drawRect(c, Offset(x, y), Size(dot, dot)); x += step }
        y += step
    }
}
