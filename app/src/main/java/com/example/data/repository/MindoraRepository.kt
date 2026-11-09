package com.example.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.local.AIConversationEntity
import com.example.data.local.AIMessageEntity
import com.example.data.local.AttachmentEntity
import com.example.data.local.BlockType
import com.example.data.local.EmbeddingEntity
import com.example.data.local.FolderEntity
import com.example.data.local.MindoraDao
import com.example.data.local.MindoraJsonCodec
import com.example.data.local.NoteBlock
import com.example.data.local.NoteEntity
import com.example.data.local.NoteStatus
import com.example.data.local.NoteTagCrossRef
import com.example.data.local.NoteVersionSnapshot
import com.example.data.local.PlanTier
import com.example.data.local.SecurityLogEntity
import com.example.data.local.ShareComment
import com.example.data.local.ShareEntity
import com.example.data.local.SharePermission
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.UserEntity
import com.example.data.local.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class MindoraRepository(
    private val dao: MindoraDao,
    private val aiRepository: AiRepository
) {
    // --- SECURITY & CRYPTO HELPERS ---
    fun hashPassword(password: String, salt: String = "mindora_saas_salt_v1"): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sanitizeInput(raw: String): String {
        return raw.replace("<script>", "", ignoreCase = true)
            .replace("</script>", "", ignoreCase = true)
            .trim()
    }

    suspend fun logSecurityEvent(userId: String, eventType: String, details: String) {
        dao.insertSecurityLog(
            SecurityLogEntity(
                userId = userId,
                eventType = eventType,
                details = details
            )
        )
    }

    // --- AUTHENTICATION & INITIAL WORKSPACE SEEDING ---
    suspend fun ensureDefaultDemoAccount(): UserEntity {
        val defaultEmail = "mendeltemgouabusiness@gmail.com"
        val existing = dao.getUserByEmail(defaultEmail)
        if (existing != null) {
            val workspaces = dao.getWorkspacesOnce(existing.id)
            if (workspaces.isEmpty()) {
                seedInitialSecondBrain(existing)
            }
            return existing
        }

        val newUser = UserEntity(
            id = "user_mindora_primary",
            email = defaultEmail,
            passwordHash = hashPassword("Mindora2026!"),
            name = "Mendel Temgoua",
            avatar = "MT",
            planTier = PlanTier.PRO.name,
            aiQuotaUsed = 4,
            isEmailVerified = true
        )
        dao.insertUser(newUser)
        seedInitialSecondBrain(newUser)
        logSecurityEvent(newUser.id, "AUTH_INIT", "Session espace de travail initialisée (Créateur : Mendel Temgoua).")
        return newUser
    }

    suspend fun loginWithEmail(email: String, password: String): Result<UserEntity> {
        val cleanEmail = sanitizeInput(email).lowercase()
        if (!cleanEmail.contains("@") || password.length < 6) {
            return Result.failure(IllegalArgumentException("Veuillez saisir un email valide et un mot de passe d'au moins 6 caractères."))
        }
        val user = dao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("Aucun compte trouvé pour cet email. Créez un compte en un clic."))
        val expectedHash = hashPassword(password)
        if (user.passwordHash != expectedHash) {
            logSecurityEvent(user.id, "AUTH_FAILED", "Tentative de connexion échouée pour $cleanEmail")
            return Result.failure(IllegalArgumentException("Mot de passe incorrect."))
        }
        logSecurityEvent(user.id, "AUTH_LOGIN", "Connexion réussie pour $cleanEmail")
        return Result.success(user)
    }

    suspend fun registerWithEmail(name: String, email: String, password: String): Result<UserEntity> {
        val cleanName = sanitizeInput(name)
        val cleanEmail = sanitizeInput(email).lowercase()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom complet est requis."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Format d'email invalide."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères."))
        }
        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Un compte existe déjà avec cet email."))
        }

        val newUser = UserEntity(
            email = cleanEmail,
            passwordHash = hashPassword(password),
            name = cleanName,
            avatar = cleanName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
            planTier = PlanTier.FREE.name,
            aiQuotaUsed = 0,
            isEmailVerified = true
        )
        dao.insertUser(newUser)
        seedInitialSecondBrain(newUser)
        logSecurityEvent(newUser.id, "AUTH_REGISTER", "Nouveau compte créé et isolé : $cleanEmail")
        return Result.success(newUser)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<String> {
        val cleanEmail = sanitizeInput(email).lowercase()
        val user = dao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("Aucun compte associé à $cleanEmail"))
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Le nouveau mot de passe doit comporter au moins 6 caractères."))
        }
        dao.updateUser(user.copy(passwordHash = hashPassword(newPassword)))
        logSecurityEvent(user.id, "PASSWORD_RESET", "Mot de passe réinitialisé avec succès")
        return Result.success("Mot de passe mis à jour pour $cleanEmail")
    }

    suspend fun updateSubscriptionPlan(user: UserEntity, newPlan: PlanTier) {
        dao.updateUser(user.copy(planTier = newPlan.name))
        logSecurityEvent(user.id, "PLAN_CHANGED", "Passage à l'offre SaaS ${newPlan.displayName}")
    }

    // --- STREAMS ---
    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)
    fun observeWorkspaces(userId: String): Flow<List<WorkspaceEntity>> = dao.observeWorkspaces(userId)
    fun observeFolders(userId: String): Flow<List<FolderEntity>> = dao.observeFolders(userId)
    fun observeActiveNotes(userId: String): Flow<List<NoteEntity>> = dao.observeActiveNotes(userId)
    fun observeTrashedNotes(userId: String): Flow<List<NoteEntity>> = dao.observeTrashedNotes(userId)
    fun observeTags(userId: String): Flow<List<TagEntity>> = dao.observeTags(userId)
    fun observeNoteTagCrossRefs(): Flow<List<NoteTagCrossRef>> = dao.observeAllNoteTagCrossRefs()
    fun observeTasks(userId: String): Flow<List<TaskEntity>> = dao.observeTasks(userId)
    fun observeAllAttachments(): Flow<List<AttachmentEntity>> = dao.observeAllAttachments()
    fun observeShares(userId: String): Flow<List<ShareEntity>> = dao.observeShares(userId)
    fun observeConversations(userId: String): Flow<List<AIConversationEntity>> = dao.observeConversations(userId)
    fun observeMessages(conversationId: String): Flow<List<AIMessageEntity>> = dao.observeMessages(conversationId)
    fun observeSecurityLogs(userId: String): Flow<List<SecurityLogEntity>> = dao.observeSecurityLogs(userId)

    // --- NOTE OPERATIONS WITH AUTO-EMBEDDING & VERSION HISTORY ---
    suspend fun saveNoteWithIndexing(
        note: NoteEntity,
        authorName: String,
        recordVersionSnapshot: Boolean = false
    ) {
        val updatedHistory = if (recordVersionSnapshot) {
            val currentVersions = MindoraJsonCodec.decodeVersions(note.versionHistoryJson).toMutableList()
            currentVersions.add(
                0,
                NoteVersionSnapshot(
                    title = note.title,
                    contentPreview = note.content.take(140),
                    timestamp = System.currentTimeMillis(),
                    authorName = authorName
                )
            )
            MindoraJsonCodec.encodeVersions(currentVersions)
        } else {
            note.versionHistoryJson
        }

        val finalNote = note.copy(
            versionHistoryJson = updatedHistory,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertNote(finalNote)

        // Mise à jour des embeddings RAG par chunks
        dao.deleteEmbeddingsForNote(finalNote.id)
        val chunks = finalNote.content
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(finalNote.title) }

        chunks.forEach { chunk ->
            val vec = aiRepository.computeEmbeddingVector("${finalNote.title} $chunk")
            dao.insertEmbedding(
                EmbeddingEntity(
                    noteId = finalNote.id,
                    contentChunk = chunk,
                    vectorCsv = vec.joinToString(",")
                )
            )
        }
    }

    suspend fun toggleNoteFavorite(note: NoteEntity) {
        dao.updateNote(note.copy(isFavorite = !note.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveNoteToTrash(note: NoteEntity) {
        dao.updateNote(note.copy(isTrashed = true, updatedAt = System.currentTimeMillis()))
        logSecurityEvent(note.userId, "NOTE_TRASHED", "Note déplacée dans la corbeille : ${note.title}")
    }

    suspend fun restoreNoteFromTrash(note: NoteEntity) {
        dao.updateNote(note.copy(isTrashed = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNoteForever(noteId: String, userId: String) {
        dao.deleteNotePermanently(noteId, userId)
        logSecurityEvent(userId, "NOTE_DELETED_PERMANENT", "Suppression définitive de la note $noteId")
    }

    suspend fun emptyUserTrash(userId: String) {
        dao.emptyTrash(userId)
        logSecurityEvent(userId, "TRASH_EMPTIED", "Corbeille vidée par l'utilisateur")
    }

    suspend fun markNoteViewed(note: NoteEntity) {
        dao.updateNote(note.copy(lastViewedAt = System.currentTimeMillis()))
    }

    // --- FOLDERS, TAGS & WORKSPACES ---
    suspend fun createWorkspace(userId: String, name: String, description: String, colorHex: String): WorkspaceEntity {
        val ws = WorkspaceEntity(
            name = sanitizeInput(name),
            description = sanitizeInput(description),
            colorHex = colorHex,
            ownerId = userId
        )
        dao.insertWorkspace(ws)
        logSecurityEvent(userId, "WORKSPACE_CREATED", "Nouvel espace de travail : ${ws.name}")
        return ws
    }

    suspend fun createFolder(
        userId: String,
        workspaceId: String,
        name: String,
        parentId: String?,
        colorHex: String,
        collectionTag: String
    ): FolderEntity {
        val folder = FolderEntity(
            name = sanitizeInput(name),
            parentId = parentId,
            workspaceId = workspaceId,
            userId = userId,
            colorHex = colorHex,
            collectionTag = sanitizeInput(collectionTag).ifBlank { "Général" }
        )
        dao.insertFolder(folder)
        return folder
    }

    suspend fun deleteFolder(folderId: String, userId: String) {
        dao.deleteFolder(folderId, userId)
    }

    suspend fun createAndAttachTag(userId: String, noteId: String?, tagName: String, colorHex: String): TagEntity {
        val clean = sanitizeInput(tagName).removePrefix("#")
        val existing = dao.getTagsOnce(userId).firstOrNull { it.name.equals(clean, ignoreCase = true) }
        val tag = existing ?: TagEntity(name = clean, colorHex = colorHex, userId = userId).also {
            dao.insertTag(it)
        }
        if (noteId != null) {
            dao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tag.id))
        }
        return tag
    }

    suspend fun toggleTagOnNote(noteId: String, tagId: String, isCurrentlyAttached: Boolean) {
        if (isCurrentlyAttached) {
            dao.removeNoteTagCrossRef(noteId, tagId)
        } else {
            dao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
    }

    // --- TASKS ---
    suspend fun saveTask(task: TaskEntity) {
        dao.insertTask(task)
    }

    suspend fun cycleTaskStatus(task: TaskEntity) {
        val next = when (TaskStatus.entries.find { it.name == task.status } ?: TaskStatus.TODO) {
            TaskStatus.TODO -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.DONE
            TaskStatus.DONE -> TaskStatus.WAITING
            TaskStatus.WAITING -> TaskStatus.TODO
        }
        dao.updateTask(task.copy(status = next.name))
    }

    suspend fun updateTaskStatus(task: TaskEntity, status: TaskStatus) {
        dao.updateTask(task.copy(status = status.name))
    }

    suspend fun deleteTask(taskId: String, userId: String) {
        dao.deleteTask(taskId, userId)
    }

    // --- ATTACHMENTS & VOICE NOTES ---
    suspend fun addAttachment(attachment: AttachmentEntity) {
        dao.insertAttachment(attachment)
    }

    // --- COLLABORATION & SHARES ---
    suspend fun shareNoteWithCollaborator(
        noteId: String,
        ownerUserId: String,
        invitedEmail: String,
        invitedName: String,
        permission: SharePermission
    ): ShareEntity {
        val initialComment = ShareComment(
            authorName = invitedName.ifBlank { invitedEmail.substringBefore("@") },
            authorEmail = invitedEmail,
            content = "Merci pour le partage ! J'ai bien accès au document en mode ${permission.label}.",
            createdAt = System.currentTimeMillis()
        )
        val share = ShareEntity(
            noteId = noteId,
            userId = ownerUserId,
            invitedEmail = sanitizeInput(invitedEmail),
            invitedName = sanitizeInput(invitedName).ifBlank { invitedEmail.substringBefore("@") },
            permission = permission.name,
            commentsJson = MindoraJsonCodec.encodeComments(listOf(initialComment))
        )
        dao.insertShare(share)
        logSecurityEvent(
            ownerUserId,
            "NOTE_SHARED",
            "Note $noteId partagée avec ${share.invitedEmail} (${permission.name})"
        )
        return share
    }

    suspend fun addCommentToShare(share: ShareEntity, authorName: String, authorEmail: String, commentText: String) {
        val existing = MindoraJsonCodec.decodeComments(share.commentsJson).toMutableList()
        existing.add(
            ShareComment(
                authorName = authorName,
                authorEmail = authorEmail,
                content = sanitizeInput(commentText),
                createdAt = System.currentTimeMillis()
            )
        )
        dao.updateShare(share.copy(commentsJson = MindoraJsonCodec.encodeComments(existing)))
    }

    suspend fun updateSharePermission(share: ShareEntity, newPermission: SharePermission) {
        dao.updateShare(share.copy(permission = newPermission.name))
        logSecurityEvent(share.userId, "SHARE_PERMISSION_UPDATED", "Permission mise à jour vers ${newPermission.name} pour ${share.invitedEmail}")
    }

    suspend fun revokeShare(share: ShareEntity) {
        dao.deleteShare(share.id, share.userId)
        logSecurityEvent(share.userId, "SHARE_REVOKED", "Accès révoqué pour ${share.invitedEmail}")
    }

    // --- AI CONVERSATIONS & RAG ---
    suspend fun ensureActiveConversation(userId: String): AIConversationEntity {
        val conv = AIConversationEntity(
            id = "conv_default_$userId",
            userId = userId,
            title = "Exploration Base de Connaissances"
        )
        dao.insertConversation(conv)
        return conv
    }

    suspend fun sendRagQuestion(
        user: UserEntity,
        conversationId: String,
        question: String,
        allUserNotes: List<NoteEntity>
    ): RagAnswer {
        val cleanQ = sanitizeInput(question)
        dao.insertMessage(
            AIMessageEntity(
                conversationId = conversationId,
                role = "USER",
                content = cleanQ
            )
        )
        val ragAnswer = aiRepository.queryKnowledgeBaseRag(cleanQ, allUserNotes)
        dao.insertMessage(
            AIMessageEntity(
                conversationId = conversationId,
                role = "ASSISTANT",
                content = ragAnswer.answerText,
                citedNoteIdsCsv = ragAnswer.citations.joinToString(",") { it.noteId },
                citedNoteTitlesCsv = ragAnswer.citations.joinToString(" • ") { it.noteTitle }
            )
        )
        dao.updateUser(user.copy(aiQuotaUsed = user.aiQuotaUsed + 1))
        return ragAnswer
    }

    // --- EXPORT TO PDF, MARKDOWN, TXT ---
    fun exportNoteToFile(context: Context, note: NoteEntity, format: String): File {
        val exportDir = File(context.cacheDir, "mindora_exports").apply { mkdirs() }
        val safeTitle = note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "note" }

        return when (format.uppercase()) {
            "PDF" -> {
                val pdfFile = File(exportDir, "${safeTitle}_mindora.pdf")
                val pdfDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                val titlePaint = Paint().apply {
                    color = AndroidColor.rgb(16, 17, 20)
                    textSize = 20f
                    isFakeBoldText = true
                }
                val metaPaint = Paint().apply {
                    color = AndroidColor.rgb(116, 87, 245)
                    textSize = 11f
                }
                val bodyPaint = Paint().apply {
                    color = AndroidColor.rgb(40, 42, 54)
                    textSize = 12f
                }

                var y = 55f
                canvas.drawText("MINDORA AI — Deuxième Cerveau Numérique", 40f, y, metaPaint)
                y += 30f
                canvas.drawText(note.title.take(55), 40f, y, titlePaint)
                y += 28f

                if (note.summary.isNotBlank()) {
                    canvas.drawText("Résumé IA : ${note.summary.take(80)}", 40f, y, metaPaint)
                    y += 24f
                }

                note.content.lines().forEach { line ->
                    val chunks = line.chunked(82).ifEmpty { listOf("") }
                    chunks.forEach { chunk ->
                        if (y < 790f) {
                            canvas.drawText(chunk, 40f, y, bodyPaint)
                            y += 18f
                        }
                    }
                }

                pdfDoc.finishPage(page)
                pdfFile.outputStream().use { out -> pdfDoc.writeTo(out) }
                pdfDoc.close()
                pdfFile
            }

            "MD", "MARKDOWN" -> {
                val mdFile = File(exportDir, "${safeTitle}.md")
                mdFile.writeText(
                    buildString {
                        appendLine("# ${note.title}")
                        appendLine()
                        if (note.summary.isNotBlank()) {
                            appendLine("> **Résumé IA :** ${note.summary}")
                            appendLine()
                        }
                        appendLine(note.content)
                    }
                )
                mdFile
            }

            else -> {
                val txtFile = File(exportDir, "${safeTitle}.txt")
                txtFile.writeText("${note.title.uppercase()}\n====================\n\n${note.content}")
                txtFile
            }
        }
    }

    fun exportFullWorkspaceBackup(
        context: Context,
        user: UserEntity,
        notes: List<NoteEntity>,
        tasks: List<TaskEntity>
    ): File {
        val exportDir = File(context.cacheDir, "mindora_exports").apply { mkdirs() }
        val backupFile = File(exportDir, "Mindora_AI_Backup_${user.name.replace(" ", "_")}.md")
        backupFile.writeText(
            buildString {
                appendLine("# SAUVEGARDE COMPLÈTE MINDORA AI")
                appendLine("- **Auteur / Propriétaire :** ${user.name} (${user.email})")
                appendLine("- **Offre SaaS :** ${user.planTier}")
                appendLine("- **Notes exportées :** ${notes.size}")
                appendLine("- **Tâches exportées :** ${tasks.size}")
                appendLine()
                appendLine("---")
                appendLine("## 1. NOTES & CONNAISSANCES")
                notes.forEachIndexed { idx, n ->
                    appendLine("### ${idx + 1}. ${n.title}")
                    if (n.summary.isNotBlank()) appendLine("> Résumé IA : ${n.summary}")
                    appendLine()
                    appendLine(n.content)
                    appendLine()
                    appendLine("---")
                }
                appendLine("## 2. TÂCHES & PLAN D'ACTION")
                tasks.forEach { t ->
                    appendLine("- [${if (t.status == "DONE") "x" else " "}] **${t.title}** (${t.priority} • ${t.status}) : ${t.description}")
                }
            }
        )
        return backupFile
    }

    // --- SEED INITIAL RICH DATA FOR REALISTIC SECOND BRAIN ---
    private suspend fun seedInitialSecondBrain(user: UserEntity) {
        val now = System.currentTimeMillis()

        // 1. Workspaces
        val wsProduct = WorkspaceEntity(
            id = "ws_saas_${user.id}",
            name = "Architecture & Produit SaaS",
            description = "Stratégie produit, architecture cloud, IA & roadmap Mindora",
            iconName = "rocket",
            colorHex = "#7457F5",
            ownerId = user.id,
            createdAt = now - 300_000_000L
        )
        val wsPersonal = WorkspaceEntity(
            id = "ws_personal_${user.id}",
            name = "Deuxième Cerveau & Veille IA",
            description = "Notes de recherche, lectures, idées et réflexions personnelles",
            iconName = "brain",
            colorHex = "#4285F4",
            ownerId = user.id,
            createdAt = now - 250_000_000L
        )
        val wsTeam = WorkspaceEntity(
            id = "ws_team_${user.id}",
            name = "Équipe & Collaboration",
            description = "Comptes rendus de réunions, sprints et partages clients",
            iconName = "team",
            colorHex = "#10B981",
            ownerId = user.id,
            createdAt = now - 200_000_000L
        )
        dao.insertWorkspace(wsProduct)
        dao.insertWorkspace(wsPersonal)
        dao.insertWorkspace(wsTeam)

        // 2. Nested Folders & Sub-folders
        val folderArch = FolderEntity(
            id = "folder_arch_${user.id}",
            name = "Architecture & IA",
            parentId = null,
            workspaceId = wsProduct.id,
            userId = user.id,
            colorHex = "#7457F5",
            collectionTag = "Ingénierie"
        )
        val folderRagSub = FolderEntity(
            id = "folder_rag_sub_${user.id}",
            name = "Système RAG & Embeddings",
            parentId = folderArch.id,
            workspaceId = wsProduct.id,
            userId = user.id,
            colorHex = "#4285F4",
            collectionTag = "IA & Vecteurs"
        )
        val folderMeetings = FolderEntity(
            id = "folder_meetings_${user.id}",
            name = "Réunions & Décisions",
            parentId = null,
            workspaceId = wsTeam.id,
            userId = user.id,
            colorHex = "#10B981",
            collectionTag = "Management"
        )
        val folderIdeas = FolderEntity(
            id = "folder_ideas_${user.id}",
            name = "Idées & Projets Innovants",
            parentId = null,
            workspaceId = wsPersonal.id,
            userId = user.id,
            colorHex = "#F59E0B",
            collectionTag = "Idéation"
        )
        dao.insertFolder(folderArch)
        dao.insertFolder(folderRagSub)
        dao.insertFolder(folderMeetings)
        dao.insertFolder(folderIdeas)

        // 3. Custom Tags
        val tagSaas = TagEntity(id = "tag_saas_${user.id}", name = "SaaS", colorHex = "#7457F5", userId = user.id)
        val tagRag = TagEntity(id = "tag_rag_${user.id}", name = "RAG & IA", colorHex = "#4285F4", userId = user.id)
        val tagMeeting = TagEntity(id = "tag_meeting_${user.id}", name = "Réunion", colorHex = "#10B981", userId = user.id)
        val tagSecurity = TagEntity(id = "tag_sec_${user.id}", name = "Sécurité", colorHex = "#EF4444", userId = user.id)
        val tagRoadmap = TagEntity(id = "tag_roadmap_${user.id}", name = "Roadmap", colorHex = "#F59E0B", userId = user.id)
        listOf(tagSaas, tagRag, tagMeeting, tagSecurity, tagRoadmap).forEach { dao.insertTag(it) }

        // 4. Rich Notes with Blocks, Bidirectional Links, and History
        val note1Id = "note_saas_arch_${user.id}"
        val note2Id = "note_meeting_q4_${user.id}"
        val note3Id = "note_rag_pipeline_${user.id}"
        val note4Id = "note_ideas_voice_${user.id}"

        val note1Blocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "Architecture SaaS Mindora AI v2.0"),
            NoteBlock(type = BlockType.PARAGRAPH, text = "Notre vision est de créer un véritable deuxième cerveau numérique combinant prise de notes structurée, capture vocale et recherche sémantique RAG."),
            NoteBlock(type = BlockType.HEADING_2, text = "Piliers Techniques & Sécurité"),
            NoteBlock(type = BlockType.CHECKLIST, text = "Isolation stricte des données par utilisateur (Zero-Trust)", isChecked = true),
            NoteBlock(type = BlockType.CHECKLIST, text = "Pipeline d'embeddings vectoriels pour la recherche sémantique", isChecked = true),
            NoteBlock(type = BlockType.CHECKLIST, text = "Export multi-format natif PDF, Markdown et TXT", isChecked = false),
            NoteBlock(type = BlockType.CODE, text = "SELECT * FROM embeddings WHERE cosine_similarity(vector, query_vec) > 0.75;"),
            NoteBlock(type = BlockType.QUOTE, text = "Capture. Understand. Organize. Create. — Slogan officiel Mindora AI")
        )

        val note1 = NoteEntity(
            id = note1Id,
            title = "Architecture Projet SaaS & Feuille de Route Mindora",
            content = """
                # Architecture Projet SaaS & Feuille de Route Mindora
                
                Notre vision est de créer un véritable deuxième cerveau numérique combinant prise de notes structurée, capture vocale et recherche sémantique RAG.
                
                ## Piliers Techniques & Sécurité
                - [x] Isolation stricte des données par utilisateur (Zero-Trust)
                - [x] Pipeline d'embeddings vectoriels pour la recherche sémantique
                - [ ] Export multi-format natif PDF, Markdown et TXT
                
                ```sql
                SELECT * FROM embeddings WHERE cosine_similarity(vector, query_vec) > 0.75;
                ```
                
                > Capture. Understand. Organize. Create. — Slogan officiel Mindora AI
            """.trimIndent(),
            blocksJson = MindoraJsonCodec.encodeBlocks(note1Blocks),
            summary = "Architecture cible du projet SaaS Mindora AI : isolation des données, moteur RAG vectoriel et export PDF/Markdown.",
            userId = user.id,
            workspaceId = wsProduct.id,
            folderId = folderArch.id,
            status = NoteStatus.ACTIVE.name,
            colorHex = "#7457F5",
            isFavorite = true,
            isPinned = true,
            linkedNoteIdsCsv = "$note3Id,$note2Id",
            versionHistoryJson = MindoraJsonCodec.encodeVersions(
                listOf(
                    NoteVersionSnapshot(
                        title = "Architecture Projet SaaS & Feuille de Route Mindora",
                        contentPreview = "Ajout de la spécification Zero-Trust et du pipeline RAG.",
                        timestamp = now - 3600_000L,
                        authorName = user.name
                    ),
                    NoteVersionSnapshot(
                        title = "Brouillon Architecture SaaS",
                        contentPreview = "Première esquisse des modules de prise de notes et base de données.",
                        timestamp = now - 86_400_000L,
                        authorName = user.name
                    )
                )
            ),
            createdAt = now - 172_800_000L,
            updatedAt = now - 1_800_000L
        )

        val note2Blocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "Compte Rendu Réunion Produit & Décisions Clés"),
            NoteBlock(type = BlockType.PARAGRAPH, text = "Synthèse de notre dernière réunion d'alignement produit, design UX/UI et monétisation SaaS."),
            NoteBlock(type = BlockType.HEADING_2, text = "Décisions prises en réunion"),
            NoteBlock(type = BlockType.BULLET_LIST, text = "Décision 1 : Adopter la palette Noir profond (#101114) et Violet IA (#7457F5) avec typographie Inter."),
            NoteBlock(type = BlockType.BULLET_LIST, text = "Décision 2 : Structurer 3 offres d'abonnement SaaS : FREE (25 notes), PRO (12€/mois) et BUSINESS (29€/mois)."),
            NoteBlock(type = BlockType.BULLET_LIST, text = "Décision 3 : Prioriser le module de Capture Rapide (Voix, OCR Photo, Lien Web) accessible en un clic."),
            NoteBlock(type = BlockType.TABLE, text = "Offre | Quota IA | Partage Équipe\nFREE | 20 req/mois | Lecture\nPRO | 1 000 req/mois | Édition\nBUSINESS | 10 000 req/mois | Admin & Audit")
        )

        val note2 = NoteEntity(
            id = note2Id,
            title = "Décisions Réunion Comité Produit & Monétisation SaaS",
            content = """
                # Compte Rendu Réunion Produit & Décisions Clés
                
                Synthèse de notre dernière réunion d'alignement produit, design UX/UI et monétisation SaaS.
                
                ## Décisions prises lors de la dernière réunion
                • Décision 1 : Adopter la palette Noir profond (#101114) et Violet IA (#7457F5) avec typographie Inter.
                • Décision 2 : Structurer 3 offres d'abonnement SaaS : FREE (25 notes), PRO (12€/mois) et BUSINESS (29€/mois).
                • Décision 3 : Prioriser le module de Capture Rapide (Voix, OCR Photo, Lien Web) accessible en un clic.
                
                | Offre | Quota IA | Partage Équipe |
                | :--- | :--- | :--- |
                | FREE | 20 req/mois | Lecture |
                | PRO | 1 000 req/mois | Édition |
                | BUSINESS | 10 000 req/mois | Admin & Audit |
            """.trimIndent(),
            blocksJson = MindoraJsonCodec.encodeBlocks(note2Blocks),
            summary = "Décisions actées : design system #101114/#7457F5, tarification FREE/PRO/BUSINESS et lancement du module Capture Rapide.",
            userId = user.id,
            workspaceId = wsTeam.id,
            folderId = folderMeetings.id,
            status = NoteStatus.ACTIVE.name,
            colorHex = "#10B981",
            isFavorite = true,
            linkedNoteIdsCsv = note1Id,
            createdAt = now - 95_000_000L,
            updatedAt = now - 7_200_000L
        )

        val note3 = NoteEntity(
            id = note3Id,
            title = "Ingénierie RAG, Embeddings & Graphe de Connaissances",
            content = """
                # Ingénierie RAG, Embeddings & Graphe de Connaissances
                
                Le moteur de recherche sémantique découpe chaque note en segments (chunks), calcule un vecteur d'embedding normalisé et retrouve les passages pertinents par similarité cosinus.
                
                ## Fonctionnalités Clés
                1. Recherche hybride : plein texte, tags, dates, transcriptions audio et similarité vectorielle.
                2. Citations vérifiables : chaque réponse de l'assistant IA affiche les notes sources exactes.
                3. Liens bidirectionnels : détection automatique des connexions entre concepts proches.
            """.trimIndent(),
            blocksJson = MindoraJsonCodec.encodeBlocks(
                listOf(
                    NoteBlock(type = BlockType.HEADING_1, text = "Ingénierie RAG, Embeddings & Graphe de Connaissances"),
                    NoteBlock(type = BlockType.PARAGRAPH, text = "Le moteur de recherche sémantique découpe chaque note en segments (chunks), calcule un vecteur d'embedding normalisé et retrouve les passages pertinents par similarité cosinus."),
                    NoteBlock(type = BlockType.NUMBERED_LIST, text = "Recherche hybride : plein texte, tags, dates, transcriptions audio et similarité vectorielle."),
                    NoteBlock(type = BlockType.NUMBERED_LIST, text = "Citations vérifiables : chaque réponse de l'assistant IA affiche les notes sources exactes.")
                )
            ),
            summary = "Spécification du pipeline RAG : découpage en chunks, similarité cosinus et citations des sources dans le chat IA.",
            userId = user.id,
            workspaceId = wsProduct.id,
            folderId = folderRagSub.id,
            status = NoteStatus.REVIEW.name,
            colorHex = "#4285F4",
            isFavorite = false,
            linkedNoteIdsCsv = "$note1Id,$note4Id",
            createdAt = now - 60_000_000L,
            updatedAt = now - 14_400_000L
        )

        val note4 = NoteEntity(
            id = note4Id,
            title = "Idées de Projets IA & Automatisation Vocale Mobile",
            content = """
                # Idées de Projets IA & Automatisation Vocale Mobile
                
                Liste de mes idées de projets prioritaires à développer ce trimestre :
                • Idée 1 : Assistant de réunion vocal avec identification des intervenants et création automatique de tâches Kanban.
                • Idée 2 : Scanner OCR intelligent pour extraire instantanément les tableaux et dates depuis un tableau blanc ou un contrat PDF.
                • Idée 3 : Briefing matinal « Votre journée avec l'IA » synthétisant les priorités, échéances et idées à reprendre.
            """.trimIndent(),
            blocksJson = MindoraJsonCodec.encodeBlocks(
                listOf(
                    NoteBlock(type = BlockType.HEADING_1, text = "Idées de Projets IA & Automatisation Vocale Mobile"),
                    NoteBlock(type = BlockType.BULLET_LIST, text = "Idée 1 : Assistant de réunion vocal avec identification des intervenants et création automatique de tâches Kanban."),
                    NoteBlock(type = BlockType.BULLET_LIST, text = "Idée 2 : Scanner OCR intelligent pour extraire instantanément les tableaux et dates depuis un tableau blanc."),
                    NoteBlock(type = BlockType.BULLET_LIST, text = "Idée 3 : Briefing matinal « Votre journée avec l'IA » synthétisant les priorités.")
                )
            ),
            summary = "Trois idées de projets phares : diarisation vocale vers Kanban, OCR de tableaux blancs et briefing quotidien IA.",
            userId = user.id,
            workspaceId = wsPersonal.id,
            folderId = folderIdeas.id,
            status = NoteStatus.ACTIVE.name,
            colorHex = "#F59E0B",
            isFavorite = true,
            linkedNoteIdsCsv = note3Id,
            createdAt = now - 40_000_000L,
            updatedAt = now - 3_600_000L
        )

        listOf(note1, note2, note3, note4).forEach { note ->
            saveNoteWithIndexing(note, user.name, recordVersionSnapshot = false)
        }

        // Attach tags
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note1Id, tagSaas.id))
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note1Id, tagSecurity.id))
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note2Id, tagMeeting.id))
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note2Id, tagRoadmap.id))
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note3Id, tagRag.id))
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note4Id, tagRag.id))
        dao.insertNoteTagCrossRef(NoteTagCrossRef(note4Id, tagRoadmap.id))

        // 5. Voice & OCR Attachments
        dao.insertAttachment(
            AttachmentEntity(
                fileName = "Brief_Vocal_Architecture_SaaS.m4a",
                fileType = "VOICE",
                fileUrl = "local://voice/brief_saas_01.m4a",
                transcriptOrOcrText = "[Intervenant 1 - Alex] : Nous devons absolument garantir que la recherche sémantique fonctionne même lorsque l'utilisateur ne connaît pas le mot exact.\n[Intervenant 2 - Clara] : Entendu, je valide l'indexation par chunks avec citations cliquables dans le chat IA.",
                durationSeconds = 94,
                speakerLabels = "Intervenant 1 (Alex), Intervenant 2 (Clara)",
                summaryText = "Validation du moteur de recherche sémantique et des citations cliquables pour le MVP.",
                extractedDecisions = "Activer l'indexation vectorielle automatique à chaque sauvegarde de note.",
                noteId = note1Id,
                createdAt = now - 10_000_000L
            )
        )

        dao.insertAttachment(
            AttachmentEntity(
                fileName = "Schema_Whiteboard_OCR.png",
                fileType = "PHOTO",
                fileUrl = "local://ocr/whiteboard_rag.png",
                transcriptOrOcrText = "OCR EXTRAIT : Pipeline Mindora -> [Capture Multimodale] -> [Découpage Blocs] -> [Vecteurs 24D] -> [Assistant RAG & Graphe]",
                durationSeconds = 0,
                speakerLabels = "Scanner OCR Mindora",
                summaryText = "Schéma d'architecture extrait par reconnaissance optique de caractères (OCR).",
                extractedDecisions = "Conserver un flux unifié entre la capture rapide et l'éditeur de blocs.",
                noteId = note3Id,
                createdAt = now - 8_000_000L
            )
        )

        // 6. Tasks (TODO, IN_PROGRESS, DONE, WAITING)
        dao.insertTask(
            TaskEntity(
                title = "Finaliser le pipeline RAG avec citations des sources",
                description = "Permettre de cliquer sur chaque note citée par l'assistant IA pour l'ouvrir dans l'éditeur.",
                status = TaskStatus.IN_PROGRESS.name,
                priority = TaskPriority.URGENT.name,
                category = "IA & RAG",
                dueDate = now + 43_200_000L, // Demain
                noteId = note3Id,
                workspaceId = wsProduct.id,
                userId = user.id
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Valider la grille tarifaire FREE / PRO / BUSINESS",
                description = "Vérifier les quotas de notes et d'appels IA définis lors du comité produit.",
                status = TaskStatus.TODO.name,
                priority = TaskPriority.HIGH.name,
                category = "SaaS & Produit",
                dueDate = now + 86_400_000L * 2,
                noteId = note2Id,
                workspaceId = wsTeam.id,
                userId = user.id
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Implémenter l'isolation Zero-Trust et les logs d'audit",
                description = "Journaliser toutes les connexions, partages et suppressions sensibles.",
                status = TaskStatus.DONE.name,
                priority = TaskPriority.HIGH.name,
                category = "Cybersécurité",
                dueDate = now - 86_400_000L,
                noteId = note1Id,
                workspaceId = wsProduct.id,
                userId = user.id
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Tester l'export PDF et Markdown sur mobile et tablette",
                description = "Vérifier la mise en page des titres, tableaux et blocs de code.",
                status = TaskStatus.WAITING.name,
                priority = TaskPriority.MEDIUM.name,
                category = "Qualité & QA",
                dueDate = now + 86_400_000L * 4,
                noteId = note1Id,
                workspaceId = wsProduct.id,
                userId = user.id
            )
        )

        // 7. Collaboration Share
        shareNoteWithCollaborator(
            noteId = note2Id,
            ownerUserId = user.id,
            invitedEmail = "clara.dubois@mindora.ai",
            invitedName = "Clara Dubois (Lead Design)",
            permission = SharePermission.EDIT
        )
        shareNoteWithCollaborator(
            noteId = note1Id,
            ownerUserId = user.id,
            invitedEmail = "marc.lefevre@invest-tech.fr",
            invitedName = "Marc Lefèvre (Board)",
            permission = SharePermission.READ
        )

        // 8. Initial AI Conversation
        val conv = ensureActiveConversation(user.id)
        dao.insertMessage(
            AIMessageEntity(
                conversationId = conv.id,
                role = "ASSISTANT",
                content = "Bonjour ${user.name} ! Je suis votre Assistant **Mindora AI**.\n\nJ'ai indexé vos **4 notes**, vos **transcriptions vocales** et vos **extractions OCR**. Posez-moi n'importe quelle question sur vos projets, réunions ou idées (ex : *« Quelles décisions ai-je prises lors de ma dernière réunion ? »* ou *« Quelles sont mes idées de projets ? »*).",
                citedNoteIdsCsv = "$note1Id,$note2Id,$note4Id",
                citedNoteTitlesCsv = "Architecture Projet SaaS • Décisions Réunion Comité Produit • Idées de Projets IA"
            )
        )
    }
}
