package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.donnees.MemoirePc
import io.github.jordanjacquot.mindcare.donnees.phraseCachees
import io.github.jordanjacquot.mindcare.donnees.rapport
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Enquete
import io.github.jordanjacquot.mindcare.moteur.EnqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.GesteTaperCommande
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc
import io.github.jordanjacquot.mindcare.moteur.concerne
import io.github.jordanjacquot.mindcare.moteur.demandeLeTypeDePc
import io.github.jordanjacquot.mindcare.moteur.enquete
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.gestePourTelephone
import io.github.jordanjacquot.mindcare.moteur.horsSujet
import io.github.jordanjacquot.mindcare.moteur.pistesEcartees
import io.github.jordanjacquot.mindcare.moteur.pistesPossibles
import io.github.jordanjacquot.mindcare.moteur.reponsesAvantLaDerniere
import io.github.jordanjacquot.mindcare.moteur.titreReponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** « Ton PC » sur le téléphone : le profil gardé, la question du type répondue toute seule, les pistes hors sujet cachées. */
class TonPcTest {
    private val catalogue = Catalogue.lire(File("src/main/assets/catalogue/enquetes.json").readText())
    private val portableHp = ProfilPc(TypePc.Portable, "HP")
    private val fixe = ProfilPc(TypePc.Fixe, null)

    /** Une enquête menée jusqu'au bout, en donnant toujours la même réponse (ou au plus 60 étapes). */
    private fun jusquAuBout(p: Objet, profil: ProfilPc, reponse: Int = 0, garder: (EnqueteTelephone) -> Unit = {}): EnqueteTelephone {
        var et = catalogue.enqueteTelephone(p, emptyList(), profil)
        repeat(60) {
            garder(et)
            if (et.e.etape == null) return et
            et = catalogue.enqueteTelephone(p, et.choix + reponse, profil)
        }
        return et
    }

    private val pannesAvecType = catalogue.pannes.filter { p -> catalogue.etapes(p).any { it.demandeLeTypeDePc() } }

    // ---------------------------------------------------------------- le profil gardé

    @Test fun leProfilEstGardeSurLeTelephone() {
        var garde: String? = null
        val m = MemoirePc({ garde }, { garde = it })
        assertNull("première ouverture : rien de choisi", m.profil.value)
        m.choisir(portableHp)
        assertEquals(portableHp, m.profil.value)
        // (l'appli rouverte relit ce qui est gardé)
        assertEquals(portableHp, MemoirePc({ garde }, { garde = it }).profil.value)
        m.choisir(ProfilPc.Inconnu)
        val relu = MemoirePc({ garde }, { garde = it }).profil.value
        assertEquals("« Je ne sais pas » est un choix : il ne redemande pas « Ton PC »", ProfilPc.Inconnu, relu)
        m.choisir(ProfilPc(TypePc.Fixe, "ASUS"))
        assertEquals(ProfilPc(TypePc.Fixe, "ASUS"), MemoirePc({ garde }, { }).profil.value)
    }

    @Test fun unProfilIllisibleRedemandeTonPc() {
        assertNull(MemoirePc({ "pas du json" }, { }).profil.value)
        assertNull(MemoirePc({ "" }, { }).profil.value)
        // une marque inconnue est oubliée, le genre reste
        assertEquals(ProfilPc(TypePc.Portable, null), MemoirePc({ """{"Type":"portable","Marque":"Inconnue"}""" }, { }).profil.value)
    }

    // ---------------------------------------------------------------- la question du type

    @Test fun laQuestionDuTypeSeRepondToutSeuleAvecLeProfil() {
        assertTrue("des pannes posent la question du type", pannesAvecType.size >= 5)
        for ((profil, note) in listOf(portableHp to "portable", fixe to "fixe")) {
            var vue = 0
            for (p in pannesAvecType) for (r in listOf(0, 1, -1)) {
                val fin = jusquAuBout(p, profil, r) { et ->
                    assertFalse("${p.texte("Id")} : la question du type est posée avec un profil $note", et.e.etape?.demandeLeTypeDePc() == true)
                }
                // la réponse donnée toute seule est celle du profil, et ne compte pas comme une étape
                fin.e.faits.forEachIndexed { i, f ->
                    if (f.etape.demandeLeTypeDePc()) {
                        vue++
                        assertTrue(i in fin.autos)
                        assertTrue(fin.e.notes.contains(note))
                    }
                }
            }
            assertTrue("la question du type a bien été répondue toute seule ($note)", vue > 0)
        }
    }

    @Test fun sansProfilLaQuestionDuTypeEstPosee() {
        var posee = 0
        for (p in pannesAvecType) {
            jusquAuBout(p, ProfilPc.Inconnu, -1) { et -> if (et.e.etape?.demandeLeTypeDePc() == true) posee++ }
            // une marque seule ne dit rien du genre : la question est posée aussi
            jusquAuBout(p, ProfilPc(null, "HP"), -1) { et -> if (et.e.etape?.demandeLeTypeDePc() == true) posee++ }
        }
        assertTrue("« Je ne sais pas » : la question du type est posée normalement", posee > 0)
    }

