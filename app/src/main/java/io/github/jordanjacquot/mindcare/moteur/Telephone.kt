package io.github.jordanjacquot.mindcare.moteur

// Ce qui change sur le téléphone, par-dessus le moteur commun : le téléphone n'est jamais le PC en panne.

/** La question « Mindcare est-il ouvert sur le PC qui a le problème ? » : ses réponses retiennent le mot « cepc ». */
private fun Objet.demandeSiCePc(): Boolean = objets("Reponses").any { it.texte("Note") == "cepc" }

/** Une enquête sur le téléphone, et les réponses que le téléphone a données tout seul (pas montrées comme des étapes). */
data class EnqueteTelephone(val e: Enquete, val choix: List<Int>, val auto: Int)

/**
 * L'enquête du téléphone : comme sur PC, sauf que la question « sur ce PC ? » se répond toute seule (« Non, sur un autre
 * appareil ») : le téléphone ne voit pas les journaux de Windows.
 */
fun Catalogue.enqueteTelephone(panne: Objet, choix: List<Int>): EnqueteTelephone {
    var c = choix
    var e = enquete(panne, c)
    var auto = e.faits.count { it.etape.demandeSiCePc() }
    while (e.etape?.demandeSiCePc() == true) {
        val non = e.etape!!.objets("Reponses").indexOfFirst { it.texte("Note") != "cepc" }
        c = e.choix + non
        e = enquete(panne, c)
        auto++
    }
    return EnqueteTelephone(e, e.choix, auto)
}

/** La consigne de sécurité seulement pour les gestes sur le matériel (comme sur PC), pas pour une manipulation dans Windows. */
private val GestesMateriel = Regex("débranch|câble|barrette|boîtier|capot|ventilat|multiprise|ouvr(e|ir) le PC|tournevis|batterie", RegexOption.IGNORE_CASE)
fun Objet.surLeMateriel(): Boolean = GestesMateriel.containsMatchIn(liste("Gestes").joinToString(" "))
