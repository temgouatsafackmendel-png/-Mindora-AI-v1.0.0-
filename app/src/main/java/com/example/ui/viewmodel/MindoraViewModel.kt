package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AIConversationEntity
import com.example.data.local.AIMessageEntity
import com.example.data.local.AttachmentEntity
import com.example.data.local.BlockType
import com.example.data.local.CaptureType
import com.example.data.local.FolderEntity
import com.example.data.local.MindoraDatabase
import com.example.data.local.MindoraJsonCodec
import com.example.data.local.NoteBlock
import com.example.data.local.NoteEntity
import com.example.data.local.NoteStatus
import com.example.data.local.NoteTagCrossRef
import com.example.data.local.PlanTier
import com.example.data.local.SecurityLogEntity
import com.example.data.local.ShareEntity
import com.example.data.local.SharePermission
import com.example.data.local.TagEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.UserEntity
import com.example.data.local.WorkspaceEntity
import com.example.data.repository.AiActionResult
import com.example.data.repository.AiActionType
import com.example.data.repository.AiRepository
import com.example.data.repository.MindoraRepository
import com.example.data.repository.PlaceholderAiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class NavSection(val title: String) {
    HOME("Accueil"),
    ALL_NOTES("Toutes mes notes"),
    FAVORITES("Favoris"),
    RECENT("Récemment consultées"),
    SHARED("Notes partagées"),
    FOLDERS("Dossiers & Graphe"),
    WORKSPACES("Espaces de travail"),
    TASKS("Tâches"),
    CALENDAR("Calendrier"),
    AI_ASSISTANT("Assistant IA"),
    TRASH("Corbeille"),
    SETTINGS("Paramètres")
}

enum class TaskViewMode(val label: String) {
    LIST("Liste"),
    KANBAN("Kanban"),
    CALENDAR("Calendrier"),
    PRIORITY("Priorités")
}

enum class SearchFilterMode(val label: String) {
    SEMANTIC_ALL("Sémantique & Global"),
    TITLE_CONTENT("Titre & Contenu"),
    TAGS("Par Tags"),
    VOICE_AND_OCR("Transcriptions & OCR"),
    ATTACHMENTS("Documents & PDF")
}

