package io.github.jordanjacquot.mindcare.moteur

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Les pannes qui concernent un PC : le champ « PourPc » d'une panne du catalogue, et le profil du PC (portable ou fixe,
// et sa marque) qui dit lesquelles montrer. Le portage exact de pc.rs du moteur Rust de Mindcare PC.
//
// Dans enquetes.json, une panne propre à un genre de PC porte par exemple « "PourPc": { "Type": "portable" } » ou
// « "PourPc": { "Marque": ["HP"] } ». Les deux champs peuvent aller ensemble (les deux doivent alors aller) ; « Marque »
// est une liste (une des marques suffit). Une panne sans « PourPc » concerne tous les PC.
//
// La règle d'or : ce qu'on ne sait pas ne cache rien. Un profil dont le type est inconnu montre les pannes « portable »
// et « fixe » ; une marque inconnue montre les pannes de toutes les marques. « Je ne sais pas » montre donc tout.
// (le profil est choisi par la personne sur le téléphone : il ne voit pas le PC)

/** Portable ou PC fixe (une tour, un PC tout-en-un...). */
enum class TypePc(val nom: String, val libelle: String) {
    Portable("portable", "Portable"),
    Fixe("fixe", "PC fixe");

    companion object {
        /** Depuis le mot du catalogue (sans tenir compte des majuscules ; « tour » vaut « fixe »). */
        fun depuis(t: String): TypePc? = when (t.trim().lowercase()) {
            "portable" -> Portable
            "fixe", "tour" -> Fixe
            else -> null
        }
    }
}

/** Les marques qu'on peut citer dans « PourPc » et choisir, avec leurs noms simples (les mêmes que sur PC). */
val Marques = listOf(
    "Acer", "Apple", "ASRock", "ASUS", "Biostar", "Dell", "EVGA", "Fujitsu", "Gigabyte", "HP", "Huawei", "Intel", "Lenovo",
    "LG", "Medion", "Microsoft Surface", "MSI", "Razer", "Samsung", "Sony", "Toshiba", "Xiaomi",
)

/** Le nom simple d'une marque de la liste (sans tenir compte des majuscules). */
fun marqueConnue(m: String): String? = Marques.firstOrNull { it.equals(m.trim(), ignoreCase = true) }

/** Ce qu'on sait du PC pour trier les pannes. Chaque champ peut être inconnu (null) : il ne cache alors rien. */
data class ProfilPc(val type: TypePc? = null, val marque: String? = null) {
    /** Rien n'est connu (tout est montré). */
    val inconnu: Boolean get() = type == null && marque.isNullOrBlank()

    /** Le libellé court : « Portable HP », « PC fixe ASUS », « Portable », « PC Dell », « Tous les PC ». */
    val libelleCourt: String get() {
        val m = marque?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            type != null && m != null -> "${type.libelle} $m"
            type != null -> type.libelle
            m != null -> "PC $m"
            else -> "Tous les PC"
        }
    }

    companion object {
        /** « Je ne sais pas » : tout est montré. */
        val Inconnu = ProfilPc()
    }
}

/**
 * Une panne concerne-t-elle ce profil ? Sans « PourPc », ou un « PourPc » mal écrit (refusé par la validation du
 * catalogue sur PC) : oui. Ce qui est inconnu dans le profil ne cache rien.
 */
fun Objet.concerne(profil: ProfilPc): Boolean {
    val pour = champ("PourPc") as? JsonObject ?: return true
    val p = Objet(pour)
    val type = p.champ("Type")
    if (type is JsonPrimitive && type.isString && profil.type != null) {
        val voulu = TypePc.depuis(type.content)
        if (voulu != null && voulu != profil.type) return false
    }
    val marques = p.champ("Marque")
    val m = profil.marque
    if (marques is JsonArray && m != null && m.isNotBlank() && marques.isNotEmpty() &&
        marques.none { it is JsonPrimitive && it !is JsonNull && it.isString && egal(it.content.trim(), m.trim()) }
    ) return false
    return true
}

/** Des pannes triées pour un profil : celles à montrer (dans l'ordre reçu) et le nombre de celles cachées. */
data class Tri(val visibles: List<Objet>, val cachees: Int)

/** Trie des pannes pour un profil. */
fun trier(pannes: List<Objet>, profil: ProfilPc): Tri {
    val visibles = pannes.filter { it.concerne(profil) }
    return Tri(visibles, pannes.size - visibles.size)
}

/** Toutes les pannes qui concernent ce PC, et combien sont cachées. */
fun Catalogue.pannesPour(profil: ProfilPc): Tri = trier(pannes, profil)

/** Les pannes d'une famille qui concernent ce PC, et combien sont cachées. */
fun Catalogue.pannesFamillePour(famille: String, profil: ProfilPc): Tri = trier(pannesFamille(famille), profil)

/** La recherche par mots pour ce PC : les pannes trouvées qui le concernent (même ordre), et combien sont cachées. */
fun Catalogue.chercherPour(requete: String, profil: ProfilPc): Tri = trier(chercher(requete), profil)
