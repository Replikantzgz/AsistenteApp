package com.iatualcance.alfred.google

import com.iatualcance.alfred.Util
import com.iatualcance.alfred.MailFile
import com.iatualcance.alfred.data.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ApiError(val code: Int, message: String) : Exception(message)

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS)
        .build()
    val json = Json { ignoreUnknownKeys = true }
    val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
}

fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull
fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject
fun JsonObject.arr(k: String): JsonArray = this[k] as? JsonArray ?: JsonArray(emptyList())

data class CalEvent(
    val id: String, val title: String, val start: ZonedDateTime, val end: ZonedDateTime, val allDay: Boolean,
    val description: String = "", val location: String = "", val attendees: List<String> = emptyList(), val link: String? = null,
)

data class MailAttachment(val id: String, val name: String, val mime: String, val size: Long)

data class Mail(
    val id: String, val threadId: String, val from: String, val to: String, val subject: String,
    val snippet: String, val date: Long, val unread: Boolean, val body: String = "", val messageId: String? = null,
    val attachments: List<MailAttachment> = emptyList(),
)

data class Contact(val resource: String, val name: String, val emails: List<String>, val phones: List<String>)

data class DrivePerm(val id: String, val email: String, val name: String, val role: String, val type: String)

data class DriveFile(
    val id: String, val name: String, val mime: String, val link: String?, val modified: String?,
    val shared: Boolean = false, val ownedByMe: Boolean = true, val owner: String? = null, val size: Long? = null,
) {
    val isFolder get() = mime == FOLDER
    val isGoogleDoc get() = mime == DOC
    val isSheet get() = mime == SHEET
    companion object {
        const val FOLDER = "application/vnd.google-apps.folder"
        const val DOC = "application/vnd.google-apps.document"
        const val SHEET = "application/vnd.google-apps.spreadsheet"
        const val SLIDES = "application/vnd.google-apps.presentation"
    }
}

/** Llamadas REST a Calendar, Gmail, Contactos (People) y Drive con el token del usuario. */
class GoogleApi(private val auth: GoogleAuth) {

