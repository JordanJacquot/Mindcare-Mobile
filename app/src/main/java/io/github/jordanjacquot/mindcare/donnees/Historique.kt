package io.github.jordanjacquot.mindcare.donnees

import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Enquete
import io.github.jordanjacquot.mindcare.moteur.EnqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.demandeSiCePc
import io.github.jordanjacquot.mindcare.moteur.pistesEcartees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

// Les enquêtes gardées : les 10 dernières, finies ou non, comme sur PC (diagnostics.json, même format), pour en reprendre
// une, revoir sa réponse, ou en faire un rapport pour le réparateur. Elles restent sur le téléphone.

/** Une enquête gardée : sa panne, ses réponses (voir moteur/Enquete.kt), ses dates, et sa réponse une fois finie. */
@Serializable
data class Diagnostic(
    val Id: String,
    val Panne: String,
    val Choix: List<Int>,
    val Debut: String,
    val Maj: String,
    val Fini: Boolean = false,
    val Suspect: String = "",
    val Titre: String = "",
)

@Serializable
private data class FichierHistorique(val Version: Int = 2, val Diagnostics: List<Diagnostic> = emptyList())

class Historique(private val fichier: File) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _liste = MutableStateFlow(lire())
    /** De la plus récente à la plus ancienne. */
    val liste: StateFlow<List<Diagnostic>> = _liste

    private fun lire(): List<Diagnostic> = runCatching {
        if (fichier.exists()) json.decodeFromString<FichierHistorique>(fichier.readText()).Diagnostics.filter { it.Id.isNotEmpty() && it.Panne.isNotEmpty() } else emptyList()
    }.getOrDefault(emptyList())

    fun trouver(id: String): Diagnostic? = _liste.value.firstOrNull { it.Id == id }

    /**
     * Enregistre où en est une enquête, qui passe en tête de liste. Sans aucune réponse, elle n'est pas gardée ; revoir
     * une enquête sans rien y changer ne la déplace pas (Save-Diagnostic sur PC).
     */
    fun enregistrer(id: String, panne: String, choix: List<Int>, fini: Boolean, suspect: String?, titre: String) {
        val ancien = trouver(id)
        if (ancien != null && ancien.Choix == choix && ancien.Fini == fini && ancien.Titre == titre) return
        var liste = _liste.value.filter { it.Id != id }
        if (choix.isNotEmpty()) {
            val maintenant = LocalDateTime.now().withNano(0).toString()
            liste = listOf(Diagnostic(id, panne, choix, ancien?.Debut ?: maintenant, maintenant, fini, suspect ?: "", titre)) + liste
        }
        _liste.value = liste.take(Max)
        runCatching {
            fichier.parentFile?.mkdirs()
            val tmp = File(fichier.path + ".tmp")
            tmp.writeText(json.encodeToString(FichierHistorique.serializer(), FichierHistorique(2, _liste.value)))
            if (!tmp.renameTo(fichier)) { fichier.delete(); tmp.renameTo(fichier) }
        }
    }

    /**
     * Enregistre où en est une enquête pas finie, à chaque réponse donnée ou retirée. Deux différences avec
     * [enregistrer] :
     * - une enquête déjà finie n'est pas remise « pas finie » (revoir une réponse, puis revenir) ;
     * - revenir avant la toute première réponse ne l'efface pas : elle reste telle quelle, avec sa première réponse.
     *   Sur le téléphone, le retour ramène à l'étape d'avant, une réponse à la fois : pour revenir à l'accueil, il
     *   fallait retirer toutes ses réponses, et l'enquête, devenue vide, était effacée. La carte « Reprendre
     *   l'enquête » n'apparaissait donc jamais après un retour (vu pendant les essais du 05/11/2026).
     */
    fun garderEnCours(id: String, panne: String, choix: List<Int>) {
        val ancien = trouver(id)
        if (ancien?.Fini == true) return
        if (choix.isEmpty() && ancien != null) return
        enregistrer(id, panne, choix, false, null, "")
    }

    companion object {
        const val Max = 10
        fun nouvelId(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + "%04x".format(Random.nextInt(65536))
    }
}

/**
 * L'enquête à proposer en « Reprendre l'enquête » sur l'accueil : la plus récente, si elle n'est pas finie (et que sa
 * panne existe encore), comme l'accueil de Mindcare PC (la dernière enquête gardée, à reprendre si elle n'est pas
 * finie, à revoir si elle l'est).
 */
fun List<Diagnostic>.aReprendre(catalogue: Catalogue): Diagnostic? =
    firstOrNull()?.takeIf { !it.Fini && catalogue.panne(it.Panne) != null }

private val Fr = Locale.FRANCE

/** « 19 octobre à 15h55 » (« 1er » le premier du mois). */
fun formaterDate(iso: String, modele: String = "d MMMM 'à' HH'h'mm"): String = runCatching {
    val t = LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern(modele, Fr))
    if (t.startsWith("1 ")) "1er" + t.substring(1) else t
}.getOrDefault(iso)

/**
 * Le rapport pour le réparateur (Get-ResumeTexte sur PC), en texte simple pour un SMS, WhatsApp ou un e-mail : la
 * panne, le PC choisi dans « Ton PC » (s'il est connu), la réponse, les preuves, les pistes écartées (jamais celles
 * hors sujet, qui ne concernent pas ce genre de PC), les réponses. Les réponses que le téléphone a données tout seul
 * (`autos` : « sur ce PC ? », « portable ou fixe ? » répondue par « Ton PC ») n'y sont pas : la ligne « PC choisi » le
 * dit déjà.
 */
fun Catalogue.rapport(panne: Objet, e: Enquete, titre: String, version: String, profil: ProfilPc? = null, autos: Set<Int> = emptySet()): String {
    val l = ArrayList<String>()
    l += "Mindcare : rapport de l'enquête"; l += ""
    l += "Date : ${formaterDate(LocalDateTime.now().withNano(0).toString(), "d MMMM yyyy 'à' HH'h'mm")}"
    l += "Panne : ${panne.texte("Titre")}"
    if (profil != null && !profil.inconnu) l += "PC choisi : ${profil.libelleCourt}"
    l += ""; l += "Réponse : $titre"
    if (e.preuves.isNotEmpty()) { l += ""; l += "Comment on le sait :"; e.preuves.forEach { l += "- $it" } }
    val ecartes = pistesEcartees(e, profil ?: ProfilPc.Inconnu).map { "${suspect(it.id)?.texte("Titre")}" + if (it.raison.isNotEmpty()) " (${it.raison})" else "" }
    if (ecartes.isNotEmpty()) { l += ""; l += "Écartés :"; ecartes.forEach { l += "- $it" } }
    if (e.verdict?.type == "reparateur") { l += ""; l += "Il reste : ${e.verdict.restants.joinToString(", ") { suspect(it)?.texte("Titre") ?: it }}" }
    l += ""; l += "Les réponses :"
    e.faits.filterIndexed { i, f -> i !in autos && !f.etape.demandeSiCePc() }
        .forEach { l += "- ${it.texte}"; l += "  ${it.reponse}" }
    l += ""; l += "Fait avec Mindcare Mobile $version, sur un téléphone."
    return l.joinToString("\n")
}

/** Le rapport d'une enquête du téléphone, sans les réponses qu'il a données tout seul. */
fun Catalogue.rapport(panne: Objet, et: EnqueteTelephone, titre: String, version: String, profil: ProfilPc? = null): String =
    rapport(panne, et.e, titre, version, profil, et.autos)
