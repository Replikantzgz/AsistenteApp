package com.iatualcance.alfred.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Familia de estilo: decide cabecera, barra inferior, fondo y emblemas. */
enum class Look { POKEMON, SOBER, MARVEL, DC, RETRO, CYBER }

/**
 * Un aspecto completo de la app: paleta y estilo. POKEMON usa las ilustraciones propias, la cabecera de Pokédex y la textura;
 * el resto (SOBER, MARVEL, DC, RETRO, CYBER) usan cabecera y barra propias con iconos de línea o emblemas dibujados.
 * Para añadir una skin nueva basta con añadirla a [Skins.all].
 */
class Skin(
    val id: String, val label: String, val sub: String, val look: Look, val dark: Boolean,
    val paper: Color, val surf: Color, val surfHigh: Color, val ink: Color, val muted: Color, val line: Color,
    val accent: Color, val onAccent: Color, val star: Color, val onInk: Color,
    /** Segundo color de acento (azul Marvel, cian neón…). */
    val accent2: Color = accent,
    /** Burbuja del usuario. */
    val userBubble: Color = ink, val onUserBubble: Color = onInk,
    /** Barra inferior: fondo, color de las etiquetas inactivas y activas, y filete superior. */
    val barBg: Color = surf, val navIdle: Color = muted, val navActive: Color = accent, val barLine: Color = line, val barLineW: Float = 1f,
    /** Borde de tarjetas y campos, y su grosor. */
    val cardBorder: Color = line, val cardBorderW: Float = 1f,
    val shape: Shape = RoundedCornerShape(14.dp),
    val chipBg: Color = surf, val chipFg: Color = ink, val chipShape: Shape = RoundedCornerShape(50),
    val inputShape: Shape = RoundedCornerShape(26.dp),
) {
    val pokemon get() = look == Look.POKEMON
}

