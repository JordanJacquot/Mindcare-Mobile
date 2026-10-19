package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jordanjacquot.mindcare.maj.BandeauMiseAJour
import io.github.jordanjacquot.mindcare.ui.theme.Etiquette
import io.github.jordanjacquot.mindcare.ui.theme.LogoMindcare
import io.github.jordanjacquot.mindcare.ui.theme.Themes
import io.github.jordanjacquot.mindcare.ui.theme.Verre
import io.github.jordanjacquot.mindcare.ui.theme.mc

/** L'accueil (étape 1 du plan : le squelette) : le logo, le bandeau des mises à jour, et le choix du thème. */
@Composable
fun Accueil(themeId: String, choisirTheme: (String) -> Unit) {
    val c = mc
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
        Text("1. Ça concerne quoi ?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        Verre(Modifier.fillMaxWidth()) {
            Text("Les pannes arrivent bientôt", fontWeight = FontWeight.SemiBold, color = c.texte)
            Text("Le catalogue de Mindcare PC (102 pannes) sera ici à la prochaine étape.", style = MaterialTheme.typography.bodySmall, color = c.discret, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(4.dp))
        Etiquette("Thème", c.discret)
        Verre(Modifier.fillMaxWidth()) {
            Themes.forEach { t ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { choisirTheme(t.id) }.padding(vertical = 8.dp),
                ) {
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
