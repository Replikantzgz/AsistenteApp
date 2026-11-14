package com.iatualcance.alfred

import com.iatualcance.alfred.ai.Calc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcTest {
    private fun v(s: String) = Calc.eval(s)

    @Test fun precedenceAndParentheses() {
        assertEquals(180.0, v("5 + 7 * 25"), 1e-9)
        assertEquals(300.0, v("(5 + 7) * 25"), 1e-9)
        assertEquals(512.0, v("2^3^2"), 1e-9)
    }
    @Test fun theUsersExample() {
        assertEquals(5 + 7 * 25 - 48 * Math.sqrt(3.0), v("5 + 7 * 25 - 48 * raiz(3)"), 1e-9)
        assertEquals("96,8615", Calc.format(v("5 + 7 * 25 - 48 * raiz(3)")).take(7))
    }
    @Test fun decimalsPercentAndUnary() {
        assertEquals(2.5, v("5 / 2"), 1e-9); assertEquals(3.75, v("1,5 * 2,5"), 1e-9)
        assertEquals(15.0, v("10 + 50%  * 10"), 1e-9)
        assertEquals(-4.0, v("-(2+2)"), 1e-9)
        assertEquals(6.0, v("2 x 3"), 1e-9)
    }
    @Test fun formatting() {
        assertEquals("42", Calc.format(42.0)); assertEquals("3,5", Calc.format(3.5)); assertEquals("0,333333", Calc.format(1.0 / 3))
    }
    @Test fun errors() {
        listOf("1/0", "2 +", "(2+3", "raiz(-4)", "hola").forEach { e ->
            assertTrue(e, runCatching { v(e) }.exceptionOrNull() is Calc.CalcError)
        }
    }
}

class UsageCostTest {
    @Test fun costEstimateUsesPrudentPrices() {
        val u = com.iatualcance.alfred.data.Settings.Usage(msgs = 1500, liteIn = 9_750_000, liteOut = 450_000)
        assertEquals(2.90, u.costEur, 0.02)
        assertEquals(10_200_000L, u.tokens)
    }
}
