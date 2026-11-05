package io.github.jordanjacquot.mindcare.moteur

// Ce qui change sur le téléphone, par-dessus le moteur commun (qui reste le même que sur PC, voir EquivalenceTest) :
// le téléphone n'est jamais le PC en panne, et il connaît le PC choisi par la personne (« Ton PC »).

/** La question « Mindcare est-il ouvert sur le PC qui a le problème ? » : ses réponses retiennent le mot « cepc ». */
fun Objet.demandeSiCePc(): Boolean = objets("Reponses").any { it.texte("Note") == "cepc" }

/**
 * La question « Ton PC, c'est un ordinateur portable ou un PC fixe ? » (le test « type » du catalogue) : une réponse
 * retient « portable », une autre « fixe ».
 */
fun Objet.demandeLeTypeDePc(): Boolean {
    val types = objets("Reponses").map { TypePc.depuis(it.texte("Note")) }
    return TypePc.Portable in types && TypePc.Fixe in types
}

/**
 * Une enquête sur le téléphone, et les réponses que le téléphone a données tout seul (pas montrées comme des étapes).
 * `autos` : les numéros de ces réponses dans `choix` (et dans `e.faits`).
 */
data class EnqueteTelephone(val e: Enquete, val choix: List<Int>, val autos: Set<Int>, val aVenir: Int) {
    /** Combien de réponses le téléphone a données tout seul. */
    val auto: Int get() = autos.size
    /** Les réponses données par la personne (numéro dans `choix`, et ce qu'elle a répondu). */
    val faitsMontres: List<IndexedValue<Fait>> get() = e.faits.withIndex().filter { it.index !in autos }
    /** Le numéro de l'étape en cours, pour « Étape 3 sur 6 » (les réponses données toutes seules ne comptent pas). */
    val numero: Int get() = faitsMontres.size + 1
    /** Combien d'étapes au plus, celle-ci comprise. */
    val total: Int get() = numero + aVenir
}

/**
 * L'enquête du téléphone : comme sur PC, sauf deux questions qui se répondent toutes seules :
 * - « sur ce PC ? » : « Non, sur un autre appareil » (le téléphone ne voit pas les journaux de Windows) ;
 * - « portable ou fixe ? » : avec le PC choisi dans « Ton PC », quand il dit portable ou fixe (« Je ne sais pas » :
 *   la question est posée normalement).
 * Le moteur n'en sait rien : on lui donne simplement ces réponses d'avance, comme si la personne les avait données.
 */
fun Catalogue.enqueteTelephone(panne: Objet, choix: List<Int>, profil: ProfilPc = ProfilPc.Inconnu): EnqueteTelephone {
    fun reponseDuType(et: Objet): Int? = profil.type?.let { t ->
        if (!et.demandeLeTypeDePc()) null
        else et.objets("Reponses").indexOfFirst { TypePc.depuis(it.texte("Note")) == t }.takeIf { it >= 0 }
    }
    fun reponseToute(et: Objet): Int? = when {
        et.demandeSiCePc() -> et.objets("Reponses").indexOfFirst { it.texte("Note") != "cepc" }
        else -> reponseDuType(et)
    }
    var e = enquete(panne, choix)
    while (true) {
        val et = e.etape ?: break
        val r = reponseToute(et) ?: break
        e = enquete(panne, e.choix + r)
    }
    // (une réponse au type compte comme donnée toute seule si c'est celle du PC choisi)
    val autos = e.faits.indices.filter { i ->
        val et = e.faits[i].etape
        et.demandeSiCePc() || reponseDuType(et) == e.choix[i]
    }.toSet()
    // Les étapes à venir, sans la question du type, qui se répondra toute seule
    val faites = e.faits.map { it.etape.texte("Id").lowercase() }.toSet() + (e.etape?.texte("Id")?.lowercase() ?: "")
    val typeAVenir = if (e.etape == null || profil.type == null) 0
        else etapes(panne).count { it.demandeLeTypeDePc() && it.texte("Id").lowercase() !in faites }
    return EnqueteTelephone(e, e.choix, autos, (e.aVenir - typeAVenir).coerceAtLeast(0))
}

/**
 * Les réponses à garder pour revenir à l'étape précédente : tout ce qui précède la dernière réponse donnée par la
 * personne (les réponses données toutes seules se refont toutes seules). Null : aucune, on sort de l'enquête.
 */
