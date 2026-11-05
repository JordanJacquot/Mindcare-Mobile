package io.github.jordanjacquot.mindcare.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Ecarte
import io.github.jordanjacquot.mindcare.moteur.EnqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.gestePourTelephone
import io.github.jordanjacquot.mindcare.moteur.pistesEcartees
import io.github.jordanjacquot.mindcare.moteur.pistesPossibles
import io.github.jordanjacquot.mindcare.moteur.surLeMateriel
import io.github.jordanjacquot.mindcare.ui.theme.BarreHaut
import io.github.jordanjacquot.mindcare.ui.theme.Bouton
import io.github.jordanjacquot.mindcare.ui.theme.Groupe
import io.github.jordanjacquot.mindcare.ui.theme.Ic
import io.github.jordanjacquot.mindcare.ui.theme.Icone
import io.github.jordanjacquot.mindcare.ui.theme.Lien
import io.github.jordanjacquot.mindcare.ui.theme.Separation
import io.github.jordanjacquot.mindcare.ui.theme.TitreBloc
import io.github.jordanjacquot.mindcare.ui.theme.mc
import io.github.jordanjacquot.mindcare.ui.theme.plasma
import kotlinx.coroutines.launch

/**
 * Une étape de l'enquête (écrans 5, 6 et 7 de la maquette) : le fil d'Ariane dans la barre, « Étape 3 sur 6 » et ses
 * segments, la question en grand, le dessin, « Tes réponses » replié ; pour un test, la commande en très gros et les
 * gestes à cocher. En bas, sous le pouce : le bandeau des pistes (qui ouvre la feuille), les réponses, « Je ne sais
 * pas » et « Étape précédente ». Les pistes hors sujet (qui ne concernent pas ce genre de PC) ne s'affichent jamais.
 */
@Composable
fun EcranEnquete(
    catalogue: Catalogue,
    panne: Objet,
    et: EnqueteTelephone,
    profil: ProfilPc,
    ouvrirPc: () -> Unit,
    repondre: (Int) -> Unit,
    changerReponse: (Int) -> Unit,
    precedente: () -> Unit,
    plusTard: () -> Unit,
) {
    val e = et.e
    val etape = e.etape ?: return
    // (une nouvelle étape repart du haut, gestes décochés)
    key(et.choix) { Etape(catalogue, panne, et, etape, profil, ouvrirPc, repondre, changerReponse, precedente, plusTard) }
}

@Composable
private fun Etape(
    catalogue: Catalogue,
    panne: Objet,
    et: EnqueteTelephone,
    etape: Objet,
    profil: ProfilPc,
    ouvrirPc: () -> Unit,
    repondre: (Int) -> Unit,
    changerReponse: (Int) -> Unit,
    precedente: () -> Unit,
    plusTard: () -> Unit,
) {
    val c = mc
    val e = et.e
    val test = etape.texte("Type").equals("test", ignoreCase = true)
    var pistesOuvertes by rememberSaveable { mutableStateOf(false) }
    val possibles = catalogue.pistesPossibles(e)
    val ecartees = catalogue.pistesEcartees(e)
    val reponses = etape.objets("Reponses")
    val hauteurMax = (LocalConfiguration.current.screenHeightDp * 0.42f).dp

    EcranTelephone(
        barre = {
            BarreHaut(panne.texte("Titre"), retour = precedente, parent = catalogue.famille(panne.texte("Famille"))?.texte("Titre"),
                profil = profil, ouvrirPc = ouvrirPc)
        },
        pied = {
            BandeauPistes(possibles.size, ecartees.size) { pistesOuvertes = true }
            if (test) TitreBloc("Ensuite, dis ce que ça a donné")
            val deuxCourtes = reponses.size == 2 && reponses.all { it.texte("Texte").length <= 20 }
            if (deuxCourtes) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    reponses.forEachIndexed { i, r -> BoutonReponse(r.texte("Texte"), Modifier.weight(1f)) { repondre(i) } }
                }
            } else {
                Column(Modifier.heightIn(max = hauteurMax).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reponses.forEachIndexed { i, r -> BoutonReponse(r.texte("Texte"), Modifier.fillMaxWidth()) { repondre(i) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Lien("Je ne sais pas", { repondre(-1) }, Modifier.weight(1f), couleur = c.p)
                Lien("Étape précédente", precedente, Modifier.weight(1f))
            }
        },
    ) {
        Avancement(et.numero, et.total)
        Column {
            Text(etape.texte("Texte"), fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
            val duree = etape.texte("Duree")
            if (test) Text(if (duree.isNotEmpty()) "Sur le PC, $duree" else "Sur le PC", fontSize = 15.sp, color = c.discret, modifier = Modifier.padding(top = 4.dp))
            val aide = etape.texte("Aide")
            if (aide.isNotEmpty()) Text(aide, fontSize = 15.sp, lineHeight = 21.sp, color = c.discret, modifier = Modifier.padding(top = 6.dp))
        }
        if (etape.texte("Image").isNotEmpty()) {
            val forme = RoundedCornerShape(18.dp)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(forme).background(c.surface).border(1.dp, c.bord, forme).padding(10.dp)) {
                Dessin(etape.texte("Image"), Modifier.fillMaxWidth().height(150.dp))
            }
        }
        if (test) {
            etape.objet("Commande")?.let { CarteCommande(it) }
            if (etape.liste("Gestes").isNotEmpty()) {
                TitreBloc("Coche chaque geste une fois fait", Modifier.padding(top = 4.dp))
                Gestes(etape)
            }
            if (etape.surLeMateriel()) catalogue.securite.firstOrNull()?.let { NoteSecurite(it) }
            Bouton("Je le ferai plus tard", plusTard, principal = false)
        }
        TesReponses(et, changerReponse)
    }

    if (pistesOuvertes) FeuillePistes(catalogue, possibles, ecartees) { pistesOuvertes = false }
}

/** « Étape 3 sur 6 » et ses segments : faits, celui-ci, à venir. */
@Composable
private fun Avancement(n: Int, total: Int) {
    val c = mc
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Étape $n sur $total", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.discret)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { i ->
                val m = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                when {
                    i < n - 1 -> Box(m.background(Brush.horizontalGradient(listOf(c.p, c.s))))
                    i == n - 1 -> Box(m.background(c.p.copy(alpha = 0.55f)))
                    else -> Box(m.background(c.ligne))
                }
            }
        }
    }
}

