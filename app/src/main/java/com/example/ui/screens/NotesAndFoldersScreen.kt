package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NoteTagCrossRef
import com.example.data.local.TagEntity
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraSuccess
import com.example.ui.theme.MindoraVioletAI
import com.example.ui.theme.MindoraWarning
import com.example.ui.viewmodel.SearchFilterMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesListScreen(
    sectionTitle: String,
    notes: List<NoteEntity>,
    folders: List<FolderEntity>,
    tags: List<TagEntity>,
    noteTagCrossRefs: List<NoteTagCrossRef>,
    selectedFolderId: String?,
    selectedTagId: String?,
    searchQuery: String,
    searchFilterMode: SearchFilterMode,
    onSearchFilterModeChange: (SearchFilterMode) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onOpenNote: (String) -> Unit,
    onToggleFavorite: (NoteEntity) -> Unit,
    onCreateNewNote: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.FRENCH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notes_list_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${notes.size} document(s) accessible(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCreateNewNote,
                    colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nouvelle note")
                }
            }
        }

        // Intelligent Search Mode Filters (Section 9)
        if (searchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilterMode.entries.forEach { mode ->
                        FilterChip(
                            selected = searchFilterMode == mode,
                            onClick = { onSearchFilterModeChange(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }
        }

        // Folder & Tag Filter Bar
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFolderId == null && selectedTagId == null,
                    onClick = {
                        onSelectFolder(null)
                        onSelectTag(null)
                    },
                    label = { Text("Tous") }
                )
                folders.forEach { f ->
                    FilterChip(
                        selected = selectedFolderId == f.id,
                        onClick = { onSelectFolder(if (selectedFolderId == f.id) null else f.id) },
                        label = { Text("📁 ${f.name}") }
                    )
                }
                tags.forEach { t ->
                    FilterChip(
                        selected = selectedTagId == t.id,
                        onClick = { onSelectTag(if (selectedTagId == t.id) null else t.id) },
                        label = { Text("#${t.name}") }
                    )
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aucune note trouvée pour ce filtre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Créez une note en un clic ou utilisez la Capture Rapide.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(notes, key = { it.id }) { note ->
                val noteTagIds = noteTagCrossRefs.filter { it.noteId == note.id }.map { it.tagId }.toSet()
                val noteTags = tags.filter { it.id in noteTagIds }
                val folderName = folders.find { it.id == note.folderId }?.name
                val linkCount = note.linkedNoteIdsCsv.split(",").count { it.isNotBlank() }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNote(note.id) }
                        .testTag("note_card_${note.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MindoraVioletAI)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onToggleFavorite(note) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favori",
                                    tint = if (note.isFavorite) MindoraWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = note.summary.ifBlank { note.content },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (folderName != null) {
                                Surface(
                                    color = MindoraBlueSecondary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "📁 $folderName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindoraBlueSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            noteTags.forEach { tag ->
                                Surface(
                                    color = MindoraVioletAI.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "#${tag.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindoraVioletAI,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (linkCount > 0) {
                                Surface(
                                    color = MindoraSuccess.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🔗 $linkCount lien(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindoraSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mis à jour : ${df.format(Date(note.updatedAt))} • Statut : ${note.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersAndKnowledgeGraphScreen(
    folders: List<FolderEntity>,
    notes: List<NoteEntity>,
    onCreateFolder: (name: String, parentId: String?, collectionTag: String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenFolderNotes: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onAutoDiscoverRelations: () -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    var newCollectionTag by remember { mutableStateOf("Connaissances") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }

    val rootFolders = folders.filter { it.parentId == null }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("folders_graph_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Interactive Knowledge Graph Visualization (Section 10)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = MindoraVioletAI)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Graphe des Connexions de Connaissances",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Touchez un nœud pour ouvrir la note • Liens bidirectionnels",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = onAutoDiscoverRelations) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Découvrir liens IA")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    KnowledgeGraphCanvas(
                        notes = notes.take(8),
                        onNodeClick = onOpenNote
                    )
                }
            }
        }

        // 2. Create Folder / Nested Sub-folder Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Créer un dossier ou sous-dossier imbriqué",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Nom du dossier") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCollectionTag,
                            onValueChange = { newCollectionTag = it },
                            label = { Text("Collection") },
                            singleLine = true,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dossier parent :", style = MaterialTheme.typography.labelSmall)
                        FilterChip(
                            selected = selectedParentId == null,
                            onClick = { selectedParentId = null },
                            label = { Text("Racine") }
                        )
                        rootFolders.forEach { rf ->
                            FilterChip(
                                selected = selectedParentId == rf.id,
                                onClick = { selectedParentId = rf.id },
                                label = { Text(rf.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onCreateFolder(newFolderName, selectedParentId, newCollectionTag)
                                newFolderName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter à l'arborescence")
                    }
                }
            }
        }

        // 3. Nested Folders Tree
        item {
            Text(
                text = "Arborescence des Dossiers & Sous-dossiers (${folders.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(rootFolders, key = { it.id }) { root ->
            val subFolders = folders.filter { it.parentId == root.id }
            val countInRoot = notes.count { it.folderId == root.id }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFolderNotes(root.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MindoraVioletAI)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = root.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Collection : ${root.collectionTag} • $countInRoot note(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteFolder(root.id) }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Supprimer dossier")
                        }
                    }

                    // Render Nested Sub-folders
                    subFolders.forEach { sub ->
                        val subCount = notes.count { it.folderId == sub.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 8.dp)
                                .clickable { onOpenFolderNotes(sub.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SubdirectoryArrowRight,
                                    contentDescription = null,
                                    tint = MindoraBlueSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MindoraBlueSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${sub.name} ($subCount notes)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeGraphCanvas(
    notes: List<NoteEntity>,
    onNodeClick: (String) -> Unit
) {
    var nodePositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101114))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(notes) {
                    detectTapGestures { tapOffset ->
                        val hit = nodePositions.entries.firstOrNull { (_, pos) ->
                            hypot((tapOffset.x - pos.x).toDouble(), (tapOffset.y - pos.y).toDouble()) <= 44.0
                        }
                        if (hit != null) {
                            onNodeClick(hit.key)
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (size.minDimension / 2f) * 0.65f
            val count = notes.size.coerceAtLeast(1)

            val computed = mutableMapOf<String, Offset>()
            notes.forEachIndexed { idx, n ->
                val angle = (2.0 * Math.PI * idx / count).toFloat()
                val px = cx + radius * cos(angle) * 1.35f
                val py = cy + radius * sin(angle) * 0.8f
                computed[n.id] = Offset(px, py)
            }
            nodePositions = computed

            // Draw bidirectional edges
            notes.forEach { n ->
                val start = computed[n.id] ?: return@forEach
                val linkedIds = n.linkedNoteIdsCsv.split(",").map { it.trim() }
                linkedIds.forEach { targetId ->
                    val end = computed[targetId]
                    if (end != null) {
                        drawLine(
                            color = Color(0xFF7457F5).copy(alpha = 0.55f),
                            start = start,
                            end = end,
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Draw glowing nodes
            notes.forEachIndexed { idx, n ->
                val pos = computed[n.id] ?: return@forEachIndexed
                val nodeColor = if (idx % 2 == 0) Color(0xFF7457F5) else Color(0xFF4285F4)
                drawCircle(
                    color = nodeColor.copy(alpha = 0.28f),
                    radius = 28f,
                    center = pos
                )
                drawCircle(
                    color = nodeColor,
                    radius = 15f,
                    center = pos
                )
            }
        }

        // Overlay legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notes.take(4).forEach { n ->
                Surface(
                    color = Color(0xCC1E2028),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onNodeClick(n.id) }
                ) {
                    Text(
                        text = "• ${n.title.take(18)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TrashScreen(
    trashedNotes: List<NoteEntity>,
    onRestoreNote: (NoteEntity) -> Unit,
    onDeleteForever: (NoteEntity) -> Unit,
    onEmptyTrash: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("trash_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Corbeille (${trashedNotes.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Restaurez vos notes ou supprimez-les définitivement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trashedNotes.isNotEmpty()) {
                    OutlinedButton(onClick = onEmptyTrash) {
                        Text("Vider la corbeille")
                    }
                }
            }
        }

        items(trashedNotes, key = { it.id }) { note ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = note.content.take(90),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    Row {
                        IconButton(onClick = { onRestoreNote(note) }) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restaurer", tint = MindoraSuccess)
                        }
                        IconButton(onClick = { onDeleteForever(note) }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Supprimer définitivement", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
