package com.iatualcance.alfred.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Un aspecto completo de la app: paleta y estilo. [pokemon] activa las ilustraciones, la cabecera de Pokédex y la textura;
 * el resto de skins usan el estilo sobrio (iconos de línea, tipografía con serifa, sin sombras).
 * Para añadir una skin nueva basta con añadirla a [Skins.all].
 */
class Skin(
    val id: String, val label: String, val sub: String,
    val dark: Boolean, val pokemon: Boolean,
    val paper: Color, val surf: Color, val surfHigh: Color, val ink: Color, val muted: Color, val line: Color,
    val accent: Color, val onAccent: Color, val star: Color, val onInk: Color,
)

object Skins {
    val POKEMON = Skin(
        "pokemon", "Pokémon", "Game Boy Color, con tus ilustraciones", dark = false, pokemon = true,
        paper = Color(0xFFF4F5F9), surf = Color.White, surfHigh = Color(0xFFEEF0F6), ink = Color(0xFF1F1D2B), muted = Color(0xFF5A5870), line = Color(0xFFD7DBE8),
        accent = Color(0xFFE63B2E), onAccent = Color.White, star = Color(0xFFFFCB05), onInk = Color.White,
    )
    val MARFIL = Skin(
        "alfred_marfil", "Alfred · Marfil", "Sobrio, marfil y oro", dark = false, pokemon = false,
        paper = Color(0xFFFAF8F4), surf = Color.White, surfHigh = Color(0xFFF1EEE7), ink = Color(0xFF1C1B1F), muted = Color(0xFF77726A), line = Color(0xFFE8E3D9),
        accent = Color(0xFFA9854F), onAccent = Color.White, star = Color(0xFFE2C48A), onInk = Color.White,
    )
    val MEDIANOCHE = Skin(
        "alfred_medianoche", "Alfred · Medianoche", "Sobrio, modo oscuro", dark = true, pokemon = false,
        paper = Color(0xFF0E1320), surf = Color(0xFF161D2E), surfHigh = Color(0xFF1E2740), ink = Color(0xFFF1ECE2), muted = Color(0xFF8C95A9), line = Color(0xFF262F47),
        accent = Color(0xFFD6B26C), onAccent = Color(0xFF0E1320), star = Color(0xFF0E1320), onInk = Color(0xFF0E1320),
    )
    val PIZARRA = Skin(
        "alfred_pizarra", "Alfred · Pizarra", "Sobrio, blanco frío y azul", dark = false, pokemon = false,
        paper = Color(0xFFF5F7FB), surf = Color.White, surfHigh = Color(0xFFEAEFF7), ink = Color(0xFF0F172A), muted = Color(0xFF64748B), line = Color(0xFFE2E8F0),
        accent = Color(0xFF2F5BEA), onAccent = Color.White, star = Color(0xFF9DB4FF), onInk = Color.White,
    )

    val all = listOf(POKEMON, MARFIL, MEDIANOCHE, PIZARRA)
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
