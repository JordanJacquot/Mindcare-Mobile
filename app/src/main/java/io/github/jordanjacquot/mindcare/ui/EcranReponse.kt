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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.Ecarte
import io.github.jordanjacquot.mindcare.moteur.EnqueteTelephone
import io.github.jordanjacquot.mindcare.moteur.Objet
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc
import io.github.jordanjacquot.mindcare.moteur.pistesEcartees
import io.github.jordanjacquot.mindcare.moteur.titreReponse
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

/** Un élément de « Ce qu'il faut faire » : numéroté, ou un conseil avec son icône et son début en gras. */
private data class AFaire(val texte: String, val icone: Ic? = null, val titre: String = "")

/** Le badge de la réponse (comme sur PC) : sa couleur, son icône, son sur-titre. Jamais une coche sur « Urgent ». */
private data class Badge(val couleur: Color, val icone: Ic, val texte: String)

/**
 * La réponse (écran 8 de la maquette) : la carte verdict en haut (pastille et sur-titre sur une ligne, la conclusion
 * en grand dessous), « Ce qu'il faut faire » numéroté avec les conseils, « Comment Mindcare le sait » replié, la
 * sécurité en petit ; en bas, « Pour le réparateur » et « Retour à l'accueil ». Les pistes hors sujet n'apparaissent
 * jamais.
 */
