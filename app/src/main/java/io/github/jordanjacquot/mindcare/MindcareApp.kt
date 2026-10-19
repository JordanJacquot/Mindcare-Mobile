package io.github.jordanjacquot.mindcare

import android.app.Application
import io.github.jordanjacquot.mindcare.donnees.Historique
import io.github.jordanjacquot.mindcare.maj.MiseAJour
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** L'application : crée une seule fois ce que tous les écrans partagent. */
class MindcareApp : Application() {
    /** Ce qui doit continuer même quand on change d'écran (téléchargements). */
    val portee = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Les mises à jour de Mindcare (le bandeau de l'accueil). */
    val maj: MiseAJour by lazy { MiseAJour(this, portee) }

    /** Le catalogue de pannes : celui de l'appli (recopié de Mindcare PC). */
    val catalogue: Catalogue by lazy { Catalogue.lire(assets.open("catalogue/enquetes.json").bufferedReader().use { it.readText() }) }

    /** Les 10 dernières enquêtes (diagnostics.json, sur le téléphone). */
    val historique: Historique by lazy { Historique(java.io.File(filesDir, "diagnostics.json")) }

    /** Le thème choisi : neon, crepuscule, terminal, arcade ou rubis (les 5 de la suite). */
    private val _theme by lazy { MutableStateFlow(getSharedPreferences("apparence", MODE_PRIVATE).getString("theme", "neon") ?: "neon") }
    val theme: StateFlow<String> get() = _theme
    fun choisirTheme(id: String) {
        _theme.value = id
        getSharedPreferences("apparence", MODE_PRIVATE).edit().putString("theme", id).apply()
    }

    override fun onCreate() {
        super.onCreate()
        // Une nouvelle version ? (au plus une fois par jour, sans gêner le lancement)
        portee.launch { delay(5_000); maj.verifierSiBesoin() }
    }
}
