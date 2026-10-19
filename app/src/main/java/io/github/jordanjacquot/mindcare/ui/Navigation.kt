package io.github.jordanjacquot.mindcare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.jordanjacquot.mindcare.MindcareApp
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone

/**
 * Les écrans et le passage de l'un à l'autre : l'accueil (« Ça concerne quoi ? », puis « Qu'est-ce qui se passe ? »,
 * ou la recherche), l'enquête (une étape à la fois), puis la réponse. L'état tient en trois valeurs (la famille
 * choisie, la panne et les réponses données) : il survit à la rotation de l'écran.
 */
@Composable
fun MindcareNavigation(themeId: String, choisirTheme: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as MindcareApp
    val catalogue = remember { app.catalogue }
    var famille by rememberSaveable { mutableStateOf("") }
    var panneId by rememberSaveable { mutableStateOf("") }
    var choix by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val panne = if (panneId.isEmpty()) null else catalogue.panne(panneId)

    fun accueil() { panneId = ""; choix = emptyList() }

    if (panne == null) {
        BackHandler(enabled = famille.isNotEmpty()) { famille = "" }
        Accueil(
            catalogue = catalogue,
            famille = famille,
            choisirFamille = { famille = it },
            ouvrirPanne = { id -> famille = catalogue.panne(id)?.texte("Famille") ?: famille; panneId = id; choix = emptyList() },
            themeId = themeId,
            choisirTheme = choisirTheme,
        )
        return
    }

    val et = catalogue.enqueteTelephone(panne, choix)
    // Revenir d'une étape (les réponses données par le téléphone tout seul se défont avec celle d'avant)
    fun precedente() {
        var c = et.choix
        if (c.size <= et.auto) { accueil(); return }
        c = c.dropLast(1)
        while (c.isNotEmpty() && catalogue.enqueteTelephone(panne, c).choix.size > c.size) c = c.dropLast(1)
        choix = c
    }
    BackHandler { precedente() }
    if (et.e.etape != null) {
        EcranEnquete(
            catalogue = catalogue, panne = panne, et = et,
            repondre = { n -> choix = et.choix + n },
            changerReponse = { k -> choix = et.choix.take(k) },
            precedente = ::precedente,
            accueil = ::accueil,
        )
    } else {
        EcranReponse(
            catalogue = catalogue, panne = panne, e = et.e,
            changerDerniere = ::precedente,
            accueil = ::accueil,
        )
    }
}