/** Une réponse : un grand bouton au bord de plasma, sous le pouce. */
@Composable
private fun BoutonReponse(texte: String, modifier: Modifier, onClick: () -> Unit) {
    val forme = RoundedCornerShape(18.dp)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.heightIn(min = 60.dp).plasma(forme).clip(forme).clickable(onClick = onClick).semantics { role = Role.Button }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(texte, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = mc.texte)
    }
}

/** « 4 pistes encore possibles », avec l'anneau de ce qui reste ; un toucher ouvre la feuille des pistes. */
@Composable
private fun BandeauPistes(possibles: Int, ecartees: Int, ouvrir: () -> Unit) {
    val c = mc
    val forme = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(forme).background(c.surface2).border(1.dp, c.ligne, forme)
            .clickable(onClick = ouvrir).padding(start = 14.dp, end = 12.dp),
    ) {
        val part = if (possibles + ecartees == 0) 1f else possibles.toFloat() / (possibles + ecartees)
        Canvas(Modifier.size(26.dp)) {
            val ep = 5.dp.toPx()
            val coin = Offset(ep / 2, ep / 2)
            val taille = Size(size.width - ep, size.height - ep)
            drawArc(c.ligne, 0f, 360f, false, coin, taille, style = Stroke(ep))
            drawArc(c.p, -90f, 360f * part, false, coin, taille, style = Stroke(ep))
        }
        Spacer(Modifier.width(10.dp))
        val texte = buildAnnotatedString {
            when (possibles) {
                0 -> append("Aucune piste encore possible")
                1 -> { withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("1 piste") }; append(" encore possible") }
                else -> { withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$possibles pistes") }; append(" encore possibles") }
            }
        }
        Text(texte, fontSize = 15.sp, color = c.texte, modifier = Modifier.weight(1f))
        Icone(Ic.ChevronHaut, c.p)
    }
}

/** « 4 encore possibles, 1 écartée » */
fun resumePistes(possibles: Int, ecartees: Int): String {
    val a = if (possibles == 1) "1 encore possible" else "$possibles encore possibles"
    return when (ecartees) {
        0 -> a
        1 -> "$a, 1 écartée"
        else -> "$a, $ecartees écartées"
    }
}

