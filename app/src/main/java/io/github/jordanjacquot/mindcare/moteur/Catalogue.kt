package io.github.jordanjacquot.mindcare.moteur

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

// Le catalogue de pannes : enquetes.json de Mindcare PC, lu tel quel. Il ne s'écrit jamais ici (voir CLAUDE.md) :
// la copie de l'appli est recopiée depuis le dépôt Mindcare, ou téléchargée (la dernière version publiée).
// Les champs gardent leur nom du fichier ; tout ce qui n'est pas compris est ignoré, comme sur PC. Comme sur PC aussi, un
// champ se lit sans tenir compte des majuscules (« titre » lit « Titre »), et les Id se comparent de même (voir egal).

/** Le format le plus récent que cette version de l'appli sait lire (« Version » en tête de enquetes.json). */
const val FormatConnu = 2

/** Un objet du fichier (une panne, une étape, une réponse...), avec des lectures simples de ses champs. */
@JvmInline
value class Objet(val json: JsonObject) {
    /** Un champ, sans tenir compte des majuscules (le nom exact d'abord), comme `champ` du moteur Rust du PC. */
    fun champ(nom: String): JsonElement? = json[nom] ?: json.entries.firstOrNull { it.key.equals(nom, ignoreCase = true) }?.value
    /** Un texte (« » si absent). */
    fun texte(nom: String): String = (champ(nom) as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull ?: ""
    /** Une liste de textes : un tableau, ou un seul texte (comme @() sur PC). */
    fun liste(nom: String): List<String> = when (val v = champ(nom)) {
        is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.filter { it.isNotEmpty() }
        is JsonPrimitive -> listOfNotNull(v.contentOrNull?.takeIf { it.isNotEmpty() })
        else -> emptyList()
    }
    fun objet(nom: String): Objet? = (champ(nom) as? JsonObject)?.let { Objet(it) }
    fun objets(nom: String): List<Objet> = when (val v = champ(nom)) {
        is JsonArray -> v.mapNotNull { (it as? JsonObject)?.let { o -> Objet(o) } }
        is JsonObject -> listOf(Objet(v))
        else -> emptyList()
    }
    fun vrai(nom: String): Boolean = (champ(nom) as? JsonPrimitive)?.booleanOrNull ?: false
    fun a(nom: String): Boolean = champ(nom).let { it != null && it !is JsonNull }
}

/** Deux textes égaux sans tenir compte des majuscules (`-eq` de PowerShell, `egal` du moteur Rust du PC). */
fun egal(a: String, b: String): Boolean = a == b || a.lowercase() == b.lowercase()

/** Un champ d'un objet JSON, sans tenir compte des majuscules (le nom exact d'abord). */
private fun JsonObject.champ(nom: String): JsonElement? = this[nom] ?: entries.firstOrNull { it.key.equals(nom, ignoreCase = true) }?.value

/** Écrit un champ : un champ du même nom (majuscules ou pas) est remplacé à sa place, sinon il est ajouté à la fin. */
private fun LinkedHashMap<String, JsonElement>.mettre(nom: String, valeur: JsonElement) {
    this[keys.firstOrNull { it.equals(nom, ignoreCase = true) } ?: nom] = valeur
}

class Catalogue(val json: JsonObject) {
    val version: Int = (json["Version"] as? JsonPrimitive)?.intOrNull ?: 1
    val familles: List<Objet> = Objet(json).objets("Familles")
    val pannes: List<Objet> = Objet(json).objets("Pannes")
    val securite: List<String> = Objet(json).liste("Securite")
    private val suspects: JsonObject = json["Suspects"] as? JsonObject ?: JsonObject(emptyMap())
    private val tests: JsonObject = json["Tests"] as? JsonObject ?: JsonObject(emptyMap())

    // (les Id se comparent sans tenir compte des majuscules ; une panne ou une famille sans Id n'est jamais trouvée)
    fun panne(id: String): Objet? = pannes.firstOrNull { it.a("Id") && egal(it.texte("Id"), id) }
    fun famille(id: String): Objet? = familles.firstOrNull { it.a("Id") && egal(it.texte("Id"), id) }
    fun suspect(id: String?): Objet? = if (id.isNullOrEmpty()) null else (suspects.champ(id) as? JsonObject)?.let { Objet(it) }
    fun test(id: String): Objet? = (tests.champ(id) as? JsonObject)?.let { Objet(it) }

    /** Les pannes d'une famille (son Id, sans tenir compte des majuscules), dans l'ordre du fichier. */
    fun pannesFamille(famille: String): List<Objet> = pannes.filter { egal(it.texte("Famille"), famille) }

    /**
     * Les groupes d'une famille (son champ « Groupes »), dans l'ordre du fichier, chacun avec ses pannes (celles dont le
     * champ « Groupe » le cite, dans l'ordre du fichier). Une panne sans groupe connu n'est jamais perdue : elle va dans
     * un dernier groupe « Autres pannes » (Id vide). Une famille sans groupes : un seul groupe « Autres pannes ».
     * Le portage exact de `Catalogue::groupes` du moteur Rust du PC.
     */
    fun groupes(famille: String): List<Groupe> = groupesDe(pannesFamille(famille), famille)

    /**
     * Les groupes d'une famille pour ce PC (« PourPc », voir Pc.kt) : un groupe vidé par le filtre disparaît, et
     * `cachees` dit combien de pannes de la famille sont cachées.
     */
    fun groupesPour(famille: String, profil: ProfilPc): GroupesFamille {
        val tri = trier(pannesFamille(famille), profil)
        return GroupesFamille(groupesDe(tri.visibles, famille).filter { it.pannes.isNotEmpty() }, tri.cachees)
    }

    private fun groupesDe(pannes: List<Objet>, famille: String): List<Groupe> {
        val f = famille(famille)
        val connus = (f?.objets("Groupes") ?: emptyList())
            .map { g -> Groupe(g.texte("Id"), g.texte("Nom"), g.texte("Exemple"), g.texte("Icone"), emptyList()) }
            .filter { it.id.isNotEmpty() }
        val siennes = connus.map { ArrayList<Objet>() }
        val autres = ArrayList<Objet>()
        for (p in pannes) {
            val id = p.texte("Groupe")
            val i = if (id.isEmpty()) -1 else connus.indexOfFirst { egal(it.id, id) }
            if (i >= 0) siennes[i] += p else autres += p
        }
        val groupes = connus.mapIndexed { i, g -> g.copy(pannes = siennes[i]) }
        return if (autres.isEmpty()) groupes else groupes + Groupe("", "Autres pannes", "", f?.texte("Icone") ?: "", autres)
    }

    /**
     * Les étapes d'une panne, prêtes (Get-Etapes sur PC) : une étape « Ref » prend le test partagé du même nom (et son
     * Id), puis ses propres champs par-dessus.
     */
    fun etapes(panne: Objet): List<Objet> = panne.objets("Etapes").map { e ->
        val o = LinkedHashMap<String, JsonElement>()
        val ref = e.texte("Ref")
        val base = if (ref.isNotEmpty()) test(ref) else null
        if (base != null) { for ((k, v) in base.json) o.mettre(k, v); o.mettre("Id", JsonPrimitive(ref)) }
        for ((k, v) in e.json) if (!k.equals("Ref", ignoreCase = true)) o.mettre(k, v)
        Objet(JsonObject(o))
    }

    companion object {
        private val lecteur = Json { ignoreUnknownKeys = true }
        fun lire(texte: String): Catalogue = Catalogue(lecteur.parseToJsonElement(texte.removePrefix("﻿")).jsonObject)
    }
}

/** Un groupe de pannes d'une famille (une carte dépliable de l'écran de la famille) : « Il ne s'allume pas »... */
data class Groupe(
    val id: String,
    /** Le nom court (« Il ne s'allume pas »). */
    val nom: String,
    /** Une phrase d'exemple (« Rien ne se passe, ou juste une lumière »). */
    val exemple: String,
    /** Le nom d'une image du catalogue (images/<icone>.svg). */
    val icone: String,
    /** Ses pannes, dans l'ordre du fichier. */
    val pannes: List<Objet>,
)

/** Les groupes d'une famille pour un PC : ceux qui gardent au moins une panne, et combien de pannes sont cachées. */
data class GroupesFamille(val groupes: List<Groupe>, val cachees: Int)
