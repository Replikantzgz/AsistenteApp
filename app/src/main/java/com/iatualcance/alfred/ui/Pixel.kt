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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
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

val PxShape: androidx.compose.ui.graphics.Shape get() = Skins.current.shape
val PxLine: Color get() = SoftLine
private val ShadowAmbient = Color(0x221B2A5E)
private val ShadowSpot = Color(0x331B2A5E)

/** Borde y sombra de la skin: Pokémon, sombra suave; Marvel, sombra dura de viñeta; el resto, solo borde. */
private fun Modifier.skinShadow(shape: androidx.compose.ui.graphics.Shape, elevation: Dp): Modifier {
    val k = Skins.current
    return when (k.look) {
        Look.POKEMON -> this.shadow(elevation, shape, clip = false, ambientColor = ShadowAmbient, spotColor = ShadowSpot)
        Look.MARVEL -> this.drawBehind {
            val o = shape.createOutline(size, layoutDirection, this)
            val c = Color(0xFF111111)
            translate(4.dp.toPx(), 4.dp.toPx()) {
                when (o) {
                    is androidx.compose.ui.graphics.Outline.Rectangle -> drawRect(c, o.rect.topLeft, o.rect.size)
                    is androidx.compose.ui.graphics.Outline.Rounded -> drawPath(Path().apply { addRoundRect(o.roundRect) }, c)
                    is androidx.compose.ui.graphics.Outline.Generic -> drawPath(o.path, c)
                }
            }
        }
        else -> this
    }
}

/** Sombra + borde. El color de fondo lo pone quien lo usa (Surface, Column, etc.). */
fun Modifier.pxBorder(color: Color = Skins.current.cardBorder) = this
    .skinShadow(PxShape, 3.dp)
    .border(Skins.current.cardBorderW.dp, color, PxShape)

/** Tarjeta completa: sombra, fondo y borde de la skin. */
fun Modifier.card(shape: androidx.compose.ui.graphics.Shape = PxShape, borderColor: Color = Skins.current.cardBorder, elevation: Dp = 3.dp) = this
    .skinShadow(shape, elevation)
    .background(Surf, shape)
    .border(Skins.current.cardBorderW.dp, borderColor, shape)

/** Ilustración de la app. En Pokémon, tus PNG; en las demás skins, el emblema o icono equivalente. */
@Composable
fun Art(@DrawableRes id: Int, size: Dp, description: String? = null, modifier: Modifier = Modifier) {
    if (!Skins.current.pokemon) { SkinArt(id, size, modifier); return }
    Image(painterResource(id), description, modifier.size(size), contentScale = ContentScale.Fit)
}

private fun emblemFor(look: Look, tab: Tab): Emblem? = when (look) {
    Look.MARVEL -> when (tab) { Tab.MAIL -> Emblem.BOW; Tab.AGENDA -> Emblem.HOURGLASS; Tab.NOTES -> Emblem.HAMMER; Tab.DRIVE -> Emblem.SHIELD; else -> null }
    Look.DC -> when (tab) { Tab.MAIL -> Emblem.BOLT; Tab.AGENDA -> Emblem.RING; Tab.NOTES -> Emblem.TRIDENT; Tab.DRIVE -> Emblem.SUPER; else -> null }
    Look.CYBER -> when (tab) { Tab.MAIL -> Emblem.PACKET; Tab.AGENDA -> Emblem.CLOCK; Tab.NOTES -> Emblem.CHIP; Tab.DRIVE -> Emblem.CUBE; else -> null }
    else -> null
}

