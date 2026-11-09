package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NoteEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskStatus
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraDanger
import com.example.ui.theme.MindoraSuccess
import com.example.ui.theme.MindoraVioletAI
import com.example.ui.theme.MindoraWarning
import com.example.ui.viewmodel.TaskViewMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksAndCalendarScreen(
    tasks: List<TaskEntity>,
    notes: List<NoteEntity>,
    activeViewMode: TaskViewMode,
    forceCalendarView: Boolean = false,
    onViewModeChange: (TaskViewMode) -> Unit,
    onCreateTask: (title: String, description: String, priority: TaskPriority, category: String, dueInDays: Int) -> Unit,
    onCycleTaskStatus: (TaskEntity) -> Unit,
    onUpdateTaskStatus: (TaskEntity, TaskStatus) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onOpenSourceNote: (String) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Projet SaaS") }
    var newPriority by remember { mutableStateOf(TaskPriority.HIGH) }
    var dueInDays by remember { mutableStateOf(2) }

    val effectiveMode = if (forceCalendarView) TaskViewMode.CALENDAR else activeViewMode
    val df = remember { SimpleDateFormat("EEE dd MMM yyyy", Locale.FRENCH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasks_calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (forceCalendarView) "Calendrier & Échéances" else "Gestion des Tâches",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tasks.count { it.status != TaskStatus.DONE.name }} en cours • ${tasks.count { it.status == TaskStatus.DONE.name }} terminée(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddForm = !showAddForm },
                    colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                    modifier = Modifier.testTag("tasks_new_task_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nouvelle tâche")
                }
            }
        }

        // View Switcher (Liste, Kanban, Calendrier, Priorités) — Section 11
        if (!forceCalendarView) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = activeViewMode == mode,
                            onClick = { onViewModeChange(mode) },
                            label = { Text("Vue ${mode.label}") },
                            modifier = Modifier.testTag("task_view_mode_${mode.name}")
                        )
                    }
                }
            }
        }

        // New Task Creation Card
        if (showAddForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Créer une tâche actionnable",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Titre de la tâche") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskPriority.entries.forEach { prio ->
                                FilterChip(
                                    selected = newPriority == prio,
                                    onClick = { newPriority = prio },
                                    label = { Text(prio.label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    onCreateTask(newTitle, newDesc, newPriority, newCategory, dueInDays)
                                    newTitle = ""
                                    newDesc = ""
                                    showAddForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                            modifier = Modifier.testTag("task_save_btn")
                        ) {
                            Text("Enregistrer la tâche")
                        }
                    }
                }
            }
        }

        when (effectiveMode) {
            // VUE 1: KANBAN (À faire, En cours, Terminée, En attente)
            TaskViewMode.KANBAN -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TaskStatus.entries.forEach { statusCol ->
                            val columnTasks = tasks.filter { it.status == statusCol.name }
                            Card(
                                modifier = Modifier.width(280.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = statusCol.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = MindoraVioletAI.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = columnTasks.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MindoraVioletAI,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    columnTasks.forEach { task ->
                                        val sourceNote = notes.find { it.id == task.noteId }
                                        TaskItemCard(
                                            task = task,
                                            sourceNoteTitle = sourceNote?.title,
                                            dateText = df.format(Date(task.dueDate)),
                                            onCycleStatus = { onCycleTaskStatus(task) },
                                            onDelete = { onDeleteTask(task) },
                                            onOpenSourceNote = {
                                                if (task.noteId != null) onOpenSourceNote(task.noteId)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // VUE 2: PRIORITÉS
            TaskViewMode.PRIORITY -> {
                TaskPriority.entries.reversed().forEach { prio ->
                    val prioTasks = tasks.filter { it.priority == prio.name }
                    if (prioTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "🔥 Priorité ${prio.label} (${prioTasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor(prio)
                            )
                        }
                        items(prioTasks, key = { it.id }) { task ->
                            val sourceNote = notes.find { it.id == task.noteId }
                            TaskItemCard(
                                task = task,
                                sourceNoteTitle = sourceNote?.title,
                                dateText = df.format(Date(task.dueDate)),
                                onCycleStatus = { onCycleTaskStatus(task) },
                                onDelete = { onDeleteTask(task) },
                                onOpenSourceNote = {
                                    if (task.noteId != null) onOpenSourceNote(task.noteId)
                                }
                            )
                        }
                    }
                }
            }

            // VUE 3: CALENDRIER & CHRONOLOGIE
            TaskViewMode.CALENDAR -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MindoraVioletAI)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Planning Chronologique des Échéances & Notes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Synchronisé avec vos tâches extraites par l'IA et vos notes de réunion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                items(tasks.sortedBy { it.dueDate }, key = { it.id }) { task ->
                    val sourceNote = notes.find { it.id == task.noteId }
                    TaskItemCard(
                        task = task,
                        sourceNoteTitle = sourceNote?.title,
                        dateText = "📅 Échéance : ${df.format(Date(task.dueDate))}",
                        onCycleStatus = { onCycleTaskStatus(task) },
                        onDelete = { onDeleteTask(task) },
                        onOpenSourceNote = {
                            if (task.noteId != null) onOpenSourceNote(task.noteId)
                        }
                    )
                }
            }

            // VUE 4: LISTE
            TaskViewMode.LIST -> {
                items(tasks, key = { it.id }) { task ->
                    val sourceNote = notes.find { it.id == task.noteId }
                    TaskItemCard(
                        task = task,
                        sourceNoteTitle = sourceNote?.title,
                        dateText = df.format(Date(task.dueDate)),
                        onCycleStatus = { onCycleTaskStatus(task) },
                        onDelete = { onDeleteTask(task) },
                        onOpenSourceNote = {
                            if (task.noteId != null) onOpenSourceNote(task.noteId)
                        }
                    )
                }
            }
        }
    }
}

private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.URGENT -> MindoraDanger
    TaskPriority.HIGH -> MindoraWarning
    TaskPriority.MEDIUM -> MindoraBlueSecondary
    TaskPriority.LOW -> MindoraSuccess
}

@Composable
private fun TaskItemCard(
    task: TaskEntity,
    sourceNoteTitle: String?,
    dateText: String,
    onCycleStatus: () -> Unit,
    onDelete: () -> Unit,
    onOpenSourceNote: () -> Unit
) {
    val prio = TaskPriority.entries.find { it.name == task.priority } ?: TaskPriority.MEDIUM
    val statusObj = TaskStatus.entries.find { it.name == task.status } ?: TaskStatus.TODO
    val isDone = statusObj == TaskStatus.DONE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCycleStatus() }
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Changer statut",
                        tint = if (isDone) MindoraSuccess else priorityColor(prio)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer tâche", modifier = Modifier.size(16.dp))
                }
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = priorityColor(prio).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = priorityColor(prio),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = prio.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor(prio)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onCycleStatus() }
                    ) {
                        Text(
                            text = statusObj.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sourceNoteTitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MindoraVioletAI.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onOpenSourceNote() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MindoraVioletAI,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Note source : ${sourceNoteTitle.take(32)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MindoraVioletAI
                        )
                    }
                }
            }
        }
    }
}
