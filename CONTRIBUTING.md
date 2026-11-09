# Guide de Contribution — MINDORA AI

Merci de votre intérêt pour **Mindora AI**, conçu et développé par **Mendel Temgoua**.

## Architecture du Code

- `app/src/main/java/com/example/ui/theme/` : Design System Material 3 (`Color.kt`, `Type.kt`, `Theme.kt`).
- `app/src/main/java/com/example/data/local/` : Entités relationnelles Room SQLite (`MindoraEntities.kt`), DAO réactif (`MindoraDao.kt`) et base de données (`MindoraDatabase.kt`).
- `app/src/main/java/com/example/data/repository/` : Couche métier, sécurité SHA-256, export PDF/Markdown et moteur d'embeddings vectoriels RAG (`MindoraRepository.kt`, `AiRepository.kt`).
- `app/src/main/java/com/example/ui/viewmodel/` : Gestion d'état MVVM réactive (`MindoraViewModel.kt`).
- `app/src/main/java/com/example/ui/screens/` : Écrans modulaires Jetpack Compose (Dashboard, Éditeur de blocs, Graphe de connaissances, Capture vocale/OCR, Tâches Kanban, Chat RAG, Collaboration & Paramètres).

## Standards de Qualité

1. Toutes les requêtes de base de données doivent respecter l'isolation stricte par `userId`.
2. Exécutez toujours `gradle :app:testDebugUnitTest` avant de soumettre une Pull Request.
3. Vérifiez la génération de l'APK avec `gradle :app:assembleDebug`.
