# Mindcare Mobile : à savoir avant de toucher au code

## Le catalogue n'est PAS ici
Les pannes, suspects, tests et images viennent de **Mindcare PC** : `D:\Projets\Mindcare\Mindcare\fichiers\enquetes.json`
et `D:\Projets\Mindcare\Mindcare\fichiers\images\` (dépôt JordanJacquot/Mindcare). C'est la seule source.

- **Ne jamais écrire ni corriger une panne dans ce dépôt.** La copie de l'appli est recopiée par le script de
  construction ; une correction faite ici serait écrasée et manquerait au PC.
- Ajouter une panne = la faire dans le dépôt Mindcare (voir son `CLAUDE.md`), puis la publier : le téléphone télécharge
  le catalogue tout seul.
- Le moteur d'enquête et la recherche d'ici doivent donner **exactement** les mêmes résultats que ceux du PC
  (`modules\enquete.ps1`) : le test d'équivalence le vérifie. Si le PC change sa logique, changer la même ici.
- Un nouveau champ dans `enquetes.json` doit être appris ici aussi ; un format trop récent (`Version`) n'est pas lu :
  l'appli garde son catalogue et propose de se mettre à jour.

## Le reste
- Le cadrage : `CADRAGE.md` ; le plan : `README.md`.
- Le modèle technique : Evermind Mobile (`D:\Projets\Evermind-Mobile`), et la fiche `telephone.md` du skill
  « nouvelle-appli ».
- Le suivi de la suite : `C:\Users\death\.claude\projects\C--Users-death\memory\portail-suite.md`.
- Les règles de la suite : `C:\Users\death\.claude\CLAUDE.md` (français simple, aucun tiret long...).