    private suspend fun call(
        method: String, url: String, query: Map<String, String?> = emptyMap(),
        body: JsonElement? = null, rawBody: RequestBody? = null,
    ): JsonObject = withContext(Dispatchers.IO) {
        val u = url.toHttpUrl().newBuilder().apply { query.forEach { (k, v) -> if (v != null) addQueryParameter(k, v) } }.build()
        val rb = rawBody ?: body?.toString()?.toRequestBody(Http.JSON_TYPE)
            ?: if (method == "POST" || method == "PATCH") ByteArray(0).toRequestBody(null) else null
        repeat(2) { attempt ->
            val req = Request.Builder().url(u).method(method, rb).header("Authorization", "Bearer " + auth.token()).build()
            Http.client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    return@withContext if (text.isBlank()) JsonObject(emptyMap()) else Http.json.parseToJsonElement(text).jsonObject
                }
                if (resp.code == 401 && attempt == 0) { auth.invalidate(); return@repeat }
                val msg = runCatching { Http.json.parseToJsonElement(text).jsonObject.obj("error")?.str("message") }.getOrNull()
                throw ApiError(resp.code, explain(resp.code, msg ?: text.take(200)))
            }
        }
        throw ApiError(401, "Sesión de Google caducada. Vuelve a conectar tu cuenta en Ajustes.")
    }

    private fun explain(code: Int, msg: String) = when {
        code == 403 && ("has not been used" in msg || "is disabled" in msg) ->
            "La API de Google no está activada en el proyecto de Google Cloud. $msg"
        code == 403 && "insufficient" in msg.lowercase() -> "Faltan permisos de Google. Vuelve a conectar tu cuenta en Ajustes y acepta todos los permisos."
        else -> "Google ($code): $msg"
    }

    // ------------------------------------------------------------ Perfil
    suspend fun profile(): Account {
        val j = call("GET", "https://www.googleapis.com/oauth2/v3/userinfo")
        return Account(j.str("name") ?: j.str("email").orEmpty(), j.str("email").orEmpty(), j.str("picture"))
    }

    // ------------------------------------------------------------ Calendar
    private val cal = "https://www.googleapis.com/calendar/v3/calendars/primary/events"

    private fun parseEvent(e: JsonObject): CalEvent? {
        val zone = ZoneId.systemDefault()
        val s = e.obj("start") ?: return null
        val en = e.obj("end") ?: s
        val allDay = s.str("date") != null
        val start = if (allDay) LocalDate.parse(s.str("date")).atStartOfDay(zone) else Util.parseDateTime(s.str("dateTime") ?: return null, zone)
        val end = if (allDay) LocalDate.parse(en.str("date") ?: s.str("date")).atStartOfDay(zone) else Util.parseDateTime(en.str("dateTime") ?: return null, zone)
        return CalEvent(
            id = e.str("id") ?: return null, title = e.str("summary") ?: "(Sin título)", start = start, end = end, allDay = allDay,
            description = e.str("description").orEmpty(), location = e.str("location").orEmpty(),
            attendees = e.arr("attendees").mapNotNull { it.jsonObject.str("email") }, link = e.str("htmlLink"),
        )
    }

    suspend fun listEvents(from: ZonedDateTime, to: ZonedDateTime, text: String? = null): List<CalEvent> {
        val j = call("GET", cal, mapOf(
            "singleEvents" to "true", "orderBy" to "startTime", "maxResults" to "250",
            "timeMin" to Util.rfc3339(from), "timeMax" to Util.rfc3339(to), "q" to text?.ifBlank { null },
        ))
        return j.arr("items").mapNotNull { parseEvent(it.jsonObject) }
    }

    private fun eventBody(
        title: String?, start: ZonedDateTime?, end: ZonedDateTime?, allDay: Boolean,
        description: String?, location: String?, attendees: List<String>?,
    ) = buildJsonObject {
        title?.let { put("summary", it) }
        description?.let { put("description", it) }
        location?.let { put("location", it) }
        if (start != null) {
            val e = end ?: start.plusHours(1)
            if (allDay) {
                putJsonObject("start") { put("date", start.toLocalDate().toString()); put("dateTime", JsonNull); put("timeZone", JsonNull) }
                val endDate = if (e.toLocalDate().isAfter(start.toLocalDate())) e.toLocalDate() else start.toLocalDate().plusDays(1)
                putJsonObject("end") { put("date", endDate.toString()); put("dateTime", JsonNull); put("timeZone", JsonNull) }
            } else {
                putJsonObject("start") { put("dateTime", Util.rfc3339(start)); put("timeZone", start.zone.id); put("date", JsonNull) }
                putJsonObject("end") { put("dateTime", Util.rfc3339(e)); put("timeZone", e.zone.id); put("date", JsonNull) }
            }
        }
        attendees?.let { list -> put("attendees", buildJsonArray { list.filter { "@" in it }.forEach { add(buildJsonObject { put("email", it.trim()) }) } }) }
    }

    suspend fun createEvent(
        title: String, start: ZonedDateTime, end: ZonedDateTime?, allDay: Boolean = false,
        description: String? = null, location: String? = null, attendees: List<String>? = null,
    ): CalEvent = parseEvent(call("POST", cal, mapOf("sendUpdates" to "all"),
        eventBody(title, start, end, allDay, description, location, attendees)))!!

    suspend fun updateEvent(
        id: String, title: String? = null, start: ZonedDateTime? = null, end: ZonedDateTime? = null, allDay: Boolean = false,
        description: String? = null, location: String? = null, attendees: List<String>? = null,
    ): CalEvent = parseEvent(call("PATCH", "$cal/$id", mapOf("sendUpdates" to "all"),
        eventBody(title, start, end, allDay, description, location, attendees)))!!

    suspend fun deleteEvent(id: String) { call("DELETE", "$cal/$id", mapOf("sendUpdates" to "all")) }

    // ------------------------------------------------------------ Gmail
    private val gm = "https://gmail.googleapis.com/gmail/v1/users/me"

    private fun header(payload: JsonObject?, name: String) =
        payload?.arr("headers")?.firstOrNull { it.jsonObject.str("name").equals(name, true) }?.jsonObject?.str("value").orEmpty()

    private fun findPart(p: JsonObject, mime: String): String? {
        if (p.str("mimeType") == mime) p.obj("body")?.str("data")?.let { return Util.decodeB64Url(it) }
        for (c in p.arr("parts")) findPart(c.jsonObject, mime)?.let { return it }
        return null
    }

    private fun collectAttachments(p: JsonObject, out: MutableList<MailAttachment>) {
        val name = p.str("filename"); val body = p.obj("body"); val aid = body?.str("attachmentId")
        if (!name.isNullOrBlank() && aid != null) out += MailAttachment(aid, name, p.str("mimeType").orEmpty(), body.str("size")?.toLongOrNull() ?: 0L)
        p.arr("parts").forEach { collectAttachments(it.jsonObject, out) }
    }

    private fun parseMail(j: JsonObject, full: Boolean): Mail {
        val p = j.obj("payload")
        val body = if (full && p != null) findPart(p, "text/plain") ?: findPart(p, "text/html")?.let { Util.htmlToText(it) } ?: "" else ""
        return Mail(
            id = j.str("id").orEmpty(), threadId = j.str("threadId").orEmpty(), from = header(p, "From"), to = header(p, "To"),
            subject = header(p, "Subject").ifBlank { "(Sin asunto)" }, snippet = Util.htmlToText(j.str("snippet").orEmpty()),
            date = j.str("internalDate")?.toLongOrNull() ?: 0L, unread = j.arr("labelIds").any { it.jsonPrimitive.content == "UNREAD" },
            body = body, messageId = header(p, "Message-ID").ifBlank { null },
            attachments = if (full && p != null) mutableListOf<MailAttachment>().also { collectAttachments(p, it) } else emptyList(),
        )
    }

    suspend fun listMail(query: String = "in:inbox", max: Int = 25): List<Mail> = coroutineScope {
        val ids = call("GET", "$gm/messages", mapOf("q" to query, "maxResults" to max.coerceIn(1, 50).toString()))
            .arr("messages").mapNotNull { it.jsonObject.str("id") }
        ids.map { id ->
            async {
                val u = "$gm/messages/$id?format=metadata&metadataHeaders=From&metadataHeaders=To&metadataHeaders=Subject"
                parseMail(call("GET", u), false)
            }
        }.awaitAll()
    }

    suspend fun readMail(id: String): Mail = parseMail(call("GET", "$gm/messages/$id", mapOf("format" to "full")), true)

    private val gmUpload = "https://www.googleapis.com/upload/gmail/v1/users/me"

    /** Envía con adjuntos por la subida multipart de Gmail (hasta ~35 MB). */
    private suspend fun uploadRaw(path: String, raw: ByteArray, meta: JsonObject) {
        val multi = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody(Http.JSON_TYPE)).addPart(raw.toRequestBody("message/rfc822".toMediaType())).build()
        call("POST", "$gmUpload/$path", mapOf("uploadType" to "multipart"), rawBody = multi)
    }

    suspend fun sendMail(to: String, subject: String, body: String, threadId: String? = null, inReplyTo: String? = null, files: List<MailFile> = emptyList()) {
        if (files.isNotEmpty()) { uploadRaw("messages/send", Util.rawMessage(to, subject, body, inReplyTo, files), buildJsonObject { threadId?.let { put("threadId", it) } }); return }
        call("POST", "$gm/messages/send", body = buildJsonObject {
            put("raw", Util.mimeMessage(to, subject, body, inReplyTo)); threadId?.let { put("threadId", it) }
        })
    }

    suspend fun createDraft(to: String, subject: String, body: String, files: List<MailFile> = emptyList()) {
        if (files.isNotEmpty()) { uploadRaw("drafts", Util.rawMessage(to, subject, body, null, files), buildJsonObject { putJsonObject("message") {} }); return }
        call("POST", "$gm/drafts", body = buildJsonObject { putJsonObject("message") { put("raw", Util.mimeMessage(to, subject, body)) } })
    }

    suspend fun downloadAttachment(messageId: String, attachmentId: String): ByteArray {
        val data = call("GET", "$gm/messages/$messageId/attachments/$attachmentId").str("data").orEmpty()
        return java.util.Base64.getUrlDecoder().decode(data)
    }

    suspend fun markRead(id: String) {
        call("POST", "$gm/messages/$id/modify", body = buildJsonObject { put("removeLabelIds", buildJsonArray { add(JsonPrimitive("UNREAD")) }) })
    }

    suspend fun trashMail(id: String) { call("POST", "$gm/messages/$id/trash") }

    // ------------------------------------------------------------ Contactos
    private val pp = "https://people.googleapis.com/v1"

    private fun parseContact(p: JsonObject) = Contact(
        resource = p.str("resourceName").orEmpty(),
        name = p.arr("names").firstOrNull()?.jsonObject?.str("displayName").orEmpty(),
        emails = p.arr("emailAddresses").mapNotNull { it.jsonObject.str("value") },
        phones = p.arr("phoneNumbers").mapNotNull { it.jsonObject.str("value") },
    )

    suspend fun listContacts(): List<Contact> {
        val out = mutableListOf<Contact>()
        var page: String? = null
        do {
            val j = call("GET", "$pp/people/me/connections", mapOf(
                "personFields" to "names,emailAddresses,phoneNumbers", "pageSize" to "1000",
                "sortOrder" to "FIRST_NAME_ASCENDING", "pageToken" to page,
            ))
            out += j.arr("connections").map { parseContact(it.jsonObject) }
            page = j.str("nextPageToken")
        } while (page != null && out.size < 5000)
        return out.filter { it.name.isNotBlank() || it.emails.isNotEmpty() || it.phones.isNotEmpty() }
    }

    suspend fun createContact(name: String, email: String?, phone: String?): Contact = parseContact(
        call("POST", "$pp/people:createContact", body = buildJsonObject {
            put("names", buildJsonArray { add(buildJsonObject { put("givenName", name) }) })
            if (!email.isNullOrBlank()) put("emailAddresses", buildJsonArray { add(buildJsonObject { put("value", email) }) })
            if (!phone.isNullOrBlank()) put("phoneNumbers", buildJsonArray { add(buildJsonObject { put("value", phone) }) })
        })
    )

    suspend fun deleteContact(resource: String) { call("DELETE", "$pp/$resource:deleteContact") }

    // ------------------------------------------------------------ Drive
    private val dr = "https://www.googleapis.com/drive/v3/files"
    private val folderCache = mutableMapOf<String, String>()
    private val fileFields = "id,name,mimeType,webViewLink,modifiedTime,shared,ownedByMe,owners(displayName,emailAddress),size"

    private fun parseFile(f: JsonObject) = DriveFile(
        f.str("id").orEmpty(), f.str("name").orEmpty(), f.str("mimeType").orEmpty(), f.str("webViewLink"), f.str("modifiedTime"),
        shared = f.str("shared") == "true", ownedByMe = f.str("ownedByMe") != "false",
        owner = f.arr("owners").firstOrNull()?.jsonObject?.let { it.str("displayName") ?: it.str("emailAddress") }, size = f.str("size")?.toLongOrNull(),
    )

    /** Carpeta "Alfred" en la raíz de Drive, o una subcarpeta suya ("Notas", "Escaneos", "Facturas"…). Se crea si no existe. */
    suspend fun folder(sub: String? = null): String {
        val key = sub ?: ""
        folderCache[key]?.let { return it }
        val parent = if (sub == null) null else folder(null)
        val name = sub ?: "Alfred"
        val q = "name = '${Util.escapeDriveQuery(name)}' and mimeType = '${DriveFile.FOLDER}' and trashed = false" +
            if (parent != null) " and '$parent' in parents" else " and 'root' in parents"
        val found = call("GET", dr, mapOf("q" to q, "fields" to "files(id)")).arr("files").firstOrNull()?.jsonObject?.str("id")
        val id = found ?: call("POST", dr, mapOf("fields" to "id"), buildJsonObject {
            put("name", name); put("mimeType", DriveFile.FOLDER)
            if (parent != null) put("parents", buildJsonArray { add(JsonPrimitive(parent)) })
        }).str("id")!!
        folderCache[key] = id
        return id
    }

    suspend fun listFiles(parent: String): List<DriveFile> =
        call("GET", dr, mapOf(
            "q" to "'$parent' in parents and trashed = false", "fields" to "files($fileFields)",
            "orderBy" to "folder,modifiedTime desc", "pageSize" to "200",
        )).arr("files").map { parseFile(it.jsonObject) }

    /** Sube un fichero. Si [existingId] no es nulo, sustituye su contenido en vez de crear otro. */
    suspend fun upload(bytes: ByteArray, name: String, mime: String, parent: String, existingId: String? = null, convertTo: String? = null): DriveFile {
        val meta = buildJsonObject {
            put("name", name)
            convertTo?.let { put("mimeType", it) }
            if (existingId == null) put("parents", buildJsonArray { add(JsonPrimitive(parent)) })
        }
        val type = runCatching { mime.toMediaType() }.getOrDefault("application/octet-stream".toMediaType())
        val multi = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody(Http.JSON_TYPE))
            .addPart(bytes.toRequestBody(type))
            .build()
        val base = "https://www.googleapis.com/upload/drive/v3/files"
        val q = mapOf("uploadType" to "multipart", "fields" to fileFields)
        return try {
            parseFile(if (existingId == null) call("POST", base, q, rawBody = multi) else call("PATCH", "$base/$existingId", q, rawBody = multi))
        } catch (e: ApiError) {
            if (existingId != null && e.code == 404) upload(bytes, name, mime, parent, null) else throw e
        }
    }

    suspend fun deleteFile(id: String) { call("DELETE", "$dr/$id") }

    // ---- Explorador de Drive (permiso completo) ----------------------------------------------------------------
    private val driveAll = mapOf("supportsAllDrives" to "true", "includeItemsFromAllDrives" to "true")

    /**
     * Lista archivos de Drive. [parent] = carpeta ("root" para Mi unidad); con [sharedWithMe] lista lo que otros han compartido;
     * con [query] busca por nombre en todo el Drive.
     */
    suspend fun listDrive(parent: String? = "root", query: String? = null, sharedWithMe: Boolean = false): List<DriveFile> {
        val parts = mutableListOf("trashed = false")
        if (sharedWithMe) parts += "sharedWithMe = true"
        if (!query.isNullOrBlank()) parts += "name contains '${Util.escapeDriveQuery(query.trim())}'"
        else if (!sharedWithMe && parent != null) parts += "'$parent' in parents"
        return call("GET", dr, driveAll + mapOf(
            "q" to parts.joinToString(" and "), "fields" to "files($fileFields)", "pageSize" to "200",
            "orderBy" to if (query.isNullOrBlank()) "folder,name_natural" else "modifiedTime desc",
        )).arr("files").map { parseFile(it.jsonObject) }
    }

    suspend fun createFolder(name: String, parent: String?): DriveFile = createDriveItem(name, DriveFile.FOLDER, parent)

    /** Crea un documento, hoja o presentación de Google vacío. */
    suspend fun createDriveItem(name: String, mime: String, parent: String?): DriveFile = parseFile(
        call("POST", dr, driveAll + mapOf("fields" to fileFields), buildJsonObject {
            put("name", name); put("mimeType", mime)
            if (parent != null) put("parents", buildJsonArray { add(JsonPrimitive(parent)) })
        })
    )

    /** Crea un documento de Google con texto inicial (se convierte al subirlo). */
    suspend fun createDocWithText(name: String, text: String, parent: String): DriveFile =
        upload(text.toByteArray(), name, "text/plain", parent, null, convertTo = DriveFile.DOC)

    suspend fun trash(id: String) { call("PATCH", "$dr/$id", driveAll + mapOf("fields" to "id"), buildJsonObject { put("trashed", true) }) }

    suspend fun rename(id: String, name: String) { call("PATCH", "$dr/$id", driveAll + mapOf("fields" to "id"), buildJsonObject { put("name", name) }) }

    suspend fun permissions(id: String): List<DrivePerm> =
        call("GET", "$dr/$id/permissions", driveAll + mapOf("fields" to "permissions(id,type,role,emailAddress,displayName)")).arr("permissions").map {
            val p = it.jsonObject
            DrivePerm(p.str("id").orEmpty(), p.str("emailAddress").orEmpty(), p.str("displayName") ?: p.str("emailAddress").orEmpty(), p.str("role").orEmpty(), p.str("type").orEmpty())
        }

    /** Comparte con una persona. [role]: "writer" (editor), "commenter" o "reader" (lector). Google le avisa por correo. */
    suspend fun share(id: String, email: String, role: String, message: String? = null) {
        call("POST", "$dr/$id/permissions", driveAll + mapOf("sendNotificationEmail" to "true", "emailMessage" to message), buildJsonObject {
            put("type", "user"); put("role", role); put("emailAddress", email.trim())
        })
    }

    suspend fun unshare(id: String, permissionId: String) { call("DELETE", "$dr/$id/permissions/$permissionId", driveAll) }

    fun forgetCache() = folderCache.clear()
}
