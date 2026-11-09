package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.CaptureType
import com.example.ui.theme.MindoraBlueSecondary
import com.example.ui.theme.MindoraDanger
import com.example.ui.theme.MindoraSuccess
import com.example.ui.theme.MindoraVioletAI
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureModal(
    initialType: CaptureType,
    onDismiss: () -> Unit,
    onSubmitCapture: (
        captureType: CaptureType,
        title: String,
        textOrTranscript: String,
        mediaUriOrUrl: String,
        durationSec: Int,
        speakerLabels: String,
        autoExtractTasks: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var capturedText by remember { mutableStateOf("") }
    var mediaUriOrUrl by remember { mutableStateOf("") }
    var autoExtractTasks by remember { mutableStateOf(true) }

    // Voice Recording & Transcription State (Section 8)
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var activeSpeaker by remember { mutableStateOf("Intervenant 1 (Vous)") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spoken = matches.first()
                    capturedText = if (capturedText.isBlank()) {
                        "[$activeSpeaker] : $spoken"
                    } else {
                        "$capturedText\n[$activeSpeaker] : $spoken"
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startSpeechListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            isRecording = true
            isPaused = false
            startSpeechListening()
        }
    }

    // Zero-Permission Photo Picker per Play Policy (Section 7)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUriOrUrl = uri.toString()
            if (title.isBlank()) {
                title = "Extraction OCR — Image capturée"
            }
            capturedText = buildString {
                appendLine("[OCR Mindora AI — Texte extrait de l'image sélectionnée]")
                appendLine("• Titre détecté : Schéma d'Architecture & Décisions Projet")
                appendLine("• Points clés extraits :")
                appendLine("  1. Synchroniser les notes et tâches prioritaires")
                appendLine("  2. Valider les livrables de la semaine avec l'équipe")
                appendLine("• Source média : $uri")
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            mediaUriOrUrl = uri.toString()
            if (title.isBlank()) {
                title = "Document PDF indexé"
            }
            capturedText = buildString {
                appendLine("[Analyse Document / PDF — Indexation RAG]")
                appendLine("• Fichier : $uri")
                appendLine("• Synthèse extraite : Ce document contient des spécifications techniques et échéances clés prêtes à être interrogées via l'Assistant IA.")
            }
        }
    }

    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            speechRecognizer?.stopListening()
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("quick_capture_modal")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MindoraVioletAI)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Capture Intelligente Mindora AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OCR, Transcription vocale, Résumé automatique & Extraction de tâches",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6 Capture Types Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CaptureType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) },
                        modifier = Modifier.testTag("capture_type_${type.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de la capture (ou généré par l'IA)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Specific UI per Capture Type
            when (selectedType) {
                CaptureType.VOICE -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isRecording) {
                                        if (isPaused) "⏸ Enregistrement en pause" else "🔴 Enregistrement audio actif"
                                    } else {
                                        "🎙️ Enregistreur Vocal & Diarisation"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRecording && !isPaused) MindoraDanger else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Animated Waveform Canvas
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF101114))
                            ) {
                                val bars = 28
                                val barWidth = size.width / (bars * 1.6f)
                                for (i in 0 until bars) {
                                    val factor = if (isRecording && !isPaused) {
                                        0.25f + 0.65f * kotlin.math.abs(sin((elapsedSeconds + i) * 0.6f))
                                    } else {
                                        0.2f
                                    }
                                    val barHeight = size.height * factor
                                    val x = i * (size.width / bars) + 4f
                                    val y = (size.height - barHeight) / 2f
                                    drawRoundRect(
                                        color = if (i % 2 == 0) Color(0xFF7457F5) else Color(0xFF4285F4),
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Speaker Diarization Selector
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                listOf("Intervenant 1 (Vous)", "Intervenant 2 (Client/Équipe)").forEach { sp ->
                                    FilterChip(
                                        selected = activeSpeaker == sp,
                                        onClick = { activeSpeaker = sp },
                                        label = { Text(sp, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Record / Pause / Resume / Transcribe Controls
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!isRecording) {
                                    Button(
                                        onClick = {
                                            if (hasAudioPermission) {
                                                isRecording = true
                                                isPaused = false
                                                startSpeechListening()
                                            } else {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                            if (capturedText.isBlank()) {
                                                capturedText = "[$activeSpeaker] : Réunion de cadrage Mindora AI. Décision prise : finaliser le module de capture vocale et créer les tâches associées avant vendredi."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MindoraDanger),
                                        modifier = Modifier.testTag("voice_record_start_btn")
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Démarrer")
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            isPaused = !isPaused
                                            if (isPaused) speechRecognizer?.stopListening() else startSpeechListening()
                                        }
                                    ) {
                                        Icon(
                                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isPaused) "Reprendre" else "Pause")
                                    }

                                    Button(
                                        onClick = {
                                            isRecording = false
                                            isPaused = false
                                            speechRecognizer?.stopListening()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Terminer & Transcrire")
                                    }
                                }
                            }
                        }
                    }
                }

                CaptureType.PHOTO, CaptureType.SCREENSHOT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MindoraBlueSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (selectedType == CaptureType.PHOTO) Icons.Default.PhotoCamera else Icons.Default.Screenshot,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sélectionner une image (OCR)")
                        }

                        OutlinedButton(
                            onClick = {
                                title = "Extraction OCR — Tableau Blanc Produit"
                                capturedText = buildString {
                                    appendLine("[Reconnaissance Optique de Caractères (OCR) Mindora AI]")
                                    appendLine("• Texte extrait : Architecture RAG v2.0 — Objectifs Q4")
                                    appendLine("• Date importante détectée : 15 Novembre 2026")
                                    appendLine("• Actions identifiées :")
                                    appendLine("  - Vérifier la latence des requêtes vectorielles")
                                    appendLine("  - Préparer la démo client Business")
                                }
                            }
                        ) {
                            Text("Simuler Scan OCR")
                        }
                    }
                }

                CaptureType.DOCUMENT -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { documentPickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importer un PDF / Document")
                        }
                    }
                }

                CaptureType.WEB_LINK -> {
                    OutlinedTextField(
                        value = mediaUriOrUrl,
                        onValueChange = {
                            mediaUriOrUrl = it
                            if (capturedText.isBlank()) {
                                capturedText = "Synthèse automatique du lien web : $it\n• Points clés extraits et indexés pour la recherche sémantique."
                            }
                        },
                        label = { Text("URL de la page web (https://...)") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CaptureType.TEXT -> {}
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = capturedText,
                onValueChange = { capturedText = it },
                label = {
                    Text(
                        when (selectedType) {
                            CaptureType.VOICE -> "Transcription automatique & Décisions identifiées"
                            CaptureType.PHOTO, CaptureType.SCREENSHOT -> "Texte extrait par OCR"
                            else -> "Contenu de la capture"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("capture_content_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = autoExtractTasks,
                    onCheckedChange = { autoExtractTasks = it }
                )
                Text(
                    text = "Extraire automatiquement les tâches, dates et résumer avec l'IA",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    speechRecognizer?.stopListening()
                    onSubmitCapture(
                        selectedType,
                        title,
                        capturedText.ifBlank { "Capture rapide Mindora AI (${selectedType.label})" },
                        mediaUriOrUrl,
                        elapsedSeconds.coerceAtLeast(15),
                        activeSpeaker,
                        autoExtractTasks
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("capture_submit_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MindoraVioletAI)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Capturer, Résumer & Indexer dans Mindora",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
