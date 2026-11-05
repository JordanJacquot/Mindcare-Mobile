package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Groupe
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc
import io.github.jordanjacquot.mindcare.moteur.chercher
import io.github.jordanjacquot.mindcare.moteur.chercherPour
import io.github.jordanjacquot.mindcare.moteur.enquete
import io.github.jordanjacquot.mindcare.moteur.pannesPour
import io.github.jordanjacquot.mindcare.moteur.titreVerdict
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le PC et le téléphone donnent exactement les mêmes enquêtes et les mêmes recherches, sur le même catalogue.
 * La référence (reference-pc.json) est faite par le vrai moteur de Mindcare PC, en Rust (outils\exporter-reference.ps1) :
 * les enquêtes, les recherches, les groupes de chaque famille et le tri des pannes selon le PC (« PourPc »).
 */
class EquivalenceTest {
    private val catalogueTexte = File("src/main/assets/catalogue/enquetes.json").readText()
    private val catalogue = Catalogue.lire(catalogueTexte)
    private val reference = Json.parseToJsonElement(javaClass.getResource("/reference-pc.json")!!.readText().removePrefix("﻿")).jsonObject

    private fun JsonObject.textes(nom: String) = (this[nom] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
    private fun JsonObject.texte(nom: String) = this[nom]?.jsonPrimitive?.content ?: ""

    /** La copie de l'appli est bien celle du PC (quand le dépôt Mindcare est à côté). */
    @Test fun laCopieEstCelleDuPc() {
        val pc = File("../../Mindcare/Mindcare/fichiers/enquetes.json")
        if (pc.exists()) assertEquals("La copie du catalogue n'est pas celle du PC : reconstruis l'appli.", pc.readText(), catalogueTexte)
    }

    @Test fun lesEnquetesSontLesMemes() {
        val cas = reference["Cas"]!!.jsonArray.map { it.jsonObject }
        assertTrue("trop peu de cas : ${cas.size}", cas.size > 3000)
        val ecarts = ArrayList<String>()
        for (c in cas) {
            val panne = catalogue.panne(c.texte("Panne"))!!
            val entree = c["Entree"]!!.jsonArray.map { it.jsonPrimitive.int }
            val e = catalogue.enquete(panne, entree)
            val v = e.verdict
            val obtenu = linkedMapOf(
                "Etape" to listOf(e.etape?.texte("Id") ?: ""),
                "Choix" to e.choix.map { it.toString() },
                "Restants" to e.restants,
                "Ecartes" to e.ecartes.map { "${it.id}|${it.raison}" },
                "Notes" to e.notes,
                "Preuves" to e.preuves,
                "Faits" to e.faits.map { "${it.court}|${it.reponse}" },
                "Verdict" to listOf(if (v == null) "" else "${v.type}|${v.suspect ?: ""}|${v.detail}|${v.restants.joinToString(",")}"),
                "Titre" to listOf(catalogue.titreVerdict(v)),
                "AVenir" to listOf(e.aVenir.toString()),
            )
            for ((nom, valeur) in obtenu) {
                val attendu = when (nom) {
                    "Etape", "Verdict", "Titre" -> listOf(c.texte(nom))
                    "AVenir" -> listOf(c[nom]!!.jsonPrimitive.int.toString())
                    "Choix" -> c[nom]!!.jsonArray.map { it.jsonPrimitive.int.toString() }
                    else -> c.textes(nom)
                }
                if (attendu != valeur) ecarts += "${c.texte("Panne")} ${entree} $nom : PC $attendu, téléphone $valeur"
            }
        }
        assertTrue("${ecarts.size} différences avec le PC, dont :\n" + ecarts.take(15).joinToString("\n"), ecarts.isEmpty())
    }

    @Test fun lesRecherchesSontLesMemes() {
        val recherches = reference["Recherches"]!!.jsonArray.map { it.jsonObject }
        val ecarts = recherches.mapNotNull { r ->
            val attendu = r.textes("Pannes")
            val obtenu = catalogue.chercher(r.texte("Requete")).map { it.texte("Id") }
            if (attendu != obtenu) "« ${r.texte("Requete")} » : PC $attendu, téléphone $obtenu" else null
        }
        assertTrue("${ecarts.size} recherches différentes sur ${recherches.size}, dont :\n" + ecarts.take(15).joinToString("\n"), ecarts.isEmpty())
    }

    /** Les groupes de chaque famille (les cartes dépliables), avec leurs pannes, dans l'ordre du PC. */
    @Test fun lesGroupesSontLesMemes() {
        val familles = reference["Groupes"]!!.jsonArray.map { it.jsonObject }
        assertEquals("familles dans la référence", catalogue.familles.map { it.texte("Id") }, familles.map { it.texte("Famille") })
        val ecarts = familles.mapNotNull { f ->
            val attendu = f["Groupes"]!!.jsonArray.map { it.jsonObject }
                .map { g -> listOf(g.texte("Id"), g.texte("Nom"), g.texte("Exemple"), g.texte("Icone")) + g.textes("Pannes") }
            val obtenu = catalogue.groupes(f.texte("Famille")).map { g -> listOf(g.id, g.nom, g.exemple, g.icone) + ids(g) }
            if (attendu != obtenu) "${f.texte("Famille")} : PC $attendu, téléphone $obtenu" else null
        }
        assertTrue("${ecarts.size} familles rangées autrement, dont :\n" + ecarts.take(5).joinToString("\n"), ecarts.isEmpty())
    }

    /** Le tri selon le PC (« PourPc ») : pour chaque profil, les pannes gardées et cachées, par famille et par recherche. */
    @Test fun leTriSelonLePcEstLeMeme() {
        val profils = reference["Tri"]!!.jsonArray.map { it.jsonObject }
        assertTrue("trop peu de profils : ${profils.size}", profils.size >= 5)
        val ecarts = ArrayList<String>()
        for (t in profils) {
            val type = t.texte("Type").takeIf { it.isNotEmpty() }?.let { n -> TypePc.entries.first { it.nom == n } }
            val profil = ProfilPc(type, t.texte("Marque").takeIf { it.isNotEmpty() })
            val nom = "profil « ${t.texte("Type")} ${t.texte("Marque")} »"
            val toutes = catalogue.pannesPour(profil)
            if (toutes.visibles.map { it.texte("Id") } != t.textes("Pannes") || toutes.cachees != t.entier("Cachees"))
                ecarts += "$nom : pannes gardées ou cachées (${toutes.cachees} cachées, PC ${t.entier("Cachees")})"
            for (f in t["Familles"]!!.jsonArray.map { it.jsonObject }) {
                val attendu = f["Groupes"]!!.jsonArray.map { it.jsonObject }.map { g -> listOf(g.texte("Id")) + g.textes("Pannes") } to f.entier("Cachees")
                val gf = catalogue.groupesPour(f.texte("Famille"), profil)
                val obtenu = gf.groupes.map { g -> listOf(g.id) + ids(g) } to gf.cachees
                if (attendu != obtenu) ecarts += "$nom, famille ${f.texte("Famille")} : PC $attendu, téléphone $obtenu"
            }
            for (r in t["Recherches"]!!.jsonArray.map { it.jsonObject }) {
                val tri = catalogue.chercherPour(r.texte("Requete"), profil)
                val attendu = r.textes("Pannes") to r.entier("Cachees")
                val obtenu = tri.visibles.map { it.texte("Id") } to tri.cachees
                if (attendu != obtenu) ecarts += "$nom, « ${r.texte("Requete")} » : PC $attendu, téléphone $obtenu"
            }
        }
        assertTrue("${ecarts.size} différences de tri avec le PC, dont :\n" + ecarts.take(15).joinToString("\n"), ecarts.isEmpty())
    }

    private fun ids(g: Groupe) = g.pannes.map { it.texte("Id") }
    private fun JsonObject.entier(nom: String) = this[nom]!!.jsonPrimitive.int
}