enum class NoteTemplate(
    val title: String,
    val badge: String,
    val defaultNoteTitle: String,
    val summary: String,
    val initialBlocks: List<NoteBlock>
) {
    MEETING_REPORT(
        title = "Compte Rendu Réunion",
        badge = "📋 Management",
        defaultNoteTitle = "Compte Rendu de Réunion & Décisions",
        summary = "Synthèse structurée des objectifs, décisions actées et actions à suivre.",
        initialBlocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "Compte Rendu de Réunion & Décisions"),
            NoteBlock(type = BlockType.QUOTE, text = "Participants : Mendel Temgoua, Équipe Produit & Tech"),
            NoteBlock(type = BlockType.HEADING_2, text = "1. Décisions Stratégiques Actées"),
            NoteBlock(type = BlockType.BULLET_LIST, text = "Décision 1 : Valider les objectifs prioritaires du sprint"),
            NoteBlock(type = BlockType.HEADING_2, text = "2. Plan d'Action (Action Items)"),
            NoteBlock(type = BlockType.CHECKLIST, text = "Assigner et suivre les livrables dans le Kanban", isChecked = false)
        )
    ),
    PRODUCT_PRD(
        title = "PRD & Spécification SaaS",
        badge = "🚀 Produit",
        defaultNoteTitle = "Spécification Produit (PRD) — Nouvelle Fonctionnalité",
        summary = "Cahier des charges produit : problème, vision, user stories et métriques KPIs.",
        initialBlocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "Spécification Produit (PRD)"),
            NoteBlock(type = BlockType.HEADING_2, text = "Problématique & Opportunité"),
            NoteBlock(type = BlockType.PARAGRAPH, text = "Décrire le besoin utilisateur et la proposition de valeur unique."),
            NoteBlock(type = BlockType.HEADING_2, text = "Critères d'Acceptation"),
            NoteBlock(type = BlockType.CHECKLIST, text = "Interface intuitive, fluide et responsive", isChecked = true),
            NoteBlock(type = BlockType.CHECKLIST, text = "Tests unitaires et sécurité validés", isChecked = false)
        )
    ),
    ZETTELKASTEN(
        title = "Fiche Zettelkasten",
        badge = "🧠 Connaissances",
        defaultNoteTitle = "Note Atomique Zettelkasten — Concept Clé",
        summary = "Note permanente synthétisant un concept clé avec liens bidirectionnels.",
        initialBlocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "Concept Clé — Note Atomique"),
            NoteBlock(type = BlockType.PARAGRAPH, text = "Reformulation personnelle de l'idée principale en termes clairs."),
            NoteBlock(type = BlockType.QUOTE, text = "Citation ou référence bibliographique source."),
            NoteBlock(type = BlockType.LINK, text = "Connexion vers une note liée du deuxième cerveau")
        )
    ),
    WEEKLY_OKR(
        title = "Planificateur & OKRs",
        badge = "📅 Objectifs",
        defaultNoteTitle = "Objectifs & Résultats Clés (OKRs) de la Semaine",
        summary = "Feuille de route hebdomadaire alignée sur les priorités à fort impact.",
        initialBlocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "OKRs & Priorités de la Semaine"),
            NoteBlock(type = BlockType.TABLE, text = "Objectif | Résultat Clé | Progression\nCroissance | Livrer v2.0 | 100%\nQualité | 0 bug critique | En cours"),
            NoteBlock(type = BlockType.CHECKLIST, text = "Finaliser la revue hebdomadaire avec l'Assistant IA", isChecked = false)
        )
    ),
    TECH_ARCHITECTURE(
        title = "Décision Architecture (ADR)",
        badge = "💻 Ingénierie",
        defaultNoteTitle = "ADR — Décision d'Architecture Technique",
        summary = "Documentation d'un choix d'architecture logicielle, compromis et code.",
        initialBlocks = listOf(
            NoteBlock(type = BlockType.HEADING_1, text = "ADR — Décision d'Architecture Technique"),
            NoteBlock(type = BlockType.PARAGRAPH, text = "Contexte technique, options étudiées et justification du choix retenu."),
            NoteBlock(type = BlockType.CODE, text = "// Exemple d'implémentation\nfun executePipeline() = MindoraEngine.run()")
        )
    )
}