fun EnqueteTelephone.reponsesAvantLaDerniere(): List<Int>? {
    val derniere = faitsMontres.lastOrNull()?.index ?: return null
    return choix.take(derniere)
}

// Les pistes « hors sujet » : un suspect que le moteur écarte parce qu'il ne concerne pas ce genre de PC (son champ
// « Sauf » contient une note de l'enquête : le bloc d'alimentation sur un portable). Le moteur continue de les
// calculer, mais elles ne s'affichent jamais : ni dans les pistes, ni dans les décomptes, ni dans la réponse, ni dans
// le rapport.

/** La raison que le moteur donne à un suspect hors sujet (Enquete.kt). */
const val RaisonHorsSujet = "Ne concerne pas ce type de PC"

fun Ecarte.horsSujet(): Boolean = raison == RaisonHorsSujet

/** Un suspect urgent (un danger) ne s'affiche jamais comme une piste. */
fun Catalogue.urgent(id: String): Boolean = suspect(id)?.vrai("Urgent") == true

/** Les pistes encore possibles, à montrer. */
fun Catalogue.pistesPossibles(e: Enquete): List<String> = e.restants.filter { !urgent(it) }

/**
 * Ce suspect n'a-t-il rien à voir avec ce PC ? Son « Sauf » contient un des mots retenus par l'enquête (« portable »,
 * « fixe »...) et il ne reste pas. La même règle que Enquete::hors_sujet sur PC : un suspect écarté par une vraie
 * preuve, puis devenu hors sujet, ne concerne pas ce PC non plus.
 */
fun Catalogue.horsSujet(e: Enquete, id: String): Boolean =
    e.restants.none { it.equals(id, ignoreCase = true) } &&
        (suspect(id)?.liste("Sauf") ?: emptyList()).any { s -> e.notes.any { it.equals(s, ignoreCase = true) } }

/** Les pistes écartées, à montrer : ni hors sujet ni urgentes, une seule fois chacune. */
fun Catalogue.pistesEcartees(e: Enquete): List<Ecarte> =
    e.ecartes.filter { !it.horsSujet() && !horsSujet(e, it.id) && !urgent(it.id) }.distinctBy { it.id.lowercase() }

/** La phrase de la réponse, et ce qu'il faut dire d'une pièce à changer (titre_reponse sur PC). */
data class TitreReponse(val titre: String, val piece: Boolean, val supposee: Boolean)

/**
 * La phrase de la réponse finale, sauf pour une pièce seulement restée par élimination : elle n'a pas été testée,
 * « À vérifier : ... » (on ne fait pas acheter sur une supposition). Comme `titre_reponse` de Mindcare PC.
 */
fun Catalogue.titreReponse(v: Verdict?): TitreReponse {
    val s = suspect(v?.suspect)
    val piece = s?.vrai("Piece") == true
    val supposee = piece && v?.type == "elimination"
    val titre = if (supposee) "À vérifier : ${minuscule(s.texte("Titre"))}" else titreVerdict(v)
    return TitreReponse(titre, piece, supposee)
}

/** La consigne de sécurité seulement pour les gestes sur le matériel (comme sur PC), pas pour une manipulation dans Windows. */
private val GestesMateriel = Regex("débranch|câble|barrette|boîtier|capot|ventilat|multiprise|ouvr(e|ir) le PC|tournevis|batterie", RegexOption.IGNORE_CASE)
fun Objet.surLeMateriel(): Boolean = GestesMateriel.containsMatchIn(liste("Gestes").joinToString(" "))

// Les gestes du catalogue sont écrits pour Mindcare PC, où la commande a un bouton « Copier » à côté. Sur le
// téléphone, ce bouton n'aurait pas de sens (le téléphone ne colle rien dans le PC) : la commande s'affiche en grand
// au-dessus des gestes, à recopier au clavier du PC (ou à s'envoyer pour la coller). Seul l'affichage change : le
// catalogue, qui vient du PC, n'est jamais modifié.
private val GesteCopier = Regex("Clique sur « Copier »[^.]*\\.")
const val GesteTaperCommande = "Tape la commande du dessus dans la fenêtre noire, puis appuie sur Entrée."

/** Un geste du catalogue, tel qu'on le lit sur le téléphone. */
fun gestePourTelephone(geste: String): String = geste.replace(GesteCopier, GesteTaperCommande)