    @Test fun lEtapePrecedenteSauteLaReponseDonneeToutSeule() {
        for (p in pannesAvecType) {
            jusquAuBout(p, portableHp, 0) { et ->
                if (et.e.etape == null) return@jusquAuBout
                val avant = et.reponsesAvantLaDerniere() ?: return@jusquAuBout
                val retour = catalogue.enqueteTelephone(p, avant, portableHp)
                // on revient exactement d'une étape montrée (jamais sur la question du type)
                assertEquals(et.numero - 1, retour.numero)
                assertFalse(retour.e.etape?.demandeLeTypeDePc() == true)
            }
        }
    }

    @Test fun leMoteurPartageNEstPasChange() {
        // Donner d'avance la réponse du profil revient exactement à la donner soi-même : même enquête que le moteur du PC
        for (p in pannesAvecType) {
            val et = jusquAuBout(p, portableHp, 0)
            val moteur: Enquete = catalogue.enquete(p, et.choix)
            assertEquals(moteur, et.e)
        }
    }

    // ---------------------------------------------------------------- les pistes hors sujet

    /** Des enquêtes finies sur un portable où le moteur a écarté une piste hors sujet (le bloc d'alimentation...). */
    private fun avecHorsSujet(): List<Pair<Objet, EnqueteTelephone>> = pannesAvecType.flatMap { p ->
        listOf(0, 1, -1).map { r -> p to jusquAuBout(p, portableHp, r) }
    }.filter { (_, et) -> et.e.ecartes.any { it.horsSujet() } }

    @Test fun lesPistesHorsSujetNeSAffichentJamais() {
        val cas = avecHorsSujet()
        assertTrue("le moteur écarte bien des pistes hors sujet", cas.isNotEmpty())
        for ((p, et) in cas) {
            val horsSujet = et.e.ecartes.filter { it.horsSujet() }.map { it.id }.toSet()
            assertTrue(catalogue.pistesEcartees(et.e).none { it.horsSujet() || it.id in horsSujet })
            assertTrue(catalogue.pistesPossibles(et.e).none { it in horsSujet })
            val r = catalogue.rapport(p, et.e, catalogue.titreReponse(et.e.verdict).titre, "0.2.0", portableHp)
            assertFalse(r.contains("Ne concerne pas ce type de PC"))
            for (id in horsSujet) assertFalse("${p.texte("Id")} : $id dans le rapport", r.contains("- ${catalogue.suspect(id)!!.texte("Titre")} ("))
        }
    }

    /**
     * Dès la première étape, avant la question « portable ou fixe » : sur un portable, aucune piste « Sauf portable »
     * (le bloc d'alimentation de « Il s'éteint d'un coup ») ni dans les pistes possibles ni dans les écartées.
     * Trouvé sur le téléphone virtuel : la feuille des pistes de l'étape 1 montrait le bloc d'alimentation.
     */
    @Test fun lesPistesDUnAutreGenreDePcNeSAffichentPasAvantLaQuestionDuType() {
        fun sauf(id: String) = catalogue.suspect(id)?.liste("Sauf") ?: emptyList()
        var vues = 0
        for (p in pannesAvecType) {
            jusquAuBout(p, portableHp, 1) { et ->
                assertTrue(p.texte("Id"), catalogue.pistesPossibles(et.e, portableHp).none { "portable" in sauf(it) })
                assertTrue(p.texte("Id"), catalogue.pistesEcartees(et.e, portableHp).none { "portable" in sauf(it.id) })
                if (et.e.restants.any { "portable" in sauf(it) }) vues++
            }
        }
        assertTrue("le moteur garde bien de telles pistes au début", vues > 0)
        // « Il s'éteint d'un coup », étape 1 : le cas vu à l'écran
        val seteint = catalogue.pannes.first { it.texte("Id") == "seteint" }
        val debut = catalogue.enqueteTelephone(seteint, emptyList(), portableHp)
        assertTrue("alimentation" in debut.e.restants)
        assertFalse("alimentation" in catalogue.pistesPossibles(debut.e, portableHp))
        assertTrue("sur un PC fixe, elle reste", "alimentation" in catalogue.pistesPossibles(debut.e, fixe))
    }

    // ---------------------------------------------------------------- le rapport

