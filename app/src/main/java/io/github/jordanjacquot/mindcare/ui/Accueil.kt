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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jordanjacquot.mindcare.MindcareApp
import io.github.jordanjacquot.mindcare.donnees.Diagnostic
import io.github.jordanjacquot.mindcare.donnees.aReprendre
import io.github.jordanjacquot.mindcare.donnees.formaterDate
import io.github.jordanjacquot.mindcare.maj.BandeauMiseAJour
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.enqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.minuscule
import io.github.jordanjacquot.mindcare.ui.theme.BarreHaut
import io.github.jordanjacquot.mindcare.ui.theme.Groupe
import io.github.jordanjacquot.mindcare.ui.theme.Ic
import io.github.jordanjacquot.mindcare.ui.theme.Icone
import io.github.jordanjacquot.mindcare.ui.theme.LigneListe
import io.github.jordanjacquot.mindcare.ui.theme.Separation
import io.github.jordanjacquot.mindcare.ui.theme.Themes
import io.github.jordanjacquot.mindcare.ui.theme.TitreBloc
import io.github.jordanjacquot.mindcare.ui.theme.mc
import io.github.jordanjacquot.mindcare.ui.theme.plasma

/**
 * L'accueil (écran 1 de la maquette) : l'enquête pas finie d'abord (c'est souvent pour elle qu'on rouvre l'appli),
 * puis « Raconte ton problème », les familles en grille de 2, les dernières enquêtes et « Comment ça marche ». Tout se
 * fait défiler vers le bas.
 */
