package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.Verdict
import io.github.jordanjacquot.mindcare.moteur.chercher
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.titreAffiche
import io.github.jordanjacquot.mindcare.moteur.titreAvecDetail
import io.github.jordanjacquot.mindcare.moteur.titreReponse
import io.github.jordanjacquot.mindcare.moteur.titreVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelephoneTest {
    private val catalogue = Catalogue.lire(File("src/main/assets/catalogue/enquetes.json").readText())

    /** Le téléphone n'est jamais le PC en panne : la question « sur ce PC ? » n'est jamais posée. */
    @Test fun laQuestionSurCePcNestJamaisPosee() {
        for (p in catalogue.pannes) {
            var choix = listOf<Int>()
            repeat(40) {
                val et = catalogue.enqueteTelephone(p, choix)
                val etape = et.e.etape ?: return@repeat
                assertTrue("${p.texte("Id")} : « sur ce PC ? » posée", etape.objets("Reponses").none { it.texte("Note") == "cepc" })
                assertTrue(et.e.notes.none { it == "cepc" })
                choix = et.choix + 0
            }
        }
    }

    @Test fun ilRedemarreToutSeulCommenceParUneVraieQuestion() {
        val et = catalogue.enqueteTelephone(catalogue.panne("redemarre")!!, emptyList())
        assertNotEquals("cepc", et.e.etape?.texte("Id"))
        assertEquals(et.choix.size, et.auto)
    }

    /**
     * La phrase de la réponse quand un détail a confirmé le suspect : la même que sur PC (titres.rs du moteur Rust,
     * la même liste), sans répéter ce que le détail précise.
     */
    @Test fun lesTitresAvecDetailSontCeuxDuPc() {
        val attendus = listOf(
        Triple("aeration", "il est enfermé", "L'air ne circule pas autour du PC : il est enfermé"),
        Triple("arnaque", "ils ont eu accès au PC ou à des codes", "Danger : c'est une arnaque au faux support (ils ont eu accès au PC ou à des codes)"),
        Triple("arnaque", "rien de grave si rien n'a été fait", "Danger : c'est une arnaque au faux support (rien de grave si rien n'a été fait)"),
        Triple("batterie", "la batterie", "La batterie lâche"),
        Triple("batterie", "le chargeur ou la prise de charge", "Le chargeur ou la prise de charge lâche"),
        Triple("batteriegonflee", "dans le doute, traite-la comme gonflée", "Danger : la batterie a gonflé (dans le doute, traite-la comme gonflée)"),
        Triple("bluetoothson", "la distance ou les ondes autour", "La liaison Bluetooth était mal faite : la distance ou les ondes autour"),
        Triple("chargeur", "il chauffe mais sans danger visible", "Le chargeur ne donne plus de courant : il chauffe mais sans danger visible"),
        Triple("courant", "la prise", "Le courant n'arrive pas jusqu'au PC : la prise"),
        Triple("courant", "le câble ou la multiprise", "Le courant n'arrive pas jusqu'au PC : le câble ou la multiprise"),
        Triple("danger", "débranche et éloigne-toi tout de suite", "Danger : une pièce a peut-être grillé (débranche et éloigne-toi tout de suite)"),
        Triple("danger", "le chargeur", "Danger : une pièce a peut-être grillé (le chargeur)"),
        Triple("ecranalim", "il suffisait de l'allumer ou de rebrancher son câble", "L'écran lui-même était éteint ou débranché : il suffisait de l'allumer ou de rebrancher son câble"),
        Triple("ecranportable", "la dalle est cassée", "L'écran du portable (ou son câble) est abîmé : la dalle est cassée"),
        Triple("energie", "le portable marche sur batterie", "Le PC était bridé pour économiser l'énergie : le portable marche sur batterie"),
        Triple("localiser", "cherche-le avec « Localiser mon appareil »", "Windows peut localiser (et protéger) le portable perdu : cherche-le avec « Localiser mon appareil »"),
        Triple("localiser", "verrouille-le et change tes mots de passe", "Windows peut localiser (et protéger) le portable perdu : verrouille-le et change tes mots de passe"),
        Triple("majbloquee", "la dernière mise à jour", "Une mise à jour de Windows était en train de s'installer : la dernière mise à jour"),
        Triple("materiel", "une pièce mal remise", "Une pièce mal remise"),
        Triple("memoire", "celle avec laquelle il ne démarre pas", "Une barrette de mémoire (RAM) est abîmée : celle avec laquelle il ne démarre pas"),
        Triple("pilote", "le pilote de la carte graphique manque", "Le pilote de la carte graphique manque"),
        Triple("prise", "l'électricité de la maison", "Le courant coupe : l'électricité de la maison"),
        Triple("prise", "la multiprise", "Le courant coupe : la multiprise"),
        Triple("reinit", "choisis « Conserver mes fichiers »", "Réinitialiser Windows peut régler les problèmes que rien d'autre ne règle : choisis « Conserver mes fichiers »"),
        Triple("reinit", "choisis « Supprimer tout » (et « Nettoyer le lecteur »)", "Réinitialiser Windows peut régler les problèmes que rien d'autre ne règle : choisis « Supprimer tout » (et « Nettoyer le lecteur »)"),
        Triple("surchauffe", "la poussière", "Il chauffe trop : la poussière"),
        Triple("surchauffe", "un ventilateur ne tourne plus", "Il chauffe trop : un ventilateur ne tourne plus"),
        Triple("verrou", "Verr Num", "Une touche de verrouillage était activée : Verr Num"),
        Triple("wifioff", "le Bluetooth ou le mode avion", "Le Wi-Fi était désactivé (ou le mode avion activé) : le Bluetooth ou le mode avion"),
        Triple("windowsabime", "réparé par l'outil de redémarrage", "Windows est abîmé : réparé par l'outil de redémarrage"),
        Triple("windowsabime", "réparé par la restauration", "Windows est abîmé : réparé par la restauration"),
        )
        // Tous les détails du catalogue sont dans la liste
        val couples = LinkedHashSet<Pair<String, String>>()
        fun parcourir(o: kotlinx.serialization.json.JsonElement) {
            when (o) {
                is kotlinx.serialization.json.JsonObject -> {
                    val x = Objet(o)
                    if (x.texte("Confirme").isNotEmpty() && x.texte("Detail").isNotEmpty()) couples += x.texte("Confirme") to x.texte("Detail")
                    o.values.forEach { parcourir(it) }
                }
                is kotlinx.serialization.json.JsonArray -> o.forEach { parcourir(it) }
                else -> {}
            }
        }
        parcourir(catalogue.json)
        assertEquals(attendus.map { it.first to it.second }.toSet(), couples)
        for ((s, d, attendu) in attendus) {
            assertEquals("$s / $d", attendu, titreAvecDetail(catalogue.suspect(s)!!.texte("Trouve"), d))
            assertEquals(attendu, catalogue.titreReponse(Verdict("trouve", s, d, listOf(s))).titre)
        }
        // Le titre du moteur (comparé au PC par EquivalenceTest) ne change pas ; sans détail, rien ne change
        val batterie = Verdict("trouve", "batterie", "la batterie", listOf("batterie"))
        assertEquals("La batterie (ou le chargeur) lâche : la batterie", catalogue.titreVerdict(batterie))
        assertEquals("La batterie lâche", catalogue.titreAffiche(batterie))
        val sansDetail = Verdict("elimination", "batterie", "", listOf("batterie"))
        assertEquals(catalogue.titreVerdict(sansDetail), catalogue.titreAffiche(sansDetail))
    }

    /** « il s'éteint tout seul » met « Il s'éteint d'un coup » en tête, comme sur PC (phrases.rs du moteur Rust). */
    @Test fun ilSEteintToutSeulTrouveLaBonnePanne() {
        for (q in listOf("il s'éteint tout seul", "mon pc s'éteint tout seul")) assertEquals(q, "seteint", catalogue.chercher(q).first().texte("Id"))
        assertEquals("casque", catalogue.chercher("casque").first().texte("Id"))
    }
}
