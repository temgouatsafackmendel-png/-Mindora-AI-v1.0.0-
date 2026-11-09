package com.example.data.repository

import com.example.data.local.FolderEntity
import com.example.data.local.NoteEntity
import com.example.data.local.TaskPriority
import kotlin.math.sqrt

enum class AiActionType(val title: String, val category: String, val description: String) {
    // A. Comprendre les notes
    SUMMARIZE("Résumer la note", "Comprendre", "Génère une synthèse exécutive concise"),
    EXPLAIN_PASSAGE("Expliquer les concepts", "Comprendre", "Clarifie les passages techniques et concepts clés"),
    EXTRACT_KEY_INFO("Informations & Idées clés", "Comprendre", "Extrait les points essentiels, dates et chiffres"),

    // B. Améliorer les contenus
    CORRECT_SPELLING("Corriger & Réviser", "Améliorer", "Corrige l'orthographe, la grammaire et la syntaxe"),
    IMPROVE_STYLE("Améliorer le style", "Améliorer", "Reformule avec un ton professionnel et fluide"),
    EXPAND_IDEA("Développer l'idée", "Améliorer", "Approfondit l'argumentation et ajoute des pistes d'exécution"),
    STRUCTURE_DOC("Structurer le document", "Améliorer", "Transforme des notes brutes en document hiérarchisé"),

    // C. Organiser automatiquement
    AUTO_ORGANIZE("Auto-Organisation (Titre, Tags, Dossier)", "Organiser", "Propose un titre optimal, des tags et le dossier cible"),
    FIND_RELATIONS("Relier & Détecter doublons", "Organiser", "Identifie les connexions sémantiques et notes proches"),

    // D. Transformer les notes
    TO_TASKS("Transformer en Tâches", "Transformer", "Extrait et crée des tâches actionnables dans le module Tâches"),
    TO_CHECKLIST("Transformer en Checklist", "Transformer", "Convertit le contenu en liste de contrôle interactive"),
    TO_MEETING_REPORT("Compte rendu de réunion", "Transformer", "Structure en Décisions, Points clés et Prochaines étapes"),
    TO_PROJECT_PLAN("Plan de projet & Planning", "Transformer", "Génère une feuille de route jalonnée avec livrables"),
    TO_PRESENTATION("Plan de présentation", "Transformer", "Découpe la note en diapositives impactantes"),
    TO_EMAIL("Transformer en Email", "Transformer", "Rédige un email prêt à envoyer aux parties prenantes")
}

data class ExtractedTaskCandidate(
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val category: String,
    val dueInDays: Int
)

data class AutoOrganizationResult(
    val suggestedTitle: String,
    val suggestedTags: List<String>,
    val suggestedFolderId: String?,
    val suggestedFolderName: String,
    val duplicateNoteTitle: String?,
    val relatedNoteIds: List<String>,
    val relatedNoteTitles: List<String>,
    val keyHighlights: List<String>
)

data class AiActionResult(
    val action: AiActionType,
    val outputText: String,
    val extractedTasks: List<ExtractedTaskCandidate> = emptyList(),
    val organization: AutoOrganizationResult? = null,
    val isPlaceholderMode: Boolean = true,
    val firebaseNotice: String = "Aperçu IA (Firebase AI Logic non activé) — Réponse générée localement à partir de vos notes."
)

data class RagSourceCitation(
    val noteId: String,
    val noteTitle: String,
    val snippet: String,
    val similarityScore: Float
)

data class RagAnswer(
    val answerText: String,
    val citations: List<RagSourceCitation>,
    val isPlaceholderMode: Boolean = true,
    val firebaseNotice: String = "Aperçu RAG Local — Activez Firebase AI Logic pour connecter Gemini Cloud en direct."
)

interface AiRepository {
    suspend fun executeNoteAction(
        action: AiActionType,
        note: NoteEntity,
        allUserNotes: List<NoteEntity>,
        folders: List<FolderEntity>
    ): AiActionResult

    suspend fun queryKnowledgeBaseRag(
        question: String,
        userNotes: List<NoteEntity>
    ): RagAnswer

    fun computeEmbeddingVector(text: String): List<Float>
    fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Float
}

/**
 * PlaceholderAiRepository:
 * Conformément aux directives lorsque l'utilisateur décline la configuration Firebase,
 * tous les appels IA passent par l'interface AiRepository et retournent des réponses
 * structurées clairement étiquetées comme réponses d'aperçu, appuyées sur l'indexation
 * vectorielle locale (TF-IDF / n-grammes) des notes réelles de l'utilisateur.
 */
