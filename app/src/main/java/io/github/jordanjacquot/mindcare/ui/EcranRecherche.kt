package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.chercher
import io.github.jordanjacquot.mindcare.moteur.concerne
import io.github.jordanjacquot.mindcare.ui.theme.BarreHaut
import io.github.jordanjacquot.mindcare.ui.theme.Groupe
import io.github.jordanjacquot.mindcare.ui.theme.Ic
import io.github.jordanjacquot.mindcare.ui.theme.Icone
import io.github.jordanjacquot.mindcare.ui.theme.LigneListe
import io.github.jordanjacquot.mindcare.ui.theme.Separation
import io.github.jordanjacquot.mindcare.ui.theme.TitreBloc
import io.github.jordanjacquot.mindcare.ui.theme.mc
import io.github.jordanjacquot.mindcare.ui.theme.plasma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Au plus autant de résultats (les plus proches d'abord). */
private const val MaxResultats = 15

/**
 * « Raconte ton problème » (écran 4 de la maquette) : le champ, le rappel du micro du clavier, et les pannes qui
 * correspondent, pendant qu'on tape, en lignes pleine largeur (titre, phrase, famille), triées selon « Ton PC ».
 */
@Composable
fun EcranRecherche(
    catalogue: Catalogue,
    texte: String,
    changerTexte: (String) -> Unit,
    profil: ProfilPc,
    toutAfficher: Boolean,
    basculerTout: () -> Unit,
    ouvrirPanne: (String) -> Unit,
    retour: () -> Unit,
    ouvrirPc: () -> Unit,
) {
    val c = mc
    val focus = remember { FocusRequester() }
    val clavier = LocalFocusManager.current
    LaunchedEffect(Unit) { if (texte.isEmpty()) runCatching { focus.requestFocus() } }

    EcranTelephone(barre = { BarreHaut("Ton problème", retour = retour, profil = profil, ouvrirPc = ouvrirPc) }) {
        val forme = RoundedCornerShape(18.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).plasma(forme, fort = true).clip(forme).padding(start = 16.dp, end = 4.dp),
        ) {
            Icone(Ic.Loupe, c.p)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                if (texte.isEmpty()) Text("Par exemple : « bip », « écran bleu »", fontSize = 16.sp, color = c.tresDiscret, maxLines = 1)
                BasicTextField(
                    value = texte, onValueChange = changerTexte, singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, color = c.texte, fontFamily = androidx.compose.material3.MaterialTheme.typography.bodyLarge.fontFamily),
                    cursorBrush = SolidColor(c.p),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { clavier.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Raconte ton problème" },
                )
            }
            if (texte.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable { changerTexte(""); runCatching { focus.requestFocus() } }
                        .semantics { contentDescription = "Effacer" },
                ) { Icone(Ic.Croix, c.discret) }
            }
        }
        LigneDicter()

        if (texte.isBlank()) {
            Text("Écris ce qui se passe avec tes mots : « il ne s'allume plus », « pas de son », « écran bleu »...", fontSize = 15.sp, lineHeight = 21.sp, color = c.discret)
            return@EcranTelephone
        }
        // La recherche tourne hors du fil de l'écran : faite à chaque lettre pendant l'affichage, elle le bloquait
        // (0,6 s par lettre sur le téléphone virtuel) et des lettres tapées vite se perdaient dans le champ.
        // Pendant qu'elle cherche, les résultats de la lettre d'avant restent affichés.
        val resultat by produceState<Pair<String, List<Objet>>?>(null, catalogue, texte) {
            value = texte to withContext(Dispatchers.Default) { catalogue.chercher(texte) }
        }
        val (cherche, trouvees) = resultat ?: return@EcranTelephone
        val cachees = trouvees.filter { !it.concerne(profil) }
        val montrees = (if (toutAfficher) trouvees else trouvees.filter { it.concerne(profil) }).take(MaxResultats)
        BandeAdapte(profil, cachees, toutAfficher, basculerTout)
        if (montrees.isEmpty()) {
            Text("Rien ne correspond à « ${cherche.trim()} ». Essaie d'autres mots, ou choisis ce qui est concerné dans « Ça concerne quoi ? » à l'accueil.",
                fontSize = 15.sp, lineHeight = 21.sp, color = c.discret)
            return@EcranTelephone
        }
        TitreBloc(if (montrees.size == 1) "1 panne correspond" else "${montrees.size} pannes correspondent", Modifier.padding(top = 6.dp), droite = "Les plus proches d'abord")
        Groupe {
            montrees.forEachIndexed { i, p ->
                if (i > 0) Separation()
                LigneListe(p.texte("Titre"), { ouvrirPanne(p.texte("Id")) }, petit = p.texte("Phrase"),
                    etiquette = catalogue.famille(p.texte("Famille"))?.texte("Titre"))
            }
        }
        Text("Pas trouvé ? Essaie d'autres mots, ou choisis ce qui est concerné dans « Ça concerne quoi ? » à l'accueil.", fontSize = 13.sp, lineHeight = 19.sp, color = c.discret)
    }
}
