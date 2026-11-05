# Mindcare Mobile

Trouve pourquoi ton PC est en panne, depuis ton téléphone Android. Version téléphone de
[Mindcare](https://github.com/JordanJacquot/Mindcare) : les mêmes pannes, les mêmes questions, les mêmes réponses.

## Installer

Télécharger le fichier `.apk` de la [dernière version](https://github.com/JordanJacquot/Mindcare-Mobile/releases) sur le
téléphone (Android 8 ou plus récent), l'ouvrir et autoriser l'installation. Ensuite, Mindcare propose lui-même ses mises
à jour.

## Le catalogue partagé

Les pannes et les images viennent de Mindcare PC (`Mindcare\fichiers\enquetes.json` et `images\`) : c'est la seule
source, elles ne s'écrivent jamais ici (voir `CLAUDE.md`). Le téléphone en a une copie dans l'appli et va chercher la
dernière version publiée quand il a internet. Un test vérifie que le PC et le téléphone donnent les mêmes enquêtes.

## Plan

Validé avec Jordan le 2026-10-19 (fiche : [CADRAGE.md](CADRAGE.md)).

- [x] 1. Le squelette : l'appli Android (logo en pouls, 5 thèmes, accueil vide, bandeau des mises à jour), la clé de
      signature dédiée et sa sauvegarde chiffrée, le dépôt GitHub.
- [x] 2. Le moteur partagé : le catalogue recopié depuis Mindcare, l'enquête et la recherche en Kotlin ; le test qui
      compare le PC et le téléphone sur les 102 pannes.
- [x] 3. Les écrans : l'accueil en questions guidées et la recherche, l'enquête pas à pas, la réponse.
- [x] 4. Les images : les dessins des questions et les icônes des familles, dans le thème.
- [x] 5. Les enquêtes gardées : les 10 dernières, la reprise, le rapport partagé.
- [x] 6. Le catalogue téléchargé : publié avec Mindcare PC, vérifié par le téléphone.
- [x] 7. Première publication : Mindcare Mobile 0.1.0-beta.

Plan 2, validé avec Jordan le 2026-11-05 : rattraper Mindcare PC 0.91.0, avec un design pensé pour le téléphone (pas
un écran de PC rétréci).

- [x] 1. Le moteur à niveau : 300 pannes, les groupes, le tri selon le PC ; la référence exportée du moteur Rust.
- [x] 2. La maquette du nouveau design sur téléphone, dans les 5 thèmes, validée par Jordan.
- [x] 3. Les écrans dans le nouveau design : accueil, une famille en groupes, « Raconte ton problème », l'enquête et
      la feuille des pistes, la réponse, « Pour le réparateur » (le rapport partagé en message texte).
- [x] 4. « Ton PC » : demandé à la première ouverture et gardé ; les pannes triées, « Tout afficher » pour une fois ;
      la question portable ou fixe répondue toute seule ; les pistes qui ne concernent pas ce type de PC jamais
      montrées ; « PC choisi » dans le rapport.
- [ ] 5. Les essais sur téléphone virtuel dans les 5 thèmes, les captures, puis Mindcare Mobile 0.2.0.

## Développer

Il faut Android Studio (son Java sert à Gradle : `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`).

- `gradlew test` : les tests sur PC, dont le test d'équivalence : 20 000 enquêtes, 3 300 recherches, les groupes de
  chaque famille et le tri des pannes selon le PC (« PourPc »), joués par le vrai moteur de Mindcare PC, en Rust
  (`app/src/test/resources/reference-pc.json`), que le téléphone doit retrouver à l'identique.
- `outils\exporter-reference.ps1` : refait cette référence avec Mindcare PC (après un changement du catalogue ou de la
  logique sur PC ; la construction recopie d'abord le catalogue). Il lance l'exemple `exporter_reference` du moteur Rust
  (`rust\mindcare-moteur\examples\` du dépôt Mindcare) ; Rust doit être installé (`D:\Outils\Rust`).
- `gradlew assembleRelease` : l'APK signé avec la clé de Mindcare Mobile (`app/build/outputs/apk/release/`).
- `bash outils/publier.sh <version> "- ce qui change"` : publie une version sur GitHub (APK et son empreinte).
- La clé de signature : `C:\Users\death\.android\mindcare-mobile.jks` et `mindcare-mobile.properties` (hors du dépôt).
  Sauvegarde chiffrée : `Cle-Android-Mindcare-Mobile.zip`, sur le Google Drive de Jordan ; le mot de passe est dans son
  gestionnaire de mots de passe Google. Sans cette clé, plus aucune mise à jour ne peut s'installer sur les téléphones.

Le code est dans `app/src/main/java/io/github/jordanjacquot/mindcare/` :

- `moteur/` : le catalogue (`Catalogue`), l'enquête (`enquete`, portage exact de `Get-Enquete`) et la recherche
  (`chercher`), les groupes d'une famille (`groupes`, `groupesPour`) et le tri selon le PC (`Pc.kt` : `ProfilPc`,
  `concerne`, `pannesPour`, `chercherPour`), portés du moteur Rust du PC ;
- `ui/theme/` : la DA de la suite (fond de nuit, verre, boutons, logo en pouls) et ses 5 thèmes, avec la même
  « traduction » des couleurs que sur PC ;
- `ui/` : les écrans ;
- `maj/` : les mises à jour de l'appli (les versions GitHub, comme Evermind Mobile).
