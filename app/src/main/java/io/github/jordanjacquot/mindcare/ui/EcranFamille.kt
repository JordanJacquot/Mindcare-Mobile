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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.donnees.ToutMontre
import io.github.jordanjacquot.mindcare.donnees.phraseCachees
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Groupe
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc
import io.github.jordanjacquot.mindcare.moteur.concerne
import io.github.jordanjacquot.mindcare.ui.theme.BarreHaut
import io.github.jordanjacquot.mindcare.ui.theme.CarreIcone
import io.github.jordanjacquot.mindcare.ui.theme.Ic
import io.github.jordanjacquot.mindcare.ui.theme.Icone
import io.github.jordanjacquot.mindcare.ui.theme.Lien
import io.github.jordanjacquot.mindcare.ui.theme.Separation
import io.github.jordanjacquot.mindcare.ui.theme.mc
import io.github.jordanjacquot.mindcare.ui.theme.plasma

/**
 * Une famille (écran 3 de la maquette) : ses pannes par groupes, en cartes dépliables l'une sous l'autre (une seule
 * ouverte à la fois, la première d'abord), triées selon « Ton PC » ; la bande « Adapté à ton PC » dit combien de
 * pannes sont cachées, et « Tout afficher » les montre pour cette fois seulement.
 */
@Composable
fun EcranFamille(
    catalogue: Catalogue,
    famille: String,
    profil: ProfilPc,
    toutAfficher: Boolean,
    basculerTout: () -> Unit,
    ouvrirPanne: (String) -> Unit,
    ouvrirRecherche: () -> Unit,
    retour: () -> Unit,
    ouvrirPc: () -> Unit,
) {
    val c = mc
    val f = catalogue.famille(famille) ?: return
    val cachees = catalogue.pannesFamille(famille).filter { !it.concerne(profil) }
    val groupes = if (toutAfficher) catalogue.groupes(famille).filter { it.pannes.isNotEmpty() } else catalogue.groupesPour(famille, profil).groupes
    var ouverte by rememberSaveable(famille, toutAfficher) { mutableStateOf(0) }

    EcranTelephone(barre = { BarreHaut(f.texte("Titre"), retour = retour, profil = profil, ouvrirPc = ouvrirPc) }) {
        Row(verticalAlignment = Alignment.Top) {
            CarreIcone { Dessin(f.texte("Icone"), Modifier.size(28.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(f.texte("Titre"), fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
                Text("Déplie la carte qui ressemble le plus", fontSize = 15.sp, lineHeight = 21.sp, color = c.discret, modifier = Modifier.padding(top = 4.dp))
            }
        }
        BandeAdapte(profil, cachees, toutAfficher, basculerTout)

        groupes.forEachIndexed { i, g ->
            CarteGroupe(f, g, ouverte == i, { ouverte = if (ouverte == i) -1 else i }, ouvrirPanne)
        }
        if (groupes.isEmpty()) {
            Text(
                if (cachees.isNotEmpty()) "Aucune panne de cette famille ne concerne ce PC. « Tout afficher » les montre quand même."
                else "Arrive bientôt : cette famille n'a pas encore de panne connue.",
                fontSize = 15.sp, color = c.discret,
            )
        }
        Lien("Pas trouvé ? Raconte ton problème avec tes mots", ouvrirRecherche, couleur = c.p, centre = false)
    }
}

/**
 * La bande « Adapté à ton PC » (rien si rien n'est caché) : la phrase, et « Tout afficher » en bouton sous le texte ;
 * après « Tout afficher » : « Toutes les pannes sont affichées » et « Adapter à mon PC ».
 */
@Composable
fun BandeAdapte(profil: ProfilPc, cachees: List<Objet>, toutAfficher: Boolean, basculerTout: () -> Unit) {
    val c = mc
    val phrase = (if (toutAfficher) ToutMontre.takeIf { cachees.isNotEmpty() } else phraseCachees(profil, cachees)) ?: return
    val forme = RoundedCornerShape(16.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().clip(forme).background(c.p.copy(alpha = 0.11f)).border(1.dp, c.p.copy(alpha = 0.30f), forme).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icone(if (profil.type == TypePc.Fixe) Ic.Fixe else Ic.Portable, c.p, Modifier.padding(top = 1.dp))
            Spacer(Modifier.width(10.dp))
            Text(phrase, fontSize = 14.5.sp, lineHeight = 20.sp, color = c.texte)
        }
        val formeB = RoundedCornerShape(22.dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).widthIn(min = 120.dp).clip(formeB).background(c.fondHaut)
                .border(1.dp, c.ligne, formeB).clickable(onClick = basculerTout).padding(horizontal = 18.dp),
        ) {
            Text(if (toutAfficher) "Adapter à mon PC" else "Tout afficher", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
        }
    }
}

/** Une carte dépliable : le dessin du groupe, son nom, son exemple, combien de pannes ; dépliée, ses pannes. */
@Composable
private fun CarteGroupe(f: Objet, g: Groupe, ouverte: Boolean, basculer: () -> Unit, ouvrirPanne: (String) -> Unit) {
    val c = mc
    val forme = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth()
            .then(if (ouverte) Modifier.plasma(forme, fort = true) else Modifier.background(c.surface, forme).border(1.dp, c.bord, forme))
            .clip(forme),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = basculer).padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            CarreIcone { Dessin(g.icone.ifEmpty { f.texte("Icone") }, Modifier.size(30.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.nom, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = c.texte)
                if (g.exemple.isNotEmpty()) Text(g.exemple, fontSize = 13.sp, lineHeight = 18.sp, color = c.discret, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = 26.dp).widthIn(min = 26.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2).padding(horizontal = 8.dp)) {
                Text("${g.pannes.size}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.discret)
            }
            Spacer(Modifier.width(6.dp))
            Icone(if (ouverte) Ic.ChevronHaut else Ic.ChevronBas, c.p)
        }
        if (ouverte) {
            g.pannes.forEach { p ->
                Separation()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { ouvrirPanne(p.texte("Id")) }.padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                ) {
                    Text(p.texte("Titre"), fontSize = 15.5.sp, lineHeight = 21.sp, color = c.texte, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    Icone(Ic.Chevron, c.p, taille = 18.dp)
                }
            }
        }
    }
}