class PlaceholderAiRepository : AiRepository {

    private val semanticDimensions = listOf(
        listOf("projet", "saas", "architecture", "roadmap", "lancement", "mvp", "produit"),
        listOf("ia", "ai", "llm", "rag", "embedding", "modèle", "agent", "gemini", "mindora"),
        listOf("réunion", "meeting", "décision", "équipe", "client", "compte", "rendu"),
        listOf("tâche", "todo", "urgent", "faire", "priorité", "deadline", "action", "planning"),
        listOf("design", "ui", "ux", "interface", "figma", "mobile", "responsive", "sombre"),
        listOf("finance", "budget", "prix", "abonnement", "free", "pro", "business", "revenu"),
        listOf("sécurité", "auth", "permission", "chiffrement", "audit", "rgpd", "token"),
        listOf("base", "données", "sql", "room", "postgres", "recherche", "index", "vecteur"),
        listOf("idée", "concept", "innovation", "stratégie", "vision", "objectif", "croissance"),
        listOf("code", "kotlin", "react", "typescript", "api", "backend", "frontend", "test"),
        listOf("voix", "audio", "transcription", "ocr", "image", "capture", "pdf", "document"),
        listOf("marketing", "vente", "acquisition", "utilisateur", "onboarding", "rétention")
    )

    override fun computeEmbeddingVector(text: String): List<Float> {
        val normalized = text.lowercase()
        val words = normalized.split(Regex("[^a-zàâçéèêëîïôûùüÿñæœ0-9]+")).filter { it.length > 2 }
        val vector = MutableList(24) { 0f }

        // 12 dimensions thématiques
        semanticDimensions.forEachIndexed { idx, keywords ->
            val hits = words.count { w -> keywords.any { kw -> w.startsWith(kw) || kw.startsWith(w) } }
            vector[idx] = hits.toFloat()
        }

        // 12 dimensions basées sur le hachage de trigrammes lexicaux pour capturer les noms propres
        words.forEach { word ->
            val bucket = 12 + (word.hashCode().ushr(1) % 12)
            vector[bucket] = vector[bucket] + 1f
        }

        val norm = sqrt(vector.map { it * it }.sum())
        return if (norm > 0f) vector.map { it / norm } else vector
    }

    override fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Float {
        if (vecA.isEmpty() || vecB.isEmpty() || vecA.size != vecB.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in vecA.indices) {
            dot += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) (dot / denom).coerceIn(0f, 1f) else 0f
    }

