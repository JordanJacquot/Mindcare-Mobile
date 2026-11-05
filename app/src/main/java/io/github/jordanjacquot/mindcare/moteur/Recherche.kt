package io.github.jordanjacquot.mindcare.moteur

import java.text.Normalizer

// La recherche par mots, sans internet : le portage exact de la recherche du moteur Rust du PC
// (rust\mindcare-moteur\src\recherche.rs). Les pannes trouvées sont celles de Find-Pannes (l'ancien moteur PowerShell) ;
// depuis le 05/11/2026, elles sont classées autrement (« il s'éteint tout seul » met « Il s'éteint d'un coup » en tête,
// et non plus 6e) : voir scoreClassement. Le test d'équivalence vérifie que le PC et le téléphone donnent le même ordre.

private val Vides = setOf(
    "il", "le", "la", "les", "un", "une", "de", "du", "des", "ne", "pas", "plus", "mon", "ma", "mes", "pc", "ordi", "ordinateur",
    "et", "ou", "est", "en", "sur", "qui", "que", "se", "ca", "tout", "au", "aux", "je", "j", "me", "quand", "avec",
)
/** Les mots qui insistent sans rien dire de la panne : ils trouvent les pannes, mais ne comptent pas pour les classer. */
private val Faibles = setOf(
    "tres", "beaucoup", "trop", "fait", "ai", "vraiment", "super", "hyper", "grave", "completement", "encore", "devenu", "temps",
)
/** Les mots qui disent que quelque chose ne marche pas. */
private val Negations = setOf("ne", "pas", "plus", "rien", "jamais", "n", "aucun", "aucune")
// (tous les signes combinants, comme le moteur Rust du PC : Mn, mais aussi les rares Mc et Me)
private val Accents = Regex("[\\p{Mn}\\p{Mc}\\p{Me}]")
private val PasSimple = Regex("[^a-z0-9 ]")
private val Espaces = Regex("\\s+")

/** Sans accents, en minuscules, seulement des lettres, des chiffres et des espaces simples (ConvertTo-Simple). */
fun simple(texte: String): String =
    Normalizer.normalize(texte, Normalizer.Form.NFD).replace(Accents, "").lowercase()
        .replace(PasSimple, " ").replace(Espaces, " ").trim()

/** Le texte (déjà simple) contient-il ce mot au début d'un mot ? */
private fun debutDeMot(t: String, racine: String): Boolean {
    var i = t.indexOf(racine)
    while (i >= 0) {
        if (i == 0 || t[i - 1] == ' ') return true
        i = t.indexOf(racine, i + 1)
    }
    return false
}

/** La racine d'un mot tapé : sans sa dernière lettre s'il a plus de 5 lettres (« redemarre » trouve « redemarrage »). */
private fun racine(m: String): String = if (m.length > 5) m.substring(0, m.length - 1) else m

private fun negatif(t: String): Boolean = t.split(' ').any { it in Negations }

/** Les mots qui comptent dans un texte simple pour le classement (ni petits mots, ni mots qui insistent). */
private fun motsForts(t: String): List<String> = t.split(' ').filter { it.length >= 2 && it !in Vides && it !in Faibles }

/** Une panne mise en texte simple pour la recherche (préparée une fois par catalogue). */
internal class FicheRecherche(
    val titre: String,
    /** Tout son texte : le nom, la phrase, les mots, le nom de la famille. */
    val tout: String,
    val phrase: String,
    val famille: String,
    /** Ses mots (le champ « Mots »), chacun en texte simple. */
    val mots: List<String>,
    val motsJoints: String,
)

internal fun Catalogue.preparerRecherche(): List<FicheRecherche> = pannes.map { p ->
    val fam = famille(p.texte("Famille"))?.texte("Titre") ?: ""
    val mots = p.liste("Mots")
    FicheRecherche(
        titre = simple(p.texte("Titre")),
        tout = simple("${p.texte("Titre")} ${p.texte("Phrase")} ${mots.joinToString(" ")} $fam"),
        phrase = simple(p.texte("Phrase")),
        famille = simple(fam),
        mots = mots.map { simple(it) },
        motsJoints = simple(mots.joinToString(" ")),
    )
}

/** Le score de Find-Pannes : il choisit les pannes trouvées (score non nul), et donnait leur ordre avant. */
private fun scoreTrouvee(f: FicheRecherche, mots: List<String>, phrase: String): Int {
    var score = 0
    for (m in mots) {
        val r = racine(m)
        if (debutDeMot(f.titre, r)) score += 3 else if (debutDeMot(f.tout, r)) score += 2
    }
    // La phrase tapée telle quelle (« pas de son ») compte plus que des mots épars
    if (phrase.length >= 4) { if (f.titre.contains(phrase)) score += 6 else if (f.tout.contains(phrase)) score += 4 }
    return score
}

/**
 * Le score qui classe les pannes trouvées, en nombres entiers, exactement comme `score_classement` du PC : chaque mot
 * pèse 2000 / (n + 5) (n : le nombre de pannes qui l'ont), 3 fois dans le nom ou les mots de la panne, 2 fois dans sa
 * phrase ou sa famille ; la phrase entière (600 ou 400) ; la part du nom couverte (jusqu'à 300) ; 200 de moins si l'un
 * dit que ça ne marche pas et l'autre non (sauf un seul mot tapé sans négation) ; 100 par mot fort d'un des mots de la
 * panne écrit en entier dans la phrase.
 */
private fun scoreClassement(f: FicheRecherche, mots: List<String>, phrase: String, nombres: List<Int>): Int {
    val racines = mots.indices.filter { mots[it] !in Faibles }.map { racine(mots[it]) to 2000 / (nombres[it] + 5) }
    var score = 0
    for ((r, poids) in racines) {
        if (debutDeMot(f.titre, r) || debutDeMot(f.motsJoints, r)) score += 3 * poids
        else if (debutDeMot(f.phrase, r) || debutDeMot(f.famille, r)) score += 2 * poids
    }
    if (phrase.length >= 4) { if (f.titre.contains(phrase)) score += 600 else if (f.tout.contains(phrase)) score += 400 }
    val duNom = motsForts(f.titre)
    if (duNom.isNotEmpty()) {
        val couverts = duNom.count { m -> racines.any { (r, _) -> m.startsWith(r) } }
        score += 300 * couverts / duNom.size
    }
    val tapeNegatif = negatif(phrase)
    if (tapeNegatif != negatif(f.titre) && (tapeNegatif || mots.size >= 2)) score -= 200
    val entouree = " $phrase "
    val ecrits = f.mots.filter { it.length >= 4 && entouree.contains(" $it ") }.maxOfOrNull { motsForts(it).size } ?: 0
    return score + 100 * ecrits
}

/** Les pannes qui correspondent aux mots tapés, les plus proches d'abord (à égalité, l'ordre du catalogue). */
fun Catalogue.chercher(requete: String): List<Objet> {
    val phrase = simple(requete)
    val mots = phrase.split(Espaces).filter { it.length >= 2 && it !in Vides }
    if (mots.isEmpty()) return emptyList()
    val fiches = fichesRecherche
    val nombres = mots.map { m -> fiches.count { debutDeMot(it.tout, racine(m)) } }
    return fiches.indices
        .filter { scoreTrouvee(fiches[it], mots, phrase) != 0 }
        .map { it to scoreClassement(fiches[it], mots, phrase, nombres) }
        .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenBy { it.first })
        .map { pannes[it.first] }
}
