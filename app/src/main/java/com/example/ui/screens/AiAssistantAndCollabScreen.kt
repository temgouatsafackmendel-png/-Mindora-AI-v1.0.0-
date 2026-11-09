package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AIMessageEntity
import com.example.data.local.MindoraJsonCodec
import com.example.data.local.NoteEntity
import com.example.data.local.PlanTier
import com.example.data.local.SecurityLogEntity
import com.example.data.local.ShareEntity
import com.example.data.local.SharePermission
import com.example.data.local.UserEntity
import com.example.data.local.WorkspaceEntity
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraSuccess
import com.example.ui.theme.MindoraVioletAI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantRagScreen(
    messages: List<AIMessageEntity>,
    notes: List<NoteEntity>,
    isProcessing: Boolean,
    onAskQuestion: (String) -> Unit,
    onOpenCitedNote: (String) -> Unit
) {
    var questionInput by remember { mutableStateOf("") }

    val examplePrompts = listOf(
        "Quelles sont mes idées de projets ?",
        "Résume mes notes de cette semaine.",
        "Quelles décisions ai-je prises lors de ma dernière réunion ?",
        "Retrouve ma note concernant mon projet SaaS.",
        "Quels sont les sujets que je dois traiter ?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_assistant_rag_screen")
    ) {
        // RAG Header Card + Firebase AI Logic Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MindoraVioletAI)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Assistant IA & Interrogation RAG de vos Connaissances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${notes.size} notes indexées par embeddings vectoriels • Mode Aperçu IA (Activez Firebase AI Logic pour Gemini Cloud)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    examplePrompts.forEach { sample ->
                        AssistChip(
                            onClick = { onAskQuestion(sample) },
                            label = { Text(sample, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Conversation Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "USER"
                val citedIds = msg.citedNoteIdsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) {
                            MindoraVioletAI.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isUser) "Vous" else "✨ Assistant Mindora AI (RAG)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUser) MindoraVioletAI else MindoraBlueSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Cited Sources clickable chips
                        if (!isUser && citedIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "📚 Sources documentaires utilisées :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                citedIds.forEach { nId ->
                                    val sourceNote = notes.find { it.id == nId }
                                    if (sourceNote != null) {
                                        AssistChip(
                                            onClick = { onOpenCitedNote(sourceNote.id) },
                                            label = { Text("Source : ${sourceNote.title.take(26)}") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Description,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = questionInput,
                onValueChange = { questionInput = it },
                placeholder = { Text("Posez une question sur vos notes, réunions ou projets…") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("rag_chat_input")
            )
            Button(
                onClick = {
                    if (questionInput.isNotBlank()) {
                        onAskQuestion(questionInput)
                        questionInput = ""
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("rag_chat_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
            }
        }
    }
}

@Composable
fun SharedNotesCollaborationScreen(
    shares: List<ShareEntity>,
    notes: List<NoteEntity>,
    onShareNewNote: (noteId: String, email: String, name: String, perm: SharePermission) -> Unit,
    onAddComment: (ShareEntity, String) -> Unit,
    onUpdatePermission: (ShareEntity, SharePermission) -> Unit,
    onRevokeShare: (ShareEntity) -> Unit,
    onOpenNote: (String) -> Unit
) {
    var selectedNoteId by remember { mutableStateOf(notes.firstOrNull()?.id ?: "") }
    var invitedEmail by remember { mutableStateOf("") }
    var invitedName by remember { mutableStateOf("") }
    var selectedPerm by remember { mutableStateOf(SharePermission.EDIT) }
    var commentDrafts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shared_notes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Collaboration & Notes Partagées (${shares.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Invitations par email, permissions Lecture/Modification & commentaires collaboratifs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Invite Collaborator Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Inviter un collaborateur sur une note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        notes.forEach { n ->
                            FilterChip(
                                selected = selectedNoteId == n.id,
                                onClick = { selectedNoteId = n.id },
                                label = { Text(n.title.take(24)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invitedEmail,
                            onValueChange = { invitedEmail = it },
                            label = { Text("Email du collaborateur") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = invitedName,
                            onValueChange = { invitedName = it },
                            label = { Text("Nom") },
                            singleLine = true,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SharePermission.entries.forEach { perm ->
                                FilterChip(
                                    selected = selectedPerm == perm,
                                    onClick = { selectedPerm = perm },
                                    label = { Text(perm.label) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (selectedNoteId.isNotBlank() && invitedEmail.isNotBlank()) {
                                    onShareNewNote(selectedNoteId, invitedEmail, invitedName, selectedPerm)
                                    invitedEmail = ""
                                    invitedName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                        ) {
                            Text("Inviter")
                        }
                    }
                }
            }
        }

        items(shares, key = { it.id }) { share ->
            val sharedNote = notes.find { it.id == share.noteId }
            val comments = MindoraJsonCodec.decodeComments(share.commentsJson)
            val currentDraft = commentDrafts[share.id] ?: ""

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { if (sharedNote != null) onOpenNote(sharedNote.id) }
                        ) {
                            Text(
                                text = sharedNote?.title ?: "Note partagée",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Partagé avec : ${share.invitedName} (${share.invitedEmail})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindoraBlueSecondary
                            )
                        }
                        IconButton(onClick = { onRevokeShare(share) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Révoquer l'accès", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Permission Switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharePermission.entries.forEach { perm ->
                            FilterChip(
                                selected = share.permission == perm.name,
                                onClick = { onUpdatePermission(share, perm) },
                                label = { Text(perm.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Comments Thread
                    Text(
                        text = "💬 Fil de commentaires (${comments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    comments.forEach { c ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${c.authorName} (${c.authorEmail})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindoraVioletAI,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = c.content,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentDraft,
                            onValueChange = { commentDrafts = commentDrafts + (share.id to it) },
                            placeholder = { Text("Ajouter un commentaire…") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (currentDraft.isNotBlank()) {
                                    onAddComment(share, currentDraft)
                                    commentDrafts = commentDrafts - share.id
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                        ) {
                            Icon(Icons.Default.Comment, contentDescription = "Commenter", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspacesScreen(
    workspaces: List<WorkspaceEntity>,
    selectedWorkspaceId: String?,
    notes: List<NoteEntity>,
    onSelectWorkspace: (String?) -> Unit,
    onCreateWorkspace: (name: String, desc: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workspaces_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Espaces de Travail (${workspaces.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cloisonnez vos projets personnels, d'équipe et de recherche IA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nouvel espace de travail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom de l'espace") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description / Objectif") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreateWorkspace(name, desc, "#7457F5")
                                name = ""
                                desc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Créer l'espace")
                    }
                }
            }
        }

        items(workspaces, key = { it.id }) { ws ->
            val isSelected = selectedWorkspaceId == ws.id
            val noteCount = notes.count { it.workspaceId == ws.id }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectWorkspace(if (isSelected) null else ws.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MindoraVioletAI.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Workspaces, contentDescription = null, tint = MindoraVioletAI)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(ws.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(ws.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$noteCount note(s) dans cet espace", style = MaterialTheme.typography.labelSmall, color = MindoraBlueSecondary)
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Actif", tint = MindoraSuccess)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndSaasPlansScreen(
    user: UserEntity?,
    notesCount: Int,
    isDarkMode: Boolean,
    securityLogs: List<SecurityLogEntity>,
    onToggleDarkMode: () -> Unit,
    onSelectPlan: (PlanTier) -> Unit,
    onExportFullBackup: () -> Unit,
    onLogout: () -> Unit
) {
    val activePlan = PlanTier.entries.find { it.name == user?.planTier } ?: PlanTier.PRO
    val df = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale.FRENCH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_saas_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Creator Attribution, Mobile APK & GitHub Release Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("creator_apk_github_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🚀 MINDORA AI v2.0.0 — APK & GitHub Ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Projet réalisé par Mendel Temgoua (mendeltemgouabusiness@gmail.com)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindoraVioletAI,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Package Android : com.aistudio.mindoraai.vqxkpl (Signé & Prêt à installer)\n• Pipeline CI/CD : .github/workflows/android-apk-release.yml inclus\n• Licence Open-Source / SaaS : MIT (Copyright © 2026 Mendel Temgoua)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onExportFullBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_full_backup_btn")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporter la Sauvegarde Complète (.md)")
                    }
                }
            }
        }

        // 1. User Profile & Theme Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = user?.name ?: "Mendel Temgoua",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${user?.email ?: ""} • Offre actuelle : ${activePlan.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindoraVioletAI
                            )
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("settings_logout_btn")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Déconnexion")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Dark / Light Theme Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MindoraVioletAI
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Apparence de l'interface",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDarkMode) "Mode Sombre (#101114 Obsidian)" else "Mode Clair (#F5F6FA Clarté)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            modifier = Modifier.testTag("settings_theme_switch")
                        )
                    }
                }
            }
        }

        // 2. SaaS Quotas Monitor (Section 19)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Consommation & Quotas SaaS (${activePlan.displayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Notes actives : $notesCount / ${activePlan.maxNotes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (notesCount.toFloat() / activePlan.maxNotes.toFloat()).coerceIn(0.02f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MindoraVioletAI
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val aiUsed = user?.aiQuotaUsed ?: 0
                    Text(
                        text = "Requêtes Assistant IA & RAG ce mois : $aiUsed / ${activePlan.maxAiQueriesPerMonth}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (aiUsed.toFloat() / activePlan.maxAiQueriesPerMonth.toFloat()).coerceIn(0.02f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MindoraBlueSecondary
                    )
                }
            }
        }

        // 3. SaaS Subscription Tiers (FREE, PRO, BUSINESS — Section 19)
        item {
            Text(
                text = "Plans d'Abonnement SaaS Mindora AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Architecture de gestion des quotas sans simulation de paiement bancaire",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(PlanTier.entries) { tier ->
            val isCurrent = activePlan == tier
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_card_${tier.name}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MindoraVioletAI.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Offre ${tier.displayName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tier.priceLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MindoraVioletAI
                            )
                        }
                        Button(
                            onClick = { onSelectPlan(tier) },
                            enabled = !isCurrent,
                            colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                        ) {
                            Text(if (isCurrent) "Plan Actif" else "Activer ce plan")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    tier.features.forEach { feat ->
                        Text(
                            text = "✓ $feat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Security & Audit Log (Section 14)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MindoraSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Journal d'Audit de Sécurité (${securityLogs.size} événements)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    securityLogs.take(8).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[${log.eventType}] ${log.details}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = df.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
