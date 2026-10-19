package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.ui.lireSvg
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DessinTest {
    private val dossier = File("src/main/assets/catalogue/images")
    private val catalogue = Catalogue.lire(File("src/main/assets/catalogue/enquetes.json").readText())

    /** Chaque dessin se lit, avec des formes (le même sous-ensemble de SVG que sur PC). */
    @Test fun chaqueDessinSeLit() {
        val fichiers = dossier.listFiles { f -> f.extension == "svg" }!!
        assertTrue(fichiers.size >= 20)
        for (f in fichiers) {
            val d = f.inputStream().use { lireSvg(it) }
            assertNotNull("${f.name} illisible", d)
            assertTrue("${f.name} sans forme", d!!.formes.isNotEmpty() && d.largeur > 0 && d.hauteur > 0)
        }
    }

    /** Chaque image citée par le catalogue (étapes et familles) existe. */
    @Test fun chaqueImageCiteeExiste() {
        val citees = catalogue.familles.map { it.texte("Icone") } +
            catalogue.pannes.flatMap { p -> catalogue.etapes(p).map { it.texte("Image") } }
        val manquantes = citees.filter { it.isNotEmpty() && !File(dossier, "$it.svg").exists() }.distinct()
        assertTrue("images manquantes : $manquantes", manquantes.isEmpty())
    }
}
