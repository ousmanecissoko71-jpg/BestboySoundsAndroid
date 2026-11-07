# Bestboy Sounds — projet Android natif

Application Android Kotlin + Jetpack Compose, avec lecteur Media3, lecture en arrière-plan via MediaSessionService, bibliothèque audio locale, mini-lecteur, écran de lecture, favoris et création de playlists (noms).

## Ouvrir le projet
1. Décompresser le ZIP.
2. Ouvrir Android Studio puis **Open** et choisir le dossier `android` (celui qui contient `settings.gradle.kts`).
3. Laisser Gradle télécharger les dépendances, avec Internet activé.
4. Connecter un téléphone Android (Android 8.0 ou plus) et accepter les permissions Audio et Notifications.
5. Lancer la configuration `app` avec le bouton Run.

## AdMob
Les identifiants d'application et de formats publicitaires sont ceux de test officiels Google. Ils ne génèrent aucun revenu. Remplacer les identifiants de test par les identifiants du compte AdMob propriétaire avant toute diffusion publique. Ne jamais cliquer sur ses propres annonces réelles.

## Notes de version
- Package : `com.bestboy.sounds`
- Min Android : API 26 / Android 8.0
- Cible : API 35
- Kotlin + Jetpack Compose + Material 3
- Media3 ExoPlayer et MediaSession
- Les pistes sont lues depuis la bibliothèque audio du téléphone (MediaStore).

La compilation APK doit être effectuée dans Android Studio avec le SDK Android installé. Le ZIP source ne constitue pas encore un APK installable.
