package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import android.speech.tts.TextToSpeech
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.AttachmentEntity
import com.example.data.local.BlockType
import com.example.data.local.FolderEntity
import com.example.data.local.MindoraJsonCodec
import com.example.data.local.NoteBlock
import com.example.data.local.NoteEntity
import com.example.data.local.NoteStatus
import com.example.data.local.NoteTagCrossRef
import com.example.data.local.SharePermission
import com.example.data.local.TagEntity
import com.example.data.repository.AiActionResult
import com.example.data.repository.AiActionType
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraSuccess
import com.example.ui.theme.MindoraVioletAI
import com.example.ui.theme.MindoraWarning
import com.example.ui.theme.NoteColorPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    allNotes: List<NoteEntity>,
    folders: List<FolderEntity>,
    tags: List<TagEntity>,
    noteTagCrossRefs: List<NoteTagCrossRef>,
    attachments: List<AttachmentEntity>,
    saveStatusText: String,
    isAiProcessing: Boolean,
    latestAiResult: AiActionResult?,
    onBack: () -> Unit,
    onAutoSave: (
        newTitle: String,
        newContent: String,
        newBlocks: List<NoteBlock>,
        newFolderId: String?,
        newStatus: String,
        newColorHex: String,
        newLinkedIdsCsv: String,
        recordSnapshot: Boolean
    ) -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToTrash: () -> Unit,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String) -> Unit,
    onRunAiAction: (AiActionType) -> Unit,
    onApplyAiOutput: (String, Boolean) -> Unit,
    onDismissAiResult: () -> Unit,
    onExportNote: (String) -> Unit,
    onShareNote: (String, String, SharePermission) -> Unit,
    onOpenLinkedNote: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var title by remember(note.id) { mutableStateOf(note.title) }
    var rawMarkdownContent by remember(note.id) { mutableStateOf(note.content) }
    var selectedFolderId by remember(note.id) { mutableStateOf(note.folderId) }
    var selectedStatus by remember(note.id) { mutableStateOf(note.status) }
    var selectedColorHex by remember(note.id) { mutableStateOf(note.colorHex) }
    var linkedNoteIdsCsv by remember(note.id) { mutableStateOf(note.linkedNoteIdsCsv) }

    val blocks = remember(note.id) {
        val decoded = MindoraJsonCodec.decodeBlocks(note.blocksJson)
        mutableStateListOf<NoteBlock>().apply {
            if (decoded.isEmpty()) {
                add(NoteBlock(type = BlockType.PARAGRAPH, text = note.content))
            } else {
                addAll(decoded)
            }
        }
    }

    var isBlockEditorMode by remember { mutableStateOf(true) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    val attachedTagIds = noteTagCrossRefs.filter { it.noteId == note.id }.map { it.tagId }.toSet()
    val noteAttachments = attachments.filter { it.noteId == note.id }
    val versionHistory = MindoraJsonCodec.decodeVersions(note.versionHistoryJson)
    val linkedNotes = allNotes.filter {
        it.id != note.id && linkedNoteIdsCsv.split(",").map { s -> s.trim() }.contains(it.id)
    }

    // v2.0.0 Real-time Analytics & Android TextToSpeech (TTS)
    val wordCount = remember(rawMarkdownContent) {
        rawMarkdownContent.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = rawMarkdownContent.length
    val readingTimeMin = (wordCount / 180).coerceAtLeast(1)

    var isSpeakingTts by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.FRENCH
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    fun syncAndAutoSave(recordSnapshot: Boolean = false) {
        val compiledContent = if (isBlockEditorMode) {
            blocks.joinToString("\n\n") { b ->
                when (b.type) {
                    BlockType.HEADING_1 -> "# ${b.text}"
                    BlockType.HEADING_2 -> "## ${b.text}"
                    BlockType.BULLET_LIST -> "• ${b.text}"
                    BlockType.NUMBERED_LIST -> "1. ${b.text}"
                    BlockType.CHECKLIST -> if (b.isChecked) "- [x] ${b.text}" else "- [ ] ${b.text}"
                    BlockType.QUOTE -> "> ${b.text}"
                    BlockType.CODE -> "```\n${b.text}\n```"
                    BlockType.TABLE -> b.text
                    BlockType.LINK -> "🔗 ${b.text}"
                    BlockType.PARAGRAPH -> b.text
                }
            }
        } else {
            rawMarkdownContent
        }
        rawMarkdownContent = compiledContent
        onAutoSave(
            title,
            compiledContent,
            blocks.toList(),
            selectedFolderId,
            selectedStatus,
            selectedColorHex,
            linkedNoteIdsCsv,
            recordSnapshot
        )
    }

    fun applyInlineFormat(wrapperPrefix: String, wrapperSuffix: String = wrapperPrefix) {
        rawMarkdownContent = "$rawMarkdownContent $wrapperPrefix" + "texte" + "$wrapperSuffix"
        blocks.add(NoteBlock(type = BlockType.PARAGRAPH, text = "${wrapperPrefix}Texte enrichi${wrapperSuffix}"))
        syncAndAutoSave()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("note_editor_screen")
    ) {
        // 1. Editor Top Action Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            syncAndAutoSave(recordSnapshot = true)
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    Column {
                        Text(
                            text = saveStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MindoraSuccess
                        )
                        Text(
                            text = "$wordCount mots • $charCount car. • ~$readingTimeMin min lecture • ${blocks.size} blocs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // AI Assistant Button
                    Button(
                        onClick = { showAiSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("editor_ai_copilot_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("IA Copilot", style = MaterialTheme.typography.labelLarge)
                    }

                    // v2.0.0 Text-To-Speech Audio Reader Button
                    IconButton(
                        onClick = {
                            if (isSpeakingTts) {
                                ttsEngine?.stop()
                                isSpeakingTts = false
                            } else {
                                ttsEngine?.speak(
                                    "$title. $rawMarkdownContent",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "mindora_tts_${note.id}"
                                )
                                isSpeakingTts = true
                            }
                        },
                        modifier = Modifier.testTag("editor_tts_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeakingTts) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                            contentDescription = "Écouter la note (Synthèse vocale)",
                            tint = if (isSpeakingTts) MindoraBlueSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favori",
                            tint = if (note.isFavorite) MindoraWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(Icons.Default.History, contentDescription = "Historique des modifications")
                    }

                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Partager la note")
                    }

                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("editor_export_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Exporter PDF / MD / TXT")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exporter en PDF (.pdf)") },
                                onClick = {
                                    showExportMenu = false
                                    onExportNote("PDF")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exporter en Markdown (.md)") },
                                onClick = {
                                    showExportMenu = false
                                    onExportNote("MARKDOWN")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exporter en Texte (.txt)") },
                                onClick = {
                                    showExportMenu = false
                                    onExportNote("TXT")
                                }
                            )
                        }
                    }

                    IconButton(onClick = onMoveToTrash) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Mettre à la corbeille",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 2. Rich Formatting & Block Insertion Toolbar (Section 5)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isBlockEditorMode,
                    onClick = { isBlockEditorMode = !isBlockEditorMode },
                    label = { Text(if (isBlockEditorMode) "Mode Blocs" else "Mode Markdown") }
                )
                IconButton(onClick = { applyInlineFormat("**") }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Gras")
                }
                IconButton(onClick = { applyInlineFormat("*") }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italique")
                }
                IconButton(onClick = { applyInlineFormat("<u>", "</u>") }) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "Souligné")
                }
                IconButton(onClick = { applyInlineFormat("~~") }) {
                    Icon(Icons.Default.FormatStrikethrough, contentDescription = "Barré")
                }
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.HEADING_1, text = "Nouveau titre de section"))
                        syncAndAutoSave()
                    },
                    label = { Text("H1") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.CHECKLIST, text = "Élément à vérifier", isChecked = false))
                        syncAndAutoSave()
                    },
                    label = { Text("Case à cocher") },
                    leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.BULLET_LIST, text = "Idée clé"))
                        syncAndAutoSave()
                    },
                    label = { Text("Puces") },
                    leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.NUMBERED_LIST, text = "Étape numérotée"))
                        syncAndAutoSave()
                    },
                    label = { Text("1. Liste") },
                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.TABLE, text = "Colonne A | Colonne B\nValeur 1 | Valeur 2"))
                        syncAndAutoSave()
                    },
                    label = { Text("Tableau") },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.QUOTE, text = "Citation ou décision clé"))
                        syncAndAutoSave()
                    },
                    label = { Text("Citation") },
                    leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.CODE, text = "// Bloc de code\nval status = \"Mindora Ready\""))
                        syncAndAutoSave()
                    },
                    label = { Text("Code") },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        blocks.add(NoteBlock(type = BlockType.LINK, text = "https://mindora.ai/documentation"))
                        syncAndAutoSave()
                    },
                    label = { Text("Lien") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // 3. Main Scrollable Note Canvas
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Note Title Input
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        syncAndAutoSave()
                    },
                    label = { Text("Titre de la note") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )
            }

            // Metadata Row: Folder, Status, Color Picker
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder selector chips
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = {
                                    selectedFolderId = folder.id
                                    syncAndAutoSave()
                                },
                                label = { Text("📁 ${folder.name}") }
                            )
                        }
                        // Status chips
                        NoteStatus.entries.forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st.name,
                                onClick = {
                                    selectedStatus = st.name
                                    syncAndAutoSave()
                                },
                                label = { Text(st.label) }
                            )
                        }
                    }

                    // Color Palette & Tags
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Couleur :", style = MaterialTheme.typography.labelMedium)
                        NoteColorPalette.forEach { (hex, color) ->
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColorHex = hex
                                        syncAndAutoSave()
                                    }
                            )
                        }
                    }

                    // Tags FlowRow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            val attached = tag.id in attachedTagIds
                            FilterChip(
                                selected = attached,
                                onClick = { onToggleTag(tag.id) },
                                label = { Text("#${tag.name}") }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTagInput,
                                onValueChange = { newTagInput = it },
                                placeholder = { Text("+ Nouveau tag") },
                                singleLine = true,
                                modifier = Modifier.width(150.dp)
                            )
                            if (newTagInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onCreateTag(newTagInput)
                                        newTagInput = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Ajouter tag")
                                }
                            }
                        }
                    }
                }
            }

            // Latest AI Action Output Card (if active)
            if (latestAiResult != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_ai_result_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                color = MindoraVioletAI.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = latestAiResult.firebaseNotice,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindoraVioletAI,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = latestAiResult.outputText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onApplyAiOutput(latestAiResult.outputText, false)
                                        blocks.add(NoteBlock(type = BlockType.PARAGRAPH, text = latestAiResult.outputText))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                                ) {
                                    Text("Insérer dans la note")
                                }
                                OutlinedButton(
                                    onClick = {
                                        rawMarkdownContent = latestAiResult.outputText
                                        onApplyAiOutput(latestAiResult.outputText, true)
                                    }
                                ) {
                                    Text("Remplacer")
                                }
                                TextButton(onClick = onDismissAiResult) {
                                    Text("Fermer")
                                }
                            }
                        }
                    }
                }
            }

            // Movable Blocks Editor OR Raw Markdown Editor
            if (isBlockEditorMode) {
                itemsIndexed(blocks, key = { _, b -> b.id }) { index, block ->
                    MovableNoteBlockItem(
                        block = block,
                        canMoveUp = index > 0,
                        canMoveDown = index < blocks.lastIndex,
                        onTextChange = { updatedText ->
                            blocks[index] = block.copy(text = updatedText)
                            syncAndAutoSave()
                        },
                        onToggleCheck = { checked ->
                            blocks[index] = block.copy(isChecked = checked)
                            syncAndAutoSave()
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val item = blocks.removeAt(index)
                                blocks.add(index - 1, item)
                                syncAndAutoSave()
                            }
                        },
                        onMoveDown = {
                            if (index < blocks.lastIndex) {
                                val item = blocks.removeAt(index)
                                blocks.add(index + 1, item)
                                syncAndAutoSave()
                            }
                        },
                        onDeleteBlock = {
                            if (blocks.size > 1) {
                                blocks.removeAt(index)
                                syncAndAutoSave()
                            }
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            blocks.add(NoteBlock(type = BlockType.PARAGRAPH, text = ""))
                            syncAndAutoSave()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter un bloc de contenu")
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = rawMarkdownContent,
                        onValueChange = {
                            rawMarkdownContent = it
                            syncAndAutoSave()
                        },
                        label = { Text("Contenu Markdown enrichi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .testTag("editor_markdown_input")
                    )
                }
            }

            // Voice Recordings, OCR & File Attachments on this Note
            if (noteAttachments.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        text = "📎 Pièces jointes, Transcriptions Vocales & OCR (${noteAttachments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(noteAttachments, key = { it.id }) { att ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MindoraVioletAI
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${att.fileName} (${att.fileType})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (att.speakerLabels.isNotBlank()) {
                                        Text(
                                            text = "Intervenants : ${att.speakerLabels} • Durée : ${att.durationSeconds}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (att.transcriptOrOcrText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = att.transcriptOrOcrText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (att.extractedDecisions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Décisions extraites : ${att.extractedDecisions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindoraSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Bidirectional Note Connections (Section 10)
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    text = "🔗 Connexions bidirectionnelles (${linkedNotes.size} notes reliées)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    linkedNotes.forEach { linked ->
                        AssistChip(
                            onClick = { onOpenLinkedNote(linked.id) },
                            label = { Text("[[ ${linked.title.take(28)} ]]") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    AssistChip(
                        onClick = { onRunAiAction(AiActionType.FIND_RELATIONS) },
                        label = { Text("+ Relier automatiquement avec l'IA") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }

    // Modal 1: Integrated AI Copilot Sheet (Section 6: A. Comprendre, B. Améliorer, C. Organiser, D. Transformer)
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MindoraVioletAI)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Assistant IA Mindora — Actions sur la note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Mode Aperçu IA — Activez Firebase AI Logic pour connecter Gemini Cloud.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isAiProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyse intelligente en cours…")
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(380.dp)
                ) {
                    items(AiActionType.entries) { action ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAiSheet = false
                                    onRunAiAction(action)
                                }
                                .testTag("ai_action_${action.name}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "[${action.category}] ${action.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MindoraVioletAI
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal 2: Modification History Sheet (Section 5)
    if (showHistorySheet) {
        val df = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.FRENCH)
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Historique des modifications (${versionHistory.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (versionHistory.isEmpty()) {
                    Text("Aucun instantané enregistré pour le moment.")
                } else {
                    versionHistory.forEach { snap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${snap.title} • par ${snap.authorName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = df.format(Date(snap.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindoraBlueSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = snap.contentPreview,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal 3: Collaboration Share Sheet (Section 13)
    if (showShareDialog) {
        var collaboratorEmail by remember { mutableStateOf("") }
        var collaboratorName by remember { mutableStateOf("") }
        var selectedPerm by remember { mutableStateOf(SharePermission.EDIT) }

        ModalBottomSheet(onDismissRequest = { showShareDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Partager « ${note.title} »",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = collaboratorEmail,
                    onValueChange = { collaboratorEmail = it },
                    label = { Text("Email du collaborateur") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = collaboratorName,
                    onValueChange = { collaboratorName = it },
                    label = { Text("Nom / Rôle (facultatif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SharePermission.entries.forEach { perm ->
                        FilterChip(
                            selected = selectedPerm == perm,
                            onClick = { selectedPerm = perm },
                            label = { Text(perm.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onShareNote(collaboratorEmail, collaboratorName, selectedPerm)
                        showShareDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                ) {
                    Text("Envoyer l'invitation de collaboration")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MovableNoteBlockItem(
    block: NoteBlock,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTextChange: (String) -> Unit,
    onToggleCheck: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDeleteBlock: () -> Unit
) {
    val containerColor = when (block.type) {
        BlockType.CODE -> Color(0xFF14161D)
        BlockType.QUOTE -> MindoraVioletAI.copy(alpha = 0.08f)
        BlockType.TABLE -> MindoraBlueSecondary.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = block.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Row {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Monter le bloc", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre le bloc", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteBlock, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer le bloc", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (block.type == BlockType.CHECKLIST) {
                    Checkbox(
                        checked = block.isChecked,
                        onCheckedChange = onToggleCheck
                    )
                }
                OutlinedTextField(
                    value = block.text,
                    onValueChange = onTextChange,
                    textStyle = when (block.type) {
                        BlockType.HEADING_1 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        BlockType.HEADING_2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        BlockType.CODE -> MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                        BlockType.QUOTE -> MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                        BlockType.CHECKLIST -> MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (block.isChecked) TextDecoration.LineThrough else TextDecoration.None
                        )
                        else -> MaterialTheme.typography.bodyLarge
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
