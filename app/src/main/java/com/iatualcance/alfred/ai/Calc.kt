package com.iatualcance.alfred.ai

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Calculadora exacta para el asistente: así las cuentas no dependen de que el modelo "calcule de cabeza".
 * Admite + - * / ^ ( ), %, decimales con coma o punto, x y ÷, y las funciones sqrt/raiz, cbrt, abs, round, floor, ceil.
 * Kotlin puro, para poder probarlo.
 */
object Calc {
    class CalcError(msg: String) : Exception(msg)

    fun eval(expr: String): Double {
        val p = Parser(expr.lowercase().replace("×", "*").replace("÷", "/").replace("−", "-").replace("raíz", "raiz").replace(Regex("""(?<=\d)\s*x\s*(?=[\d(])"""), "*").replace(",", "."))
        val v = p.parseAll()
        if (v.isNaN() || v.isInfinite()) throw CalcError("El resultado no es un número válido")
        return v
    }

    /** Resultado listo para decir: sin ceros de más y con coma decimal. */
    fun format(v: Double): String {
        val r = round(v * 1e9) / 1e9
        return if (r == floor(r) && abs(r) < 1e15) r.toLong().toString() else "%.6f".format(java.util.Locale.ROOT, r).trimEnd('0').trimEnd('.').replace('.', ',')
    }

    private class Parser(val s: String) {
        var i = 0
        fun parseAll(): Double { val v = expr(); ws(); if (i < s.length) throw CalcError("No entiendo «${s.substring(i).take(12)}»"); return v }
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun peek(): Char? { ws(); return s.getOrNull(i) }
        fun expr(): Double {
            var v = term()
            while (true) { when (peek()) { '+' -> { i++; v += term() }; '-' -> { i++; v -= term() }; else -> return v } }
        }
        fun term(): Double {
            var v = power()
            while (true) { when (peek()) { '*' -> { i++; v *= power() }; '/' -> { i++; val d = power(); if (d == 0.0) throw CalcError("División entre cero"); v /= d }; else -> return v } }
        }
        fun power(): Double {
            val base = unary()
            if (peek() == '^') { i++; return base.pow(power()) }
            return base
        }
        fun unary(): Double {
            when (peek()) { '-' -> { i++; return -unary() }; '+' -> { i++; return unary() } }
            var v = atom()
            while (peek() == '%') { i++; v /= 100.0 }
            return v
        }
        fun atom(): Double {
            val c = peek() ?: throw CalcError("Falta un número")
            if (c == '(') { i++; val v = expr(); if (peek() != ')') throw CalcError("Falta cerrar un paréntesis"); i++; return v }
            if (c.isDigit() || c == '.') {
                val st = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                return s.substring(st, i).toDoubleOrNull() ?: throw CalcError("Número no válido")
            }
            if (c.isLetter()) {
                val st = i; while (i < s.length && s[i].isLetter()) i++
                val name = s.substring(st, i)
                if (name == "pi") return Math.PI
                val arg = if (peek() == '(') { i++; val v = expr(); if (peek() != ')') throw CalcError("Falta cerrar un paréntesis"); i++; v } else unary()
                return when (name) {
                    "sqrt", "raiz" -> { if (arg < 0) throw CalcError("Raíz de un número negativo"); sqrt(arg) }
                    "cbrt" -> cbrt(arg); "abs" -> abs(arg); "round" -> round(arg); "floor" -> floor(arg); "ceil" -> ceil(arg)
                    else -> throw CalcError("No conozco «$name»")
                }
            }
            throw CalcError("No entiendo «$c»")
        }
    }
}
