# Mindcare Mobile : cadrage (2026-10-19)

**Pour qui** : les amis et la famille de Jordan, pas à l'aise avec l'informatique, dont le PC est en panne. Le
téléphone sert surtout quand le PC ne démarre plus, ou pour suivre les gestes à côté du PC.
**Le problème** : trouver pourquoi son PC est en panne, et le dépanner soi-même, depuis son téléphone.

## Première version
1. Le même catalogue que Mindcare PC (102 pannes, 13 familles) : l'accueil en questions guidées (« Ça concerne
   quoi ? », puis « Qu'est-ce qui se passe ? ») et la recherche par mots, hors ligne.
2. La même enquête par élimination : une étape à la fois, « Étape 2 sur 5 », les gestes à cocher, les suspects
   barrés ; les commandes affichées en grand pour les taper sur le PC, avec un bouton pour se les envoyer (message,
   e-mail).
3. La même réponse : trouvé, tout le reste écarté, ou pour le réparateur ; ce qu'il faut faire, la sécurité.
4. Les 10 dernières enquêtes gardées, la reprise d'une enquête pas finie, le rapport pour le réparateur partagé par le
   téléphone (message, e-mail).
5. Les mises à jour : le catalogue se met à jour tout seul (quand une panne est ajoutée sur le PC, elle arrive sur le
   téléphone), et l'appli elle-même par un bandeau « Mettre à jour ».

## Ce qu'elle ne fera pas
- Pas d'IA : le même arbre que le PC, toujours les mêmes réponses pour les mêmes choix.
- Pas de lecture du PC : le téléphone ne voit pas les journaux de Windows ; les questions que le PC lit tout seul sont
  posées à la personne.
- Pas de diagnostic du téléphone lui-même.
- Pas de vente ni de pub, pas de compte, rien d'envoyé nulle part.
- Pas de Play Store dans la première version (APK sur GitHub, comme Evermind Mobile).

## Plus tard
- Le Play Store.
- Les pannes du téléphone lui-même, si Jordan le veut.
- Envoyer une enquête du téléphone vers le PC (et l'inverse).

## Le catalogue partagé (décidé avec Jordan)
- **Une seule source** : `Mindcare\fichiers\enquetes.json` et `Mindcare\fichiers\images\` dans le dépôt Mindcare.
  On n'écrit jamais une panne dans le dépôt du téléphone.
- **Le PC** le reçoit par Mindset, comme aujourd'hui.
- **Le téléphone** en a une copie dans l'appli (recopiée par un script à la construction), et va chercher la dernière
  version publiée sur GitHub quand il a internet (vérifiée avant d'être utilisée ; sinon il garde la sienne).
- **Un test** vérifie que le PC et le téléphone donnent exactement les mêmes enquêtes, les mêmes réponses et les mêmes
  résultats de recherche sur le même catalogue.
- **Le format du catalogue a un numéro de version** : un téléphone trop ancien pour un nouveau format garde son
  catalogue et propose de mettre l'appli à jour.

**PC / téléphone** : Android seulement (Android 8 minimum), Kotlin et Jetpack Compose, sur le modèle d'Evermind Mobile.
**Avec les autres applis** : appli séparée, hors de Mindset (pas de portail sur téléphone, décidé le 2026-10-19).
Mindcare PC reste la référence du catalogue.
**Droits** : aucun droit particulier sur le téléphone ; Internet seulement pour les mises à jour.
**Internet** : le catalogue et l'appli, depuis GitHub. Rien d'autre, rien d'envoyé.
**Données gardées** : les 10 dernières enquêtes, le thème choisi, le dernier catalogue téléchargé. Sur le téléphone.
**Thèmes** : les 5 de la suite (Néon, Crépuscule, Rubis, Arcade, Terminal). Pas de thèmes perso (ils ne vont jamais sur
GitHub).
**Signature** : une clé dédiée à Mindcare Mobile, sauvegardée chiffrée (Jordan la range dans son Google Drive), pas la
clé de test du PC.
**Nom** : Mindcare Mobile (dépôt JordanJacquot/Mindcare-Mobile)   **Couleur** : #FF8A00 (orange, comme Mindcare)