@Composable
private fun SkinArt(@DrawableRes id: Int, size: Dp, modifier: Modifier) {
    val k = Skins.current
    val tab = when (id) { R.drawable.art_correo -> Tab.MAIL; R.drawable.art_agenda -> Tab.AGENDA; R.drawable.art_notas -> Tab.NOTES; R.drawable.art_drive -> Tab.DRIVE; else -> null }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            tab == null && (id == R.drawable.art_pikachu || id == R.drawable.art_pikachu_round) -> AlfredMark(size * 0.8f)
            tab != null && emblemFor(k.look, tab) != null -> EmblemIcon(emblemFor(k.look, tab)!!, size * 0.8f, k.paper)
            else -> androidx.compose.material3.Icon(
                when (tab) { Tab.MAIL -> Icons.Outlined.MailOutline; Tab.AGENDA -> Icons.Outlined.CalendarMonth; Tab.NOTES -> Icons.Outlined.StickyNote2; Tab.DRIVE -> Icons.Outlined.FolderOpen
                    else -> if (id == R.drawable.art_ajustes) Icons.Outlined.Settings else Icons.Outlined.PersonOutline },
                null, Modifier.size(size * 0.6f), tint = k.accent,
            )
        }
    }
}

/** Fondo de la skin: Pokémon, textura de iconos; Marvel, puntos de cómic; DC, haz de luz; Retro, atardecer sintético; Cyberpunk, rejilla. */
@Composable
fun Modifier.texture(): Modifier {
    val k = Skins.current
    if (k.pokemon) {
        val tile = ImageBitmap.imageResource(R.drawable.bg_pattern)
        val brush = remember(tile) { ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)) }
        return this.drawBehind { drawRect(brush) }
    }
    return this.background(k.paper).drawBehind {
        val w = size.width; val h = size.height
        when (k.look) {
            Look.MARVEL -> {
                val step = 9.dp.toPx(); val reach = 260.dp.toPx(); var y = 0f
                while (y < reach) { var x = w - reach; while (x < w) {
                    val r = (1f - kotlin.math.hypot(x - w, y) / reach).coerceAtLeast(0f) * 2.6.dp.toPx()
                    if (r > .3.dp.toPx()) drawCircle(Color(0xFFE9CFCB), r, Offset(x, y)); x += step }; y += step }
            }
            Look.DC -> {
                val p = Path().apply { moveTo(w * .72f, 0f); lineTo(w, 0f); lineTo(w, h * .38f); lineTo(w * .36f, h * .38f); close() }
                drawPath(p, Color(0x0FF5C400))
            }
            Look.CYBER -> {
                val g = 26.dp.toPx(); var x = 0f; while (x < w) { drawLine(Color(0xFF0F1020), Offset(x, 0f), Offset(x, h), 1f); x += g }
                var y = 0f; while (y < h) { drawLine(Color(0xFF0F1020), Offset(0f, y), Offset(w, y), 1f); y += g }
            }
            Look.RETRO -> {
                drawRect(Brush.verticalGradient(0f to Color(0xFF12053A), .6f to Color(0xFF4B1478), 1f to Color(0xFF8A2A9E)))
                val cx = w / 2f; val sy = h * .78f; val sr = 92.dp.toPx(); val pink = Color(0xFFFF3CAC)
                val sun = Brush.verticalGradient(listOf(Color(0xFFFFD25A), Color(0xFFFF5FA2)), startY = sy - sr, endY = sy + sr)
                val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx, sy), sr)) }
                val band = sr * 2 / 22f
                for (i in 0 until 22) {
                    if (i > 11 && i % 2 == 0) continue
                    val top = sy - sr + i * band
                    clipPath(circle) { drawRect(sun, Offset(cx - sr, top), Size(sr * 2, band)) }
                }
                val horizon = sy + 10.dp.toPx()
                drawRect(Color(0xFF1A0838), Offset(0f, horizon), Size(w, h - horizon))
                for (i in -12..12) drawLine(pink.copy(alpha = .7f), Offset(cx + i * 8.dp.toPx(), horizon), Offset(cx + i * 80.dp.toPx(), h), 1f)
                for (j in 1..8) { val yy = horizon + (j * j) * 3.2.dp.toPx(); if (yy < h) drawLine(pink.copy(alpha = .7f), Offset(0f, yy), Offset(w, yy), 1f) }
            }
            else -> {}
        }
    }
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
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Search, "Buscar", tint = if (Skins.current.pokemon) Color.White else LocalContentColor.current)
    }
    androidx.compose.material3.IconButton(onClick = { ui.overlay = "settings" }) {
        if (Skins.current.pokemon) Art(R.drawable.art_ajustes, 34.dp, "Ajustes")
        else androidx.compose.material3.Icon(Icons.Outlined.Settings, "Ajustes", tint = LocalContentColor.current)
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

// --------------------------------------------------------------------------- Skins no Pokémon
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

private val CutBadge = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

/** Distintivo del asistente (botón central y avatar): cada skin tiene el suyo. */
@Composable
fun AlfredMark(size: Dp) {
    val k = Skins.current
    val bw = (size.value * 0.055f).coerceAtLeast(1.5f).dp
    when (k.look) {
        Look.MARVEL -> Box(Modifier.size(size).background(Color(0xFF0B0B0B), CircleShape).border(bw, Color.White, CircleShape), Alignment.Center) { EmblemIcon(Emblem.REACTOR, size * 0.66f) }
        Look.DC -> Box(Modifier.size(size).background(k.paper, CircleShape).border(bw, k.accent, CircleShape), Alignment.Center) { EmblemIcon(Emblem.BAT, size * 0.7f, k.paper) }
        Look.CYBER -> Box(Modifier.size(size).background(k.paper, CutBadge).border(bw, k.accent, CutBadge), Alignment.Center) { EmblemIcon(Emblem.EYE, size * 0.66f) }
        Look.RETRO -> Box(Modifier.size(size).background(k.barBg, CircleShape).border(bw, k.accent, CircleShape), Alignment.Center) { AlfredStar(size * 0.46f, k.accent2) }
        else -> Box(Modifier.size(size).background(k.ink, CircleShape), Alignment.Center) { AlfredStar(size * 0.46f, k.star) }
    }
}

private data class LineNav(val tab: Tab, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/** Placa que enmarca el emblema de una pestaña. */
@Composable
private fun Badge(k: Skin, active: Boolean, content: @Composable () -> Unit) {
    when (k.look) {
        Look.MARVEL -> Box(Modifier.size(44.dp).background(Color.White, CircleShape).border(2.4.dp, if (active) k.star else k.accent, CircleShape), Alignment.Center) { content() }
        Look.DC -> Box(Modifier.size(44.dp).background(k.surfHigh, CircleShape).border(if (active) 2.dp else 1.4.dp, if (active) k.accent else Color(0xFF3A3E49), CircleShape), Alignment.Center) { content() }
        else -> Box(Modifier.size(44.dp).background(Color(0xFF10121C), CutBadge).border(1.4.dp, if (active) k.accent else Color(0xFF1E5760), CutBadge), Alignment.Center) { content() }
    }
}

@Composable
private fun AlfredNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val k = Skins.current
    val emblems = k.look == Look.MARVEL || k.look == Look.DC || k.look == Look.CYBER
    val upper = k.look == Look.MARVEL || k.look == Look.CYBER
    val mono = k.look == Look.CYBER
    val left = listOf(LineNav(Tab.MAIL, "Correo", Icons.Outlined.MailOutline), LineNav(Tab.AGENDA, "Agenda", Icons.Outlined.CalendarMonth))
    val right = listOf(LineNav(Tab.NOTES, "Notas", Icons.Outlined.StickyNote2), LineNav(Tab.DRIVE, "Drive", Icons.Outlined.FolderOpen))
    val h = if (emblems) 80.dp else 70.dp
    fun label(s: String) = if (upper) s.uppercase() else s
    Column(Modifier.fillMaxWidth().background(k.barBg).navigationBarsPadding()) {
        if (k.barLineW > 0f) Box(Modifier.fillMaxWidth().height(k.barLineW.dp).background(k.barLine))
        Row(Modifier.fillMaxWidth().height(h)) {
            @Composable fun RowScope.Item(n: LineNav) {
                val on = selected == n.tab
                Column(Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(n.tab) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val em = emblemFor(k.look, n.tab)
                    if (em != null) Badge(k, on) { EmblemIcon(em, 28.dp, k.paper) }
                    else androidx.compose.material3.Icon(n.icon, n.label, Modifier.size(24.dp), tint = if (on) k.navActive else if (k.look == Look.RETRO) k.accent2 else k.navIdle)
                    Text(
                        label(n.label), fontSize = if (mono) 9.5.sp else 10.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
                        fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                        color = if (on) k.navActive else k.navIdle, modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            left.forEach { Item(it) }
            val on = selected == Tab.CHAT
            Column(Modifier.weight(1.1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(Tab.CHAT) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.offset(y = (-12).dp)) { Box(Modifier.size(62.dp).background(k.barBg, CircleShape).padding(3.dp)) { AlfredMark(56.dp) } }
                Text(label("Alfred"), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (on) k.navActive else if (k.look == Look.SOBER) k.ink else k.navActive.takeIf { emblems } ?: k.navIdle, modifier = Modifier.offset(y = (-10).dp),
                    fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else null)
                Box(Modifier.offset(y = (-8).dp).width(24.dp).height(2.dp).background(if (on) k.navActive else Color.Transparent))
            }
            right.forEach { Item(it) }
        }
    }
}

/** Cabecera de las skins no Pokémon: cada una con su estilo (sobria, cómic, Gotham, neón, terminal). */
@Composable
private fun AlfredBar(title: @Composable () -> Unit, navigationIcon: (@Composable () -> Unit)?, actions: @Composable RowScope.() -> Unit, global: Boolean) {
    val k = Skins.current
    val bg = if (k.look == Look.MARVEL) k.accent else if (k.look == Look.RETRO) Color.Transparent else k.paper
    val fg = when (k.look) { Look.MARVEL -> Color.White; Look.CYBER -> k.accent; else -> k.ink }
    Column(Modifier.fillMaxWidth().background(bg).statusBarsPadding()) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Row(Modifier.fillMaxWidth().height(if (k.look == Look.MARVEL) 62.dp else 60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                navigationIcon?.invoke()
                if (navigationIcon == null) when (k.look) {
                    Look.DC -> { Spacer(Modifier.width(6.dp)); EmblemIcon(Emblem.BAT, 44.dp, k.paper); Spacer(Modifier.width(8.dp)) }
                    Look.SOBER -> {
                        Box(Modifier.padding(start = 8.dp).size(30.dp).border(1.4.dp, k.accent, CircleShape), contentAlignment = Alignment.Center) { Text("A", color = k.accent, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                        Spacer(Modifier.width(10.dp))
                    }
                    else -> Spacer(Modifier.width(8.dp))
                }
                Box(Modifier.weight(1f)) {
                    when (k.look) {
                        Look.MARVEL -> ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, letterSpacing = 0.5.sp)) { title() }
                        Look.RETRO -> ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, shadow = androidx.compose.ui.graphics.Shadow(k.accent, Offset(3f, 3f), 8f))) { title() }
                        Look.CYBER -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("[ ", color = fg, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, letterSpacing = 0.sp)) { title() }
                            Text(" ]", color = fg, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        else -> ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = 0.sp)) { title() }
                    }
                }
                actions()
                if (global) GlobalActions()
            }
        }
        when (k.look) {
            Look.MARVEL -> Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF111111)))
            Look.DC -> Box(Modifier.fillMaxWidth().height(3.dp).background(k.accent))
            Look.RETRO -> Box(Modifier.fillMaxWidth().height(1.5.dp).background(k.accent2))
            Look.CYBER -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Box(Modifier.width(40.dp).height(3.dp).background(k.accent)); Box(Modifier.weight(1f).height(1.dp).align(Alignment.Bottom).background(k.accent2)) }
            else -> Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(k.line))
        }
    }
}
