package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.donnees.Historique
import io.github.jordanjacquot.mindcare.donnees.rapport
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.titreVerdict
import org.junit.Assert.assertEquals
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
}
