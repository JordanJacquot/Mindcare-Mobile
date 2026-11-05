package io.github.jordanjacquot.mindcare.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.donnees.formaterDate
import io.github.jordanjacquot.mindcare.donnees.rapport
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.EnqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.demandeSiCePc
import io.github.jordanjacquot.mindcare.moteur.pistesEcartees
import io.github.jordanjacquot.mindcare.moteur.titreReponse
import io.github.jordanjacquot.mindcare.ui.theme.BarreHaut
import io.github.jordanjacquot.mindcare.ui.theme.Bouton
import io.github.jordanjacquot.mindcare.ui.theme.Groupe
import io.github.jordanjacquot.mindcare.ui.theme.Ic
import io.github.jordanjacquot.mindcare.ui.theme.Icone
import io.github.jordanjacquot.mindcare.ui.theme.Separation
import io.github.jordanjacquot.mindcare.ui.theme.TitreBloc
import io.github.jordanjacquot.mindcare.ui.theme.mc
import io.github.jordanjacquot.mindcare.ui.theme.plasma
import java.time.LocalDateTime

/**
 * Pour le réparateur (écran 9 de la maquette) : tout ce que Mindcare a vu, en une colonne (la panne et la réponse, le
 * PC choisi, ce qui est sûr, ce qui est écarté et pourquoi, les questions et les réponses), et un seul bouton
 * « Partager » en bas : le menu de partage d'Android, en texte simple (SMS, WhatsApp, e-mail...).
 */
@Composable
fun EcranReparateur(
    catalogue: Catalogue,
    panne: Objet,
    et: EnqueteTelephone,
    profil: ProfilPc,
    version: String,
    date: String?,
    ouvrirPc: () -> Unit,
    retour: () -> Unit,
) {
    val c = mc
    val contexte = LocalContext.current
    val e = et.e
    val v = e.verdict ?: return
    val titre = catalogue.titreReponse(v).titre
    val ecartees = catalogue.pistesEcartees(e, profil)

    EcranTelephone(
        barre = { BarreHaut("Pour le réparateur", retour = retour, profil = profil, ouvrirPc = ouvrirPc) },
        pied = {
            Bouton("Partager", {
                val texte = catalogue.rapport(panne, e, titre, version, profil)
                contexte.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Mindcare : ${panne.texte("Titre")}").putExtra(Intent.EXTRA_TEXT, texte),
                    "Envoyer le rapport"))
            }, icone = Ic.Partage)
        },
    ) {
        Text("Tout ce que Mindcare a vu, pour qu'un réparateur gagne du temps (et toi de l'argent).", fontSize = 15.sp, lineHeight = 22.sp, color = c.discret)
        val forme = RoundedCornerShape(20.dp)
        Column(Modifier.fillMaxWidth().plasma(forme, fort = true).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Panne : ${panne.texte("Titre")}", fontSize = 13.5.sp, color = c.discret)
            Text(titre, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, color = c.texte)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icone(Ic.Horloge, c.discret, taille = 14.dp)
                Spacer(Modifier.width(6.dp))
                Text("Enquête du ${formaterDate(date ?: LocalDateTime.now().withNano(0).toString(), "d MMMM yyyy 'à' HH'h'mm")}", fontSize = 13.sp, color = c.discret)
            }
        }

        TitreBloc("Le PC", Modifier.padding(top = 6.dp))
        Groupe(forme = RoundedCornerShape(16.dp)) {
            LigneCle("PC choisi", if (profil.inconnu) "Pas choisi" else profil.libelleCourt, c.p, gras = true)
            Separation()
            LigneCle("Lu sur le PC", "Rien : le téléphone ne lit pas le PC", c.texte)
        }

        TitreBloc("Ce qui est sûr", Modifier.padding(top = 6.dp))
        Groupe(forme = RoundedCornerShape(16.dp)) {
            if (e.preuves.isEmpty()) Text("Rien de sûr : les réponses n'ont écarté aucune piste.", fontSize = 14.5.sp, color = c.discret, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp))
            e.preuves.forEachIndexed { i, p -> if (i > 0) Separation(); LignePreuve(p) }
        }

        if (v.type == "reparateur" && v.restants.isNotEmpty()) {
            TitreBloc("Ce qui reste à vérifier", Modifier.padding(top = 6.dp))
            Groupe(forme = RoundedCornerShape(16.dp)) {
                v.restants.forEachIndexed { i, id ->
                    if (i > 0) Separation()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Icone(Ic.Aide, c.ambre, Modifier.padding(top = 1.dp), taille = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(catalogue.suspect(id)?.texte("Titre") ?: id, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = c.texte)
                    }
                }
            }
        }

        if (ecartees.isNotEmpty()) {
            TitreBloc("Ce qui est écarté, et pourquoi", Modifier.padding(top = 6.dp))
            Groupe(forme = RoundedCornerShape(16.dp)) {
                ecartees.forEachIndexed { i, x -> if (i > 0) Separation(); LigneEcartee(catalogue, x) }
            }
        }

        val faits = e.faits.filter { !it.etape.demandeSiCePc() }
        if (faits.isNotEmpty()) {
            TitreBloc("Les questions et tes réponses", Modifier.padding(top = 6.dp))
            Groupe(forme = RoundedCornerShape(16.dp)) {
                faits.forEachIndexed { i, f ->
                    if (i > 0) Separation()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Text(f.court.ifEmpty { f.texte }, fontSize = 13.sp, color = c.discret)
                        Text(f.reponse, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = c.texte)
                    }
                }
            }
        }
        Text("Rien n'est envoyé tout seul : c'est toi qui choisis à qui le montrer.", fontSize = 13.sp, lineHeight = 19.sp, color = c.discret)
    }
}

/** Une ligne « clé : valeur » (« PC choisi : Portable HP »). */
@Composable
private fun LigneCle(cle: String, valeur: String, couleur: Color, gras: Boolean = false) {
    val c = mc
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
        Text(cle, fontSize = 14.5.sp, color = c.discret, modifier = Modifier.width(104.dp))
        Text(valeur, fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = if (gras) FontWeight.ExtraBold else FontWeight.Normal, color = couleur, modifier = Modifier.weight(1f))
    }
}