@Composable
fun EcranReponse(
    catalogue: Catalogue,
    panne: Objet,
    et: EnqueteTelephone,
    profil: ProfilPc,
    ouvrirPc: () -> Unit,
    pourLeReparateur: () -> Unit,
    changerDerniere: () -> Unit,
    retour: () -> Unit,
    accueil: () -> Unit,
) {
    val c = mc
    val e = et.e
    val v = e.verdict ?: return
    val tr = catalogue.titreReponse(v)
    val suspect = catalogue.suspect(v.suspect)
    val urgent = suspect?.vrai("Urgent") == true
    val badge = when (v.type) {
        "trouve" -> if (urgent) Badge(c.rouge, Ic.Alerte, "Urgent") else Badge(c.vert, Ic.Coche, "Trouvé")
        "elimination" -> if (tr.supposee) Badge(c.ambre, Ic.NeSaisPas, "La piste la plus probable") else Badge(c.vert, Ic.Coche, "Tout le reste est écarté")
        "reparateur" -> Badge(c.ambre, Ic.NeSaisPas, "À faire vérifier par un réparateur")
        else -> Badge(c.ambre, Ic.NeSaisPas, "Aucune piste ne correspond")
    }
    val explication = when {
        suspect != null -> suspect.texte("Pourquoi")
        v.type == "reparateur" -> "Ces pistes se départagent avec un outil de réparateur."
        else -> "Aucun suspect ne tient après tes réponses : la panne est peut-être ailleurs."
    }
    val ecartees = catalogue.pistesEcartees(e, profil)
    var commentOuvert by rememberSaveable { mutableStateOf(false) }

    EcranTelephone(
        barre = { BarreHaut("La réponse", retour = retour, parent = panne.texte("Titre"), profil = profil, ouvrirPc = ouvrirPc) },
        pied = {
            Bouton("Pour le réparateur", pourLeReparateur, principal = false, icone = Ic.Rapport)
            Bouton("Retour à l'accueil", accueil)
        },
    ) {
        // La carte verdict
        val forme = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().plasma(forme, fort = true).padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).background(badge.couleur.copy(alpha = 0.16f), CircleShape).border(1.dp, badge.couleur.copy(alpha = 0.45f), CircleShape),
                ) { Icone(badge.icone, badge.couleur, taille = 26.dp, trait = 2.6f) }
                Spacer(Modifier.width(12.dp))
                Text(badge.texte.uppercase(), fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp, color = badge.couleur)
            }
            Text(tr.titre, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold, color = c.texte)
            if (explication.isNotEmpty()) Text(explication, fontSize = 15.sp, lineHeight = 22.sp, color = c.discret)
        }

        // Ce qui reste à vérifier (sans suspect trouvé, pour le réparateur)
        if (suspect == null && v.type == "reparateur" && v.restants.isNotEmpty()) {
            TitreBloc("Ce qui reste à vérifier", Modifier.padding(top = 6.dp))
            Groupe {
                v.restants.forEachIndexed { i, id ->
                    if (i > 0) Separation()
                    val r = catalogue.suspect(id)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Icone(Ic.Aide, c.ambre, Modifier.padding(top = 1.dp), taille = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(r?.texte("Titre") ?: id, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = c.texte)
                            val pq = r?.texte("Pourquoi") ?: ""
                            if (pq.isNotEmpty()) Text(pq, fontSize = 14.sp, lineHeight = 20.sp, color = c.discret, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }

        // Ce qu'il faut faire : la liste numérotée, puis le conseil pour le réparateur (et avant d'acheter une pièce)
        val aFaire = ArrayList<AFaire>()
        val chez = "Ensuite, chez un réparateur :"
        when {
            suspect != null -> {
                suspect.liste("AFaire").forEach { aFaire += AFaire(it) }
                suspect.texte("Pro").takeIf { it.isNotEmpty() }?.let { aFaire += AFaire(it, Ic.Cle, chez) }
            }
            v.type == "reparateur" -> aFaire += AFaire("montre-lui le rapport (bouton « Pour le réparateur ») : il saura par où commencer.", Ic.Cle, chez)
            else -> {
                aFaire += AFaire("Essaie une autre panne qui ressemble à ce qui se passe.")
                aFaire += AFaire("montre-lui le rapport (bouton « Pour le réparateur »).", Ic.Cle, chez)
            }
        }
        if (tr.piece) aFaire += AFaire(
            if (tr.supposee) "Mindcare n'a pas pu tester cette pièce : elle reste parce que tout le reste est écarté. Avant d'en acheter une neuve, fais-la vérifier par un réparateur, ou essaie avec celle d'un proche."
            else "Si le PC a moins de 2 ans, il est encore sous garantie : contacte d'abord le magasin ou le fabricant, la réparation peut être gratuite. Dans le doute, un réparateur peut confirmer avant que tu achètes.",
            Ic.Euro, "Avant d'acheter une pièce :",
        )
        TitreBloc("Ce qu'il faut faire", Modifier.padding(top = 6.dp))
        Groupe {
            var n = 0
            aFaire.forEachIndexed { i, a ->
                if (i > 0) Separation()
                Row(
                    Modifier.fillMaxWidth().then(if (a.icone != null) Modifier.background(c.p.copy(alpha = 0.06f)) else Modifier)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                        if (a.icone != null) Icone(a.icone, c.p)
                        else {
                            n++
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).background(Brush.linearGradient(listOf(c.p, c.s)), CircleShape)) {
                                Text("$n", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = c.encre)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    val texte = buildAnnotatedString {
                        if (a.titre.isNotEmpty()) { withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(a.titre) }; append(" ") }
                        append(a.texte)
                    }
                    Text(texte, fontSize = 15.5.sp, lineHeight = 22.sp, color = c.texte, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }

        // Comment Mindcare le sait, replié (une ligne avec son résumé)
        Groupe(forme = RoundedCornerShape(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { commentOuvert = !commentOuvert }.padding(horizontal = 14.dp),
            ) {
                Text("Comment Mindcare le sait", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = c.texte, modifier = Modifier.weight(1f))
                Text(resumeComment(e.preuves.size, ecartees.size), fontSize = 13.sp, color = c.discret, modifier = Modifier.padding(horizontal = 8.dp))
                Icone(if (commentOuvert) Ic.ChevronHaut else Ic.ChevronBas, c.p)
            }
            if (commentOuvert) {
                if (e.preuves.isEmpty()) {
                    Separation()
                    Text("Tes réponses ne permettaient pas d'écarter de piste.", fontSize = 14.5.sp, color = c.discret, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp))
                }
                e.preuves.forEach { Separation(); LignePreuve(it) }
                ecartees.forEach { Separation(); LigneEcartee(catalogue, it) }
            }
        }

        // (sur un portable, pas la phrase du bloc d'alimentation : un portable n'en a pas)
        val securite = catalogue.securite.filter { profil.type != TypePc.Portable || !it.contains("bloc d'alimentation", ignoreCase = true) }
        if (securite.isNotEmpty()) NoteSecurite(securite.joinToString(" "))
        Lien("Changer ma dernière réponse", changerDerniere, couleur = c.p, centre = false)
    }
}

/** « 1 indice, 1 écartée » */
private fun resumeComment(indices: Int, ecartees: Int): String {
    val l = ArrayList<String>()
    if (indices > 0) l += if (indices == 1) "1 indice" else "$indices indices"
    if (ecartees > 0) l += if (ecartees == 1) "1 écartée" else "$ecartees écartées"
    return if (l.isEmpty()) "Rien d'écarté" else l.joinToString(", ")
}

/** Une chose sûre (une preuve), avec sa coche verte. */
@Composable
fun LignePreuve(texte: String) {
    val c = mc
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
        Icone(Ic.Coche, c.vert, Modifier.padding(top = 1.dp), taille = 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(texte, fontSize = 14.5.sp, lineHeight = 20.sp, color = c.texte)
    }
}

/** Une piste écartée, et pourquoi (jamais une piste hors sujet : voir pistesEcartees). */
@Composable
fun LigneEcartee(catalogue: Catalogue, x: Ecarte) {
    val c = mc
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
        Icone(Ic.Croix, c.discret, Modifier.padding(top = 1.dp), taille = 18.dp)
        Spacer(Modifier.width(10.dp))
        val texte = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(catalogue.suspect(x.id)?.texte("Titre") ?: x.id) }
            if (x.raison.isNotEmpty()) append(" : ${x.raison}")
        }
        Text(texte, fontSize = 14.5.sp, lineHeight = 20.sp, color = c.texte)
    }
}
