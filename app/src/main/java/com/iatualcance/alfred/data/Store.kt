package com.iatualcance.alfred.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Datos locales del usuario: notas, tareas, plantillas y conversación. Se guardan en JSON dentro del móvil. */
class Store(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dir = File(ctx.filesDir, "datos").apply { mkdirs() }
    val attachDir = File(ctx.filesDir, "adjuntos").apply { mkdirs() }

    val notes = MutableStateFlow(load("notas.json", Note.serializer()))
    val tasks = MutableStateFlow(load("tareas.json", Task.serializer()))
    val templates = MutableStateFlow(load("plantillas.json", Template.serializer()))
    val chat = MutableStateFlow(load("chat.json", ChatMsg.serializer()))

    private fun <T> load(name: String, s: KSerializer<T>): List<T> = runCatching {
        json.decodeFromString(ListSerializer(s), File(dir, name).readText())
    }.getOrDefault(emptyList())

    @Synchronized
    private fun <T> save(name: String, s: KSerializer<T>, value: List<T>) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(json.encodeToString(ListSerializer(s), value))
        tmp.renameTo(File(dir, name))
    }

    // ---- Notas ----
    fun upsertNote(n: Note): Note {
        val note = n.copy(updatedAt = System.currentTimeMillis())
        notes.update { list -> if (list.any { it.id == note.id }) list.map { if (it.id == note.id) note else it } else listOf(note) + list }
        save("notas.json", Note.serializer(), notes.value)
        return note
    }

    fun deleteNote(id: String) {
        notes.value.firstOrNull { it.id == id }?.attachments?.forEach { fileOf(it).delete() }
        notes.update { l -> l.filterNot { it.id == id } }
        save("notas.json", Note.serializer(), notes.value)
    }

    fun note(id: String) = notes.value.firstOrNull { it.id == id }

    fun searchNotes(q: String): List<Note> {
        val words = q.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return notes.value.filter { n ->
            val hay = (n.title + " " + n.content + " " + n.tags.joinToString(" ")).lowercase()
            words.all { it in hay }
        }
    }

    fun clearNotes() {
        notes.value.forEach { n -> n.attachments.forEach { fileOf(it).delete() } }
        notes.value = emptyList(); save("notas.json", Note.serializer(), emptyList())
    }

    // ---- Tareas ----
    fun upsertTask(t: Task) {
        tasks.update { l -> if (l.any { it.id == t.id }) l.map { if (it.id == t.id) t else it } else listOf(t) + l }
        save("tareas.json", Task.serializer(), tasks.value)
    }

    fun deleteTask(id: String) {
        tasks.update { l -> l.filterNot { it.id == id } }
        save("tareas.json", Task.serializer(), tasks.value)
    }

    fun clearTasks() { tasks.value = emptyList(); save("tareas.json", Task.serializer(), emptyList()) }

    // ---- Plantillas ----
    fun upsertTemplate(t: Template) {
        templates.update { l -> if (l.any { it.id == t.id }) l.map { if (it.id == t.id) t else it } else l + t }
        save("plantillas.json", Template.serializer(), templates.value)
    }

    fun deleteTemplate(id: String) {
        templates.update { l -> l.filterNot { it.id == id } }
        save("plantillas.json", Template.serializer(), templates.value)
    }

    fun clearTemplates() { templates.value = emptyList(); save("plantillas.json", Template.serializer(), emptyList()) }

    // ---- Chat ----
    fun addChat(m: ChatMsg) {
        chat.update { (it + m).takeLast(200) }
        save("chat.json", ChatMsg.serializer(), chat.value)
    }

    fun removeChat(id: String) {
        chat.update { l -> l.filterNot { it.id == id } }
        save("chat.json", ChatMsg.serializer(), chat.value)
    }

    fun clearChat() {
        chat.value = emptyList()
        save("chat.json", ChatMsg.serializer(), emptyList())
    }

    // ---- Adjuntos ----
    fun fileOf(a: Attachment) = File(attachDir, a.fileName)

    fun newAttachmentFile(ext: String): File = File(attachDir, "${newId()}.$ext")

    /** Copia a la carpeta privada de la app un fichero elegido o compartido por el usuario. */
    fun importUri(uri: Uri, fallbackMime: String = "application/octet-stream", label: String? = null): Attachment {
        val mime = ctx.contentResolver.getType(uri) ?: fallbackMime
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: mime.substringAfter('/').substringBefore(';').ifBlank { "bin" }
        val f = newAttachmentFile(ext)
        ctx.contentResolver.openInputStream(uri)!!.use { input -> f.outputStream().use { input.copyTo(it) } }
        return Attachment(fileName = f.name, mime = mime, label = label ?: f.name)
    }

    fun exportAll(): String = json.encodeToString(
        Backup.serializer(), Backup(notes.value, tasks.value, templates.value)
    )

    fun deleteEverything() {
        attachDir.listFiles()?.forEach { it.delete() }
        dir.listFiles()?.forEach { it.delete() }
        notes.value = emptyList(); tasks.value = emptyList(); templates.value = emptyList(); chat.value = emptyList()
    }
}

@kotlinx.serialization.Serializable
data class Backup(val notas: List<Note>, val tareas: List<Task>, val plantillas: List<Template>)
