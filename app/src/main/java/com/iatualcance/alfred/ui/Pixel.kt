package com.iatualcance.alfred.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.iatualcance.alfred.R
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
val PxLine = Color(0xFF3A3566)

/** Borde duro de 2 dp, como los marcos de un juego de 16 bits. */
fun Modifier.pxBorder(color: Color = PxLine) = this.border(2.dp, color, PxShape)

// --------------------------------------------------------------------------- Iconos
/** Icono pixel-art (PNG de 32x32 ampliado sin suavizar, generado con tools/make_icons.py). */
@Composable
fun PixelIcon(@DrawableRes id: Int, size: Dp, description: String? = null, modifier: Modifier = Modifier) {
    Image(ImageBitmap.imageResource(id), description, modifier.size(size), filterQuality = FilterQuality.None)
}

// --------------------------------------------------------------------------- Barra inferior
private data class NavItem(val tab: Tab, val label: String, @DrawableRes val icon: Int)

private val sideLeft = listOf(NavItem(Tab.MAIL, "CORREO", R.drawable.px_mail), NavItem(Tab.AGENDA, "AGENDA", R.drawable.px_calendar))
private val sideRight = listOf(NavItem(Tab.NOTES, "NOTAS", R.drawable.px_notes), NavItem(Tab.CONTACTS, "CONTACTOS", R.drawable.px_contacts))

/**
 * Cinco iconos: Correo · Agenda · ALFRED (grande, centro) · Notas · Contactos.
 * El botón central es el asistente, que puede manejar todas las demás pantallas.
 */
@Composable
fun PixelNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().height(96.dp)) {
        // Barra
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(66.dp).background(Surf)) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Ink))
        }
        Row(Modifier.fillMaxWidth().height(96.dp)) {
            sideLeft.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
            CenterItem(selected == Tab.CHAT) { onSelect(Tab.CHAT) }
            sideRight.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
        }
    }
}

@Composable
private fun RowScope.SideItem(item: NavItem, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(96.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 32.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(42.dp).background(if (active) Color(0xFFFFE27A) else SurfHigh, PxShape)
                .border(2.dp, Ink, PxShape),
            contentAlignment = Alignment.Center,
        ) { PixelIcon(item.icon, 34.dp, item.label) }
        Text(
            item.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center,
            color = if (active) Ink else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun RowScope.CenterItem(active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1.25f).height(96.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(70.dp).background(Ink, CutCornerShape(10.dp))
                .border(4.dp, if (active) Color(0xFFFFE27A) else Color(0xFFF2B705), CutCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { PixelIcon(R.drawable.px_alfred, 60.dp, "Alfred, asistente") }
        Text("ALFRED", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 2.dp))
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
    Column(Modifier.fillMaxWidth().background(Surf).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            navigationIcon()
            Box(Modifier.weight(1f).padding(start = 8.dp)) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Ink) {
                    androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
                }
            }
            actions()
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFF2B705)))
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
    }
}

/** Fondo con una rejilla de puntos tenue, como papel cuadriculado de píxeles. */
fun Modifier.pixelGrid(): Modifier = this.drawBehind {
    val step = 24.dp.toPx()
    val dot = 2.dp.toPx()
    val c = Color(0x141B1740)
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) { drawRect(c, Offset(x, y), Size(dot, dot)); x += step }
        y += step
    }
}
