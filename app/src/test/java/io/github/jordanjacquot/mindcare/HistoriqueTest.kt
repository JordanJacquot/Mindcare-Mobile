package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.donnees.Historique
import io.github.jordanjacquot.mindcare.donnees.aReprendre
import io.github.jordanjacquot.mindcare.donnees.rapport
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.titreVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class HistoriqueTest {
    private fun fichier() = File(Files.createTempDirectory("mindcare").toFile(), "diagnostics.json")

    @Test fun lesDixDernieresEnTete() {
        val f = fichier()
        val h = Historique(f)
        for (i in 1..12) h.enregistrer("d$i", "redemarre", listOf(1), false, null, "")
        h.enregistrer("d12", "redemarre", listOf(1, 0), true, "memoire", "Une barrette")
        val relu = Historique(f).liste.value
        assertEquals(10, relu.size)
        assertEquals("d12", relu[0].Id)
        assertTrue(relu[0].Fini)
        assertEquals("d3", relu.last().Id)
        h.enregistrer("d5", "redemarre", emptyList(), false, null, "")   // (sans réponse : plus gardée)
        assertTrue(h.liste.value.none { it.Id == "d5" })
    }

    @Test fun leRapportDitLEssentiel() {
        val c = Catalogue.lire(File("src/main/assets/catalogue/enquetes.json").readText())
        val p = c.panne("wificoupe")!!
        val e = c.enqueteTelephone(p, listOf(1, 1, 0)).e
        val r = c.rapport(p, e, c.titreVerdict(e.verdict), "0.1.0")
        assertTrue(r, r.contains("Panne : Le Wi-Fi coupe souvent") && r.contains("Réponse : Le Wi-Fi enregistré dans le PC était abîmé") && r.contains("Les réponses :"))
    }

    /**
     * Une enquête commencée puis quittée avec le retour du téléphone (qui retire une réponse à chaque fois, jusqu'à
     * revenir à l'accueil) reste en « Reprendre l'enquête », avec sa première réponse. Avant, elle était effacée en
     * retirant sa dernière réponse, et la carte n'apparaissait pas.
     */
    @Test fun uneEnqueteQuitteeAvecLeRetourResteAReprendre() {
        val c = Catalogue.lire(File("src/main/assets/catalogue/enquetes.json").readText())
        val f = fichier()
        val h = Historique(f)
        h.enregistrer("ancienne", "wificoupe", listOf(1, 1, 0), true, "wifi", "Une réponse")
        // L'enquête, une réponse à la fois, comme Navigation (à chaque réponse donnée ou retirée)
        for (choix in listOf(emptyList(), listOf(0), listOf(0, 1), listOf(0), emptyList())) h.garderEnCours("en-cours", "redemarre", choix)
        val d = h.liste.value.aReprendre(c)
        assertEquals("en-cours", d?.Id)
        assertEquals(listOf(0), d?.Choix)
        // et elle survit à la relecture du fichier
        assertEquals("en-cours", Historique(f).liste.value.aReprendre(c)?.Id)
        // Revenue au début puis répondue autrement : elle repart de cette réponse
        h.garderEnCours("en-cours", "redemarre", listOf(1))
        assertEquals(listOf(1), h.trouver("en-cours")?.Choix)
    }

    @Test fun rienAReprendre() {
        val c = Catalogue.lire(File("src/main/assets/catalogue/enquetes.json").readText())
        val h = Historique(fichier())
        // Ouverte sans aucune réponse : rien n'est gardé
        h.garderEnCours("vide", "redemarre", emptyList())
        assertNull(h.liste.value.aReprendre(c))
        // La plus récente est finie : « Dernière enquête », pas « Reprendre »
        h.garderEnCours("a", "redemarre", listOf(0))
        h.enregistrer("b", "wificoupe", listOf(1, 1, 0), true, "wifi", "Une réponse")
        assertNull(h.liste.value.aReprendre(c))
        // Une enquête finie revue, puis une réponse retirée avec le retour : elle reste finie (comme avant)
        h.garderEnCours("b", "wificoupe", listOf(1, 1))
        assertEquals(true, h.trouver("b")?.Fini)
        // Sa panne a disparu du catalogue : rien à reprendre
        h.garderEnCours("c", "panne-disparue", listOf(0))
        assertNull(h.liste.value.aReprendre(c))
    }
}
