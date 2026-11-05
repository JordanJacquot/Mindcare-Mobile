package io.github.jordanjacquot.mindcare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jordanjacquot.mindcare.MindcareApp
import io.github.jordanjacquot.mindcare.donnees.Diagnostic
import io.github.jordanjacquot.mindcare.donnees.Historique
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.reponsesAvantLaDerniere
import io.github.jordanjacquot.mindcare.moteur.titreReponse

/**
 * Les écrans et le passage de l'un à l'autre (la maquette validée par Jordan) : « Ton PC » à la première ouverture,
 * l'accueil, une famille, « Raconte ton problème », l'enquête (une étape à la fois), la réponse et la page pour le
 * réparateur. L'état tient en quelques valeurs simples : il survit à la rotation de l'écran. Le retour du téléphone
 * marche partout : il ramène toujours à l'écran d'avant.
 */
@Composable
fun MindcareNavigation(themeId: String, choisirTheme: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as MindcareApp
    val catalogue by app.majCatalogue.catalogue.collectAsStateWithLifecycle()
    val profilGarde by app.tonPc.profil.collectAsStateWithLifecycle()
    var famille by rememberSaveable { mutableStateOf("") }
    var rechercheOuverte by rememberSaveable { mutableStateOf(false) }
    var recherche by rememberSaveable { mutableStateOf("") }
    // « Tout afficher » : pour cette fois seulement (oublié en revenant à l'accueil, jamais gardé)
    var toutAfficher by rememberSaveable { mutableStateOf(false) }
    var panneId by rememberSaveable { mutableStateOf("") }
    var choix by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var diagId by rememberSaveable { mutableStateOf("") }
    var reparateur by rememberSaveable { mutableStateOf(false) }
    var tonPcOuvert by rememberSaveable { mutableStateOf(false) }

    // « Ton PC » : demandé à la première ouverture, puis en touchant la pastille de la barre du haut
    if (profilGarde == null || tonPcOuvert) {
        BackHandler(enabled = profilGarde != null) { tonPcOuvert = false }
        EcranTonPc(
            depart = profilGarde,
            valider = { p -> app.tonPc.choisir(p); tonPcOuvert = false; toutAfficher = false },
            retour = if (profilGarde == null) null else ({ tonPcOuvert = false }),
        )
        return
    }
    val profil = profilGarde ?: ProfilPc.Inconnu
    val ouvrirPc = { tonPcOuvert = true }
    val panne = if (panneId.isEmpty()) null else catalogue.panne(panneId)

    fun ouvrirPanne(id: String) { panneId = id; choix = emptyList(); diagId = Historique.nouvelId(); reparateur = false }
    fun reprendre(d: Diagnostic) { famille = ""; rechercheOuverte = false; panneId = d.Panne; choix = d.Choix; diagId = d.Id; reparateur = false }
    fun quitterEnquete() { panneId = ""; choix = emptyList(); reparateur = false }
    fun accueil() { quitterEnquete(); famille = ""; rechercheOuverte = false; recherche = ""; toutAfficher = false }
    fun fermerRecherche() { rechercheOuverte = false; if (famille.isEmpty()) toutAfficher = false }
    fun fermerFamille() { famille = ""; toutAfficher = false }

    if (panne == null) {
        when {
            rechercheOuverte -> {
                BackHandler { fermerRecherche() }
                EcranRecherche(
                    catalogue = catalogue, texte = recherche, changerTexte = { recherche = it },
                    profil = profil, toutAfficher = toutAfficher, basculerTout = { toutAfficher = !toutAfficher },
                    ouvrirPanne = ::ouvrirPanne, retour = ::fermerRecherche, ouvrirPc = ouvrirPc,
                )
            }
            famille.isNotEmpty() && catalogue.famille(famille) != null -> {
                BackHandler { fermerFamille() }
                EcranFamille(
                    catalogue = catalogue, famille = famille, profil = profil,
                    toutAfficher = toutAfficher, basculerTout = { toutAfficher = !toutAfficher },
                    ouvrirPanne = ::ouvrirPanne, ouvrirRecherche = { rechercheOuverte = true },
                    retour = ::fermerFamille, ouvrirPc = ouvrirPc,
                )
            }
            else -> Accueil(
                catalogue = catalogue, profil = profil, ouvrirPc = ouvrirPc,
                ouvrirFamille = { famille = it; toutAfficher = false },
                ouvrirRecherche = { rechercheOuverte = true },
                reprendre = ::reprendre,
                themeId = themeId, choisirTheme = choisirTheme,
            )
        }
        return
    }

    val et = catalogue.enqueteTelephone(panne, choix, profil)
    // L'enquête est gardée à chaque réponse ; une enquête finie le reste tant qu'une nouvelle réponse ne la remplace pas,
    // et une enquête commencée le reste même si le retour retire toutes ses réponses (Historique.garderEnCours)
    LaunchedEffect(diagId, et.choix) {
        val v = et.e.verdict
        if (v != null) app.historique.enregistrer(diagId, panneId, et.choix, true, v.suspect, catalogue.titreReponse(v).titre)
        else app.historique.garderEnCours(diagId, panneId, et.choix)
    }
    // Revenir d'une étape (les réponses données toutes seules se refont toutes seules)
    fun precedente() {
        val c = et.reponsesAvantLaDerniere()
        if (c == null) quitterEnquete() else choix = c
    }
    val version = "${app.maj.versionActuelle} (catalogue ${app.majCatalogue.edition})"
    when {
        et.e.etape != null -> {
            BackHandler { precedente() }
            EcranEnquete(
                catalogue = catalogue, panne = panne, et = et, profil = profil, ouvrirPc = ouvrirPc,
                repondre = { n -> choix = et.choix + n },
                changerReponse = { k -> choix = et.choix.take(k) },
                precedente = ::precedente,
                plusTard = ::accueil,
            )
        }
        reparateur -> {
            BackHandler { reparateur = false }
            EcranReparateur(
                catalogue = catalogue, panne = panne, et = et, profil = profil, version = version,
                date = app.historique.trouver(diagId)?.Maj, ouvrirPc = ouvrirPc, retour = { reparateur = false },
            )
        }
        else -> {
            BackHandler { quitterEnquete() }
            EcranReponse(
                catalogue = catalogue, panne = panne, et = et, profil = profil, ouvrirPc = ouvrirPc,
                pourLeReparateur = { reparateur = true },
                changerDerniere = ::precedente,
                retour = ::quitterEnquete,
                accueil = ::accueil,
            )
        }
    }
}
