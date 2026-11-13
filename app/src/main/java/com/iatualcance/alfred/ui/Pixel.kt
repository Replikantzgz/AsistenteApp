package com.iatualcance.alfred.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
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
val PxLine: Color get() = SoftLine
private val ShadowAmbient = Color(0x221B2A5E)
private val ShadowSpot = Color(0x331B2A5E)

/** Sombra suave + borde fino. El color de fondo lo pone quien lo usa (Surface, Column, etc.). */
fun Modifier.pxBorder(color: Color = SoftLine) = this
    .then(if (Skins.current.pokemon) Modifier.shadow(3.dp, PxShape, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot) else Modifier)
    .border(if (Skins.current.pokemon) 1.5.dp else 1.dp, color, PxShape)

/** Tarjeta blanca completa: sombra, fondo y borde. */
fun Modifier.card(shape: androidx.compose.ui.graphics.Shape = PxShape, borderColor: Color = SoftLine, elevation: Dp = 3.dp) = this
    .then(if (Skins.current.pokemon) Modifier.shadow(elevation, shape, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot) else Modifier)
    .background(Surf, shape)
    .border(if (Skins.current.pokemon) 1.5.dp else 1.dp, borderColor, shape)

/** Ilustración de la app (PNG con fondo transparente). */
@Composable
fun Art(@DrawableRes id: Int, size: Dp, description: String? = null, modifier: Modifier = Modifier) {
    Image(painterResource(id), description, modifier.size(size), contentScale = ContentScale.Fit)
}

/** Fondo con una textura de iconos de juego casi invisible. */
@Composable
fun Modifier.texture(): Modifier {
    if (!Skins.current.pokemon) return this.background(Paper)
    val tile = ImageBitmap.imageResource(R.drawable.bg_pattern)
    val brush = remember(tile) { ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)) }
    return this.drawBehind { drawRect(brush) }
}

// --------------------------------------------------------------------------- Barra inferior
private data class NavItem(val tab: Tab, val label: String, @DrawableRes val art: Int)

private val sideLeft = listOf(NavItem(Tab.MAIL, "CORREO", R.drawable.art_correo), NavItem(Tab.AGENDA, "AGENDA", R.drawable.art_agenda))
private val sideRight = listOf(NavItem(Tab.NOTES, "NOTAS", R.drawable.art_notas), NavItem(Tab.DRIVE, "DRIVE", R.drawable.art_drive))
private val BarShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/** Correo · Agenda · ASISTENTE (Pikachu, grande, centro) · Notas · Drive. Barra flotante con la pestaña activa subrayada. */
@Composable
fun PixelNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    if (!Skins.current.pokemon) { AlfredNavBar(selected, onSelect); return }
    Box(Modifier.fillMaxWidth().navigationBarsPadding().height(106.dp)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(78.dp)
            .shadow(16.dp, BarShape, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot).background(Color.White, BarShape))
        Row(Modifier.fillMaxWidth().height(106.dp)) {
            sideLeft.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
            CenterItem(selected == Tab.CHAT) { onSelect(Tab.CHAT) }
            sideRight.forEach { SideItem(it, selected == it.tab) { onSelect(it.tab) } }
        }
    }
}

@Composable
private fun Underline(active: Boolean) {
    Box(Modifier.padding(top = 3.dp).width(34.dp).height(3.dp).background(if (active) Red else Color.Transparent, RoundedCornerShape(2.dp)))
}

@Composable
private fun RowScope.SideItem(item: NavItem, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(106.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(top = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Art(item.art, 38.dp, item.label)
        Text(
            item.label, fontSize = 10.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center,
            color = if (active) Ink else MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 2.dp),
        )
        Underline(active)
    }
}

@Composable
private fun RowScope.CenterItem(active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1.25f).height(106.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Botón sencillo: círculo blanco con una línea negra fina (la pestaña activa se marca con el subrayado, como las demás).
        Box(Modifier.padding(top = 4.dp).size(76.dp).background(Color.White, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
            Art(R.drawable.art_pikachu_round, 68.dp, "Asistente")
        }
        Text("ASISTENTE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Ink, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 2.dp))
        Underline(active)
    }
}

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
    if (!Skins.current.pokemon) { AlfredBar(title, navigationIcon, actions, global); return }
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().shadow(8.dp, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot)
                .background(Brush.verticalGradient(listOf(Color(0xFFF4493D), Color(0xFFCF2C20)))),
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
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Color(0x55801510)))
        }
    }
}

