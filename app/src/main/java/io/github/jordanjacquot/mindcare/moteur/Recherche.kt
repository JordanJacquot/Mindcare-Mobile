package io.github.jordanjacquot.mindcare.moteur

import java.text.Normalizer

// La recherche par mots, sans internet : le portage exact de Find-Pannes (modules\enquete.ps1 de Mindcare PC).

private val Vides = setOf(
    "il", "le", "la", "les", "un", "une", "de", "du", "des", "ne", "pas", "plus", "mon", "ma", "mes", "pc", "ordi", "ordinateur",
    "et", "ou", "est", "en", "sur", "qui", "que", "se", "ca", "tout", "au", "aux", "je", "j", "me", "quand", "avec",
)
private val Accents = Regex("\\p{Mn}")
private val PasSimple = Regex("[^a-z0-9 ]")
private val Espaces = Regex("\\s+")

/** Sans accents, en minuscules, seulement des lettres, des chiffres et des espaces simples (ConvertTo-Simple). */
fun simple(texte: String): String =
    Normalizer.normalize(texte, Normalizer.Form.NFD).replace(Accents, "").lowercase()
        .replace(PasSimple, " ").replace(Espaces, " ").trim()

/** Les pannes qui correspondent aux mots tapés, les plus proches d'abord (à égalité, l'ordre du catalogue). */
fun Catalogue.chercher(requete: String): List<Objet> {
    val phrase = simple(requete)
    val mots = phrase.split(Espaces).filter { it.length >= 2 && it !in Vides }
    if (mots.isEmpty()) return emptyList()
    val res = pannes.mapIndexedNotNull { ordre, p ->
        val fam = famille(p.texte("Famille"))
        val titre = simple(p.texte("Titre"))
        val tout = simple("${p.texte("Titre")} ${p.texte("Phrase")} ${p.liste("Mots").joinToString(" ")} ${fam?.texte("Titre") ?: ""}")
        var score = 0
        for (m in mots) {
            val racine = if (m.length > 5) m.substring(0, m.length - 1) else m   // (« redemarre » trouve « redemarrage »)
            val motif = Regex("\\b" + Regex.escape(racine))
            if (motif.containsMatchIn(titre)) score += 3 else if (motif.containsMatchIn(tout)) score += 2
        }
        // La phrase tapée telle quelle (« pas de son ») compte plus que des mots épars
        if (phrase.length >= 4) { if (titre.contains(phrase)) score += 6 else if (tout.contains(phrase)) score += 4 }
        if (score > 0) Triple(p, score, ordre) else null
    }
    return res.sortedWith(compareByDescending<Triple<Objet, Int, Int>> { it.second }.thenBy { it.third }).map { it.first }
}
