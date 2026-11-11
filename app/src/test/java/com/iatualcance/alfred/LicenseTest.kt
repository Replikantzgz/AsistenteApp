package com.iatualcance.alfred

import com.iatualcance.alfred.billing.LicenseResult
import com.iatualcance.alfred.billing.Licenses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.LocalDate
import java.util.Base64

class LicenseTest {
    private val kp = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    private val pub = Base64.getEncoder().encodeToString(kp.public.encoded)
    private fun b64u(b: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(b)

    private fun make(json: String, signer: java.security.PrivateKey = kp.private): String {
        val msg = "ALFRED1.${b64u(json.toByteArray())}"
        val sig = Signature.getInstance("SHA256withECDSA").run { initSign(signer); update(msg.toByteArray()); sign() }
        return "$msg.${b64u(sig)}"
    }

    private val lifetime = """{"v":1,"id":"abc","e":"ana@x.com","p":"premium","x":null}"""

    @Test fun validLifetime() {
        val r = Licenses.verify(make(lifetime), pub) as LicenseResult.Valid
        assertEquals("ana@x.com", r.data.e); assertEquals(null, r.data.x)
    }

    @Test fun pastedWithSpacesAndNewlines() {
        val code = make(lifetime)
        assertTrue(Licenses.verify(" ${code.take(20)}\n${code.drop(20)} ", pub) is LicenseResult.Valid)
    }

    @Test fun expiry() {
        val code = make("""{"v":1,"id":"a","e":"a@b.c","p":"premium","x":"2026-12-31"}""")
        assertTrue(Licenses.verify(code, pub, LocalDate.parse("2026-12-31")) is LicenseResult.Valid)
        val r = Licenses.verify(code, pub, LocalDate.parse("2027-01-01")) as LicenseResult.Invalid
        assertTrue(r.reason.contains("caducó"))
    }

    @Test fun tamperedPayloadIsRejected() {
        val parts = make(lifetime).split('.')
        val forged = """{"v":1,"id":"abc","e":"otro@x.com","p":"premium","x":null}"""
        assertTrue(Licenses.verify("${parts[0]}.${b64u(forged.toByteArray())}.${parts[2]}", pub) is LicenseResult.Invalid)
    }

    @Test fun signedWithAnotherKeyIsRejected() {
        val other = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        assertTrue(Licenses.verify(make(lifetime, other.private), pub) is LicenseResult.Invalid)
    }

    @Test fun garbageIsRejected() {
        listOf("", "hola", "ALFRED1.x.y", "ALFRED2.a.b", "ALFRED1..").forEach { assertTrue(it, Licenses.verify(it, pub) is LicenseResult.Invalid) }
    }
}