@Composable
fun Accueil(
    catalogue: Catalogue,
    profil: ProfilPc,
    ouvrirPc: () -> Unit,
    ouvrirFamille: (String) -> Unit,
    ouvrirRecherche: () -> Unit,
    reprendre: (Diagnostic) -> Unit,
    themeId: String,
    choisirTheme: (String) -> Unit,
) {
    val c = mc
    val app = LocalContext.current.applicationContext as MindcareApp
    val historique by app.historique.liste.collectAsStateWithLifecycle()
    var commentOuvert by rememberSaveable { mutableStateOf(false) }
    var themesOuverts by rememberSaveable { mutableStateOf(false) }

    EcranTelephone(barre = { BarreHaut("Mindcare", avecLogo = true, profil = profil, ouvrirPc = ouvrirPc) }) {
        BandeauMiseAJour()

        // L'enquête pas finie (un test remis à plus tard) : la reprendre est proposé tout de suite
        val enCours = historique.aReprendre(catalogue)
        if (enCours != null) {
            val p = catalogue.panne(enCours.Panne)!!
            val et = catalogue.enqueteTelephone(p, enCours.Choix, profil)
            val forme = RoundedCornerShape(20.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).plasma(forme, fort = true).clip(forme)
                    .clickable { reprendre(enCours) }.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Box {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.ambre.copy(alpha = 0.14f))) {
                        Icone(Ic.Horloge, c.ambre, taille = 22.dp)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 0.dp).size(10.dp).background(c.ambre, CircleShape))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reprendre l'enquête", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.texte)
                    val ou = if (et.e.etape != null) ", étape ${et.numero} sur ${et.total}" else ""
                    Text("${p.texte("Titre")}$ou", fontSize = 14.sp, lineHeight = 19.sp, color = c.discret, modifier = Modifier.padding(top = 3.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).background(Brush.linearGradient(listOf(c.p, c.s)), CircleShape)) {
                    Icone(Ic.Chevron, c.encre)
                }
            }
        }

        // Raconte ton problème : le champ ouvre l'écran de recherche
        TitreBloc("Raconte ton problème", Modifier.padding(top = 6.dp))
        val formeChamp = RoundedCornerShape(18.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).plasma(formeChamp).clip(formeChamp).clickable(onClick = ouvrirRecherche).padding(horizontal = 16.dp),
        ) {
            Icone(Ic.Loupe, c.p)
            Spacer(Modifier.width(10.dp))
            Text("Par exemple : « bip », « écran bleu »", fontSize = 16.sp, color = c.tresDiscret, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LigneDicter()

        // Les familles, en grille de 2
        TitreBloc("Ça concerne quoi ?", Modifier.padding(top = 6.dp), droite = "${catalogue.familles.size} familles")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            catalogue.familles.chunked(2).forEach { ligne ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    ligne.forEach { f ->
                        val forme = RoundedCornerShape(16.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 68.dp).clip(forme).background(c.surface).border(1.dp, c.bord, forme)
                                .clickable { ouvrirFamille(f.texte("Id")) }.padding(10.dp),
                        ) {
                            Dessin(f.texte("Icone"), Modifier.size(34.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(f.texte("Titre"), fontSize = 14.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = c.texte)
                        }
                    }
                    if (ligne.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Les dernières enquêtes (les 10 gardées) : un toucher pour reprendre ou revoir la réponse
        val dernieres = historique.filter { it.Id != enCours?.Id && catalogue.panne(it.Panne) != null }
        if (dernieres.isNotEmpty()) {
            TitreBloc("Dernières enquêtes", Modifier.padding(top = 6.dp))
            Groupe {
                dernieres.forEachIndexed { i, d ->
                    if (i > 0) Separation()
                    val (point, quoi) = when {
                        d.Fini && d.Suspect.isNotEmpty() -> c.vert to (if (d.Titre.startsWith("À vérifier")) d.Titre else "Trouvé : ${minuscule(d.Titre)}")
                        d.Fini -> c.ambre to "Pour le réparateur"
                        else -> c.p to "Pas finie"
                    }
                    LigneListe(catalogue.panne(d.Panne)!!.texte("Titre"), { reprendre(d) }, petit = "$quoi. Le ${formaterDate(d.Maj, "d MMMM")}", point = point)
                }
            }
        }

        // Comment ça marche, replié
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable { commentOuvert = !commentOuvert }.padding(horizontal = 2.dp),
        ) {
            Icone(Ic.Aide, c.p)
            Spacer(Modifier.width(8.dp))
            Text("Comment ça marche", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.p, modifier = Modifier.weight(1f))
            Icone(if (commentOuvert) Ic.ChevronHaut else Ic.ChevronBas, c.p)
        }
        if (commentOuvert) {
            Groupe {
                listOf(
                    "Choisis ce qui ne va pas, ou raconte-le avec tes mots.",
                    "Réponds aux questions, une à la fois. Si tu ne sais pas, dis-le : Mindcare fait avec.",
                    "Mindcare écarte les pistes une à une, puis te dit ce qu'il faut faire, ou quoi montrer à un réparateur.",
                    "Rien n'est envoyé : tout reste sur ton téléphone.",
                ).forEachIndexed { i, t ->
                    if (i > 0) Separation()
                    Text(t, fontSize = 15.sp, lineHeight = 21.sp, color = c.texte, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
                }
            }
        }

        // Le thème (les 5 de la suite), replié
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable { themesOuverts = !themesOuverts }.padding(horizontal = 2.dp),
        ) {
            val t = Themes.firstOrNull { it.id == themeId } ?: Themes.first()
            Box(Modifier.size(20.dp).background(Brush.linearGradient(listOf(Color(t.p), Color(t.s), Color(t.t))), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("Thème : ${t.nom}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.discret, modifier = Modifier.weight(1f))
            Icone(if (themesOuverts) Ic.ChevronHaut else Ic.ChevronBas, c.discret)
        }
        if (themesOuverts) {
            Groupe {
                Themes.forEachIndexed { i, t ->
                    if (i > 0) Separation()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { choisirTheme(t.id) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Box(Modifier.size(22.dp).background(Brush.linearGradient(listOf(Color(t.p), Color(t.s), Color(t.t))), CircleShape)
                            .then(if (t.id == themeId) Modifier.border(2.dp, Color.White, CircleShape) else Modifier))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.nom, fontWeight = if (t.id == themeId) FontWeight.Bold else FontWeight.Normal, color = c.texte)
                            Text(t.description, fontSize = 13.sp, color = c.discret)
                        }
                    }
                }
            }
        }
        Text("Mindcare Mobile ${app.maj.versionActuelle} · catalogue ${app.majCatalogue.edition}, le même que Mindcare sur PC",
            fontSize = 12.sp, color = c.tresDiscret)
    }
}

/** « Pour parler, touche le micro du clavier. » (pas de bouton « Parler » à nous : le micro du clavier suffit) */
@Composable
fun LigneDicter() {
    val c = mc
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp).background(c.surface2, CircleShape)) { Icone(Ic.Micro, c.p, taille = 15.dp) }
        Spacer(Modifier.width(8.dp))
        Text("Pour parler, touche le micro du clavier.", fontSize = 13.5.sp, color = c.discret)
    }
}