/** Buscar y Ajustes: las mismas acciones a la derecha de la cabecera en todas las pestañas. */
@Composable
internal fun GlobalActions() {
    val ui = LocalUi.current
    androidx.compose.material3.IconButton(onClick = { ui.overlay = "search" }) {
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Search, "Buscar", tint = if (Skins.current.pokemon) Color.White else Ink)
    }
    androidx.compose.material3.IconButton(onClick = { ui.overlay = "settings" }) {
        if (Skins.current.pokemon) Art(R.drawable.art_ajustes, 34.dp, "Ajustes")
        else androidx.compose.material3.Icon(Icons.Outlined.Settings, "Ajustes", tint = Ink)
    }
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

// --------------------------------------------------------------------------- Skins sobrias (Alfred)
/** Estrella de cuatro puntas, el símbolo del asistente en las skins sobrias. */
@Composable
fun AlfredStar(size: Dp, color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val c = center; val r = this.size.minDimension / 2f; val k = r * 0.26f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(c.x, c.y - r); lineTo(c.x + k, c.y - k); lineTo(c.x + r, c.y); lineTo(c.x + k, c.y + k)
            lineTo(c.x, c.y + r); lineTo(c.x - k, c.y + k); lineTo(c.x - r, c.y); lineTo(c.x - k, c.y - k); close()
        }
        drawPath(path, color)
    }
}

/** Círculo oscuro con la estrella: botón central y avatar del asistente. */
@Composable
fun AlfredMark(size: Dp) {
    val k = Skins.current
    Box(Modifier.size(size).background(k.ink, CircleShape), contentAlignment = Alignment.Center) { AlfredStar(size * 0.46f, k.star) }
}

private data class LineNav(val tab: Tab, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun AlfredNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val k = Skins.current
    val left = listOf(LineNav(Tab.MAIL, "Correo", Icons.Outlined.MailOutline), LineNav(Tab.AGENDA, "Agenda", Icons.Outlined.CalendarMonth))
    val right = listOf(LineNav(Tab.NOTES, "Notas", Icons.Outlined.StickyNote2), LineNav(Tab.DRIVE, "Drive", Icons.Outlined.FolderOpen))
    Column(Modifier.fillMaxWidth().background(k.surf).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(k.line))
        Row(Modifier.fillMaxWidth().height(70.dp)) {
            @Composable fun RowScope.Item(n: LineNav) {
                val on = selected == n.tab
                Column(Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(n.tab) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    androidx.compose.material3.Icon(n.icon, n.label, Modifier.size(24.dp), tint = if (on) k.accent else k.muted)
                    Text(n.label, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) k.accent else k.muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
            left.forEach { Item(it) }
            val on = selected == Tab.CHAT
            Column(Modifier.weight(1.1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(Tab.CHAT) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.offset(y = (-14).dp)) {
                    Box(Modifier.size(62.dp).background(k.surf, CircleShape).padding(3.dp)) { AlfredMark(56.dp) }
                }
                Text("Alfred", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) k.accent else k.ink, modifier = Modifier.offset(y = (-12).dp))
                Box(Modifier.offset(y = (-10).dp).width(24.dp).height(2.dp).background(if (on) k.accent else Color.Transparent))
            }
            right.forEach { Item(it) }
        }
    }
}

/** Cabecera sobria: título con serifa sobre el fondo, acciones de línea fina y un filete inferior. */
@Composable
private fun AlfredBar(title: @Composable () -> Unit, navigationIcon: (@Composable () -> Unit)?, actions: @Composable RowScope.() -> Unit, global: Boolean) {
    val k = Skins.current
    Column(Modifier.fillMaxWidth().background(k.paper).statusBarsPadding()) {
        CompositionLocalProvider(LocalContentColor provides k.ink) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                navigationIcon?.invoke()
                if (navigationIcon == null) {
                    Box(Modifier.padding(start = 8.dp).size(30.dp).border(1.4.dp, k.accent, CircleShape), contentAlignment = Alignment.Center) { Text("A", color = k.accent, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) { ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = 0.sp)) { title() } }
                actions()
                if (global) GlobalActions()
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(k.line))
    }
}