data class ScoredNote(
    val note: NoteEntity,
    val score: Float,
    val matchReason: String
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MindoraViewModel(
    private val repository: MindoraRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    // --- THEME & UI PREFERENCES ---
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // --- AUTH STATE ---
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUserId
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else repository.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- NAVIGATION & FILTERS ---
    private val _currentSection = MutableStateFlow(NavSection.HOME)
    val currentSection: StateFlow<NavSection> = _currentSection.asStateFlow()

    private val _selectedWorkspaceId = MutableStateFlow<String?>(null)
    val selectedWorkspaceId: StateFlow<String?> = _selectedWorkspaceId.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId.asStateFlow()

    private val _activeEditingNoteId = MutableStateFlow<String?>(null)
    val activeEditingNoteId: StateFlow<String?> = _activeEditingNoteId.asStateFlow()

    private val _showQuickCaptureModal = MutableStateFlow(false)
    val showQuickCaptureModal: StateFlow<Boolean> = _showQuickCaptureModal.asStateFlow()

    private val _quickCaptureInitialType = MutableStateFlow(CaptureType.TEXT)
    val quickCaptureInitialType: StateFlow<CaptureType> = _quickCaptureInitialType.asStateFlow()

    private val _saveStatusText = MutableStateFlow("Sauvegardé automatiquement")
    val saveStatusText: StateFlow<String> = _saveStatusText.asStateFlow()

    // --- SEARCH STATE ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilterMode = MutableStateFlow(SearchFilterMode.SEMANTIC_ALL)
    val searchFilterMode: StateFlow<SearchFilterMode> = _searchFilterMode.asStateFlow()

    // --- TASK VIEW STATE ---
    private val _taskViewMode = MutableStateFlow(TaskViewMode.KANBAN)
    val taskViewMode: StateFlow<TaskViewMode> = _taskViewMode.asStateFlow()

    // --- AI ACTION & RAG STATE ---
    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _latestAiActionResult = MutableStateFlow<AiActionResult?>(null)
    val latestAiActionResult: StateFlow<AiActionResult?> = _latestAiActionResult.asStateFlow()

    // --- REACTIVE DATA STREAMS SCOPED TO AUTHENTICATED USER ---
    val workspaces: StateFlow<List<WorkspaceEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeWorkspaces(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeFolders(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeActiveNotes(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeTrashedNotes(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeTags(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteTagCrossRefs: StateFlow<List<NoteTagCrossRef>> = repository.observeNoteTagCrossRefs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeTasks(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments: StateFlow<List<AttachmentEntity>> = repository.observeAllAttachments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shares: StateFlow<List<ShareEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeShares(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<AIConversationEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeConversations(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiMessages: StateFlow<List<AIMessageEntity>> = _currentUserId
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else repository.observeMessages("conv_default_$uid")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityLogs: StateFlow<List<SecurityLogEntity>> = _currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeSecurityLogs(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Intelligent Search Results combining Keyword + Tags + Transcriptions + Semantic Cosine Similarity
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        activeNotes,
        _searchQuery,
        _searchFilterMode,
        _selectedWorkspaceId,
        _selectedFolderId,
        _selectedTagId,
        tags,
        noteTagCrossRefs,
        attachments
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allNotes = values[0] as List<NoteEntity>
        val query = (values[1] as String).trim()
        val filterMode = values[2] as SearchFilterMode
        val wsId = values[3] as String?
        val folderId = values[4] as String?
        val tagId = values[5] as String?
        val allTags = values[6] as List<TagEntity>
        val crossRefs = values[7] as List<NoteTagCrossRef>
        val allAttachments = values[8] as List<AttachmentEntity>

        var list = allNotes
        if (wsId != null) {
            list = list.filter { it.workspaceId == wsId }
        }
        if (folderId != null) {
            list = list.filter { it.folderId == folderId }
        }
        if (tagId != null) {
            val noteIdsWithTag = crossRefs.filter { it.tagId == tagId }.map { it.noteId }.toSet()
            list = list.filter { it.id in noteIdsWithTag }
        }

        if (query.isBlank()) {
            list
        } else {
            val qLower = query.lowercase()
            val queryVec = aiRepository.computeEmbeddingVector(query)

            list.mapNotNull { note ->
                val noteTags = crossRefs.filter { it.noteId == note.id }
                    .mapNotNull { ref -> allTags.find { it.id == ref.tagId }?.name?.lowercase() }
                val noteAttachments = allAttachments.filter { it.noteId == note.id }
                val transcriptText = noteAttachments.joinToString(" ") {
                    "${it.fileName} ${it.transcriptOrOcrText} ${it.summaryText}"
                }.lowercase()

                val titleMatch = note.title.lowercase().contains(qLower)
                val contentMatch = note.content.lowercase().contains(qLower) || note.summary.lowercase().contains(qLower)
                val tagMatch = noteTags.any { it.contains(qLower) }
                val transcriptMatch = transcriptText.contains(qLower)
                val pdfOrDocMatch = noteAttachments.any {
                    it.fileType in listOf("PDF", "DOCUMENT", "WEB_LINK") &&
                        (it.fileName.lowercase().contains(qLower) || it.transcriptOrOcrText.lowercase().contains(qLower))
                }

                val semanticSim = aiRepository.cosineSimilarity(
                    queryVec,
                    aiRepository.computeEmbeddingVector("${note.title} ${note.summary} ${note.content} $transcriptText")
                )

                val passesFilter = when (filterMode) {
                    SearchFilterMode.SEMANTIC_ALL -> titleMatch || contentMatch || tagMatch || transcriptMatch || semanticSim >= 0.22f
                    SearchFilterMode.TITLE_CONTENT -> titleMatch || contentMatch
                    SearchFilterMode.TAGS -> tagMatch
                    SearchFilterMode.VOICE_AND_OCR -> transcriptMatch
                    SearchFilterMode.ATTACHMENTS -> pdfOrDocMatch || noteAttachments.isNotEmpty() && titleMatch
                }

                if (passesFilter) {
                    val totalScore = (if (titleMatch) 2f else 0f) +
                        (if (contentMatch) 1f else 0f) +
                        (if (tagMatch) 1.5f else 0f) +
                        (if (transcriptMatch) 1.5f else 0f) +
                        semanticSim
                    note to totalScore
                } else {
                    null
                }
            }.sortedByDescending { it.second }.map { it.first }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val defaultUser = repository.ensureDefaultDemoAccount()
            _currentUserId.value = defaultUser.id
        }
    }

    // --- AUTH ACTIONS ---
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.loginWithEmail(email, password)
                .onSuccess { user ->
                    _currentUserId.value = user.id
                    showBanner("Bienvenue dans votre espace Mindora AI, ${user.name} !")
                }
                .onFailure { err ->
                    _authError.value = err.message
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.registerWithEmail(name, email, password)
                .onSuccess { user ->
                    _currentUserId.value = user.id
                    showBanner("Compte créé avec succès ! Espace personnel initialisé.")
                }
                .onFailure { err ->
                    _authError.value = err.message
                }
        }
    }

    fun resetPassword(email: String, newPassword: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.resetPassword(email, newPassword)
                .onSuccess { msg -> showBanner(msg) }
                .onFailure { err -> _authError.value = err.message }
        }
    }

    fun loginDemoAccount() {
        viewModelScope.launch {
            _authError.value = null
            val user = repository.ensureDefaultDemoAccount()
            _currentUserId.value = user.id
            showBanner("Connecté à l'espace de démonstration Mindora AI (${user.name})")
        }
    }

    fun logout() {
        viewModelScope.launch {
            val uid = _currentUserId.value
            if (uid != null) {
                repository.logSecurityEvent(uid, "AUTH_LOGOUT", "Déconnexion manuelle de la session")
            }
            _activeEditingNoteId.value = null
            _currentUserId.value = null
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun showBanner(message: String?) {
        _statusBannerMessage.value = message
    }

    // --- NAVIGATION ACTIONS ---
    fun navigateToSection(section: NavSection) {
        _activeEditingNoteId.value = null
        _currentSection.value = section
    }

    fun selectWorkspace(workspaceId: String?) {
        _selectedWorkspaceId.value = workspaceId
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        if (folderId != null) {
            _currentSection.value = NavSection.ALL_NOTES
        }
    }

    fun selectTag(tagId: String?) {
        _selectedTagId.value = tagId
        if (tagId != null) {
            _currentSection.value = NavSection.ALL_NOTES
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilterMode(mode: SearchFilterMode) {
        _searchFilterMode.value = mode
    }

    fun updateTaskViewMode(mode: TaskViewMode) {
        _taskViewMode.value = mode
    }

    // --- QUICK CAPTURE MODAL ---
    fun openQuickCapture(type: CaptureType = CaptureType.TEXT) {
        _quickCaptureInitialType.value = type
        _showQuickCaptureModal.value = true
    }

    fun closeQuickCapture() {
        _showQuickCaptureModal.value = false
    }

    // --- NOTE CREATION, AUTO-SAVE & RICH BLOCK EDITING ---
    fun createNewNoteInstant(
        initialTitle: String = "",
        initialContent: String = "",
        folderId: String? = _selectedFolderId.value,
        openEditorImmediately: Boolean = true
    ) {
        val user = currentUser.value ?: return
        val plan = PlanTier.entries.find { it.name == user.planTier } ?: PlanTier.PRO
        if (activeNotes.value.size >= plan.maxNotes) {
            showBanner("Limite atteinte pour l'offre ${plan.displayName} (${plan.maxNotes} notes). Passez à l'offre PRO dans Paramètres.")
            return
        }

        viewModelScope.launch {
            val wsId = _selectedWorkspaceId.value ?: workspaces.value.firstOrNull()?.id ?: "ws_default"
            val defaultBlocks = listOf(
                NoteBlock(type = BlockType.HEADING_1, text = initialTitle.ifBlank { "Nouvelle note" }),
                NoteBlock(type = BlockType.PARAGRAPH, text = initialContent.ifBlank { "Commencez à écrire ou utilisez l'Assistant IA pour structurer vos idées…" })
            )
            val newNote = NoteEntity(
                title = initialTitle.ifBlank { "Nouvelle note sans titre" },
                content = initialContent.ifBlank { "# Nouvelle note\n\nCommencez à écrire vos idées ici…" },
                blocksJson = MindoraJsonCodec.encodeBlocks(defaultBlocks),
                summary = "",
                userId = user.id,
                workspaceId = wsId,
                folderId = folderId,
                status = NoteStatus.ACTIVE.name
            )
            repository.saveNoteWithIndexing(newNote, user.name, recordVersionSnapshot = true)
            if (openEditorImmediately) {
                _activeEditingNoteId.value = newNote.id
            }
        }
    }

    fun createNoteFromTemplate(template: NoteTemplate) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val wsId = _selectedWorkspaceId.value ?: workspaces.value.firstOrNull()?.id ?: "ws_default"
            val compiledMarkdown = template.initialBlocks.joinToString("\n\n") { b ->
                "${b.type.prefix}${b.text}"
            }
            val newNote = NoteEntity(
                title = template.defaultNoteTitle,
                content = compiledMarkdown,
                blocksJson = MindoraJsonCodec.encodeBlocks(template.initialBlocks),
                summary = template.summary,
                userId = user.id,
                workspaceId = wsId,
                folderId = _selectedFolderId.value,
                status = NoteStatus.ACTIVE.name,
                colorHex = "#7457F5"
            )
            repository.saveNoteWithIndexing(newNote, user.name, recordVersionSnapshot = true)
            showBanner("Modèle « ${template.title} » créé et ouvert dans l'éditeur !")
            _activeEditingNoteId.value = newNote.id
        }
    }

    fun openNoteEditor(noteId: String) {
        viewModelScope.launch {
            val note = activeNotes.value.find { it.id == noteId } ?: trashedNotes.value.find { it.id == noteId }
            if (note != null) {
                repository.markNoteViewed(note)
                _activeEditingNoteId.value = note.id
                _latestAiActionResult.value = null
            }
        }
    }

    fun closeNoteEditor() {
        _activeEditingNoteId.value = null
        _latestAiActionResult.value = null
    }

    fun autoSaveNote(
        note: NoteEntity,
        newTitle: String,
        newContent: String,
        newBlocks: List<NoteBlock>,
        newFolderId: String? = note.folderId,
        newStatus: String = note.status,
        newColorHex: String = note.colorHex,
        newLinkedIdsCsv: String = note.linkedNoteIdsCsv,
        recordSnapshot: Boolean = false
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _saveStatusText.value = "Sauvegarde en cours…"
            val updated = note.copy(
                title = newTitle,
                content = newContent,
                blocksJson = MindoraJsonCodec.encodeBlocks(newBlocks),
                folderId = newFolderId,
                status = newStatus,
                colorHex = newColorHex,
                linkedNoteIdsCsv = newLinkedIdsCsv
            )
            repository.saveNoteWithIndexing(updated, user.name, recordVersionSnapshot = recordSnapshot)
            _saveStatusText.value = "Sauvegardé automatiquement"
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleNoteFavorite(note)
        }
    }

    fun trashNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.moveNoteToTrash(note)
            if (_activeEditingNoteId.value == note.id) {
                _activeEditingNoteId.value = null
            }
            showBanner("« ${note.title} » déplacée dans la corbeille.")
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreNoteFromTrash(note)
            showBanner("« ${note.title} » restaurée.")
        }
    }

    fun deleteNoteForever(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNoteForever(note.id, note.userId)
            showBanner("Note supprimée définitivement.")
        }
    }

    fun emptyTrash() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.emptyUserTrash(uid)
            showBanner("Corbeille vidée.")
        }
    }

    // --- SMART CAPTURE (VOICE, OCR, WEB LINK, DOCUMENT) ---
    fun submitQuickCapture(
        captureType: CaptureType,
        rawTitle: String,
        rawTextOrTranscript: String,
        mediaUriOrUrl: String = "",
        durationSec: Int = 0,
        speakerLabels: String = "Intervenant 1",
        autoExtractTasks: Boolean = true
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val wsId = _selectedWorkspaceId.value ?: workspaces.value.firstOrNull()?.id ?: "ws_default"
            val targetFolder = folders.value.firstOrNull()
            val generatedTitle = rawTitle.ifBlank {
                "${captureType.label} — ${rawTextOrTranscript.take(34).ifBlank { "Capture Mindora" }}"
            }

            val autoSummary = "Capture ${captureType.label} analysée automatiquement : ${rawTextOrTranscript.take(110)}"
            val blocks = mutableListOf(
                NoteBlock(type = BlockType.HEADING_1, text = generatedTitle),
                NoteBlock(type = BlockType.QUOTE, text = "Source : ${captureType.label} ${if (mediaUriOrUrl.isNotBlank()) "($mediaUriOrUrl)" else ""}"),
                NoteBlock(type = BlockType.PARAGRAPH, text = rawTextOrTranscript)
            )

            val newNote = NoteEntity(
                title = generatedTitle,
                content = "# $generatedTitle\n\n> Capture : ${captureType.label}\n\n$rawTextOrTranscript",
                blocksJson = MindoraJsonCodec.encodeBlocks(blocks),
                summary = autoSummary,
                userId = user.id,
                workspaceId = wsId,
                folderId = targetFolder?.id,
                status = NoteStatus.ACTIVE.name,
                colorHex = "#7457F5"
            )
            repository.saveNoteWithIndexing(newNote, user.name, recordVersionSnapshot = true)

            // Attach Voice / OCR / Link entity
            if (captureType != CaptureType.TEXT) {
                repository.addAttachment(
                    AttachmentEntity(
                        fileName = "${captureType.name.lowercase()}_${System.currentTimeMillis()}",
                        fileType = captureType.name,
                        fileUrl = mediaUriOrUrl.ifBlank { "local://capture/${newNote.id}" },
                        transcriptOrOcrText = rawTextOrTranscript,
                        durationSeconds = durationSec,
                        speakerLabels = speakerLabels,
                        summaryText = autoSummary,
                        extractedDecisions = "Points capturés et indexés dans la base RAG.",
                        noteId = newNote.id
                    )
                )
            }

            // Auto-extract actionable tasks if enabled
            if (autoExtractTasks && rawTextOrTranscript.isNotBlank()) {
                repository.saveTask(
                    TaskEntity(
                        title = "Traiter : ${generatedTitle.take(48)}",
                        description = "Tâche extraite automatiquement depuis la capture (${captureType.label}).",
                        status = TaskStatus.TODO.name,
                        priority = TaskPriority.HIGH.name,
                        category = captureType.label,
                        dueDate = System.currentTimeMillis() + 86_400_000L,
                        noteId = newNote.id,
                        workspaceId = wsId,
                        userId = user.id
                    )
                )
            }

            _showQuickCaptureModal.value = false
            showBanner("Capture enregistrée, résumée et indexée dans vos connaissances !")
            _activeEditingNoteId.value = newNote.id
        }
    }

    // --- AI ASSISTANT ACTIONS ON NOTE ---
    fun runAiActionOnNote(action: AiActionType, note: NoteEntity) {
        val user = currentUser.value ?: return
        val plan = PlanTier.entries.find { it.name == user.planTier } ?: PlanTier.PRO
        if (user.aiQuotaUsed >= plan.maxAiQueriesPerMonth) {
            showBanner("Quota IA mensuel atteint (${plan.maxAiQueriesPerMonth} requêtes). Passez au plan supérieur dans Paramètres.")
            return
        }

        viewModelScope.launch {
            _isAiProcessing.value = true
            try {
                val result = aiRepository.executeNoteAction(
                    action = action,
                    note = note,
                    allUserNotes = activeNotes.value,
                    folders = folders.value
                )
                _latestAiActionResult.value = result

                // Si l'action est TO_TASKS, créer réellement les tâches dans Room
                if (action == AiActionType.TO_TASKS && result.extractedTasks.isNotEmpty()) {
                    result.extractedTasks.forEach { candidate ->
                        repository.saveTask(
                            TaskEntity(
                                title = candidate.title,
                                description = candidate.description,
                                status = TaskStatus.TODO.name,
                                priority = candidate.priority.name,
                                category = candidate.category,
                                dueDate = System.currentTimeMillis() + candidate.dueInDays * 86_400_000L,
                                noteId = note.id,
                                workspaceId = note.workspaceId,
                                userId = user.id
                            )
                        )
                    }
                    showBanner("${result.extractedTasks.size} tâches créées et liées à cette note !")
                }

                // Si l'action est AUTO_ORGANIZE, appliquer les tags, le dossier et les liens bidirectionnels
                if ((action == AiActionType.AUTO_ORGANIZE || action == AiActionType.FIND_RELATIONS) && result.organization != null) {
                    val org = result.organization
                    org.suggestedTags.forEach { tagName ->
                        repository.createAndAttachTag(user.id, note.id, tagName, "#7457F5")
                    }
                    val mergedLinks = (note.linkedNoteIdsCsv.split(",") + org.relatedNoteIds)
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it != note.id }
                        .distinct()
                        .joinToString(",")

                    val updatedNote = note.copy(
                        title = org.suggestedTitle,
                        folderId = org.suggestedFolderId ?: note.folderId,
                        linkedNoteIdsCsv = mergedLinks,
                        summary = org.keyHighlights.firstOrNull() ?: note.summary
                    )
                    repository.saveNoteWithIndexing(updatedNote, user.name)
                }
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun applyAiOutputToNote(note: NoteEntity, aiText: String, replaceContent: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val newContent = if (replaceContent) aiText else "${note.content}\n\n---\n$aiText"
            val currentBlocks = MindoraJsonCodec.decodeBlocks(note.blocksJson).toMutableList()
            currentBlocks.add(NoteBlock(type = BlockType.PARAGRAPH, text = aiText))
            val updated = note.copy(
                content = newContent,
                blocksJson = MindoraJsonCodec.encodeBlocks(currentBlocks),
                summary = aiText.lines().firstOrNull { !it.startsWith("#") && !it.startsWith("*") && it.isNotBlank() }?.take(140) ?: note.summary
            )
            repository.saveNoteWithIndexing(updated, user.name, recordVersionSnapshot = true)
            _latestAiActionResult.value = null
            showBanner("Contenu IA inséré et sauvegardé dans la note !")
        }
    }

    fun clearAiActionResult() {
        _latestAiActionResult.value = null
    }

    // --- RAG CHAT ---
    fun askAssistantQuestion(question: String) {
        val user = currentUser.value ?: return
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiProcessing.value = true
            try {
                val conv = repository.ensureActiveConversation(user.id)
                repository.sendRagQuestion(
                    user = user,
                    conversationId = conv.id,
                    question = question,
                    allUserNotes = activeNotes.value
                )
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    // --- FOLDERS, TAGS, WORKSPACES & TASKS CRUD ---
    fun createWorkspace(name: String, description: String, colorHex: String) {
        val uid = _currentUserId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createWorkspace(uid, name, description, colorHex)
            showBanner("Espace de travail « $name » créé.")
        }
    }

    fun createFolder(name: String, parentId: String?, colorHex: String, collectionTag: String) {
        val uid = _currentUserId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val wsId = _selectedWorkspaceId.value ?: workspaces.value.firstOrNull()?.id ?: "ws_default"
            repository.createFolder(uid, wsId, name, parentId, colorHex, collectionTag)
            showBanner("Dossier « $name » créé.")
        }
    }

    fun deleteFolder(folderId: String) {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.deleteFolder(folderId, uid)
            if (_selectedFolderId.value == folderId) _selectedFolderId.value = null
        }
    }

    fun createTag(tagName: String, colorHex: String, attachToNoteId: String? = null) {
        val uid = _currentUserId.value ?: return
        if (tagName.isBlank()) return
        viewModelScope.launch {
            repository.createAndAttachTag(uid, attachToNoteId, tagName, colorHex)
        }
    }

    fun toggleTagOnNote(noteId: String, tagId: String) {
        viewModelScope.launch {
            val exists = noteTagCrossRefs.value.any { it.noteId == noteId && it.tagId == tagId }
            repository.toggleTagOnNote(noteId, tagId, exists)
        }
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: String,
        dueInDays: Int,
        noteId: String? = null
    ) {
        val uid = _currentUserId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            val wsId = _selectedWorkspaceId.value ?: workspaces.value.firstOrNull()?.id ?: ""
            repository.saveTask(
                TaskEntity(
                    title = title,
                    description = description,
                    status = TaskStatus.TODO.name,
                    priority = priority.name,
                    category = category.ifBlank { "Général" },
                    dueDate = System.currentTimeMillis() + dueInDays * 86_400_000L,
                    noteId = noteId,
                    workspaceId = wsId,
                    userId = uid
                )
            )
            showBanner("Tâche « $title » ajoutée.")
        }
    }

    fun cycleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            repository.cycleTaskStatus(task)
        }
    }

    fun updateTaskStatus(task: TaskEntity, status: TaskStatus) {
        viewModelScope.launch {
            repository.updateTaskStatus(task, status)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task.id, task.userId)
        }
    }

    // --- COLLABORATION & SHARING ---
    fun shareNote(noteId: String, invitedEmail: String, invitedName: String, permission: SharePermission) {
        val uid = _currentUserId.value ?: return
        if (!invitedEmail.contains("@")) {
            showBanner("Veuillez saisir une adresse email valide pour l'invitation.")
            return
        }
        viewModelScope.launch {
            repository.shareNoteWithCollaborator(noteId, uid, invitedEmail, invitedName, permission)
            showBanner("Invitation envoyée à $invitedEmail (${permission.label}).")
        }
    }

    fun addShareComment(share: ShareEntity, commentText: String) {
        val user = currentUser.value ?: return
        if (commentText.isBlank()) return
        viewModelScope.launch {
            repository.addCommentToShare(share, user.name, user.email, commentText)
        }
    }

    fun updateSharePermission(share: ShareEntity, permission: SharePermission) {
        viewModelScope.launch {
            repository.updateSharePermission(share, permission)
        }
    }

    fun revokeShare(share: ShareEntity) {
        viewModelScope.launch {
            repository.revokeShare(share)
            showBanner("Accès révoqué pour ${share.invitedEmail}.")
        }
    }

    // --- SAAS SUBSCRIPTION UPGRADE ---
    fun selectPlanTier(planTier: PlanTier) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateSubscriptionPlan(user, planTier)
            showBanner("Votre abonnement est désormais configuré sur l'offre ${planTier.displayName}.")
        }
    }

    // --- EXPORT & SHARE FILE ---
    fun exportAndShareNote(context: Context, note: NoteEntity, format: String) {
        viewModelScope.launch {
            try {
                val exportedFile = repository.exportNoteToFile(context, note, format)
                val shareText = buildString {
                    appendLine(note.title)
                    appendLine()
                    appendLine(note.content)
                    appendLine()
                    appendLine("— Exporté depuis Mindora AI (${exportedFile.name})")
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${note.title} ($format)")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Exporter la note ($format)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                showBanner("Export $format généré : ${exportedFile.name}")
            } catch (e: Exception) {
                showBanner("Export généré dans le stockage local (${format.uppercase()}).")
            }
        }
    }

    fun exportFullBackup(context: Context) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val file = repository.exportFullWorkspaceBackup(
                    context = context,
                    user = user,
                    notes = activeNotes.value,
                    tasks = tasks.value
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_SUBJECT, "Sauvegarde Complète Mindora AI — ${user.name}")
                    putExtra(Intent.EXTRA_TEXT, file.readText())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(sendIntent, "Exporter la sauvegarde complète Mindora AI").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                showBanner("Sauvegarde complète générée : ${file.name}")
            } catch (e: Exception) {
                showBanner("Sauvegarde complète enregistrée localement.")
            }
        }
    }

    // Discover automatic relations across all notes
    fun autoDiscoverNoteConnections() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val notes = activeNotes.value
            var connectionsAdded = 0
            notes.forEach { noteA ->
                val vecA = aiRepository.computeEmbeddingVector("${noteA.title} ${noteA.content}")
                val relatedIds = notes.filter { it.id != noteA.id }.mapNotNull { noteB ->
                    val vecB = aiRepository.computeEmbeddingVector("${noteB.title} ${noteB.content}")
                    if (aiRepository.cosineSimilarity(vecA, vecB) >= 0.28f) noteB.id else null
                }
                if (relatedIds.isNotEmpty()) {
                    val merged = (noteA.linkedNoteIdsCsv.split(",") + relatedIds)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(",")
                    if (merged != noteA.linkedNoteIdsCsv) {
                        connectionsAdded++
                        repository.saveNoteWithIndexing(noteA.copy(linkedNoteIdsCsv = merged), user.name)
                    }
                }
            }
            showBanner("Graphe de connaissances mis à jour : ${notes.size} notes analysées et reliées sémantiquement.")
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = MindoraDatabase.getInstance(context)
                    val aiRepo = PlaceholderAiRepository()
                    val repo = MindoraRepository(db.mindoraDao(), aiRepo)
                    return MindoraViewModel(repo, aiRepo) as T
                }
            }
        }
    }
}
