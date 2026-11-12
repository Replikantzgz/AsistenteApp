package com.iatualcance.alfred.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iatualcance.alfred.R

// Sistema visual: tema Pokémon claro con tarjetas blancas de sombra suave, cabecera roja de Pokédex y barra flotante.

val PxShape = RoundedCornerShape(14.dp)
val SoftLine = Color(0xFFD7DBE8)
val PxLine = SoftLine
private val ShadowAmbient = Color(0x221B2A5E)
private val ShadowSpot = Color(0x331B2A5E)

/** Sombra suave + borde fino. El color de fondo lo pone quien lo usa (Surface, Column, etc.). */
fun Modifier.pxBorder(color: Color = SoftLine) = this
    .shadow(3.dp, PxShape, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot)
    .border(1.5.dp, color, PxShape)

/** Tarjeta blanca completa: sombra, fondo y borde. */
fun Modifier.card(shape: androidx.compose.ui.graphics.Shape = PxShape, borderColor: Color = SoftLine, elevation: Dp = 3.dp) = this
    .shadow(elevation, shape, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot)
    .background(Color.White, shape)
    .border(1.5.dp, borderColor, shape)

/** Ilustración de la app (PNG con fondo transparente). */
@Composable
fun Art(@DrawableRes id: Int, size: Dp, description: String? = null, modifier: Modifier = Modifier) {
    Image(painterResource(id), description, modifier.size(size), contentScale = ContentScale.Fit)
}

/** Fondo con una textura de iconos de juego casi invisible. */
@Composable
fun Modifier.texture(): Modifier {
    val tile = ImageBitmap.imageResource(R.drawable.bg_pattern)
    val brush = remember(tile) { ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)) }
    return this.drawBehind { drawRect(brush) }
}

// --------------------------------------------------------------------------- Barra inferior
private data class NavItem(val tab: Tab, val label: String, @DrawableRes val art: Int)

private val sideLeft = listOf(NavItem(Tab.MAIL, "CORREO", R.drawable.art_correo), NavItem(Tab.AGENDA, "AGENDA", R.drawable.art_agenda))
private val sideRight = listOf(NavItem(Tab.NOTES, "NOTAS", R.drawable.art_notas), NavItem(Tab.DRIVE, "DRIVE", R.drawable.art_drive))
private val BarShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/** Correo · Agenda · ASISTENTE (Pikachu, grande, centro) · Notas · Drive. Va integrada en el marco rojo de la Pokédex. */
@Composable
fun PixelNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().height(100.dp).padding(horizontal = 6.dp)) {
        sideLeft.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
        CenterItem(selected == Tab.CHAT) { onSelect(Tab.CHAT) }
        sideRight.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
    }
}

@Composable
private fun Underline(active: Boolean) {
    Box(Modifier.padding(top = 3.dp).width(34.dp).height(3.dp).background(if (active) Color.White else Color.Transparent, RoundedCornerShape(2.dp)))
}

@Composable
private fun RowScope.SideItem(item: NavItem, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(100.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cada icono va sobre una placa blanca redonda para que se lea sobre el rojo.
        Box(Modifier.size(46.dp).background(Color.White, CircleShape).border(if (active) 2.5.dp else 1.5.dp, if (active) Ink else Color(0xFF8E1B12), CircleShape), contentAlignment = Alignment.Center) {
            Art(item.art, 36.dp, item.label)
        }
        Text(
            item.label, fontSize = 10.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center,
            color = if (active) Color.White else Color(0xFFFFD9D4), letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp),
        )
        Underline(active)
    }
}

@Composable
private fun RowScope.CenterItem(active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1.2f).height(100.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Botón sencillo: círculo blanco con una línea negra fina.
        Box(Modifier.size(66.dp).background(Color.White, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
            Art(R.drawable.art_pikachu_round, 58.dp, "Asistente")
        }
        Text("ASISTENTE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp))
        Underline(active)
    }
}

// --------------------------------------------------------------------------- Marco de Pokédex
val PokedexRed = Color(0xFFD93428)
val PokedexEdge = Color(0xFF8E1B12)
private val ScreenShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)

/** Carcasa roja: pinta el degradado superior (barra de estado + cabecera) y el rojo liso del resto. */
@Composable
fun Modifier.pokedexBody(): Modifier {
    val top = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(this).toFloat() + 64.dp.toPx() }
    return this.drawBehind {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF4493D), PokedexRed), 0f, top))
        drawRect(PokedexRed, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(size.width, size.height - top))
    }
}

/** La pantalla blanca de la Pokédex: rails rojos a izquierda y derecha, esquinas inferiores redondeadas y filo oscuro. */
@Composable
fun Modifier.pokedexScreen(): Modifier = this
    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
    .padding(horizontal = 10.dp)
    .clip(ScreenShape).background(Paper).texture().border(2.dp, PokedexEdge, ScreenShape)

// --------------------------------------------------------------------------- Barra superior
/** Cabecera roja de Pokédex con degradado, brillo, lente azul y tres luces. Misma firma que TopAppBar. */
@Composable
fun PixelBar(
    title: @Composable () -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    /** Buscar y Ajustes, iguales en todas las pestañas. Se ocultan en pantallas con flecha de volver. */
    global: Boolean = navigationIcon == null,
) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Box(
            Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth().height(30.dp).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent))))
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    navigationIcon?.invoke()
                    Lens()
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) { ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp)) { title() } }
                    actions()
                    if (global) GlobalActions()
                }
            }
            
        }
    }
}

/** Buscar y Ajustes: las mismas acciones a la derecha de la cabecera en todas las pestañas. */
@Composable
private fun GlobalActions() {
    val ui = LocalUi.current
    androidx.compose.material3.IconButton(onClick = { ui.overlay = "search" }) {
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Search, "Buscar", tint = Color.White)
    }
    androidx.compose.material3.IconButton(onClick = { ui.overlay = "settings" }) { Art(R.drawable.art_ajustes, 34.dp, "Ajustes") }
}

/** Lente azul brillante con tres luces (roja, amarilla, verde), como la cabecera de un Pokédex. */
@Composable
private fun Lens() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).background(Color.White, CircleShape).padding(3.dp).background(Ink, CircleShape).padding(2.dp).drawBehind {
                drawCircle(Brush.radialGradient(listOf(Color(0xFFB8E6FF), Color(0xFF2E8BEA), Color(0xFF0B3D99)), center = Offset(size.width * 0.35f, size.height * 0.30f), radius = size.width * 0.85f))
                drawCircle(Color.White.copy(alpha = 0.85f), radius = size.width * 0.13f, center = Offset(size.width * 0.33f, size.height * 0.28f))
            },
        )
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 22.dp)) {
            listOf(Color(0xFFFF6B5F), Color(0xFFFFD83D), Color(0xFF4ADE80)).forEach { Box(Modifier.size(9.dp).background(it, CircleShape).border(1.dp, Color(0x66000000), CircleShape)) }
        }
    }
}
