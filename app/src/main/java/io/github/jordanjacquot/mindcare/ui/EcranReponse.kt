package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Enquete
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.minuscule
import io.github.jordanjacquot.mindcare.moteur.titreVerdict
import io.github.jordanjacquot.mindcare.ui.theme.BoutonAccent
import io.github.jordanjacquot.mindcare.ui.theme.BoutonPilule
import io.github.jordanjacquot.mindcare.ui.theme.Etiquette
import io.github.jordanjacquot.mindcare.ui.theme.Verre
import io.github.jordanjacquot.mindcare.ui.theme.mc

/**
 * La réponse, comme sur PC : trouvée (ou tout le reste écarté), urgente, ou ce qui reste pour un réparateur ; puis
 * comment on le sait, et ce qu'il faut faire.
 */
@Composable
fun EcranReponse(catalogue: Catalogue, panne: Objet, e: Enquete, changerDerniere: () -> Unit, accueil: () -> Unit) {
    val c = mc
    val v = e.verdict ?: return
    val suspect = catalogue.suspect(v.suspect)
    val urgent = suspect?.vrai("Urgent") == true
    val (couleur, signe, label) = when (v.type) {
        "trouve" -> if (urgent) Triple(c.rouge, "!", "Urgent") else Triple(c.vert, "✓", "Trouvé")
        "elimination" -> Triple(c.vert, "✓", "Tout le reste est écarté")
        "reparateur" -> Triple(c.ambre, "?", "À faire vérifier par un réparateur")
        else -> Triple(c.ambre, "?", "Aucune piste ne correspond")
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(panne.texte("Titre"), color = c.discret, fontWeight = FontWeight.SemiBold)
        // La réponse, en grand
        Verre(Modifier.fillMaxWidth(), teinte = couleur) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp).background(couleur.copy(alpha = 0.15f), CircleShape).border(3.dp, couleur, CircleShape)) {
                    Text(signe, color = couleur, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Etiquette(label, couleur)
                    Text(catalogue.titreVerdict(v), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // Ce qu'il faut faire
        Verre(Modifier.fillMaxWidth()) {
            Text("Ce qu'il faut faire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            when {
                suspect != null -> {
                    Text(suspect.texte("Pourquoi"), color = c.discret, modifier = Modifier.padding(top = 6.dp))
                    suspect.liste("AFaire").forEachIndexed { i, a ->
                        Row(Modifier.padding(top = 8.dp)) {
                            Text("${i + 1}.", color = c.p, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp))
                            Text(a, color = c.texte)
                        }
                    }
                    Text("Quand aller chez un réparateur", fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 14.dp))
                    Text(suspect.texte("Pro"), color = c.texte, modifier = Modifier.padding(top = 4.dp))
                }
                v.type == "reparateur" -> {
                    Text("Ces pistes se départagent avec un outil de réparateur : montre-lui ce que tu as fait, il saura par où commencer.", color = c.discret, modifier = Modifier.padding(top = 6.dp))
                    v.restants.forEach { id ->
                        val r = catalogue.suspect(id)
                        Text(r?.texte("Titre") ?: id, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 12.dp))
                        Text(r?.texte("Pourquoi") ?: "", color = c.texte, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                else -> Text("Aucun suspect ne tient après tes réponses. La panne est peut-être ailleurs : essaie une autre panne qui ressemble, ou montre ce que tu as fait à un réparateur.", color = c.discret, modifier = Modifier.padding(top = 6.dp))
            }
        }

        // Comment on le sait
        Verre(Modifier.fillMaxWidth()) {
            Text("Comment on le sait", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (e.preuves.isEmpty()) Text("Tes réponses ne permettaient pas d'écarter de piste.", color = c.discret, modifier = Modifier.padding(top = 6.dp))
            e.preuves.forEach { p ->
                Row(Modifier.padding(top = 8.dp)) {
                    Box(Modifier.padding(top = 6.dp).size(8.dp).background(c.vert, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(p, color = c.texte)
                }
            }
            val ecartes = e.ecartes.mapNotNull { catalogue.suspect(it.id)?.takeIf { s -> !s.vrai("Urgent") }?.texte("Titre")?.let(::minuscule) }.distinct()
            if (ecartes.isNotEmpty()) Text("Écartés : ${ecartes.joinToString(", ")}.", color = c.tresDiscret, modifier = Modifier.padding(top = 12.dp))
        }

        Text(catalogue.securite.joinToString(" "), color = c.ambre, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoutonAccent("Retour à l'accueil", accueil)
            BoutonPilule("Changer ma dernière réponse", changerDerniere)
        }
        Spacer(Modifier.height(12.dp))
    }
}
