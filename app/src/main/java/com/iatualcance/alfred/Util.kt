package com.iatualcance.alfred

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

/** Utilidades puras (sin dependencias de Android) para poder probarlas con tests. */
object Util {
    val ES: Locale = Locale("es", "ES")

    /** Acepta "2026-11-12T10:00", con segundos, con zona ("Z", "+01:00") o solo fecha. */
    fun parseDateTime(s: String, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        val t = s.trim().replace(' ', 'T')
        runCatching { return OffsetDateTime.parse(t).atZoneSameInstant(zone) }
        runCatching { return LocalDateTime.parse(t).atZone(zone) }
        return LocalDate.parse(t.take(10)).atStartOfDay(zone)
    }

    fun rfc3339(z: ZonedDateTime): String = z.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    fun rfc3339(ms: Long): String = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(ms))

    private fun b64url(b: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    private fun header(v: String) =
        if (v.all { it.code in 32..126 }) v else "=?UTF-8?B?" + Base64.getEncoder().encodeToString(v.toByteArray()) + "?="

    /** Mensaje RFC 2822 en texto plano UTF-8, codificado en base64url como pide la API de Gmail. */
    fun mimeMessage(to: String, subject: String, body: String, inReplyTo: String? = null): String {
        val sb = StringBuilder()
        if (to.isNotBlank()) sb.append("To: ").append(to.replace(Regex("[\r\n]+"), " ").trim()).append("\r\n")
        sb.append("Subject: ").append(header(subject.replace(Regex("[\r\n]+"), " "))).append("\r\n")
        if (!inReplyTo.isNullOrBlank()) {
            sb.append("In-Reply-To: ").append(inReplyTo).append("\r\n")
            sb.append("References: ").append(inReplyTo).append("\r\n")
        }
        sb.append("MIME-Version: 1.0\r\n")
        sb.append("Content-Type: text/plain; charset=UTF-8\r\n")
        sb.append("Content-Transfer-Encoding: base64\r\n\r\n")
        sb.append(Base64.getMimeEncoder(76, "\r\n".toByteArray()).encodeToString(body.toByteArray()))
        return b64url(sb.toString().toByteArray())
    }

    fun decodeB64Url(s: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(s.trim().trimEnd('=')))
    }.getOrDefault("")

    fun htmlToText(html: String): String = html
        .replace(Regex("(?is)<(script|style|head)[^>]*>.*?</\\1>"), "")
        .replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</tr>|</h[1-6]>|</li>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n")
        .trim()

    /** "Ana Pérez <ana@x.com>" -> ("Ana Pérez", "ana@x.com") */
    fun splitAddress(from: String): Pair<String, String> {
        val m = Regex("^\\s*\"?([^\"<]*)\"?\\s*<([^>]+)>\\s*$").find(from)
        return if (m != null) m.groupValues[1].trim().ifBlank { m.groupValues[2] } to m.groupValues[2].trim()
        else from.trim() to from.trim()
    }

    fun escapeDriveQuery(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")

    fun safeFileName(s: String) = s.replace(Regex("[\\\\/:*?\"<>|\\n\\r]+"), " ").trim().take(80).ifBlank { "sin título" }
}
