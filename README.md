# 🧠 MINDORA AI — Deuxième Cerveau Numérique & Espace de Travail Intelligent

![Version](https://img.shields.io/badge/version-2.0.0-7457F5?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-API%2024%2B-4285F4?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-10B981?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)

> **Slogan :** *Capture. Understand. Organize. Create.*  
> **Auteur & Créateur :** **Mendel Temgoua** (`mendeltemgouabusiness@gmail.com`)

**Mindora AI v2.0.0** est une application intelligente de prise de notes, d'organisation des connaissances (PKM) et de gestion de tâches augmentée par l'IA, conçue pour fonctionner comme un véritable **deuxième cerveau numérique**.

---

## 🆕 Nouveautés de la Mise à Jour v2.0.0

- **📋 Galerie de Modèles Intelligents Pro (1-Clic) :** Créez instantanément des notes structurées pré-remplies avec blocs riches (*Compte-rendu de Réunion*, *PRD Produit SaaS*, *Fiche Zettelkasten*, *Objectifs OKR Hebdo*, *Architecture Technique*).
- **⏱️ Minuteur Focus Deep Work (Pomodoro) :** Minuteur interactif intégré à l'accueil avec modes **Focus 25 min** et **Pause 5 min**, barre de progression temps réel et compteur de sessions.
- **🔊 Lecture Audio Vocale IA (`TextToSpeech` Android natif) :** Écoutez n'importe quelle note lue à voix haute en français d'un seul clic dans l'éditeur.
- **📊 Barre d'Analytique d'Écriture Temps Réel :** Calcul automatique du nombre de mots, nombre de caractères, temps de lecture estimé (en minutes) et nombre de blocs dans l'éditeur.

---

## 📱 Télécharger & Installer l'APK sur votre Téléphone Android

L'APK installable est déjà pré-compilé et signé, prêt à être installé sur tout smartphone ou tablette Android (Android 7.0+ / API 24+) :

1. **Depuis l'interface AI Studio Build (1 clic) :**
   - Cliquez sur le menu **Paramètres / Export (⚙️)** en haut à droite de l'écran.
   - Sélectionnez **Download APK** (ou **Generate APK**) pour télécharger le fichier `.apk` directement sur votre téléphone ou ordinateur.
2. **Depuis la ligne de commande locale :**
   ```bash
   gradle :app:assembleDebug
   # Fichier généré : app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🚀 Publier le Projet sur votre Compte GitHub

Le dépôt est **100 % prêt pour GitHub** (avec `.gitignore`, `LICENSE` MIT, `CHANGELOG.md`, `CONTRIBUTING.md` et `.env.example`, sans fichier `.github/workflows` bloquant les permissions OAuth) :

- **Méthode directe (AI Studio) :** Cliquez sur l'icône **GitHub / Export** en haut à droite pour pousser directement le projet vers un nouveau dépôt sur votre compte GitHub.
- **Méthode Git CLI (après export ZIP) :**
  ```bash
  git init
  git add .
  git commit -m "feat: release Mindora AI v2.0.0 by Mendel Temgoua"
  git branch -M main
  git remote add origin https://github.com/VOTRE-PROFIL/mindora-ai.git
  git push -u origin main
  ```

---

## ✨ Architecture & Fonctionnalités Complètes

### 1. Identité Visuelle & Design System
- **Palette officielle :** Noir profond (`#101114`), Blanc (`#FFFFFF`), Gris (`#858995`), Violet IA (`#7457F5`), Bleu secondaire (`#4285F4`), Fond secondaire (`#F5F6FA`).
- **Typographie :** Polices **Inter** et **JetBrains Mono** packagées localement (`app/src/main/res/font/`).
- **Responsive :** Mode Clair & Mode Sombre + adaptation automatique smartphone et tablette.

### 2. Navigation (12 Modules)
1. **Accueil :** Dashboard intelligent, KPIs et briefing **« Votre journée avec l'IA »**.
2. **Toutes mes notes :** Recherche multicritère, filtres par dossiers, tags, statuts et couleurs.
3. **Favoris :** Notes étoilées prioritaires.
4. **Récemment consultées :** Historique de consultation automatique.
5. **Notes partagées :** Invitations par email, permissions Lecture/Modification et commentaires.
6. **Dossiers & Graphe :** Dossiers et sous-dossiers imbriqués + **Graphe de connaissances interactif**.
7. **Espaces de travail :** Cloisonnement par projets (SaaS, Personnel, Équipe).
8. **Tâches :** 4 vues (**Liste**, **Kanban**, **Calendrier**, **Priorités**) reliées aux notes sources.
9. **Calendrier :** Chronologie des échéances et rappels.
10. **Assistant IA (RAG) :** Chat interrogeant l'ensemble des notes avec citations des sources.
11. **Corbeille :** Restauration ou suppression définitive.
12. **Paramètres & SaaS :** Plans **FREE / PRO / BUSINESS**, quotas IA, sauvegarde complète `.md` et journal d'audit de sécurité.

---

## 🧪 Tests & Qualité

Exécuter la suite complète de tests unitaires et d'intégration Robolectric :
```bash
gradle :app:testDebugUnitTest
```

---

## 📄 Licence & Auteur

- **Auteur :** Mendel Temgoua (`mendeltemgouabusiness@gmail.com`)
- **Licence :** Distribué sous licence [MIT](LICENSE).
