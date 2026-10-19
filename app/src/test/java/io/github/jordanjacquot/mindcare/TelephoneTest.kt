package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
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
}