    @Test fun leRapportDitLePcChoisiEtLesVraiesPreuves() {
        val (p, et) = avecHorsSujet().first { (_, et) -> et.e.preuves.isNotEmpty() && catalogue.pistesEcartees(et.e).isNotEmpty() }
        val r = catalogue.rapport(p, et.e, catalogue.titreReponse(et.e.verdict).titre, "0.2.0", portableHp)
        val lignes = r.lines()
        val i = lignes.indexOf("Panne : ${p.texte("Titre")}")
        assertTrue(r, i > 0)
        assertEquals("la ligne du PC choisi juste après la panne", "PC choisi : Portable HP", lignes[i + 1])
        for (preuve in et.e.preuves) assertTrue("la preuve « $preuve » est dans le rapport", lignes.contains("- $preuve"))
        for (x in catalogue.pistesEcartees(et.e)) assertTrue(r.contains("- ${catalogue.suspect(x.id)!!.texte("Titre")}"))
        assertTrue(r.contains("Écartés :") && r.contains("Les réponses :"))
        // texte simple pour un SMS : aucun tiret long
        assertFalse(r.contains(Char(0x2014)) || r.contains(Char(0x2013)))
    }

    /**
     * « Ton PC, c'est un ordinateur portable ou un PC fixe ? », répondue toute seule par « Ton PC », n'est ni dans
     * « Les réponses » du rapport, ni dans les réponses montrées (« Tes réponses », « Les questions et tes réponses ») :
     * la ligne « PC choisi » le dit déjà. Sans profil, la question est posée, et elle y est.
     */
    @Test fun laQuestionDuTypeRepondueToutSeuleNEstPasDansLesReponses() {
        var vus = 0
        for (p in pannesAvecType) for (r in listOf(0, 1)) {
            val et = jusquAuBout(p, portableHp, r)
            val iType = et.e.faits.indexOfFirst { it.etape.demandeLeTypeDePc() }
            if (iType < 0) continue
            vus++
            assertTrue("${p.texte("Id")} : réponse donnée toute seule", iType in et.autos)
            assertTrue(et.faitsMontres.none { it.value.etape.demandeLeTypeDePc() })
            val rapport = catalogue.rapport(p, et, catalogue.titreReponse(et.e.verdict).titre, "0.2.0", portableHp)
            val reponses = rapport.substringAfter("Les réponses :")
            assertFalse("${p.texte("Id")} : la question du type dans le rapport\n$rapport", reponses.contains(et.e.faits[iType].texte))
            assertTrue(rapport.contains("PC choisi : Portable HP"))
            // les autres réponses y sont toutes
            for ((_, f) in et.faitsMontres) assertTrue(reponses.contains("- ${f.texte}"))
        }
        assertTrue("des enquêtes passent par la question du type", vus >= 5)
        // Sans profil : posée par la personne, elle est dans le rapport
        val sansProfil = pannesAvecType.map { jusquAuBout(it, ProfilPc.Inconnu, 0) to it }
            .first { (et, _) -> et.e.faits.any { f -> f.etape.demandeLeTypeDePc() } }
        val (et, p) = sansProfil
        val type = et.e.faits.first { it.etape.demandeLeTypeDePc() }
        val rapport = catalogue.rapport(p, et, catalogue.titreReponse(et.e.verdict).titre, "0.2.0", ProfilPc.Inconnu)
        assertTrue(rapport.substringAfter("Les réponses :").contains("- ${type.texte}"))
    }

    @Test fun sansProfilConnuPasDeLignePcChoisi() {
        val p = catalogue.panne("wificoupe")!!
        val e = catalogue.enqueteTelephone(p, listOf(1, 1, 0)).e
        assertFalse(catalogue.rapport(p, e, "x", "0.2.0", ProfilPc.Inconnu).contains("PC choisi"))
        assertFalse(catalogue.rapport(p, e, "x", "0.2.0").contains("PC choisi"))
    }

    // ---------------------------------------------------------------- l'affichage

    @Test fun leGesteCopierEstReecritPourLeTelephone() {
        val gestes = (catalogue.pannes.flatMap { p -> catalogue.etapes(p) }).flatMap { it.liste("Gestes") }.distinct()
        val copier = gestes.filter { it.contains("« Copier »") }
        assertTrue(copier.isNotEmpty())
        for (g in copier) {
            val t = gestePourTelephone(g)
            assertFalse(t, t.contains("Copier") || t.contains("clic droit (ça colle)"))
            assertTrue(t, t.startsWith(GesteTaperCommande))
            // la suite du geste est gardée telle quelle
            assertTrue(t, t.endsWith(g.substringAfter("touche Entrée.")))
        }
        // les autres gestes ne changent pas
        for (g in gestes - copier.toSet()) assertEquals(g, gestePourTelephone(g))
    }

    @Test fun laBandeAdapteDitCeQuiEstCache() {
        val toutes = catalogue.pannes
        val cachees = toutes.filter { !it.concerne(portableHp) }
        val phrase = phraseCachees(portableHp, cachees)!!
        assertTrue(phrase, phrase.startsWith("Adapté à ton portable HP : ${cachees.size} pannes de PC fixe"))
        assertEquals("Adapté à ton PC fixe : 1 panne de PC portable est cachée.",
            phraseCachees(fixe, listOf(toutes.first { !it.concerne(fixe) })))
        assertNull(phraseCachees(portableHp, emptyList()))
    }
}
