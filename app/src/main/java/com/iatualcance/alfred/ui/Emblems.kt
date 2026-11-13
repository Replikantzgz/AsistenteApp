package com.iatualcance.alfred.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Emblemas dibujados con vectores para las skins temáticas. Son siluetas sencillas de objetos y símbolos clásicos
 * (arco, reloj de arena, martillo, escudo, reactor, rayo, anillo, tridente, ojo, chip…), no ilustraciones de personajes.
 */
enum class Emblem { BOW, HOURGLASS, HAMMER, SHIELD, REACTOR, BAT, BOLT, RING, TRIDENT, SUPER, EYE, PACKET, CLOCK, CHIP, CUBE }

private typealias Pt = Pair<Float, Float>

private fun DrawScope.poly(pts: List<Pt>, cx: Float, cy: Float, r: Float, fill: Color? = null, stroke: Color? = null, sw: Float = 2f) {
    val p = Path().apply { pts.forEachIndexed { i, (x, y) -> val px = cx + x * r; val py = cy + y * r; if (i == 0) moveTo(px, py) else lineTo(px, py) }; close() }
    fill?.let { drawPath(p, it) }
    stroke?.let { drawPath(p, it, style = Stroke(sw, join = StrokeJoin.Round)) }
}

private fun rot(pts: List<Pt>, a: Double) = pts.map { (x, y) -> Pair((x * cos(a) - y * sin(a)).toFloat(), (x * sin(a) + y * cos(a)).toFloat()) }

private fun DrawScope.star5(cx: Float, cy: Float, r: Float, col: Color) {
    val pts = (0 until 10).map { i -> val a = Math.toRadians(-90.0 + i * 36); val rr = if (i % 2 == 0) r else r * .4f; Pair(cx + (cos(a) * rr).toFloat(), cy + (sin(a) * rr).toFloat()) }
    val p = Path().apply { pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }; close() }
    drawPath(p, col)
}

private fun DrawScope.line(a: Offset, b: Offset, col: Color, w: Float) = drawLine(col, a, b, w, StrokeCap.Round)

private val CYAN = Color(0xFF00F0FF)
private val NEON_YELLOW = Color(0xFFFCEE0A)

