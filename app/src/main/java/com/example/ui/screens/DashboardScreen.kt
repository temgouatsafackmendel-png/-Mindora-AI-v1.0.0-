package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AttachmentEntity
import com.example.data.local.CaptureType
import com.example.data.local.NoteEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.data.local.UserEntity
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraDanger
import com.example.ui.theme.MindoraSuccess
import com.example.ui.theme.MindoraVioletAI
import com.example.ui.theme.MindoraWarning
import com.example.ui.viewmodel.NavSection
import com.example.ui.viewmodel.NoteTemplate
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    user: UserEntity?,
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    attachments: List<AttachmentEntity>,
    onOpenNote: (String) -> Unit,
    onCreateNewNote: () -> Unit,
    onCreateFromTemplate: (NoteTemplate) -> Unit = {},
    onOpenQuickCapture: (CaptureType) -> Unit,
    onNavigateSection: (NavSection) -> Unit,
    onToggleTaskStatus: (TaskEntity) -> Unit
) {
    val now = System.currentTimeMillis()
    val weekAgo = now - 7 * 86_400_000L
    val notesThisWeek = notes.count { it.createdAt >= weekAgo }
    val pendingTasks = tasks.filter { it.status != TaskStatus.DONE.name }
    val completedTasks = tasks.filter { it.status == TaskStatus.DONE.name }
    val favoriteNotes = notes.filter { it.isFavorite }
    val priorityTasks = pendingTasks.sortedByDescending {
        TaskPriority.entries.find { p -> p.name == it.priority }?.level ?: 1
    }.take(3)

    // v2.0.0 Focus Pomodoro Deep Work Timer State
    var focusSecondsLeft by remember { mutableIntStateOf(25 * 60) }
    var isFocusRunning by remember { mutableStateOf(false) }
    var isBreakMode by remember { mutableStateOf(false) }

    LaunchedEffect(isFocusRunning) {
        while (isFocusRunning && focusSecondsLeft > 0) {
            delay(1000L)
            focusSecondsLeft--
            if (focusSecondsLeft == 0) {
                isFocusRunning = false
                isBreakMode = !isBreakMode
                focusSecondsLeft = if (isBreakMode) 5 * 60 else 25 * 60
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Hero Banner with Mindora AI Identity + Quick Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mindora_hero_1794216363288),
                        contentDescription = "Bannière Deuxième Cerveau Mindora AI",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xEE101114), Color(0x99101114))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MindoraVioletAI.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "✨ DEUXIÈME CERVEAU NUMÉRIQUE • ${user?.planTier ?: "PRO"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDCD3FF),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bonjour, ${user?.name ?: "Alex"} 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Capture. Understand. Organize. Create.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB5B9C8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onCreateNewNote,
                                colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                                modifier = Modifier.testTag("dashboard_new_note_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nouvelle note")
                            }
                            FilledTonalButton(
                                onClick = { onOpenQuickCapture(CaptureType.VOICE) },
                                modifier = Modifier.testTag("dashboard_voice_capture_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Note vocale")
                            }
                            FilledTonalButton(
                                onClick = { onOpenQuickCapture(CaptureType.PHOTO) }
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OCR")
                            }
                        }
                    }
                }
            }
        }

        // 2. KPI Metrics Grid (Section 12)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatKpiCard(
                        title = "Total de notes",
                        value = notes.size.toString(),
                        subtitle = "+$notesThisWeek cette semaine",
                        icon = Icons.Default.Description,
                        accentColor = MindoraVioletAI,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateSection(NavSection.ALL_NOTES) }
                    )
                    StatKpiCard(
                        title = "Notes (7 jours)",
                        value = notesThisWeek.toString(),
                        subtitle = "${favoriteNotes.size} favorites",
                        icon = Icons.Default.Star,
                        accentColor = MindoraBlueSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateSection(NavSection.FAVORITES) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatKpiCard(
                        title = "Tâches en attente",
                        value = pendingTasks.size.toString(),
                        subtitle = "${priorityTasks.size} prioritaires",
                        icon = Icons.Default.Schedule,
                        accentColor = MindoraWarning,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateSection(NavSection.TASKS) }
                    )
                    StatKpiCard(
                        title = "Tâches terminées",
                        value = completedTasks.size.toString(),
                        subtitle = "Exécution suivie",
                        icon = Icons.Default.CheckCircle,
                        accentColor = MindoraSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateSection(NavSection.TASKS) }
                    )
                }
            }
        }

        // 3. Section Spéciale : « Votre journée avec l'IA » (Section 12)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("your_day_with_ai_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MindoraVioletAI,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Votre journée avec l'IA",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Briefing intelligent quotidien • Aperçu IA local (Firebase AI Logic disponible)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { onNavigateSection(NavSection.AI_ASSISTANT) }
                        ) {
                            Text("Interroger RAG")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // A. Tâches prioritaires
                    Text(
                        text = "🎯 Tâches prioritaires du jour",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MindoraVioletAI
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (priorityTasks.isEmpty()) {
                        Text(
                            text = "Toutes vos tâches prioritaires sont terminées !",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        priorityTasks.forEach { task ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onToggleTaskStatus(task) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = "Changer statut",
                                        tint = if (task.priority == TaskPriority.URGENT.name) MindoraDanger else MindoraWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Priorité ${task.priority} • Catégorie : ${task.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // B. Notes importantes & Idées à reprendre
                    Text(
                        text = "💡 Notes importantes & Idées à reprendre",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MindoraBlueSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    notes.take(2).forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onOpenNote(note.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MindoraWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${note.title} — ${note.summary.ifBlank { note.content }.take(65)}…",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // C. Informations récemment capturées (Voix / OCR)
                    if (attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🎙️ Informations récemment capturées (Voix & OCR)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MindoraSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        attachments.take(2).forEach { att ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onOpenNote(att.noteId) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (att.fileType == "VOICE") Icons.Default.GraphicEq else Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MindoraSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${att.fileName} : ${att.summaryText.ifBlank { att.transcriptOrOcrText }.take(70)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Suggestions Intelligentes de l'IA
        item {
            Column {
                Text(
                    text = "Suggestions intelligentes de l'IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { onNavigateSection(NavSection.AI_ASSISTANT) },
                        label = { Text("Résumer mes notes de la semaine") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { onNavigateSection(NavSection.FOLDERS) },
                        label = { Text("Explorer le graphe de connaissances") },
                        leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { onOpenQuickCapture(CaptureType.VOICE) },
                        label = { Text("Enregistrer un mémo vocal transcrit") },
                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // 4B. Mise à jour v2.0 : Galerie de Modèles Pro en 1-Clic (Templates)
        item {
            Column {
                Text(
                    text = "⚡ Modèles Intelligents Pro (Création en 1 clic — v2.0)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(NoteTemplate.entries) { tpl ->
                        Card(
                            modifier = Modifier
                                .width(225.dp)
                                .clickable { onCreateFromTemplate(tpl) }
                                .testTag("template_card_${tpl.name}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Surface(
                                    color = MindoraVioletAI.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tpl.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindoraVioletAI,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tpl.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tpl.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4C. Mise à jour v2.0 : Minuteur Focus Deep Work (Pomodoro)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_pomodoro_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = MindoraBlueSecondary.copy(alpha = 0.18f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MindoraBlueSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBreakMode) "☕ Pause Récupération (5 min)" else "🔥 Session Deep Work Focus (25 min)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%02d:%02d restantes • Concentration maximale".format(
                                    focusSecondsLeft / 60,
                                    focusSecondsLeft % 60
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MindoraVioletAI,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isFocusRunning = !isFocusRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFocusRunning) MindoraWarning else MindoraVioletAI
                            ),
                            modifier = Modifier.testTag("focus_timer_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isFocusRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFocusRunning) "Pause" else "Focus")
                        }
                        OutlinedButton(
                            onClick = {
                                isFocusRunning = false
                                isBreakMode = false
                                focusSecondsLeft = 25 * 60
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Réinitialiser", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 5. Notes Favorites (Horizontal Carousel)
        if (favoriteNotes.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "⭐ Notes favorites (${favoriteNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteNotes, key = { it.id }) { fav ->
                            Card(
                                modifier = Modifier
                                    .width(250.dp)
                                    .clickable { onOpenNote(fav.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MindoraWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = fav.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = fav.summary.ifBlank { fav.content },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Documents Récents & Activité Récente
        item {
            Text(
                text = "Documents récents & Activité",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(notes.take(6), key = { it.id }) { note ->
            RecentNoteDashboardRow(
                note = note,
                onClick = { onOpenNote(note.id) }
            )
        }
    }
}

@Composable
private fun StatKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun RecentNoteDashboardRow(
    note: NoteEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.FRENCH)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("recent_note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MindoraVioletAI.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MindoraVioletAI
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = note.summary.ifBlank { note.content },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modifié le ${dateFormat.format(Date(note.updatedAt))} • Statut : ${note.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
