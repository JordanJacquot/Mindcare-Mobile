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
// Les champs gardent leur nom du fichier ; tout ce qui n'est pas compris est ignoré, comme sur PC.

/** Le format le plus récent que cette version de l'appli sait lire (« Version » en tête de enquetes.json). */
const val FormatConnu = 2

/** Un objet du fichier (une panne, une étape, une réponse...), avec des lectures simples de ses champs. */
@JvmInline
value class Objet(val json: JsonObject) {
    /** Un texte (« » si absent). */
    fun texte(nom: String): String = (json[nom] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull ?: ""
    /** Une liste de textes : un tableau, ou un seul texte (comme @() sur PC). */
    fun liste(nom: String): List<String> = when (val v = json[nom]) {
        is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.filter { it.isNotEmpty() }
        is JsonPrimitive -> listOfNotNull(v.contentOrNull?.takeIf { it.isNotEmpty() })
        else -> emptyList()
    }
    fun objet(nom: String): Objet? = (json[nom] as? JsonObject)?.let { Objet(it) }
    fun objets(nom: String): List<Objet> = when (val v = json[nom]) {
        is JsonArray -> v.mapNotNull { (it as? JsonObject)?.let { o -> Objet(o) } }
        is JsonObject -> listOf(Objet(v))
        else -> emptyList()
    }
    fun vrai(nom: String): Boolean = (json[nom] as? JsonPrimitive)?.booleanOrNull ?: false
    fun a(nom: String): Boolean = json[nom] != null && json[nom] !is JsonNull
}

class Catalogue(val json: JsonObject) {
    val version: Int = (json["Version"] as? JsonPrimitive)?.intOrNull ?: 1
    val familles: List<Objet> = Objet(json).objets("Familles")
    val pannes: List<Objet> = Objet(json).objets("Pannes")
    val securite: List<String> = Objet(json).liste("Securite")
    private val suspects: JsonObject = json["Suspects"] as? JsonObject ?: JsonObject(emptyMap())
    private val tests: JsonObject = json["Tests"] as? JsonObject ?: JsonObject(emptyMap())

    fun panne(id: String): Objet? = pannes.firstOrNull { it.texte("Id") == id }
    fun famille(id: String): Objet? = familles.firstOrNull { it.texte("Id") == id }
    fun suspect(id: String?): Objet? = if (id.isNullOrEmpty()) null else (suspects[id] as? JsonObject)?.let { Objet(it) }
    fun test(id: String): Objet? = (tests[id] as? JsonObject)?.let { Objet(it) }

    /**
     * Les étapes d'une panne, prêtes (Get-Etapes sur PC) : une étape « Ref » prend le test partagé du même nom (et son
     * Id), puis ses propres champs par-dessus.
     */
    fun etapes(panne: Objet): List<Objet> = panne.objets("Etapes").map { e ->
        val o = LinkedHashMap<String, JsonElement>()
        val ref = e.texte("Ref")
        val base = if (ref.isNotEmpty()) test(ref) else null
        if (base != null) { o.putAll(base.json); o["Id"] = JsonPrimitive(ref) }
        for ((k, v) in e.json) if (k != "Ref") o[k] = v
        Objet(JsonObject(o))
    }

    companion object {
        private val lecteur = Json { ignoreUnknownKeys = true }
        fun lire(texte: String): Catalogue = Catalogue(lecteur.parseToJsonElement(texte.removePrefix("﻿")).jsonObject)
    }
}
