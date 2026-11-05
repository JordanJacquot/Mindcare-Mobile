package io.github.jordanjacquot.mindcare.moteur

// Le moteur des enquêtes : le portage exact de Get-Enquete (modules\enquete.ps1 de Mindcare PC). Les mêmes réponses
// donnent toujours la même enquête, sur le PC comme sur le téléphone ; le test d'équivalence le vérifie sur des milliers
// d'enquêtes. Si la logique change sur PC, la changer ici de la même façon.
//
// Un parcours, ce sont les réponses données dans l'ordre : le numéro de la réponse (0, 1...), ou -1 pour « Je ne sais
// pas ». Le téléphone n'est jamais le PC en panne : les vérifications automatiques (ce que Windows a noté) sont sautées,
// comme sur PC quand Mindcare est ouvert sur un autre appareil.

data class Ecarte(val id: String, val raison: String)
data class Fait(val etape: Objet, val reponse: String, val court: String, val texte: String)
data class Verdict(val type: String, val suspect: String?, val detail: String, val restants: List<String>)

data class Enquete(
    /** L'étape où on en est (null une fois l'enquête finie). */
    val etape: Objet?,
    val choix: List<Int>,
    val restants: List<String>,
    val ecartes: List<Ecarte>,
    val notes: List<String>,
    val preuves: List<String>,
    val faits: List<Fait>,
    val verdict: Verdict?,
    /** Les étapes qui peuvent encore venir après celle-ci (au plus) : pour « Étape 2 sur 5 ». */
    val aVenir: Int,
)

fun Catalogue.enquete(panne: Objet, choix: List<Int> = emptyList()): Enquete {
    val etapes = etapes(panne)
    val restants = ArrayList(panne.liste("Suspects"))
    val ecartes = ArrayList<Ecarte>()
    val notes = ArrayList<String>()
    val preuves = ArrayList<String>()
    var confirme: String? = null
    var detail = ""
    val faits = ArrayList<Fait>()
    val faites = HashSet<String>()
    var n = 0
    var courante: Objet? = null

    // Une réponse : ses mots retenus, les suspects qu'elle écarte, garde ou confirme, et sa preuve
    // (comme sur PC, les comparaisons de mots retenus, de preuves et d'étapes ignorent les majuscules)
    fun dans(t: String, l: Collection<String>) = l.any { it.equals(t, ignoreCase = true) }

    fun applique(effet: Objet, preuve: String) {
        var agit = false
        effet.texte("Note").takeIf { it.isNotEmpty() }?.let { notes += it }
        for (id in effet.liste("Ecarte")) if (restants.remove(id)) { ecartes += Ecarte(id, preuve); agit = true }
        val garde = effet.liste("Garde")
        if (garde.isNotEmpty()) {
            for (id in ArrayList(restants)) if (!dans(id, garde)) { restants.remove(id); ecartes += Ecarte(id, preuve); agit = true }
        }
        val c = effet.texte("Confirme")
        if (c.isNotEmpty() && c in restants) { confirme = c; detail = effet.texte("Detail"); agit = true }
        if (agit && preuve.isNotEmpty() && !dans(preuve, preuves)) preuves += preuve
    }

    fun possible(x: Objet): Boolean {
        val pour = x.liste("Pour")
        if (pour.isNotEmpty() && pour.none { it in restants }) return false
        val si = x.liste("SiNote")
        if (si.isNotEmpty() && si.none { dans(it, notes) }) return false
        val sauf = x.liste("SaufNote")
        if (sauf.isNotEmpty() && sauf.any { dans(it, notes) }) return false
        return true
    }

    while (confirme == null) {
        // Les suspects qui ne concernent pas ce PC (un portable n'a pas de bloc d'alimentation...)
        for (id in ArrayList(restants)) {
            if (suspect(id)?.liste("Sauf")?.any { dans(it, notes) } == true) { restants.remove(id); ecartes += Ecarte(id, "Ne concerne pas ce type de PC") }
        }
        val et = etapes.firstOrNull { it.texte("Id").lowercase() !in faites && possible(it) } ?: break
        faites += et.texte("Id").lowercase()
        if (et.texte("Type").equals("auto", ignoreCase = true)) continue   // (ce que Windows a noté : seulement sur le PC en panne)
        if (n >= choix.size) { courante = et; break }
        val c = choix[n]; n++
        val reponses = et.objets("Reponses")
        val r = if (c >= 0 && c < reponses.size) reponses[c] else null
        faits += Fait(et, r?.texte("Texte") ?: "Je ne sais pas", et.texte("Court"), et.texte("Texte"))
        if (r != null) applique(r, when {
            r.vrai("SansPreuve") -> ""
            r.texte("Preuve").isNotEmpty() -> r.texte("Preuve")
            else -> "${et.texte("Court")} : ${r.texte("Texte")}"
        })
    }

    val aVenir = if (courante == null) 0 else etapes.count { it.texte("Id").lowercase() !in faites && !it.texte("Type").equals("auto", ignoreCase = true) && possible(it) }
    val verdict = when {
        courante != null -> null
        confirme != null -> Verdict("trouve", confirme, detail, listOf(confirme!!))
        restants.size == 1 -> Verdict("elimination", restants[0], "", ArrayList(restants))
        restants.isEmpty() -> Verdict("rien", null, "", emptyList())
        else -> Verdict("reparateur", null, "", ArrayList(restants))
    }
    return Enquete(courante, choix.take(n), ArrayList(restants), ecartes, notes, preuves, faits, verdict, aVenir)
}

