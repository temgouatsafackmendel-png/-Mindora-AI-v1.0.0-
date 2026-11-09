package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class PlanTier(
    val displayName: String,
    val maxNotes: Int,
    val maxAiQueriesPerMonth: Int,
    val priceLabel: String,
    val features: List<String>
) {
    FREE(
        displayName = "FREE",
        maxNotes = 25,
        maxAiQueriesPerMonth = 20,
        priceLabel = "0 € / mois",
        features = listOf(
            "Jusqu'à 25 notes actives",
            "Dossiers et sous-dossiers illimités",
            "Recherche classique par mots-clés & tags",
            "Quota IA limité (20 requêtes / mois)"
        )
    ),
    PRO(
        displayName = "PRO",
        maxNotes = 10_000,
        maxAiQueriesPerMonth = 1_000,
        priceLabel = "12 € / mois",
        features = listOf(
            "Notes illimitées & Historique des versions",
            "Assistant IA complet & RAG sémantique",
            "Transcription vocale & OCR intelligent",
            "Export avancé PDF, Markdown et TXT",
            "Graphe de connaissances interactif"
        )
    ),
    BUSINESS(
        displayName = "BUSINESS",
        maxNotes = 100_000,
        maxAiQueriesPerMonth = 10_000,
        priceLabel = "29 € / mois / membre",
        features = listOf(
            "Espaces de travail collaboratifs illimités",
            "Partage granulaire (Lecture / Édition) & Commentaires",
            "Journal d'audit de sécurité & Contrôle d'accès",
            "Assistant IA prioritaire & RAG multi-espaces"
        )
    )
}

enum class NoteStatus(val label: String) {
    DRAFT("Brouillon"),
    ACTIVE("Active"),
    REVIEW("À réviser"),
    ARCHIVED("Archivée")
}

enum class TaskStatus(val label: String) {
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    DONE("Terminée"),
    WAITING("En attente")
}

enum class TaskPriority(val label: String, val level: Int) {
    LOW("Basse", 1),
    MEDIUM("Moyenne", 2),
    HIGH("Haute", 3),
    URGENT("Urgente", 4)
}

enum class SharePermission(val label: String) {
    READ("Lecture seule"),
    EDIT("Modification")
}

enum class CaptureType(val label: String) {
    TEXT("Texte rapide"),
    VOICE("Note vocale"),
    PHOTO("Photo & OCR"),
    SCREENSHOT("Capture d'écran"),
    DOCUMENT("Document / PDF"),
    WEB_LINK("Lien web")
}

enum class BlockType(val label: String, val prefix: String) {
    HEADING_1("Titre H1", "# "),
    HEADING_2("Sous-titre H2", "## "),
    PARAGRAPH("Paragraphe", ""),
    BULLET_LIST("Liste à puces", "• "),
    NUMBERED_LIST("Liste numérotée", "1. "),
    CHECKLIST("Case à cocher", "- [ ] "),
    QUOTE("Citation", "> "),
    CODE("Bloc de code", "```"),
    TABLE("Tableau", "| "),
    LINK("Lien hypertexte", "🔗 ")
}

data class NoteBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType = BlockType.PARAGRAPH,
    val text: String = "",
    val isChecked: Boolean = false,
    val metadata: String = ""
)

data class NoteVersionSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val contentPreview: String,
    val timestamp: Long,
    val authorName: String
)

