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

/** Archivo adjunto de un correo que se va a enviar. */
class MailFile(val name: String, val mime: String, val bytes: ByteArray)

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

    private fun mime76(b: ByteArray) = Base64.getMimeEncoder(76, "\r\n".toByteArray()).encodeToString(b)
    private fun asciiName(n: String) = n.map { if (it.code in 32..126 && it != '"' && it != '\\') it else '_' }.joinToString("")
    private fun pct(n: String) = java.net.URLEncoder.encode(n, "UTF-8").replace("+", "%20")

    /** Mensaje RFC 2822 completo (texto UTF-8 y, si los hay, adjuntos en multipart/mixed). */
    fun rawMessage(to: String, subject: String, body: String, inReplyTo: String? = null, files: List<MailFile> = emptyList()): ByteArray {
        val sb = StringBuilder()
        if (to.isNotBlank()) sb.append("To: ").append(to.replace(Regex("[\r\n]+"), " ").trim()).append("\r\n")
        sb.append("Subject: ").append(header(subject.replace(Regex("[\r\n]+"), " "))).append("\r\n")
        if (!inReplyTo.isNullOrBlank()) sb.append("In-Reply-To: ").append(inReplyTo).append("\r\n").append("References: ").append(inReplyTo).append("\r\n")
        sb.append("MIME-Version: 1.0\r\n")
        val text = mime76(body.toByteArray())
        if (files.isEmpty()) {
            sb.append("Content-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n").append(text)
        } else {
            val b = "alfred_" + java.util.UUID.randomUUID().toString().replace("-", "")
            sb.append("Content-Type: multipart/mixed; boundary=\"").append(b).append("\"\r\n\r\n")
            sb.append("--").append(b).append("\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n").append(text).append("\r\n")
            for (f in files) {
                val type = f.mime.ifBlank { "application/octet-stream" }
                sb.append("--").append(b).append("\r\nContent-Type: ").append(type).append("; name=\"").append(asciiName(f.name)).append("\"\r\n")
                sb.append("Content-Disposition: attachment; filename=\"").append(asciiName(f.name)).append("\"; filename*=UTF-8''").append(pct(f.name)).append("\r\n")
                sb.append("Content-Transfer-Encoding: base64\r\n\r\n").append(mime76(f.bytes)).append("\r\n")
            }
            sb.append("--").append(b).append("--\r\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** Igual que [rawMessage], codificado en base64url como pide la API de Gmail para el campo `raw`. */
    fun mimeMessage(to: String, subject: String, body: String, inReplyTo: String? = null): String = b64url(rawMessage(to, subject, body, inReplyTo))

    @Suppress("unused") private fun legacyMimeMessage(to: String, subject: String, body: String, inReplyTo: String? = null): String {
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