/** Un nom en minuscule au début (« la mémoire (RAM) »), sans toucher aux noms propres. */
fun minuscule(texte: String): String {
    if (texte.isEmpty()) return ""
    val fin = texte.offsetByCodePoints(0, 1)   // (la première lettre entière, même hors du plan de base, comme le Rust)
    return texte.substring(0, fin).lowercase() + texte.substring(fin)
}

/** La phrase de la réponse finale (Get-VerdictTitre sur PC). */
fun Catalogue.titreVerdict(v: Verdict?): String {
    if (v == null) return ""
    return when (v.type) {
        "trouve" -> { val t = suspect(v.suspect)?.texte("Trouve") ?: ""; if (v.detail.isNotEmpty()) "$t : ${v.detail}" else t }
        "elimination" -> suspect(v.suspect)?.texte("Trouve") ?: ""
        "reparateur" -> "Il reste ${v.restants.joinToString(" ou ") { minuscule(suspect(it)?.texte("Titre") ?: "") }} : un réparateur départagera"
        else -> "Aucune piste ne correspond : montre ce rapport à un réparateur"
    }
}

/** Un texte avec une majuscule au début (« la batterie » : « La batterie »). */
fun majuscule(texte: String): String {
    if (texte.isEmpty()) return ""
    val fin = texte.offsetByCodePoints(0, 1)
    return texte.substring(0, fin).uppercase() + texte.substring(fin)
}

/** Ce texte simple commence-t-il par ces mots (des mots entiers) ? */
private fun commencePar(t: String, debut: String): Boolean =
    debut.isNotEmpty() && (t == debut || (t.startsWith(debut) && t.substring(debut.length).startsWith(" ")))

private val Articles = setOf("le", "la", "l", "les", "un", "une", "des")

/**
 * La phrase de la réponse quand une réponse a confirmé le suspect avec un détail (son « Trouve » et le « Detail ») :
 * le portage exact de `titre_avec_detail` du PC. titreVerdict colle les deux (« Trouve : Detail ») et répétait parfois
 * ce que le détail précise : « La batterie (ou le chargeur) lâche : la batterie » devient « La batterie lâche ».
 */
fun titreAvecDetail(trouve: String, detail: String): String {
    val d = simple(detail)
    if (d.isEmpty()) return trouve
    val i = trouve.indexOf(" (ou ")
    if (i >= 0) {
        val j = trouve.indexOf(')', i)
        if (j >= 0) {
            val a = trouve.substring(0, i); val b = trouve.substring(i + " (ou ".length, j); val reste = trouve.substring(j + 1)
            if (d == simple(a) || commencePar(d, simple(b))) return majuscule(detail) + reste
        }
    }
    val deux = trouve.indexOf(" : ")
    if (deux >= 0) {
        val intro = trouve.substring(0, deux); val liste = trouve.substring(deux + 3)
        if (liste.split(", ").flatMap { it.split(" ou ") }.any { simple(it) == d }) return "$intro : $detail"
    }
    val mt = simple(trouve).split(' '); val md = d.split(' ')
    if (mt.size >= 2 && md.size >= 3 && mt[0] in Articles && md[0] in Articles && mt[1] == md[1]) return majuscule(detail)
    return if (trouve.contains(" : ")) "$trouve ($detail)" else "$trouve : $detail"
}

/** La phrase de la réponse à montrer (l'écran, l'historique, le rapport) : titre_affiche du PC. */
fun Catalogue.titreAffiche(v: Verdict?): String =
    if (v != null && v.type == "trouve" && v.detail.isNotEmpty()) titreAvecDetail(suspect(v.suspect)?.texte("Trouve") ?: "", v.detail)
    else titreVerdict(v)
