package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CaptureType
import com.example.ui.screens.AiAssistantRagScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FoldersAndKnowledgeGraphScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.screens.QuickCaptureModal
import com.example.ui.screens.SettingsAndSaasPlansScreen
import com.example.ui.screens.SharedNotesCollaborationScreen
import com.example.ui.screens.TasksAndCalendarScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.WorkspacesScreen
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraTheme
import com.example.ui.theme.MindoraVioletAI
import com.example.ui.viewmodel.MindoraViewModel
import com.example.ui.viewmodel.NavSection
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: MindoraViewModel = viewModel(
                factory = MindoraViewModel.provideFactory(context)
            )
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MindoraTheme(darkTheme = isDarkMode) {
                MindoraAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MindoraAppRoot(viewModel: MindoraViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val activeEditingNoteId by viewModel.activeEditingNoteId.collectAsStateWithLifecycle()
    val showQuickCapture by viewModel.showQuickCaptureModal.collectAsStateWithLifecycle()
    val quickCaptureInitialType by viewModel.quickCaptureInitialType.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val selectedWorkspaceId by viewModel.selectedWorkspaceId.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    val noteTagCrossRefs by viewModel.noteTagCrossRefs.collectAsStateWithLifecycle()
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val taskViewMode by viewModel.taskViewMode.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val shares by viewModel.shares.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val securityLogs by viewModel.securityLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFilterMode by viewModel.searchFilterMode.collectAsStateWithLifecycle()
    val saveStatusText by viewModel.saveStatusText.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val latestAiResult by viewModel.latestAiActionResult.collectAsStateWithLifecycle()

    if (currentUser == null) {
        AuthScreen(
            authError = authError,
            onLogin = viewModel::login,
            onRegister = viewModel::register,
            onResetPassword = viewModel::resetPassword,
            onQuickDemoLogin = viewModel::loginDemoAccount
        )
        return
    }

    // BackHandler for secondary screens when not on HOME
    if (activeEditingNoteId == null && currentSection != NavSection.HOME) {
        BackHandler {
            viewModel.navigateToSection(NavSection.HOME)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isExpandedScreen,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(300.dp)
                        .testTag("mindora_sidebar_drawer")
                ) {
                    MindoraSidebarContent(
                        currentSection = currentSection,
                        activeWorkspaceName = workspaces.find { it.id == selectedWorkspaceId }?.name ?: "Tous les espaces",
                        notesCount = activeNotes.size,
                        tasksCount = tasks.size,
                        trashCount = trashedNotes.size,
                        onSelectSection = { section ->
                            viewModel.navigateToSection(section)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onCreateNewNote = {
                            viewModel.createNewNoteInstant()
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    MindoraTopHeaderBar(
                        userName = currentUser?.name ?: "Alex",
                        userInitials = currentUser?.avatar?.ifBlank { "AM" } ?: "AM",
                        planName = currentUser?.planTier ?: "PRO",
                        searchQuery = searchQuery,
                        showMenuButton = !isExpandedScreen,
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onSearchQueryChange = { q ->
                            viewModel.updateSearchQuery(q)
                            if (q.isNotBlank() && activeEditingNoteId != null) {
                                viewModel.closeNoteEditor()
                                viewModel.navigateToSection(NavSection.ALL_NOTES)
                            } else if (q.isNotBlank() && currentSection == NavSection.HOME) {
                                viewModel.navigateToSection(NavSection.ALL_NOTES)
                            }
                        },
                        onNewNoteClick = { viewModel.createNewNoteInstant() },
                        onQuickCaptureClick = { viewModel.openQuickCapture(CaptureType.TEXT) },
                        onProfileClick = { viewModel.navigateToSection(NavSection.SETTINGS) }
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen && activeEditingNoteId == null) {
                        NavigationBar(modifier = Modifier.testTag("mindora_bottom_nav")) {
                            val bottomItems = listOf(
                                NavSection.HOME to Icons.Default.Home,
                                NavSection.ALL_NOTES to Icons.Default.Description,
                                NavSection.TASKS to Icons.Default.TaskAlt,
                                NavSection.AI_ASSISTANT to Icons.Default.AutoAwesome,
                                NavSection.FOLDERS to Icons.Default.Folder
                            )
                            bottomItems.forEach { (sec, icon) ->
                                NavigationBarItem(
                                    selected = currentSection == sec,
                                    onClick = { viewModel.navigateToSection(sec) },
                                    icon = { Icon(icon, contentDescription = sec.title) },
                                    label = {
                                        Text(
                                            text = sec.title.substringBefore(" "),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("bottom_nav_${sec.name}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Persistent Sidebar on Tablet / Desktop (Expanded screens)
                    if (isExpandedScreen) {
                        Surface(
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .width(270.dp)
                                .fillMaxHeight()
                        ) {
                            MindoraSidebarContent(
                                currentSection = currentSection,
                                activeWorkspaceName = workspaces.find { it.id == selectedWorkspaceId }?.name ?: "Tous les espaces",
                                notesCount = activeNotes.size,
                                tasksCount = tasks.size,
                                trashCount = trashedNotes.size,
                                onSelectSection = viewModel::navigateToSection,
                                onCreateNewNote = { viewModel.createNewNoteInstant() }
                            )
                        }
                    }

                    // Main Content Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Dismissible Status Notification Banner
                        AnimatedVisibility(visible = statusBanner != null) {
                            if (statusBanner != null) {
                                Surface(
                                    color = MindoraVioletAI.copy(alpha = 0.18f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = statusBanner ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.showBanner(null) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Fermer notification",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active Note Editor OR Selected Navigation Section
                        val editingNote = activeNotes.find { it.id == activeEditingNoteId }
                        if (editingNote != null) {
                            NoteEditorScreen(
                                note = editingNote,
                                allNotes = activeNotes,
                                folders = folders,
                                tags = tags,
                                noteTagCrossRefs = noteTagCrossRefs,
                                attachments = attachments,
                                saveStatusText = saveStatusText,
                                isAiProcessing = isAiProcessing,
                                latestAiResult = latestAiResult,
                                onBack = viewModel::closeNoteEditor,
                                onAutoSave = { newTitle, newContent, newBlocks, folderId, status, colorHex, linkedCsv, snap ->
                                    viewModel.autoSaveNote(
                                        note = editingNote,
                                        newTitle = newTitle,
                                        newContent = newContent,
                                        newBlocks = newBlocks,
                                        newFolderId = folderId,
                                        newStatus = status,
                                        newColorHex = colorHex,
                                        newLinkedIdsCsv = linkedCsv,
                                        recordSnapshot = snap
                                    )
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(editingNote) },
                                onMoveToTrash = { viewModel.trashNote(editingNote) },
                                onToggleTag = { tagId -> viewModel.toggleTagOnNote(editingNote.id, tagId) },
                                onCreateTag = { tagName -> viewModel.createTag(tagName, "#7457F5", editingNote.id) },
                                onRunAiAction = { action -> viewModel.runAiActionOnNote(action, editingNote) },
                                onApplyAiOutput = { text, replace ->
                                    viewModel.applyAiOutputToNote(editingNote, text, replace)
                                },
                                onDismissAiResult = viewModel::clearAiActionResult,
                                onExportNote = { fmt -> viewModel.exportAndShareNote(context, editingNote, fmt) },
                                onShareNote = { email, name, perm ->
                                    viewModel.shareNote(editingNote.id, email, name, perm)
                                },
                                onOpenLinkedNote = viewModel::openNoteEditor
                            )
                        } else {
                            when (currentSection) {
                                NavSection.HOME -> DashboardScreen(
                                    user = currentUser,
                                    notes = activeNotes,
                                    tasks = tasks,
                                    attachments = attachments,
                                    onOpenNote = viewModel::openNoteEditor,
                                    onCreateNewNote = { viewModel.createNewNoteInstant() },
                                    onCreateFromTemplate = viewModel::createNoteFromTemplate,
                                    onOpenQuickCapture = viewModel::openQuickCapture,
                                    onNavigateSection = viewModel::navigateToSection,
                                    onToggleTaskStatus = viewModel::cycleTaskStatus
                                )

                                NavSection.ALL_NOTES -> NotesListScreen(
                                    sectionTitle = "Toutes mes notes",
                                    notes = filteredNotes,
                                    folders = folders,
                                    tags = tags,
                                    noteTagCrossRefs = noteTagCrossRefs,
                                    selectedFolderId = selectedFolderId,
                                    selectedTagId = selectedTagId,
                                    searchQuery = searchQuery,
                                    searchFilterMode = searchFilterMode,
                                    onSearchFilterModeChange = viewModel::updateSearchFilterMode,
                                    onSelectFolder = viewModel::selectFolder,
                                    onSelectTag = viewModel::selectTag,
                                    onOpenNote = viewModel::openNoteEditor,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onCreateNewNote = { viewModel.createNewNoteInstant() }
                                )

                                NavSection.FAVORITES -> NotesListScreen(
                                    sectionTitle = "Notes Favorites ⭐",
                                    notes = filteredNotes.filter { it.isFavorite },
                                    folders = folders,
                                    tags = tags,
                                    noteTagCrossRefs = noteTagCrossRefs,
                                    selectedFolderId = selectedFolderId,
                                    selectedTagId = selectedTagId,
                                    searchQuery = searchQuery,
                                    searchFilterMode = searchFilterMode,
                                    onSearchFilterModeChange = viewModel::updateSearchFilterMode,
                                    onSelectFolder = viewModel::selectFolder,
                                    onSelectTag = viewModel::selectTag,
                                    onOpenNote = viewModel::openNoteEditor,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onCreateNewNote = { viewModel.createNewNoteInstant() }
                                )

                                NavSection.RECENT -> NotesListScreen(
                                    sectionTitle = "Récemment consultées 🕒",
                                    notes = filteredNotes.sortedByDescending { it.lastViewedAt },
                                    folders = folders,
                                    tags = tags,
                                    noteTagCrossRefs = noteTagCrossRefs,
                                    selectedFolderId = selectedFolderId,
                                    selectedTagId = selectedTagId,
                                    searchQuery = searchQuery,
                                    searchFilterMode = searchFilterMode,
                                    onSearchFilterModeChange = viewModel::updateSearchFilterMode,
                                    onSelectFolder = viewModel::selectFolder,
                                    onSelectTag = viewModel::selectTag,
                                    onOpenNote = viewModel::openNoteEditor,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onCreateNewNote = { viewModel.createNewNoteInstant() }
                                )

                                NavSection.SHARED -> SharedNotesCollaborationScreen(
                                    shares = shares,
                                    notes = activeNotes,
                                    onShareNewNote = viewModel::shareNote,
                                    onAddComment = viewModel::addShareComment,
                                    onUpdatePermission = viewModel::updateSharePermission,
                                    onRevokeShare = viewModel::revokeShare,
                                    onOpenNote = viewModel::openNoteEditor
                                )

                                NavSection.FOLDERS -> FoldersAndKnowledgeGraphScreen(
                                    folders = folders,
                                    notes = activeNotes,
                                    onCreateFolder = { name, parentId, colTag ->
                                        viewModel.createFolder(name, parentId, "#7457F5", colTag)
                                    },
                                    onDeleteFolder = viewModel::deleteFolder,
                                    onOpenFolderNotes = viewModel::selectFolder,
                                    onOpenNote = viewModel::openNoteEditor,
                                    onAutoDiscoverRelations = viewModel::autoDiscoverNoteConnections
                                )

                                NavSection.WORKSPACES -> WorkspacesScreen(
                                    workspaces = workspaces,
                                    selectedWorkspaceId = selectedWorkspaceId,
                                    notes = activeNotes,
                                    onSelectWorkspace = viewModel::selectWorkspace,
                                    onCreateWorkspace = viewModel::createWorkspace
                                )

                                NavSection.TASKS -> TasksAndCalendarScreen(
                                    tasks = tasks,
                                    notes = activeNotes,
                                    activeViewMode = taskViewMode,
                                    forceCalendarView = false,
                                    onViewModeChange = viewModel::updateTaskViewMode,
                                    onCreateTask = { t, d, p, c, days ->
                                        viewModel.createTask(t, d, p, c, days)
                                    },
                                    onCycleTaskStatus = viewModel::cycleTaskStatus,
                                    onUpdateTaskStatus = viewModel::updateTaskStatus,
                                    onDeleteTask = viewModel::deleteTask,
                                    onOpenSourceNote = viewModel::openNoteEditor
                                )

                                NavSection.CALENDAR -> TasksAndCalendarScreen(
                                    tasks = tasks,
                                    notes = activeNotes,
                                    activeViewMode = taskViewMode,
                                    forceCalendarView = true,
                                    onViewModeChange = viewModel::updateTaskViewMode,
                                    onCreateTask = { t, d, p, c, days ->
                                        viewModel.createTask(t, d, p, c, days)
                                    },
                                    onCycleTaskStatus = viewModel::cycleTaskStatus,
                                    onUpdateTaskStatus = viewModel::updateTaskStatus,
                                    onDeleteTask = viewModel::deleteTask,
                                    onOpenSourceNote = viewModel::openNoteEditor
                                )

                                NavSection.AI_ASSISTANT -> AiAssistantRagScreen(
                                    messages = aiMessages,
                                    notes = activeNotes,
                                    isProcessing = isAiProcessing,
                                    onAskQuestion = viewModel::askAssistantQuestion,
                                    onOpenCitedNote = viewModel::openNoteEditor
                                )

                                NavSection.TRASH -> TrashScreen(
                                    trashedNotes = trashedNotes,
                                    onRestoreNote = viewModel::restoreNote,
                                    onDeleteForever = viewModel::deleteNoteForever,
                                    onEmptyTrash = viewModel::emptyTrash
                                )

                                NavSection.SETTINGS -> SettingsAndSaasPlansScreen(
                                    user = currentUser,
                                    notesCount = activeNotes.size,
                                    isDarkMode = isDarkMode,
                                    securityLogs = securityLogs,
                                    onToggleDarkMode = viewModel::toggleDarkMode,
                                    onSelectPlan = viewModel::selectPlanTier,
                                    onExportFullBackup = { viewModel.exportFullBackup(context) },
                                    onLogout = viewModel::logout
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Quick Capture Modal accessible from all screens (Section 7)
        if (showQuickCapture) {
            QuickCaptureModal(
                initialType = quickCaptureInitialType,
                onDismiss = viewModel::closeQuickCapture,
                onSubmitCapture = viewModel::submitQuickCapture
            )
        }
    }
}

@Composable
private fun MindoraTopHeaderBar(
    userName: String,
    userInitials: String,
    planName: String,
    searchQuery: String,
    showMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNewNoteClick: () -> Unit,
    onQuickCaptureClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Row: Menu + Logo MINDORA AI + Nouvelle note + Capture rapide + Profil utilisateur
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showMenuButton) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("top_bar_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Ouvrir la navigation")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MindoraVioletAI,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MINDORA AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Offre $planName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MindoraVioletAI
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Bouton Nouvelle note
                    Button(
                        onClick = onNewNoteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                        modifier = Modifier.testTag("top_bar_new_note_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Note", style = MaterialTheme.typography.labelMedium)
                    }

                    // Bouton Capture rapide
                    FilledTonalButton(
                        onClick = onQuickCaptureClick,
                        modifier = Modifier.testTag("top_bar_quick_capture_btn")
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MindoraBlueSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capture", style = MaterialTheme.typography.labelMedium)
                    }

                    // Profil utilisateur
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MindoraBlueSecondary)
                            .clickable(onClick = onProfileClick)
                            .testTag("top_bar_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Global Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Recherche globale (titre, contenu, sémantique, tags, audio, PDF)…",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_bar")
            )
        }
    }
}

@Composable
private fun MindoraSidebarContent(
    currentSection: NavSection,
    activeWorkspaceName: String,
    notesCount: Int,
    tasksCount: Int,
    trashCount: Int,
    onSelectSection: (NavSection) -> Unit,
    onCreateNewNote: () -> Unit
) {
    val navItems: List<Pair<NavSection, ImageVector>> = listOf(
        NavSection.HOME to Icons.Default.Home,
        NavSection.ALL_NOTES to Icons.Default.Description,
        NavSection.FAVORITES to Icons.Default.Star,
        NavSection.RECENT to Icons.Default.History,
        NavSection.SHARED to Icons.Default.Group,
        NavSection.FOLDERS to Icons.Default.Folder,
        NavSection.WORKSPACES to Icons.Default.Workspaces,
        NavSection.TASKS to Icons.Default.TaskAlt,
        NavSection.CALENDAR to Icons.Default.CalendarMonth,
        NavSection.AI_ASSISTANT to Icons.Default.AutoAwesome,
        NavSection.TRASH to Icons.Default.Delete,
        NavSection.SETTINGS to Icons.Default.Settings
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MindoraVioletAI,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "MINDORA AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeWorkspaceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onCreateNewNote,
            colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nouvelle note rapide")
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        navItems.forEach { (section, icon) ->
            val badgeText = when (section) {
                NavSection.ALL_NOTES -> notesCount.toString()
                NavSection.TASKS -> tasksCount.toString()
                NavSection.TRASH -> if (trashCount > 0) trashCount.toString() else null
                else -> null
            }
            NavigationDrawerItem(
                label = { Text(section.title, style = MaterialTheme.typography.bodyMedium) },
                selected = currentSection == section,
                onClick = { onSelectSection(section) },
                icon = { Icon(icon, contentDescription = section.title) },
                badge = {
                    if (badgeText != null) {
                        Text(badgeText, style = MaterialTheme.typography.labelSmall)
                    }
                },
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("sidebar_item_${section.name}"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MindoraVioletAI.copy(alpha = 0.16f)
                )
            )
        }
    }
}