private fun DrawScope.drawEmblem(e: Emblem, cx: Float, cy: Float, r: Float, hole: Color) {
    val sw = r * .14f
    when (e) {
        Emblem.SHIELD -> {
            val red = Color(0xFFD22027)
            drawCircle(red, r, Offset(cx, cy)); drawCircle(Color.White, r * .78f, Offset(cx, cy)); drawCircle(red, r * .56f, Offset(cx, cy))
            drawCircle(Color(0xFF1C3F94), r * .36f, Offset(cx, cy)); star5(cx, cy, r * .34f, Color.White)
        }
        Emblem.HOURGLASS -> {
            val col = Color(0xFFE62429)
            poly(listOf(-.62f to -.85f, .62f to -.85f, .06f to -.06f), cx, cy, r, fill = col)
            poly(listOf(-.62f to .85f, .62f to .85f, .06f to .06f), cx, cy, r, fill = col)
            poly(listOf(-.62f to -.85f, .62f to -.85f, .62f to -.7f, -.62f to -.7f), cx, cy, r, fill = col)
            poly(listOf(-.62f to .85f, .62f to .85f, .62f to .7f, -.62f to .7f), cx, cy, r, fill = col)
        }
        Emblem.BOW -> {
            val pur = Color(0xFF7B2FA8)
            drawArc(pur, 90f, 180f, false, Offset(cx - r, cy - r * .95f), Size(r * 1.35f, r * 1.9f), style = Stroke(sw * 1.3f, cap = StrokeCap.Round))
            line(Offset(cx - r * .325f, cy - r * .95f), Offset(cx - r * .325f, cy + r * .95f), Color(0xFF3C3C3C), sw * .5f)
            line(Offset(cx - r * .9f, cy), Offset(cx + r * .95f, cy), Color(0xFF282828), sw * .8f)
            poly(listOf(1f to 0f, .62f to -.22f, .62f to .22f), cx, cy, r, fill = pur)
            poly(listOf(-.9f to 0f, -1.1f to -.2f, -.8f to -.05f), cx, cy, r, fill = pur)
            poly(listOf(-.9f to 0f, -1.1f to .2f, -.8f to .05f), cx, cy, r, fill = pur)
        }
        Emblem.HAMMER -> {
            val a = Math.toRadians(-35.0)
            poly(rot(listOf(-.13f to -.28f, .13f to -.28f, .13f to .98f, -.13f to .98f), a), cx, cy, r, fill = Color(0xFF7A4B22), stroke = Color(0xFF1E1E1E), sw = sw * .5f)
            poly(rot(listOf(-.78f to -.92f, .78f to -.92f, .78f to -.28f, -.78f to -.28f), a), cx, cy, r, fill = Color(0xFFC4CAD6), stroke = Color(0xFF1E1E1E), sw = sw * .6f)
            poly(rot(listOf(-.78f to -.7f, .78f to -.7f, .78f to -.5f, -.78f to -.5f), a), cx, cy, r, fill = Color(0xFF8E96A6))
        }
        Emblem.REACTOR -> {
            drawCircle(CYAN, r, Offset(cx, cy), style = Stroke(sw * 1.2f)); drawCircle(CYAN, r * .62f, Offset(cx, cy), style = Stroke(sw * .8f))
            poly(listOf(0f to -.5f, .45f to .3f, -.45f to .3f), cx, cy, r, stroke = CYAN, sw = sw)
            drawCircle(Color.White, r * .15f, Offset(cx, cy + r * .06f))
            for (d in 0 until 360 step 45) { val a = Math.toRadians(d.toDouble()); line(Offset(cx + (cos(a) * r * .7).toFloat(), cy + (sin(a) * r * .7).toFloat()), Offset(cx + (cos(a) * r * .92).toFloat(), cy + (sin(a) * r * .92).toFloat()), CYAN, sw) }
        }
        Emblem.BAT -> {
            drawOval(Color(0xFFF5C400), Offset(cx - r, cy - r * .62f), Size(r * 2, r * 1.24f))
            poly(listOf(-.74f to .05f, -.46f to -.24f, -.24f to -.08f, -.1f to -.34f, -.04f to -.2f, .04f to -.2f, .1f to -.34f, .24f to -.08f, .46f to -.24f, .74f to .05f, .46f to .3f, .12f to .14f, 0f to .34f, -.12f to .14f, -.46f to .3f), cx, cy, r, fill = hole)
        }
        Emblem.BOLT -> {
            drawCircle(Color(0xFFD7262E), r, Offset(cx, cy))
            poly(listOf(.16f to -.82f, -.42f to .08f, -.04f to .08f, -.18f to .82f, .44f to -.16f, .06f to -.16f), cx, cy, r, fill = Color(0xFFFFD60A))
        }
        Emblem.RING -> {
            val g = Color(0xFF2BE07A)
            drawCircle(g, r * .9f, Offset(cx, cy), style = Stroke(sw * 2.2f)); drawCircle(g, r * .5f, Offset(cx, cy), style = Stroke(sw))
            poly(listOf(0f to -.3f, .3f to 0f, 0f to .3f, -.3f to 0f), cx, cy, r, fill = g)
        }
        Emblem.TRIDENT -> {
            val o = Color(0xFFFF9A1F)
            line(Offset(cx, cy - r * .7f), Offset(cx, cy + r * .95f), o, sw * 1.5f)
            line(Offset(cx - r * .55f, cy - r * .25f), Offset(cx - r * .55f, cy - r * .7f), o, sw * 1.3f); line(Offset(cx + r * .55f, cy - r * .25f), Offset(cx + r * .55f, cy - r * .7f), o, sw * 1.3f)
            drawArc(o, 0f, 180f, false, Offset(cx - r * .55f, cy - r * .7f), Size(r * 1.1f, r * .9f), style = Stroke(sw * 1.5f))
            for (dx in listOf(-.55f, 0f, .55f)) poly(listOf(dx to (if (dx == 0f) -1f else -.98f), (dx - .17f) to -.7f, (dx + .17f) to -.7f), cx, cy, r, fill = o)
        }
        Emblem.SUPER -> {
            poly(listOf(-.85f to -.8f, .85f to -.8f, .85f to .12f, 0f to .98f, -.85f to .12f), cx, cy, r, fill = Color(0xFFD7262E), stroke = Color(0xFFFFD60A), sw = sw)
            val s = Path().apply {
                moveTo(cx + r * .4f, cy - r * .4f); cubicTo(cx + r * .2f, cy - r * .75f, cx - r * .5f, cy - r * .6f, cx - r * .35f, cy - r * .2f)
                cubicTo(cx - r * .2f, cy + r * .1f, cx + r * .45f, cy, cx + r * .4f, cy + r * .4f); cubicTo(cx + r * .35f, cy + r * .8f, cx - r * .3f, cy + r * .75f, cx - r * .45f, cy + r * .4f)
            }
            drawPath(s, Color(0xFFFFD60A), style = Stroke(sw * 1.5f, cap = StrokeCap.Round))
        }
        Emblem.EYE -> {
            poly(listOf(-1f to 0f, -.5f to -.55f, .5f to -.55f, 1f to 0f, .5f to .55f, -.5f to .55f), cx, cy, r, stroke = CYAN, sw = sw * 1.2f)
            drawCircle(NEON_YELLOW, r * .38f, Offset(cx, cy)); drawCircle(Color(0xFF07070D), r * .14f, Offset(cx, cy))
        }
        Emblem.PACKET -> {
            drawRect(CYAN, Offset(cx - r, cy - r * .7f), Size(r * 2, r * 1.4f), style = Stroke(sw * 1.2f))
            line(Offset(cx - r, cy - r * .7f), Offset(cx, cy + r * .05f), CYAN, sw); line(Offset(cx, cy + r * .05f), Offset(cx + r, cy - r * .7f), CYAN, sw)
            line(Offset(cx - r * .55f, cy + r * .4f), Offset(cx + r * .3f, cy + r * .4f), Color(0xFFFF2A6D), sw * .8f)
        }
        Emblem.CLOCK -> {
            val pts = (0 until 6).map { i -> val a = Math.toRadians(60.0 * i + 30); Pair(cos(a).toFloat(), sin(a).toFloat()) }
            poly(pts, cx, cy, r, stroke = CYAN, sw = sw * 1.2f)
            line(Offset(cx, cy), Offset(cx, cy - r * .6f), NEON_YELLOW, sw * 1.2f); line(Offset(cx, cy), Offset(cx + r * .45f, cy + r * .2f), NEON_YELLOW, sw * 1.2f)
        }
        Emblem.CHIP -> {
            drawRect(CYAN, Offset(cx - r * .62f, cy - r * .62f), Size(r * 1.24f, r * 1.24f), style = Stroke(sw * 1.2f))
            drawRect(NEON_YELLOW, Offset(cx - r * .26f, cy - r * .26f), Size(r * .52f, r * .52f))
            for (k in listOf(-.35f, 0f, .35f)) {
                line(Offset(cx + k * r, cy - r * .62f), Offset(cx + k * r, cy - r), CYAN, sw); line(Offset(cx + k * r, cy + r * .62f), Offset(cx + k * r, cy + r), CYAN, sw)
                line(Offset(cx - r * .62f, cy + k * r), Offset(cx - r, cy + k * r), CYAN, sw); line(Offset(cx + r * .62f, cy + k * r), Offset(cx + r, cy + k * r), CYAN, sw)
            }
        }
        Emblem.CUBE -> {
            poly(listOf(0f to -1f, .87f to -.5f, 0f to 0f, -.87f to -.5f), cx, cy, r, stroke = CYAN, sw = sw)
            poly(listOf(-.87f to -.5f, 0f to 0f, 0f to 1f, -.87f to .5f), cx, cy, r, stroke = CYAN, sw = sw)
            poly(listOf(0f to 0f, .87f to -.5f, .87f to .5f, 0f to 1f), cx, cy, r, stroke = CYAN, sw = sw)
            line(Offset(cx + r * .3f, cy + r * .35f), Offset(cx + r * .6f, cy + r * .2f), NEON_YELLOW, sw)
        }
    }
}

/** Emblema [e] dibujado en un cuadrado de [size]. [hole] es el color del fondo (para recortes como el del murciélago). */
@Composable
fun EmblemIcon(e: Emblem, size: Dp, hole: Color = Color(0xFF0C0D10)) {
    Canvas(Modifier.size(size)) { drawEmblem(e, center.x, center.y, this.size.minDimension / 2f * .92f, hole) }
}
