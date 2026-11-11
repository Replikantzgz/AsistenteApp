package com.iatualcance.alfred.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class Attachment(
    val id: String = newId(),
    val fileName: String,          // nombre del fichero dentro de la carpeta de adjuntos
    val mime: String,
    val label: String = fileName,
    val driveLink: String? = null,
)

@Serializable
data class Note(
    val id: String = newId(),
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val pinned: Boolean = false,
    val driveFileId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Task(
    val id: String = newId(),
    val title: String,
    val done: Boolean = false,
    val due: String? = null,       // yyyy-MM-dd
    val remindAt: Long? = null,    // momento exacto del aviso (ms); sin él, avisa a las 9:00 del día límite
    val priority: String = "media", // alta | media | baja
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Template(
    val id: String = newId(),
    val name: String,
    val content: String,
)

@Serializable
data class ChatMsg(
    val id: String = newId(),
    val fromUser: Boolean,
    val text: String,
    val actions: List<String> = emptyList(),
    val error: Boolean = false,
    val at: Long = System.currentTimeMillis(),
)
