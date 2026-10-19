package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.maj.MajCatalogue
import io.github.jordanjacquot.mindcare.maj.problemes
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CatalogueTelechargeTest {
    private val dossier = File("src/main/assets/catalogue")

    @Test fun leCatalogueDeLAppliEstBon() {
        val c = Catalogue.lire(File(dossier, "enquetes.json").readText())
        assertEquals(emptyList<String>(), c.problemes { File(dossier, "images/$it.svg").exists() })
    }

    @Test fun unFormatTropRecentEstRefuse() {
        val c = Catalogue.lire(File(dossier, "enquetes.json").readText().replaceFirst("\"Version\": 2", "\"Version\": 99"))
        assertTrue(c.problemes { true }.any { it.contains("trop récent") })
    }

    @Test fun leZipNeSortQueLeCatalogue() {
        val tmp = Files.createTempDirectory("cat").toFile()
        val zip = File(tmp, "catalogue.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            fun ajoute(nom: String, texte: String) { z.putNextEntry(ZipEntry(nom)); z.write(texte.toByteArray()); z.closeEntry() }
            ajoute("enquetes.json", "{}"); ajoute("images/ecran-bleu.svg", "<svg/>")
            ajoute("../evade.txt", "non"); ajoute("images/../../evade2.svg", "non"); ajoute("autre.exe", "non")
        }
        val vers = File(tmp, "sortie").apply { mkdirs() }
        MajCatalogue.extraire(zip, vers)
        assertTrue(File(vers, "enquetes.json").exists() && File(vers, "images/ecran-bleu.svg").exists())
        assertFalse(File(tmp, "evade.txt").exists() || File(tmp, "evade2.svg").exists() || File(vers, "autre.exe").exists())
        assertEquals(64, MajCatalogue.sha256(zip).length)
    }
}
