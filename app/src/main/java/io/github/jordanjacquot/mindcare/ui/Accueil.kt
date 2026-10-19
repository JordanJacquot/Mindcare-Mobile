package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jordanjacquot.mindcare.maj.BandeauMiseAJour
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.chercher
import io.github.jordanjacquot.mindcare.ui.theme.BoutonPilule
import io.github.jordanjacquot.mindcare.ui.theme.Etiquette
import io.github.jordanjacquot.mindcare.ui.theme.LogoMindcare
import io.github.jordanjacquot.mindcare.ui.theme.Themes
import io.github.jordanjacquot.mindcare.ui.theme.Verre
import io.github.jordanjacquot.mindcare.ui.theme.mc

/**
 * L'accueil, en questions guidées comme sur PC : « 1. Ça concerne quoi ? » (les familles), puis « 2. Qu'est-ce qui se
 * passe ? » (les pannes de la famille). Tout en haut, on peut aussi écrire son problème avec ses mots.
 */
@Composable
fun Accueil(
    catalogue: Catalogue,
    famille: String,
    choisirFamille: (String) -> Unit,
    ouvrirPanne: (String) -> Unit,
    themeId: String,
    choisirTheme: (String) -> Unit,
) {
    val c = mc
    var recherche by rememberSaveable { mutableStateOf("") }
    var themesOuverts by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMindcare(52.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Mindcare", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Trouve pourquoi ton PC est en panne, en répondant à quelques questions simples.", style = MaterialTheme.typography.bodySmall, color = c.discret)
            }
        }
        BandeauMiseAJour()
        OutlinedTextField(
            value = recherche, onValueChange = { recherche = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = c.p) },
            placeholder = { Text("Ou écris ton problème : « bip », « le son grésille »...", color = c.tresDiscret) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.p.copy(alpha = 0.7f), unfocusedBorderColor = c.p.copy(alpha = 0.35f),
                focusedContainerColor = Color.White.copy(alpha = 0.06f), unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                cursorColor = c.p, focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            recherche.isNotBlank() -> {
                val trouvees = catalogue.chercher(recherche).take(10)
                if (trouvees.isEmpty()) Text("Rien ne correspond à « ${recherche.trim()} ». Essaie d'autres mots, ou efface pour répondre aux questions.", color = c.discret)
                else {
                    Text(if (trouvees.size == 1) "Une panne correspond :" else "${trouvees.size} pannes correspondent :", style = MaterialTheme.typography.bodySmall, color = c.discret)
                    trouvees.forEach { p -> CartePanne(catalogue, p, avecFamille = true) { ouvrirPanne(p.texte("Id")) } }
                }
            }
            famille.isNotEmpty() && catalogue.famille(famille) != null -> {
                val f = catalogue.famille(famille)!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1. Ça concerne : ${f.texte("Titre")}", color = c.discret, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    BoutonPilule("Changer", { choisirFamille("") })
                }
                Text("2. Qu'est-ce qui se passe ?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                catalogue.pannes.filter { it.texte("Famille") == famille }.forEach { p -> CartePanne(catalogue, p) { ouvrirPanne(p.texte("Id")) } }
                Text("Rien ne ressemble ? Écris ton problème tout en haut, ou change de famille.", style = MaterialTheme.typography.bodySmall, color = c.discret)
            }
            else -> {
                Text("1. Ça concerne quoi ?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                catalogue.familles.chunked(2).forEach { ligne ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                        ligne.forEach { f ->
                            Verre(Modifier.weight(1f).fillMaxHeight(), onClick = { choisirFamille(f.texte("Id")) }, interieur = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                                Text(f.texte("Titre"), fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(f.texte("Phrase"), style = MaterialTheme.typography.bodySmall, color = c.discret, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                        if (ligne.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Le thème (les 5 de la suite), replié par défaut
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { themesOuverts = !themesOuverts }.padding(vertical = 4.dp)) {
            Etiquette("Thème : ${(Themes.firstOrNull { it.id == themeId } ?: Themes.first()).nom}", c.discret)
            Text(if (themesOuverts) "  ▴" else "  ▾", color = c.discret)
        }
        if (themesOuverts) Verre(Modifier.fillMaxWidth()) {
            Themes.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { choisirTheme(t.id) }.padding(vertical = 8.dp)) {
                    Box(Modifier.size(22.dp).background(Brush.linearGradient(listOf(Color(t.p), Color(t.s), Color(t.t))), CircleShape)
                        .then(if (t.id == themeId) Modifier.border(2.dp, Color.White, CircleShape) else Modifier))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.nom, fontWeight = if (t.id == themeId) FontWeight.Bold else FontWeight.Normal, color = c.texte)
                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = c.discret)
                    }
                }
            }
        }
    }
}

/** Une panne à choisir : son nom et sa phrase (et sa famille, dans les résultats d'une recherche). */
@Composable
private fun CartePanne(catalogue: Catalogue, p: Objet, avecFamille: Boolean = false, onClick: () -> Unit) {
    Verre(Modifier.fillMaxWidth(), onClick = onClick, interieur = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        if (avecFamille) Etiquette(catalogue.famille(p.texte("Famille"))?.texte("Titre") ?: "", mc.p, Modifier.padding(bottom = 3.dp))
        Text(p.texte("Titre"), fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(p.texte("Phrase"), style = MaterialTheme.typography.bodySmall, color = mc.discret, modifier = Modifier.padding(top = 3.dp))
    }
}