data class ShareComment(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorEmail: String,
    val content: String,
    val createdAt: Long
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val email: String,
    val passwordHash: String,
    val name: String,
    val avatar: String = "",
    val planTier: String = PlanTier.PRO.name,
    val aiQuotaUsed: Int = 0,
    val isEmailVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workspaces",
    indices = [Index(value = ["ownerId"])]
)
data class WorkspaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconName: String = "workspace",
    val colorHex: String = "#7457F5",
    val ownerId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "folders",
    indices = [Index(value = ["userId"]), Index(value = ["parentId"]), Index(value = ["workspaceId"])]
)
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val workspaceId: String,
    val userId: String,
    val colorHex: String = "#7457F5",
    val collectionTag: String = "Général",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["workspaceId"]),
        Index(value = ["folderId"]),
        Index(value = ["updatedAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val blocksJson: String = "[]",
    val summary: String = "",
    val userId: String,
    val workspaceId: String,
    val folderId: String? = null,
    val status: String = NoteStatus.ACTIVE.name,
    val colorHex: String = "#Default",
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val isPinned: Boolean = false,
    val linkedNoteIdsCsv: String = "",
    val versionHistoryJson: String = "[]",
    val lastViewedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["userId"]), Index(value = ["name", "userId"], unique = true)]
)
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#7457F5",
    val userId: String
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"]), Index(value = ["tagId"])]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["userId"]), Index(value = ["noteId"]), Index(value = ["status"])]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val status: String = TaskStatus.TODO.name,
    val priority: String = TaskPriority.MEDIUM.name,
    val category: String = "Général",
    val dueDate: Long = System.currentTimeMillis() + 86_400_000L,
    val noteId: String? = null,
    val workspaceId: String = "",
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class AttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileType: String, // VOICE, PHOTO, PDF, DOCUMENT, SCREENSHOT, WEB_LINK
    val fileUrl: String,
    val transcriptOrOcrText: String = "",
    val durationSeconds: Int = 0,
    val speakerLabels: String = "Intervenant 1",
    val summaryText: String = "",
    val extractedDecisions: String = "",
    val noteId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "embeddings",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class EmbeddingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val contentChunk: String,
    val vectorCsv: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shares",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"]), Index(value = ["userId"])]
)
data class ShareEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val userId: String,
    val invitedEmail: String,
    val invitedName: String = "",
    val permission: String = SharePermission.READ.name,
    val commentsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_conversations",
    indices = [Index(value = ["userId"])]
)
data class AIConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AIConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class AIMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "USER" or "ASSISTANT"
    val content: String,
    val citedNoteIdsCsv: String = "",
    val citedNoteTitlesCsv: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "security_logs",
    indices = [Index(value = ["userId"])]
)
data class SecurityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val eventType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

object MindoraJsonCodec {
    fun encodeBlocks(blocks: List<NoteBlock>): String {
        val array = JSONArray()
        blocks.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("type", b.type.name)
            obj.put("text", b.text)
            obj.put("isChecked", b.isChecked)
            obj.put("metadata", b.metadata)
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeBlocks(json: String): List<NoteBlock> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { idx ->
                val obj = array.getJSONObject(idx)
                val typeName = obj.optString("type", BlockType.PARAGRAPH.name)
                val blockType = BlockType.entries.find { it.name == typeName } ?: BlockType.PARAGRAPH
                NoteBlock(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    type = blockType,
                    text = obj.optString("text", ""),
                    isChecked = obj.optBoolean("isChecked", false),
                    metadata = obj.optString("metadata", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeVersions(versions: List<NoteVersionSnapshot>): String {
        val array = JSONArray()
        versions.take(20).forEach { v ->
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("title", v.title)
            obj.put("contentPreview", v.contentPreview)
            obj.put("timestamp", v.timestamp)
            obj.put("authorName", v.authorName)
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeVersions(json: String): List<NoteVersionSnapshot> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { idx ->
                val obj = array.getJSONObject(idx)
                NoteVersionSnapshot(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    contentPreview = obj.optString("contentPreview", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    authorName = obj.optString("authorName", "Utilisateur")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeComments(comments: List<ShareComment>): String {
        val array = JSONArray()
        comments.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("authorName", c.authorName)
            obj.put("authorEmail", c.authorEmail)
            obj.put("content", c.content)
            obj.put("createdAt", c.createdAt)
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeComments(json: String): List<ShareComment> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { idx ->
                val obj = array.getJSONObject(idx)
                ShareComment(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    authorName = obj.optString("authorName", ""),
                    authorEmail = obj.optString("authorEmail", ""),
                    content = obj.optString("content", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
