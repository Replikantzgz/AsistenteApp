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

class MimeAttachmentTest {
    private fun raw(files: List<MailFile>) = String(Util.rawMessage("ana@x.com", "Informe", "Hola\nAdjunto el informe", null, files), Charsets.UTF_8)

    @Test fun withoutFilesIsPlainText() {
        val r = raw(emptyList())
        assertTrue(r.contains("Content-Type: text/plain; charset=UTF-8")); assertTrue(!r.contains("multipart"))
    }

    @Test fun attachmentsRoundTrip() {
        val pdf = ByteArray(5000) { (it * 7).toByte() }
        val r = raw(listOf(MailFile("Informe ñ.pdf", "application/pdf", pdf)))
        assertTrue(r.contains("multipart/mixed; boundary="))
        val boundary = Regex("boundary=\"([^\"]+)\"").find(r)!!.groupValues[1]
        val parts = r.split("--$boundary").drop(1).filter { !it.startsWith("--") }
        assertEquals(2, parts.size)
        val text = parts[0].substringAfter("\r\n\r\n").trim()
        assertEquals("Hola\nAdjunto el informe", String(java.util.Base64.getMimeDecoder().decode(text)))
        val a = parts[1]
        assertTrue(a.contains("Content-Type: application/pdf; name=\"Informe _.pdf\""))
        assertTrue(a.contains("filename*=UTF-8''Informe%20%C3%B1.pdf"))
        assertTrue(pdf.contentEquals(java.util.Base64.getMimeDecoder().decode(a.substringAfter("\r\n\r\n").trim())))
        assertTrue(r.trimEnd().endsWith("--$boundary--"))
    }

    @Test fun jsonRawPathStillWorks() {
        val raw = String(java.util.Base64.getUrlDecoder().decode(Util.mimeMessage("a@b.c", "Hola", "x")))
        assertTrue(raw.startsWith("To: a@b.c\r\nSubject: Hola\r\n"))
    }
}

class SpeechTextTest {
    @Test fun stripsEmojiMarkdownAndLinks() {
        assertEquals("Evento creado: Cita C43 Listo", com.iatualcance.alfred.voice.SpeechText.clean("📅 Evento creado: **Cita C43**\n\n✅ Listo"))
        assertEquals("Mira un enlace ahora", com.iatualcance.alfred.voice.SpeechText.clean("Mira https://example.com/x?y=1 ahora"))
        assertEquals("", com.iatualcance.alfred.voice.SpeechText.clean("🙂 ✅ 📝"))
    }
}

class ModelInfoTest {
    @Test fun freeAndPaid() {
        listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-flash-latest", "gemini-flash-lite-latest", "gemini-3-flash-preview").forEach { assertTrue(it, com.iatualcance.alfred.ai.ModelInfo.isFree(it)) }
        listOf("gemini-2.5-pro", "gemini-3-pro-preview", "gemini-pro-latest").forEach { assertTrue(it, com.iatualcance.alfred.ai.ModelInfo.isPaid(it)); assertEquals("de pago", com.iatualcance.alfred.ai.ModelInfo.label(it)) }
        assertEquals("gratis", com.iatualcance.alfred.ai.ModelInfo.label("gemini-flash-latest"))
        assertEquals("", com.iatualcance.alfred.ai.ModelInfo.label("gemma-3-27b-it"))
    }

    @Test fun sortedPutsLiteFirstAndProLast() {
        val s = com.iatualcance.alfred.ai.ModelInfo.sorted(listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemma-3-27b-it", "gemini-2.5-flash-lite"))
        assertEquals(listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemma-3-27b-it", "gemini-2.5-pro"), s)
    }
}
