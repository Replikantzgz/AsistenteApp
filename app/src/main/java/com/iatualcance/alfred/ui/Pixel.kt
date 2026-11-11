package com.iatualcance.alfred.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iatualcance.alfred.R

// Estética Pokémon de Game Boy Color: cuadros blancos con borde grueso, cabecera roja de Pokédex.

/** Forma de los cuadros: esquinas redondeadas pequeñas, como los cuadros de diálogo de los juegos. */
val PxShape = RoundedCornerShape(10.dp)
val PxLine = Color(0xFF2A2840)

/** Borde grueso oscuro (3 dp). */
fun Modifier.pxBorder(color: Color = Ink) = this.border(3.dp, color, PxShape)

/** Ilustración de la app (PNG con fondo transparente). */
@Composable
fun Art(@DrawableRes id: Int, size: Dp, description: String? = null, modifier: Modifier = Modifier) {
    Image(painterResource(id), description, modifier.size(size), contentScale = ContentScale.Fit)
}

// --------------------------------------------------------------------------- Barra inferior
private data class NavItem(val tab: Tab, val label: String, @DrawableRes val art: Int)

private val sideLeft = listOf(NavItem(Tab.MAIL, "CORREO", R.drawable.art_correo), NavItem(Tab.AGENDA, "AGENDA", R.drawable.art_agenda))
private val sideRight = listOf(NavItem(Tab.NOTES, "NOTAS", R.drawable.art_notas), NavItem(Tab.CONTACTS, "CONTACTOS", R.drawable.art_contactos))

/** Correo · Agenda · ASISTENTE (Pikachu, grande, centro) · Notas · Contactos. */
@Composable
fun PixelNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().height(100.dp)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp).background(Surf)) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Ink))
        }
        Row(Modifier.fillMaxWidth().height(100.dp)) {
            sideLeft.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
            CenterItem(selected == Tab.CHAT) { onSelect(Tab.CHAT) }
            sideRight.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
        }
    }
}

@Composable
private fun RowScope.SideItem(item: NavItem, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(100.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 34.dp, bottom = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(46.dp).background(if (active) Yellow else Color.Transparent, RoundedCornerShape(10.dp))
                .then(if (active) Modifier.border(2.dp, Ink, RoundedCornerShape(10.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) { Art(item.art, 40.dp, item.label) }
        Text(
            item.label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, textAlign = TextAlign.Center,
            color = if (active) Ink else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun RowScope.CenterItem(active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1.25f).height(100.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Botón con forma de Pokéball: anillo oscuro, anillo rojo y centro blanco (amarillo al estar activo).
        Box(Modifier.padding(top = 2.dp).size(80.dp).background(Ink, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(74.dp).background(Red, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(62.dp).background(if (active) Yellow else Color.White, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
                    Art(R.drawable.art_pikachu, 56.dp, "Asistente")
                }
            }
        }
        Text("ASISTENTE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 1.dp))
    }
}

// --------------------------------------------------------------------------- Barra superior
/** Cabecera roja de Pokédex con su lente azul y tres lucecitas. Misma firma que TopAppBar. */
@Composable
fun PixelBar(
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(Red).statusBarsPadding()) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                navigationIcon()
                Lens()
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) { ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() } }
                actions()
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(Ink))
    }
}

/** La lente azul con tres luces (roja, amarilla, verde) de la cabecera de un Pokédex. */
@Composable
private fun Lens() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(Color.White, CircleShape).border(3.dp, Ink, CircleShape).padding(3.dp)) {
            Box(Modifier.fillMaxWidth().height(26.dp).background(Color(0xFF47B5FF), CircleShape).border(2.dp, Color(0xFF1D6FD1), CircleShape)) {
                Box(Modifier.padding(start = 5.dp, top = 4.dp).size(7.dp).background(Color.White.copy(alpha = 0.85f), CircleShape))
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)) {
                listOf(Color(0xFFFF8A80), Yellow, Color(0xFF69F0AE)).forEach { Box(Modifier.size(7.dp).background(it, CircleShape).border(1.dp, Ink, CircleShape)) }
            }
        }
    }
}

/** Fondo con una rejilla de puntos tenue. */
fun Modifier.pixelGrid(): Modifier = this.drawBehind {
    val step = 24.dp.toPx()
    val dot = 2.dp.toPx()
    val c = Color(0x121F1D2B)
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) { drawRect(c, Offset(x, y), Size(dot, dot)); x += step }
        y += step
    }
}
