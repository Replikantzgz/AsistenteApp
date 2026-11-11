package com.iatualcance.alfred

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Base64

class UtilTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    @Test fun parseLocalDateTime() {
        val z = Util.parseDateTime("2026-11-12T10:30", madrid)
        assertEquals("2026-11-12T10:30:00+01:00", Util.rfc3339(z))
    }

    @Test fun parseWithSecondsSpaceAndOffset() {
        assertEquals("2026-07-01T09:00:00+02:00", Util.rfc3339(Util.parseDateTime("2026-07-01 09:00:00", madrid)))
        assertEquals("2026-11-12T11:00:00+01:00", Util.rfc3339(Util.parseDateTime("2026-11-12T10:00:00Z", madrid)))
    }

    @Test fun parseDateOnly() {
        assertEquals("2026-11-12T00:00:00+01:00", Util.rfc3339(Util.parseDateTime("2026-11-12", madrid)))
    }

    @Test fun mimeRoundTrip() {
        val raw = String(Base64.getUrlDecoder().decode(Util.mimeMessage("ana@x.com", "Reunión mañana", "Hola Ana,\n¿te va bien a las 10?")))
        assertTrue(raw.startsWith("To: ana@x.com\r\nSubject: =?UTF-8?B?"))
        val (headers, body) = raw.split("\r\n\r\n", limit = 2)
        assertTrue(headers.contains("Content-Type: text/plain; charset=UTF-8"))
        assertEquals("Hola Ana,\n¿te va bien a las 10?", String(Base64.getMimeDecoder().decode(body)))
        val subj = Regex("Subject: =\\?UTF-8\\?B\\?(.*)\\?=").find(headers)!!.groupValues[1]
        assertEquals("Reunión mañana", String(Base64.getDecoder().decode(subj)))
    }

    @Test fun asciiSubjectStaysPlainAndNoHeaderInjection() {
        val raw = String(Base64.getUrlDecoder().decode(Util.mimeMessage("a@b.c", "Hola\r\nBcc: x@y.z", "x")))
        assertTrue(raw.contains("Subject: Hola Bcc: x@y.z\r\n"))
    }

    @Test fun html() {
        assertEquals("Hola\nMundo & cía", Util.htmlToText("<html><head><style>p{}</style></head><p>Hola</p><div>Mundo &amp; cía</div></html>"))
    }

    @Test fun address() {
        assertEquals("Ana Pérez" to "ana@x.com", Util.splitAddress("\"Ana Pérez\" <ana@x.com>"))
        assertEquals("ana@x.com" to "ana@x.com", Util.splitAddress("ana@x.com"))
        assertEquals("ana@x.com" to "ana@x.com", Util.splitAddress("<ana@x.com>"))
    }

    @Test fun b64url() {
        assertEquals("Hola ñ", Util.decodeB64Url(Base64.getUrlEncoder().encodeToString("Hola ñ".toByteArray())))
    }
}
