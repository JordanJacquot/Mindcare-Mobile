package io.github.jordanjacquot.mindcare.donnees

import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc
import io.github.jordanjacquot.mindcare.moteur.concerne
import io.github.jordanjacquot.mindcare.moteur.marqueConnue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// « Ton PC » : le genre de PC et sa marque, choisis par la personne à la première ouverture (le téléphone ne voit pas
// le PC). Gardé sur le téléphone avec les autres réglages, et modifiable en touchant la pastille « Ton PC » en haut.
// Il trie les pannes partout (familles, une famille, recherche) et répond tout seul à « portable ou fixe ? ».

/** Les 9 marques les plus courantes, en gros boutons (« Une autre marque » ouvre la liste complète, `Marques`). */
val MarquesCourantes = listOf("HP", "Lenovo", "Dell", "ASUS", "Acer", "MSI", "Apple", "Samsung", "Huawei")

@Serializable
private data class ProfilGarde(val Type: String = "", val Marque: String = "")

private val json = Json { ignoreUnknownKeys = true }

/** Le profil en texte, pour le garder (« {"Type":"portable","Marque":"HP"} » ; vide : je ne sais pas). */
fun ProfilPc.versTexte(): String = json.encodeToString(ProfilGarde.serializer(), ProfilGarde(type?.nom ?: "", marque ?: ""))

/** Le profil relu ; null : rien de gardé, ou illisible (« Ton PC » sera demandé). Une marque inconnue est oubliée. */
fun profilDepuisTexte(texte: String?): ProfilPc? {
    if (texte.isNullOrBlank()) return null
    val g = runCatching { json.decodeFromString(ProfilGarde.serializer(), texte) }.getOrNull() ?: return null
    return ProfilPc(TypePc.depuis(g.Type), marqueConnue(g.Marque))
}

/**
 * Le PC choisi, gardé grâce à `lire` et `ecrire` (les réglages du téléphone dans l'appli, une simple variable dans
 * les tests). `profil` vaut null tant que rien n'a été choisi : c'est la première ouverture.
 */
class MemoirePc(private val lire: () -> String?, private val ecrire: (String) -> Unit) {
    private val _profil = MutableStateFlow(profilDepuisTexte(lire()))
    val profil: StateFlow<ProfilPc?> = _profil

    fun choisir(p: ProfilPc) {
        _profil.value = p
        ecrire(p.versTexte())
    }
}

/**
 * La bande « Adapté à ton PC » (la phrase de Mindcare PC) : « Adapté à ton portable HP : 4 pannes de PC fixe ou
 * d'autres marques sont cachées. » Null : rien de caché.
 */
fun phraseCachees(profil: ProfilPc, cachees: List<Objet>): String? {
    if (cachees.isEmpty()) return null
    val libelle = profil.libelleCourt.let { if (it.startsWith("PC")) it else it.replaceFirstChar { c -> c.lowercase() } }
    val genreSeul = ProfilPc(profil.type, null)
    val parGenre = cachees.count { !it.concerne(genreSeul) }
    val autreGenre = when (profil.type) {
        TypePc.Fixe -> "de PC portable"
        TypePc.Portable -> "de PC fixe"
        null -> "d'un autre genre de PC"
    }
    val quoi = when (parGenre) {
        cachees.size -> autreGenre
        0 -> "d'autres marques"
        else -> "$autreGenre ou d'autres marques"
    }
    val n = cachees.size
    return if (n == 1) "Adapté à ton $libelle : 1 panne $quoi est cachée." else "Adapté à ton $libelle : $n pannes $quoi sont cachées."
}

/** Après « Tout afficher » (pour cette fois seulement). */
const val ToutMontre = "Toutes les pannes sont affichées, même celles qui ne concernent pas ton PC."
