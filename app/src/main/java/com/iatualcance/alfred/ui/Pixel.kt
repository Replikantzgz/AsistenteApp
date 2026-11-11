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

private val sideLeft = listOf(NavItem(Tab.MAIL, "CORREO", R.drawable.art_correo), NavItem(Tab.DRIVE, "DRIVE", R.drawable.art_drive))
private val sideRight = listOf(NavItem(Tab.NOTES, "NOTAS", R.drawable.art_notas), NavItem(Tab.CONTACTS, "CONTACTOS", R.drawable.art_contactos))
private val BarShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/** Correo · Agenda · ASISTENTE (Pikachu, grande, centro) · Notas · Contactos. Barra flotante con la pestaña activa subrayada. */
@Composable
fun PixelNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
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
        // Botón con forma de Pokéball: aro oscuro, aro rojo y centro blanco (amarillo al estar activo).
        Box(Modifier.padding(top = 2.dp).size(82.dp).shadow(10.dp, CircleShape, ambientColor = ShadowAmbient, spotColor = ShadowSpot).background(Ink, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(77.dp).background(Brush.verticalGradient(listOf(Color(0xFFF2473B), Color(0xFFC92A1F))), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(64.dp).background(if (active) Yellow else Color.White, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
                    Art(R.drawable.art_pikachu, 57.dp, "Asistente")
                }
            }
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
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().shadow(8.dp, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot)
                .background(Brush.verticalGradient(listOf(Color(0xFFF4493D), Color(0xFFCF2C20)))),
        ) {
            Box(Modifier.fillMaxWidth().height(30.dp).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent))))
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    navigationIcon()
                    Lens()
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) { ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() } }
                    actions()
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Color(0x55801510)))
        }
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