/**
 * La feuille des pistes, qui monte du bas : les pistes encore possibles d'abord, puis les écartées barrées avec la
 * raison en clair. Elle se ferme avec « Fermer », en la glissant vers le bas, ou avec le retour du téléphone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeuillePistes(catalogue: Catalogue, possibles: List<String>, ecartees: List<Ecarte>, fermer: () -> Unit) {
    val c = mc
    val etat = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val portee = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = fermer, sheetState = etat, containerColor = c.carte2, contentColor = c.texte,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).size(36.dp, 4.dp).background(c.tresDiscret, RoundedCornerShape(2.dp))) },
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Les pistes", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
            Text(resumePistes(possibles.size, ecartees.size), fontSize = 14.sp, color = c.discret)
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (possibles.isNotEmpty()) {
                TitreBloc("Encore possibles", Modifier.padding(top = 4.dp))
                possibles.forEach { id -> LignePiste(catalogue.suspect(id)?.texte("Titre") ?: id, null, ecartee = false) }
            }
            if (ecartees.isNotEmpty()) {
                TitreBloc("Écartées", Modifier.padding(top = 6.dp))
                ecartees.forEach { x -> LignePiste(catalogue.suspect(x.id)?.texte("Titre") ?: x.id, x.raison, ecartee = true) }
            }
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Bouton("Fermer", { portee.launch { etat.hide() }.invokeOnCompletion { fermer() } }, principal = false)
        }
    }
}

@Composable
private fun LignePiste(titre: String, raison: String?, ecartee: Boolean) {
    val c = mc
    val forme = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(forme).background(c.surface).border(1.dp, c.bord, forme).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (ecartee) c.vert else c.ambre, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(titre, fontSize = 15.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                color = if (ecartee) c.discret else c.texte, textDecoration = if (ecartee) TextDecoration.LineThrough else null)
            Spacer(Modifier.width(8.dp))
            Text(if (ecartee) "Écartée" else "À vérifier", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (ecartee) c.vert else c.ambre)
        }
        if (!raison.isNullOrEmpty()) Text(raison, fontSize = 13.5.sp, lineHeight = 19.sp, color = c.discret, modifier = Modifier.padding(start = 22.dp, top = 2.dp))
    }
}

/** « Tes réponses (2) », replié : un toucher l'ouvre ; « Changer » revient à cette question. */
@Composable
private fun TesReponses(et: EnqueteTelephone, changerReponse: (Int) -> Unit) {
    val c = mc
    val faits = et.faitsMontres
    if (faits.isEmpty()) return
    var ouvert by rememberSaveable { mutableStateOf(false) }
    Groupe(forme = RoundedCornerShape(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { ouvert = !ouvert }.padding(horizontal = 14.dp),
        ) {
            Text("Tes réponses", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = c.texte)
            Text(" (${faits.size})", fontSize = 13.sp, color = c.discret, modifier = Modifier.weight(1f))
            Icone(if (ouvert) Ic.ChevronHaut else Ic.ChevronBas, c.p)
        }
        if (ouvert) faits.forEach { (k, f) ->
            Separation()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(f.court.ifEmpty { f.texte }, fontSize = 13.sp, color = c.discret)
                    Text(f.reponse, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = c.texte)
                }
                Lien("Changer", { changerReponse(k) }, couleur = c.p)
            }
        }
    }
}

/** Les gestes d'un test, chacun avec sa case à cocher (le geste « Copier » du PC, réécrit pour le téléphone). */
@Composable
private fun Gestes(etape: Objet) {
    val c = mc
    var faits by rememberSaveable { mutableStateOf(listOf<Int>()) }
    Groupe {
        etape.liste("Gestes").forEachIndexed { i, g ->
            if (i > 0) Separation()
            val fait = i in faits
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { faits = if (fait) faits - i else faits + i }
                    .semantics { role = Role.Checkbox; selected = fait }.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                val forme = RoundedCornerShape(8.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(26.dp).clip(forme)
                        .then(if (fait) Modifier.background(Brush.linearGradient(listOf(c.p, c.s))) else Modifier.border(2.dp, c.tresDiscret, forme)),
                ) { if (fait) Icone(Ic.Coche, c.encre, taille = 16.dp, trait = 3f) }
                Spacer(Modifier.width(12.dp))
                Text("${i + 1}. ${gestePourTelephone(g)}", fontSize = 15.5.sp, lineHeight = 22.sp, color = if (fait) c.discret else c.texte)
            }
        }
    }
}

/**
 * La commande à taper sur le PC, en très gros (les espaces marqués), et « M'envoyer la commande » pour la recevoir sur
 * le PC (message, e-mail) et la coller. Pas de bouton « Copier » : le téléphone ne colle rien dans le PC.
 */
@Composable
private fun CarteCommande(commande: Objet) {
    val c = mc
    val contexte = LocalContext.current
    val lignes = commande.liste("Lignes")
    if (lignes.isEmpty()) return
    val forme = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().plasma(forme, fort = true).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icone(Ic.Terminal, c.p, taille = 16.dp)
            Spacer(Modifier.width(6.dp))
            Text(if (commande.vrai("Admin")) "La commande à taper dans le Terminal (administrateur)" else "La commande à taper dans le Terminal",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.discret)
        }
        lignes.forEach { l ->
            val texte = buildAnnotatedString {
                l.forEach { ch -> if (ch == ' ') withStyle(SpanStyle(background = c.p.copy(alpha = 0.35f))) { append(' ') } else append(ch) }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black).border(1.dp, c.ligne, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            ) {
                Text(texte, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = c.texte,
                    fontSize = when { l.length <= 16 -> 22.sp; l.length <= 26 -> 19.sp; else -> 17.sp }, lineHeight = 28.sp)
            }
        }
        if (lignes.any { ' ' in it }) {
            Text(
                if (lignes.size > 1) "Chaque case colorée est un espace : tape-le aussi. Une ligne à la fois, avec Entrée après chacune."
                else "Chaque case colorée est un espace : tape-le aussi.",
                fontSize = 13.5.sp, lineHeight = 19.sp, color = c.discret,
            )
        }
        Bouton("M'envoyer la commande", {
            contexte.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, lignes.joinToString("\n")), "Envoyer la commande"))
        }, principal = false, icone = Ic.Partage)
    }
}

/** La consigne de sécurité, en petit, au bord en pointillés. */
@Composable
fun NoteSecurite(texte: String) {
    val c = mc
    val forme = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(forme).border(1.dp, c.ligne, forme).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Icone(Ic.Alerte, c.ambre, taille = 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(texte, fontSize = 13.5.sp, lineHeight = 19.sp, color = c.discret)
    }
}
