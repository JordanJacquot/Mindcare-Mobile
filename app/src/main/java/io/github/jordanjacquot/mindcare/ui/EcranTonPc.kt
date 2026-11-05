package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.donnees.MarquesCourantes
import io.github.jordanjacquot.mindcare.moteur.Marques
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc
import io.github.jordanjacquot.mindcare.ui.theme.BarreHaut
import io.github.jordanjacquot.mindcare.ui.theme.Bouton
import io.github.jordanjacquot.mindcare.ui.theme.CarreIcone
import io.github.jordanjacquot.mindcare.ui.theme.Ic
import io.github.jordanjacquot.mindcare.ui.theme.Icone
import io.github.jordanjacquot.mindcare.ui.theme.mc

// (les valeurs gardées pendant le choix, qui survivent à la rotation : « portable », « fixe », « nsp » ; une marque ou
// « nsp »)
private const val Nsp = "nsp"

/**
 * « Ton PC » (écran 2 de la maquette) : à la première ouverture, puis en touchant la pastille en haut. Le téléphone ne
 * voit pas le PC : on demande une fois le genre de PC et sa marque, pour montrer seulement les pannes qui peuvent lui
 * arriver. `retour` null : la première ouverture (pas de retour, rien n'est encore choisi).
 */
@Composable
fun EcranTonPc(depart: ProfilPc?, valider: (ProfilPc) -> Unit, retour: (() -> Unit)?) {
    val c = mc
    var genre by rememberSaveable { mutableStateOf(depart?.let { it.type?.nom ?: Nsp } ?: "") }
    var marque by rememberSaveable { mutableStateOf(depart?.marque ?: Nsp) }
    var toutes by rememberSaveable { mutableStateOf(marque != Nsp && marque !in MarquesCourantes) }

    EcranTelephone(
        barre = {
            if (retour == null) BarreHaut("Mindcare", avecLogo = true)
            else BarreHaut("Ton PC", retour = retour)
        },
        pied = {
            Bouton(if (retour == null) "C'est parti" else "C'est bon", {
                valider(ProfilPc(TypePc.depuis(genre), marque.takeIf { it != Nsp }))
            }, actif = genre.isNotEmpty())
            Text(
                if (genre.isEmpty()) "Choisis d'abord le genre de PC (ou « Je ne sais pas »)." else "Tu pourras changer ça en touchant « Ton PC » en haut.",
                fontSize = 13.sp, color = c.discret, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CarreIcone { Icone(Ic.Portable, c.p, taille = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Ton PC", fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
                Text("Pour te montrer seulement les pannes qui peuvent arriver à ton PC.", fontSize = 15.sp, lineHeight = 21.sp, color = c.discret, modifier = Modifier.padding(top = 4.dp))
            }
        }

        TitreQuestion(1, "C'est quel genre de PC ?")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionGenre(Ic.Portable, "Un portable", "Il se replie, avec une batterie", genre == TypePc.Portable.nom) { genre = TypePc.Portable.nom }
            OptionGenre(Ic.Fixe, "Un PC fixe", "Une tour, ou un écran tout en un", genre == TypePc.Fixe.nom) { genre = TypePc.Fixe.nom }
            OptionGenre(Ic.NeSaisPas, "Je ne sais pas", "Toutes les pannes seront montrées", genre == Nsp) { genre = Nsp }
        }

        TitreQuestion(2, "Quelle est sa marque ?")
        Text("Elle est écrite sur le PC, souvent sous l'écran, sur le dessus ou sur une étiquette dessous.", fontSize = 13.sp, lineHeight = 19.sp, color = c.discret)
        val liste = if (toutes) MarquesCourantes + Marques.filter { it !in MarquesCourantes } else MarquesCourantes
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            liste.chunked(3).forEach { ligne ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ligne.forEach { m -> BoutonMarque(m, marque == m, Modifier.weight(1f)) { marque = m } }
                    repeat(3 - ligne.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (!toutes) BoutonMarque("Une autre marque", false, Modifier.fillMaxWidth(), couleur = c.p) { toutes = true }
            BoutonMarque("Je ne sais pas", marque == Nsp, Modifier.fillMaxWidth(), couleur = c.discret) { marque = Nsp }
        }
    }
}

@Composable
private fun TitreQuestion(numero: Int, texte: String) {
    val c = mc
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp).background(Brush.linearGradient(listOf(c.p, c.s)), CircleShape)) {
            Text("$numero", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.encre)
        }
        Spacer(Modifier.width(8.dp))
        Text(texte, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
    }
}

/** Un choix du genre de PC : son icône, son nom, une phrase, et le rond coché. */
@Composable
private fun OptionGenre(icone: Ic, titre: String, petit: String, choisi: Boolean, onClick: () -> Unit) {
    val c = mc
    val forme = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(forme)
            .background(if (choisi) c.p.copy(alpha = 0.10f) else c.surface)
            .border(if (choisi) 1.5.dp else 1.dp, if (choisi) c.p.copy(alpha = 0.7f) else c.bord, forme)
            .clickable(onClick = onClick).semantics { role = Role.RadioButton; selected = choisi }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        CarreIcone { Icone(icone, c.p, taille = 22.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titre, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.texte)
            Text(petit, fontSize = 13.5.sp, lineHeight = 18.sp, color = c.discret, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp).border(2.dp, if (choisi) c.p else c.tresDiscret, CircleShape)) {
            if (choisi) Box(Modifier.size(12.dp).background(c.p, CircleShape))
        }
    }
}

/** Un bouton de marque (48 dp) ; choisi, il passe en dégradé. */
@Composable
private fun BoutonMarque(texte: String, choisi: Boolean, modifier: Modifier, couleur: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val c = mc
    val forme = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = 48.dp).clip(forme)
            .then(if (choisi) Modifier.background(Brush.linearGradient(listOf(c.p, c.s))) else Modifier.background(c.surface).border(1.dp, c.bord, forme))
            .clickable(onClick = onClick).semantics { selected = choisi }
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Text(texte, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = if (choisi) c.encre else couleur ?: c.texte)
    }
}
