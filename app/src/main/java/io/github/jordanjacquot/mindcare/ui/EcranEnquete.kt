package io.github.jordanjacquot.mindcare.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.EnqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.surLeMateriel
import io.github.jordanjacquot.mindcare.ui.theme.BoutonPilule
import io.github.jordanjacquot.mindcare.ui.theme.Etiquette
import io.github.jordanjacquot.mindcare.ui.theme.Verre
import io.github.jordanjacquot.mindcare.ui.theme.mc

/**
 * Une étape de l'enquête : en haut, l'avancement (« Étape 2 sur 7 ») et le bandeau des suspects, qui se déplie ; en
 * dessous, la question ou la manipulation (gestes à cocher, commande à taper sur le PC), puis les réponses.
 */
@Composable
fun EcranEnquete(
    catalogue: Catalogue,
    panne: Objet,
    et: EnqueteTelephone,
    repondre: (Int) -> Unit,
    changerReponse: (Int) -> Unit,
    precedente: () -> Unit,
    accueil: () -> Unit,
) {
    val c = mc
    val e = et.e
    val etape = e.etape ?: return
    val test = etape.texte("Type") == "test"
    val faits = e.faits.size - et.auto
    val n = faits + 1
    val total = maxOf(n, n + e.aVenir)
    var suspectsOuverts by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        // En haut : retour, le nom de la panne, l'accueil ; l'avancement ; les suspects
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = precedente) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Étape précédente", tint = c.texte) }
            Text(panne.texte("Titre"), color = c.discret, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = accueil) { Icon(Icons.Outlined.Home, "Accueil", tint = c.texte) }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Etiquette("Étape $n sur $total", c.discret)
            LinearProgressIndicator(progress = { n.toFloat() / total }, color = c.p, trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp))
            BandeauSuspects(catalogue, panne, et, suspectsOuverts, { suspectsOuverts = !suspectsOuverts }, changerReponse)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val restants = e.restants.count { catalogue.suspect(it)?.vrai("Urgent") != true }
            val duree = etape.texte("Duree")
            Etiquette(
                when { test -> "Une manipulation à faire" + (if (duree.isNotEmpty()) " ($duree)" else ""); restants > 1 -> "Il reste $restants suspects"; else -> "Une dernière vérification" },
                if (test) c.t else c.p,
            )
            Text(etape.texte("Texte"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (etape.texte("Aide").isNotEmpty()) Text(etape.texte("Aide"), color = c.discret)
            if (test) {
                Text("Coche chaque geste une fois fait :", style = MaterialTheme.typography.bodySmall, color = c.discret)
                Gestes(etape)
                if (etape.surLeMateriel()) Text(catalogue.securite.firstOrNull() ?: "", color = c.ambre, style = MaterialTheme.typography.bodySmall)
                Text("Ensuite, dis à Mindcare ce que ça a donné :", style = MaterialTheme.typography.bodySmall, color = c.discret, modifier = Modifier.padding(top = 6.dp))
            }
            etape.objets("Reponses").forEachIndexed { i, r ->
                Verre(Modifier.fillMaxWidth(), onClick = { repondre(i) }, interieur = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(r.texte("Texte"), color = Color.White)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoutonPilule("Je ne sais pas", { repondre(-1) })
                if (test) BoutonPilule("Je le ferai plus tard", accueil)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** « Il reste 6 suspects ▾ » : un appui déplie la liste (les écartés barrés) et les réponses déjà données. */
@Composable
private fun BandeauSuspects(catalogue: Catalogue, panne: Objet, et: EnqueteTelephone, ouvert: Boolean, basculer: () -> Unit, changerReponse: (Int) -> Unit) {
    val c = mc
    val e = et.e
    val montres = panne.liste("Suspects").filter { catalogue.suspect(it)?.vrai("Urgent") != true }
    val restants = montres.count { it in e.restants }
    Verre(Modifier.fillMaxWidth(), onClick = basculer, interieur = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (restants > 1) "Il reste $restants suspects" else "Il reste 1 suspect", color = c.texte, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (ouvert) "▴" else "▾", color = c.discret)
        }
        if (ouvert) {
            montres.forEach { id ->
                val reste = id in e.restants
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Box(Modifier.size(8.dp).background(if (reste) c.p else Color.White.copy(alpha = 0.2f), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(catalogue.suspect(id)?.texte("Titre") ?: id, color = if (reste) Color.White else c.tresDiscret,
                        textDecoration = if (reste) null else TextDecoration.LineThrough)
                }
            }
            val faits = e.faits.withIndex().filter { it.index >= 0 }.filter { f -> f.value.etape.objets("Reponses").none { it.texte("Note") == "cepc" } }
            if (faits.isNotEmpty()) {
                Text("Tes réponses (touche pour en changer une)", color = c.texte, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                faits.forEach { (k, f) ->
                    Column(Modifier.fillMaxWidth().clickable { changerReponse(k) }.padding(vertical = 4.dp)) {
                        Text(f.court, style = MaterialTheme.typography.bodySmall, color = c.discret)
                        Text(f.reponse, color = c.texte, maxLines = 2)
                    }
                }
            }
        }
    }
}

/** Les gestes d'une manipulation, chacun avec sa case à cocher, et la commande à taper sous le geste qui en parle. */
@Composable
private fun Gestes(etape: Objet) {
    val c = mc
    val faits = remember(etape.texte("Id")) { mutableStateListOf<Int>() }
    val commande = etape.objet("Commande")
    var posee = false
    etape.liste("Gestes").forEachIndexed { i, g ->
        val fait = i in faits
        Row(Modifier.fillMaxWidth().clickable { if (fait) faits.remove(i) else faits.add(i) }.padding(vertical = 4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 2.dp).size(22.dp).background(if (fait) c.p else Color.Transparent, RoundedCornerShape(6.dp))
                    .border(2.dp, c.p, RoundedCornerShape(6.dp)),
            ) { if (fait) Icon(Icons.Outlined.Check, null, tint = c.fondBas, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(12.dp))
            // (sur le téléphone, la commande se tape au clavier du PC : « Copier » devient « Recopie-la »)
            Text("${i + 1}. " + g.replace("Clique sur « Copier » à côté de la commande ci-dessous, puis dans la fenêtre noire : clic droit (ça colle), et touche Entrée.",
                "Dans la fenêtre noire, tape la commande ci-dessous (ou envoie-la-toi pour la coller), puis touche Entrée."),
                color = if (fait) c.tresDiscret else c.texte)
        }
        if (commande != null && !posee && g.contains("Copier")) { Commande(commande); posee = true }
    }
    if (commande != null && !posee) Commande(commande)
}

/** La commande à taper sur le PC, en grand, avec de quoi se l'envoyer (message, e-mail) ou la copier. */
@Composable
private fun Commande(commande: Objet) {
    val c = mc
    val contexte = LocalContext.current
    val lignes = commande.liste("Lignes")
    Verre(Modifier.fillMaxWidth().padding(vertical = 4.dp), teinte = c.p) {
        Etiquette(if (commande.vrai("Admin")) "À taper dans le Terminal (administrateur)" else "À taper dans le Terminal", c.p)
        lignes.forEach { l ->
            Text(l, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            BoutonPilule("M'envoyer la commande", {
                contexte.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, lignes.joinToString("\n")), "Envoyer la commande"))
            })
            BoutonPilule("Copier", {
                (contexte.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Commande", lignes.joinToString("\n")))
            })
        }
    }
}