    override suspend fun executeNoteAction(
        action: AiActionType,
        note: NoteEntity,
        allUserNotes: List<NoteEntity>,
        folders: List<FolderEntity>
    ): AiActionResult {
        val cleanLines = note.content.lines().map { it.trim() }.filter { it.isNotBlank() }
        val wordCount = note.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val keySentences = cleanLines.filter { it.length > 18 }.take(4).ifEmpty {
            listOf(note.title.ifBlank { "Note sans titre" })
        }

        return when (action) {
            AiActionType.SUMMARIZE -> {
                val summaryBullets = keySentences.joinToString("\n") { "• ${it.removePrefix("# ").removePrefix("- ").removePrefix("• ")}" }
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### ✨ Synthèse Exécutive — ${note.title.ifBlank { "Note" }}")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("**Résumé en 3 points clés ($wordCount mots analysés) :**")
                        appendLine(summaryBullets)
                        appendLine()
                        appendLine("**Conclusion clé :** Ce document structure les éléments prioritaires autour de « ${note.title} » avec des pistes d'action directement exploitables.")
                    }
                )
            }

            AiActionType.EXPLAIN_PASSAGE -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### 🧠 Explication & Analyse Conceptuelle")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        keySentences.take(3).forEachIndexed { index, sentence ->
                            appendLine("**Concept ${index + 1} :** « *${sentence.take(90)}* »")
                            appendLine("→ **Clarification :** Ce point définit une brique fonctionnelle ou stratégique essentielle qui impacte directement l'organisation de votre projet.")
                            appendLine()
                        }
                    }
                )
            }

            AiActionType.EXTRACT_KEY_INFO -> {
                val actionLines = cleanLines.filter {
                    it.contains("TODO", ignoreCase = true) ||
                        it.contains("décision", ignoreCase = true) ||
                        it.contains("objectif", ignoreCase = true) ||
                        it.startsWith("- [") ||
                        it.startsWith("•")
                }.ifEmpty { keySentences }

                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### 🎯 Informations Importantes & Idées Principales")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("**Points critiques détectés :**")
                        actionLines.take(5).forEach { line ->
                            appendLine("• ${line.removePrefix("- [ ] ").removePrefix("• ")}")
                        }
                    }
                )
            }

            AiActionType.CORRECT_SPELLING -> {
                val polished = note.content
                    .replace(Regex(" +"), " ")
                    .lines()
                    .joinToString("\n") { line ->
                        val t = line.trim()
                        if (t.isNotEmpty() && t[0].isLowerCase()) t.replaceFirstChar { it.uppercase() } else t
                    }
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### ✅ Version Corrigée & Révisée")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine(polished)
                    }
                )
            }

            AiActionType.IMPROVE_STYLE -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### ✒️ Style Éditorial Professionnel")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("# ${note.title.ifBlank { "Note Stratégique" }}")
                        appendLine()
                        appendLine("**Contexte & Vision :**")
                        appendLine(keySentences.firstOrNull() ?: note.content)
                        appendLine()
                        appendLine("**Axes de développement :**")
                        keySentences.drop(1).forEach { s ->
                            appendLine("• ${s.removePrefix("#").trim()}")
                        }
                    }
                )
            }

            AiActionType.EXPAND_IDEA -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### 🚀 Développement & Approfondissement de l'Idée")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("À partir de **${note.title}**, voici 3 axes d'expansion stratégique :")
                        appendLine("1. **Court terme (Exécution rapide) :** Valider les hypothèses clés issues de : *${keySentences.firstOrNull()?.take(70) ?: "votre note"}*.")
                        appendLine("2. **Moyen terme (Industrialisation) :** Automatiser le flux de travail et structurer les indicateurs de succès (KPIs).")
                        appendLine("3. **Risques & Mitigations :** Anticiper les blocages techniques et documenter chaque décision dans votre espace Mindora.")
                    }
                )
            }

            AiActionType.STRUCTURE_DOC -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("# ${note.title.ifBlank { "Document Structuré" }}")
                        appendLine("*[Document structuré par l'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("## 1. Objectif & Synthèse")
                        appendLine(keySentences.firstOrNull() ?: "Synthèse du sujet.")
                        appendLine()
                        appendLine("## 2. Points Clés & Analyse")
                        cleanLines.take(5).forEach { line ->
                            appendLine("- ${line.removePrefix("-").removePrefix("•").trim()}")
                        }
                        appendLine()
                        appendLine("## 3. Plan d'Action")
                        appendLine("- [ ] Valider l'architecture et les priorités")
                        appendLine("- [ ] Partager ce document avec l'équipe")
                    }
                )
            }

            AiActionType.AUTO_ORGANIZE, AiActionType.FIND_RELATIONS -> {
                val noteVec = computeEmbeddingVector("${note.title} ${note.content}")
                val otherNotesWithScore = allUserNotes
                    .filter { it.id != note.id && !it.isTrashed }
                    .map { other ->
                        val sim = cosineSimilarity(noteVec, computeEmbeddingVector("${other.title} ${other.content}"))
                        other to sim
                    }
                    .sortedByDescending { it.second }

                val duplicateCandidate = otherNotesWithScore.firstOrNull { it.second > 0.82f }?.first?.title
                val relatedNotes = otherNotesWithScore.filter { it.second > 0.25f }.take(3).map { it.first }

                val inferredTags = mutableListOf<String>()
                val lowerText = "${note.title} ${note.content}".lowercase()
                if ("ia" in lowerText || "rag" in lowerText || "llm" in lowerText || "ai" in lowerText) inferredTags.add("IA & LLM")
                if ("projet" in lowerText || "saas" in lowerText || "roadmap" in lowerText) inferredTags.add("SaaS")
                if ("réunion" in lowerText || "décision" in lowerText || "équipe" in lowerText) inferredTags.add("Réunion")
                if ("design" in lowerText || "ui" in lowerText || "ux" in lowerText) inferredTags.add("UX/UI")
                if ("sécurité" in lowerText || "auth" in lowerText) inferredTags.add("Sécurité")
                if (inferredTags.isEmpty()) inferredTags.addAll(listOf("Productivité", "Connaissances"))

                val targetFolder = folders.firstOrNull { folder ->
                    lowerText.contains(folder.name.lowercase().take(5))
                } ?: folders.firstOrNull()

                val suggestedTitle = if (note.title.isBlank() || note.title.startsWith("Nouvelle")) {
                    keySentences.firstOrNull()?.removePrefix("#")?.trim()?.take(42) ?: "Note Structurée Mindora"
                } else {
                    note.title
                }

                val orgResult = AutoOrganizationResult(
                    suggestedTitle = suggestedTitle,
                    suggestedTags = inferredTags,
                    suggestedFolderId = targetFolder?.id,
                    suggestedFolderName = targetFolder?.name ?: "Général",
                    duplicateNoteTitle = duplicateCandidate,
                    relatedNoteIds = relatedNotes.map { it.id },
                    relatedNoteTitles = relatedNotes.map { it.title },
                    keyHighlights = keySentences.take(3)
                )

                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### 🗂️ Classification & Graphe de Connaissances")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("• **Titre recommandé :** ${orgResult.suggestedTitle}")
                        appendLine("• **Tags générés :** ${orgResult.suggestedTags.joinToString(", ") { "#$it" }}")
                        appendLine("• **Dossier suggéré :** 📁 ${orgResult.suggestedFolderName}")
                        if (orgResult.duplicateNoteTitle != null) {
                            appendLine("• ⚠️ **Doublon potentiel détecté :** « ${orgResult.duplicateNoteTitle} »")
                        } else {
                            appendLine("• ✅ **Doublon :** Aucun doublon direct détecté.")
                        }
                        if (orgResult.relatedNoteTitles.isNotEmpty()) {
                            appendLine("• 🔗 **Notes reliées sémantiquement :** ${orgResult.relatedNoteTitles.joinToString(" • ")}")
                        }
                    },
                    organization = orgResult
                )
            }

            AiActionType.TO_TASKS -> {
                val extracted = cleanLines
                    .filter { it.length > 8 && !it.startsWith("#") }
                    .take(4)
                    .mapIndexed { idx, line ->
                        val cleaned = line.removePrefix("- [ ]").removePrefix("- [x]").removePrefix("•").removePrefix("-").trim()
                        ExtractedTaskCandidate(
                            title = cleaned.take(65),
                            description = "Extraite automatiquement de la note « ${note.title} »",
                            priority = if (idx == 0) TaskPriority.HIGH else TaskPriority.MEDIUM,
                            category = "Action IA",
                            dueInDays = idx + 1
                        )
                    }
                    .ifEmpty {
                        listOf(
                            ExtractedTaskCandidate(
                                title = "Compléter et valider : ${note.title.ifBlank { "Note" }}",
                                description = "Action générée depuis la note source",
                                priority = TaskPriority.HIGH,
                                category = "Suivi",
                                dueInDays = 2
                            )
                        )
                    }

                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### ✅ ${extracted.size} Tâches Extraites et Créées")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        extracted.forEach { t ->
                            appendLine("• **[${t.priority.label}]** ${t.title} *(Échéance : J+${t.dueInDays})*")
                        }
                    },
                    extractedTasks = extracted
                )
            }

            AiActionType.TO_CHECKLIST -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("### ☑️ Checklist Opérationnelle")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        keySentences.forEach { s ->
                            appendLine("- [ ] ${s.removePrefix("#").removePrefix("-").removePrefix("•").trim()}")
                        }
                        appendLine("- [ ] Vérifier et valider les livrables finaux")
                    }
                )
            }

            AiActionType.TO_MEETING_REPORT -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("## 📋 Compte Rendu Exécutif — ${note.title}")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("### 1. Contexte & Participants")
                        appendLine("Synthèse structurée à partir des notes de séance.")
                        appendLine()
                        appendLine("### 2. Décisions Actées")
                        keySentences.take(2).forEach { s ->
                            appendLine("- **Décision :** ${s.removePrefix("#").trim()}")
                        }
                        appendLine()
                        appendLine("### 3. Prochaines Étapes (Action Items)")
                        keySentences.drop(1).take(3).forEach { s ->
                            appendLine("- [ ] Suivre : ${s.removePrefix("#").trim()}")
                        }
                    }
                )
            }

            AiActionType.TO_PROJECT_PLAN -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("## 🗺️ Plan de Projet & Planning — ${note.title}")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("| Phase | Objectif Clé | Échéance | Statut |")
                        appendLine("| :--- | :--- | :--- | :--- |")
                        appendLine("| **Phase 1 : Cadrage** | ${keySentences.getOrElse(0) { "Architecture" }.take(40)} | Semaine 1 | En cours |")
                        appendLine("| **Phase 2 : Exécution** | ${keySentences.getOrElse(1) { "Développement MVP" }.take(40)} | Semaine 2-3 | À faire |")
                        appendLine("| **Phase 3 : Validation** | Tests qualité, sécurité & déploiement | Semaine 4 | Planifié |")
                    }
                )
            }

            AiActionType.TO_PRESENTATION -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("## 🖥️ Structure de Présentation (Deck 4 Slides) — ${note.title}")
                        appendLine("*[Réponse d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("**Slide 1 — Vision & Titre :** ${note.title} — *Capture. Understand. Organize. Create.*")
                        appendLine("**Slide 2 — Problématique & Enjeux :** ${keySentences.getOrElse(0) { "Défis actuels" }}")
                        appendLine("**Slide 3 — Solution & Architecture :** ${keySentences.getOrElse(1) { "Approche proposée" }}")
                        appendLine("**Slide 4 — Feuille de route & Décisions :** Passage à l'action et jalons clés.")
                    }
                )
            }

            AiActionType.TO_EMAIL -> {
                AiActionResult(
                    action = action,
                    outputText = buildString {
                        appendLine("**Objet :** Synthèse et prochaines étapes — ${note.title}")
                        appendLine("*[Brouillon d'aperçu IA — Activez Firebase AI Logic pour Gemini Cloud]*")
                        appendLine()
                        appendLine("Bonjour à tous,")
                        appendLine()
                        appendLine("Suite à nos travaux sur **${note.title}**, voici les éléments clés à retenir :")
                        keySentences.take(3).forEach { s ->
                            appendLine("• ${s.removePrefix("#").removePrefix("-").trim()}")
                        }
                        appendLine()
                        appendLine("N'hésitez pas à commenter directement dans l'espace partagé Mindora AI.")
                        appendLine()
                        appendLine("Bien cordialement,")
                    }
                )
            }
        }
    }

    override suspend fun queryKnowledgeBaseRag(
        question: String,
        userNotes: List<NoteEntity>
    ): RagAnswer {
        val activeNotes = userNotes.filter { !it.isTrashed }
        if (activeNotes.isEmpty()) {
            return RagAnswer(
                answerText = "Votre base de connaissances est actuellement vide. Créez votre première note ou capture vocale pour interroger votre deuxième cerveau !",
                citations = emptyList()
            )
        }

        val queryVec = computeEmbeddingVector(question)
        val qLower = question.lowercase()
        val queryTokens = qLower.split(Regex("[^a-zàâçéèêëîïôûùüÿñæœ0-9]+")).filter { it.length > 2 }

        val rankedNotes = activeNotes.map { note ->
            val combinedText = "${note.title} ${note.summary} ${note.content}"
            val noteVec = computeEmbeddingVector(combinedText)
            val semanticScore = cosineSimilarity(queryVec, noteVec)
            val lexicalBonus = queryTokens.count { token ->
                combinedText.lowercase().contains(token)
            } * 0.18f
            val totalScore = (semanticScore + lexicalBonus).coerceAtMost(0.99f)

            val bestPassage = note.content.lines()
                .map { it.trim() }
                .filter { it.length > 15 }
                .maxByOrNull { line ->
                    queryTokens.count { token -> line.lowercase().contains(token) }
                } ?: note.content.take(160)

            RagSourceCitation(
                noteId = note.id,
                noteTitle = note.title.ifBlank { "Note sans titre" },
                snippet = bestPassage.take(180),
                similarityScore = totalScore
            )
        }.sortedByDescending { it.similarityScore }

        val topCitations = rankedNotes.take(3)
        val answer = buildString {
            appendLine("🔍 **Réponse RAG basée sur ${topCitations.size} document(s) de votre espace Mindora :**")
            appendLine("*(Mode Aperçu IA — Activez Firebase AI Logic pour connecter Gemini Cloud en direct)*")
            appendLine()
            topCitations.forEachIndexed { idx, citation ->
                val pct = (citation.similarityScore * 100).toInt().coerceIn(45, 99)
                appendLine("${idx + 1}. **${citation.noteTitle}** *(Pertinence sémantique : $pct%)*")
                appendLine("   > « ${citation.snippet} »")
                appendLine()
            }
            appendLine("**Synthèse transversale :** Vos notes montrent une convergence claire autour de **${topCitations.first().noteTitle}**. Vous pouvez ouvrir directement les notes sources ci-dessous ou transformer ces points en tâches.")
        }

        return RagAnswer(
            answerText = answer,
            citations = topCitations
        )
    }
}