object Skins {
    val POKEMON = Skin(
        "pokemon", "Pokémon", "Game Boy Color, con tus ilustraciones", Look.POKEMON, dark = false,
        paper = Color(0xFFF4F5F9), surf = Color.White, surfHigh = Color(0xFFEEF0F6), ink = Color(0xFF1F1D2B), muted = Color(0xFF5A5870), line = Color(0xFFD7DBE8),
        accent = Color(0xFFE63B2E), onAccent = Color.White, star = Color(0xFFFFCB05), onInk = Color.White,
        cardBorderW = 1.5f, inputShape = RoundedCornerShape(14.dp),
    )
    val MARFIL = Skin(
        "alfred_marfil", "Alfred · Marfil", "Sobrio, marfil y oro", Look.SOBER, dark = false,
        paper = Color(0xFFFAF8F4), surf = Color.White, surfHigh = Color(0xFFF1EEE7), ink = Color(0xFF1C1B1F), muted = Color(0xFF77726A), line = Color(0xFFE8E3D9),
        accent = Color(0xFFA9854F), onAccent = Color.White, star = Color(0xFFE2C48A), onInk = Color.White,
    )
    val MEDIANOCHE = Skin(
        "alfred_medianoche", "Alfred · Medianoche", "Sobrio, modo oscuro", Look.SOBER, dark = true,
        paper = Color(0xFF0E1320), surf = Color(0xFF161D2E), surfHigh = Color(0xFF1E2740), ink = Color(0xFFF1ECE2), muted = Color(0xFF8C95A9), line = Color(0xFF262F47),
        accent = Color(0xFFD6B26C), onAccent = Color(0xFF0E1320), star = Color(0xFF0E1320), onInk = Color(0xFF0E1320),
    )
    val PIZARRA = Skin(
        "alfred_pizarra", "Alfred · Pizarra", "Sobrio, blanco frío y azul", Look.SOBER, dark = false,
        paper = Color(0xFFF5F7FB), surf = Color.White, surfHigh = Color(0xFFEAEFF7), ink = Color(0xFF0F172A), muted = Color(0xFF64748B), line = Color(0xFFE2E8F0),
        accent = Color(0xFF2F5BEA), onAccent = Color.White, star = Color(0xFF9DB4FF), onInk = Color.White,
    )
    val MARVEL = Skin(
        "marvel", "Marvel", "Rojo, tinta y viñetas · emblemas de los Vengadores", Look.MARVEL, dark = false,
        paper = Color(0xFFF4F1EA), surf = Color.White, surfHigh = Color(0xFFECE7DC), ink = Color(0xFF111111), muted = Color(0xFF55524B), line = Color(0xFF111111),
        accent = Color(0xFFE62429), onAccent = Color.White, star = Color(0xFFFFE146), onInk = Color.White, accent2 = Color(0xFF1C3F94),
        userBubble = Color(0xFF1C3F94), onUserBubble = Color.White,
        barBg = Color(0xFF0B0B0B), navIdle = Color.White, navActive = Color(0xFFFFE146), barLine = Color(0xFF111111), barLineW = 0f,
        cardBorder = Color(0xFF111111), cardBorderW = 2.5f, shape = RoundedCornerShape(8.dp),
        chipBg = Color(0xFFFFE146), chipFg = Color(0xFF111111), inputShape = RoundedCornerShape(10.dp),
    )
    val DC = Skin(
        "dc", "DC · Gotham", "Negro y amarillo · emblemas de la Liga de la Justicia", Look.DC, dark = true,
        paper = Color(0xFF0C0D10), surf = Color(0xFF16181D), surfHigh = Color(0xFF1D2027), ink = Color(0xFFECECEE), muted = Color(0xFF8A8D96), line = Color(0xFF2A2D35),
        accent = Color(0xFFF5C400), onAccent = Color(0xFF0C0D10), star = Color(0xFF0C0D10), onInk = Color(0xFF0C0D10),
        userBubble = Color(0xFFF5C400), onUserBubble = Color(0xFF0C0D10),
        barBg = Color(0xFF16181D), barLine = Color(0xFFF5C400), barLineW = 2f,
        shape = RoundedCornerShape(5.dp), chipShape = RoundedCornerShape(6.dp), inputShape = RoundedCornerShape(8.dp),
    )
    val RETRO = Skin(
        "retro80", "Retro 80s", "Atardecer sintético y neón", Look.RETRO, dark = true,
        paper = Color(0xFF12053A), surf = Color(0xFF22094F), surfHigh = Color(0xFF2E0F66), ink = Color.White, muted = Color(0xFFC9B8F0), line = Color(0xFF4A2488),
        accent = Color(0xFFFF3CAC), onAccent = Color.White, star = Color(0xFF2DE2E6), onInk = Color.White, accent2 = Color(0xFF2DE2E6),
        userBubble = Color(0xFFFF3CAC), onUserBubble = Color.White,
        barBg = Color(0xFF14052E), navIdle = Color(0xFFC9B8F0), navActive = Color(0xFFFF3CAC), barLine = Color(0xFF2DE2E6), barLineW = 1.5f,
        cardBorder = Color(0xFFFF3CAC), cardBorderW = 1.6f, shape = RoundedCornerShape(16.dp),
        chipBg = Color(0xFF22094F), chipFg = Color.White,
    )
    val CYBER = Skin(
        "cyberpunk", "Cyberpunk", "Neón y esquinas cortadas", Look.CYBER, dark = true,
        paper = Color(0xFF07070D), surf = Color(0xFF0F1018), surfHigh = Color(0xFF171925), ink = Color(0xFFE8E8F0), muted = Color(0xFF7C7F95), line = Color(0xFF232538),
        accent = Color(0xFFFCEE0A), onAccent = Color(0xFF07070D), star = Color(0xFFFCEE0A), onInk = Color(0xFF07070D), accent2 = Color(0xFF00F0FF),
        userBubble = Color(0xFFFCEE0A), onUserBubble = Color(0xFF07070D),
        barBg = Color(0xFF0B0C14), navIdle = Color(0xFF7C7F95), navActive = Color(0xFFFCEE0A), barLine = Color(0xFFFCEE0A), barLineW = 2f,
        cardBorder = Color(0xFF00F0FF), cardBorderW = 1.3f, shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
        chipBg = Color(0xFF0F1018), chipFg = Color(0xFF00F0FF), chipShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        inputShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
    )

    val all = listOf(POKEMON, MARFIL, MEDIANOCHE, PIZARRA, MARVEL, DC, RETRO, CYBER)
    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: POKEMON

    /** Skin activa. Es un estado de Compose: al cambiarla se redibuja toda la app al instante. */
    var current by mutableStateOf(POKEMON)
}

val Paper: Color get() = Skins.current.paper
val Surf: Color get() = Skins.current.surf
val SurfHigh: Color get() = Skins.current.surfHigh
val Ink: Color get() = Skins.current.ink
val Red: Color get() = Skins.current.accent        // color de acento de la skin activa
val SoftLine: Color get() = Skins.current.line
