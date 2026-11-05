package io.github.jordanjacquot.mindcare.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.moteur.ProfilPc
import io.github.jordanjacquot.mindcare.moteur.TypePc

// Les pièces de la maquette téléphone validée par Jordan (maquette-mobile/index.html) : la barre du haut avec la
// pastille « Ton PC », le bord de plasma, les grands boutons du bas, les listes groupées, et les petites icônes au
// trait. Tout est pensé pour le pouce : chaque zone à toucher fait 48 dp ou plus.

/** Les petites icônes au trait de la maquette (24 x 24, trait de 2, bouts ronds). */
enum class Ic(val d: String) {
    Retour("M15 5l-7 7 7 7"),
    Chevron("M9 6l6 6-6 6"),
    ChevronBas("M6 9l6 6 6-6"),
    ChevronHaut("M6 15l6-6 6 6"),
    Portable("M5.5 5h13a1.5 1.5 0 0 1 1.5 1.5v8a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 14.5v-8A1.5 1.5 0 0 1 5.5 5z M2 19h20"),
    Fixe("M4.5 4h15A1.5 1.5 0 0 1 21 5.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 14.5v-9A1.5 1.5 0 0 1 4.5 4z M9 20h6M12 16v4"),
    NeSaisPas("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6 M12 17h.01"),
    Loupe("M4.5 11a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0-13 0 M16 16l4.5 4.5"),
    Micro("M12 3a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21"),
    Coche("M5 12.5l4.5 4.5L19 7.5"),
    Croix("M6 6l12 12M18 6L6 18"),
    Partage("M15.5 5.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M3.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M15.5 18.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M8.2 10.8l7.6-4.1M8.2 13.2l7.6 4.1"),
    Horloge("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0 M12 7.5V12l3 2"),
    Cle("M14.5 5.5a4 4 0 0 0 4.9 4.9l-9 9a2.1 2.1 0 0 1-3-3l9-9a4 4 0 0 1-1.9-1.9z"),
    Euro("M17.5 6.5A7 7 0 1 0 17.5 17.5M4.5 10.5h9M4.5 13.5h9"),
    Rapport("M7 3h7l4 4v14H7z M14 3v4h4M10 12h5M10 16h5"),
    Alerte("M12 4l9 16H3z M12 10v4M12 17h.01"),
    Terminal("M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M7 9l3 3-3 3M12 15h5"),
    Aide("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 11v5M12 8h.01"),
}

/** Une petite icône au trait. */
@Composable
fun Icone(icone: Ic, couleur: Color, modifier: Modifier = Modifier, taille: Dp = 20.dp, trait: Float = 2f) {
    val chemin = remember(icone) { PathParser().parsePathString(icone.d).toPath() }
    Canvas(modifier.size(taille)) {
        scale(size.minDimension / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(chemin, couleur, style = Stroke(width = trait, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * Le bord de plasma (comme sur PC) : un dégradé des couleurs du thème qui tourne lentement autour de la carte. `fort` :
 * le bord plein et une lueur autour, pour ce qui compte le plus sur l'écran.
 */
@Composable
fun Modifier.plasma(forme: Shape, fort: Boolean = false): Modifier {
    val c = mc
    val tour = rememberInfiniteTransition(label = "plasma").animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "tour")
    val couleurs = intArrayOf(c.p.toArgb(), c.s.toArgb(), c.t.toArgb(), c.p.toArgb())
    return this
        .drawWithCache {
            val contour = forme.createOutline(size, layoutDirection, this)
            val shader = android.graphics.SweepGradient(size.width / 2, size.height / 2, couleurs, null)
            val matrice = android.graphics.Matrix()
            val pinceau = object : ShaderBrush() { override fun createShader(size: Size): Shader = shader }
            val epaisseur = 1.5.dp.toPx()
            onDrawWithContent {
                drawOutline(contour, c.carte)
                drawContent()
                matrice.setRotate(tour.value, size.width / 2, size.height / 2)
                shader.setLocalMatrix(matrice)
                if (fort) drawOutline(contour, pinceau, alpha = 0.18f, style = Stroke(width = epaisseur * 5))
                drawOutline(contour, pinceau, alpha = if (fort) 1f else 0.55f, style = Stroke(width = epaisseur))
            }
        }
}

/** La pastille « Ton PC » de la barre du haut : le PC choisi ; la toucher le change. */
@Composable
fun PastillePc(profil: ProfilPc, onClick: () -> Unit) {
    val c = mc
    val libelle = if (profil.inconnu) "Ton PC" else profil.libelleCourt
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(onClick = onClick)
            .semantics { contentDescription = "Ton PC : $libelle. Touche pour le changer." },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.widthIn(max = 140.dp).height(34.dp)
                .border(1.dp, Brush.linearGradient(listOf(c.p.copy(alpha = 0.55f), c.p.copy(alpha = 0.35f))), CircleShape)
                .padding(start = 10.dp, end = 12.dp),
        ) {
            Icone(if (profil.type == TypePc.Fixe) Ic.Fixe else Ic.Portable, c.p, taille = 16.dp)
            Spacer(Modifier.width(6.dp))
            Text(libelle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.texte, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * La barre du haut : le retour (s'il y en a un), le titre court (sur deux lignes au plus, coupé proprement s'il est
 * encore trop long), le fil d'Ariane en petit au-dessus, et la pastille « Ton PC ».
 */
@Composable
fun BarreHaut(
    titre: String,
    retour: (() -> Unit)? = null,
    parent: String? = null,
    avecLogo: Boolean = false,
    profil: ProfilPc? = null,
    ouvrirPc: (() -> Unit)? = null,
) {
    val c = mc
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 4.dp, end = 8.dp)) {
            if (retour != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = retour).semantics { contentDescription = "Retour" },
                ) { Icone(Ic.Retour, c.texte, taille = 24.dp) }
            }
            if (avecLogo) {
                Spacer(Modifier.width(12.dp))
                LogoMindcare(32.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f).padding(vertical = 6.dp, horizontal = if (retour != null || avecLogo) 0.dp else 12.dp)) {
                if (!parent.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(parent, fontSize = 12.5.sp, color = c.discret, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Icone(Ic.Chevron, c.discret, Modifier.padding(start = 2.dp), taille = 12.dp)
                    }
                }
                Text(titre, fontSize = if (avecLogo) 19.sp else 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.texte,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (profil != null && ouvrirPc != null) {
                Spacer(Modifier.width(4.dp))
                PastillePc(profil, ouvrirPc)
            }
        }
        Separation()
    }
}

/** Une fine ligne de séparation. */
@Composable
fun Separation(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(mc.separation))
}

/** Un grand bouton du bas (52 dp) : « principal » en dégradé, sinon discret (gris, au bord fin). */
@Composable
fun Bouton(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    principal: Boolean = true,
    icone: Ic? = null,
    actif: Boolean = true,
) {
    val c = mc
    val forme = RoundedCornerShape(26.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .alpha(if (actif) 1f else 0.45f)
            .clip(forme)
            .then(
                if (principal) Modifier.background(Brush.horizontalGradient(0f to c.p, 0.6f to c.s, 1f to c.t))
                else Modifier.background(c.surface2).border(1.dp, c.ligne, forme)
            )
            .clickable(enabled = actif, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        if (icone != null) {
            Icone(icone, if (principal) c.encre else c.p)
            Spacer(Modifier.width(10.dp))
        }
        Text(texte, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (principal) c.encre else c.texte, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Un lien en texte, assez grand pour le pouce (48 dp). */
@Composable
fun Lien(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, couleur: Color? = null, centre: Boolean = true) {
    Box(
        contentAlignment = if (centre) Alignment.Center else Alignment.CenterStart,
        modifier = modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp),
    ) {
        Text(texte, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = couleur ?: mc.discret)
    }
}

/** Le titre d'un bloc, en petites majuscules (« RACONTE TON PROBLÈME »), avec un mot à droite si besoin. */
@Composable
fun TitreBloc(texte: String, modifier: Modifier = Modifier, droite: String? = null) {
    val c = mc
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Text(texte.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = c.discret, modifier = Modifier.weight(1f))
        if (droite != null) Text(droite, fontSize = 13.sp, color = c.discret, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Une liste groupée : des lignes l'une sous l'autre, dans un même cadre arrondi. */
@Composable
fun Groupe(modifier: Modifier = Modifier, forme: Shape = RoundedCornerShape(18.dp), contenu: @Composable ColumnScope.() -> Unit) {
    val c = mc
    Column(modifier.fillMaxWidth().clip(forme).background(c.surface).border(1.dp, c.bord, forme), content = contenu)
}

/** Une ligne d'une liste groupée, à toucher : un point de couleur, le titre, une phrase, une étiquette, la flèche. */
@Composable
fun LigneListe(
    titre: String,
    onClick: () -> Unit,
    petit: String? = null,
    etiquette: String? = null,
    point: Color? = null,
    titreGras: Boolean = true,
) {
    val c = mc
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (point != null) {
            Box(Modifier.size(10.dp).background(point, CircleShape))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titre, fontSize = if (titreGras) 16.sp else 15.5.sp, lineHeight = 21.sp, fontWeight = if (titreGras) FontWeight.Bold else FontWeight.Normal, color = c.texte)
            if (!petit.isNullOrEmpty()) Text(petit, fontSize = 13.5.sp, lineHeight = 19.sp, color = c.discret, modifier = Modifier.padding(top = 3.dp))
            if (!etiquette.isNullOrEmpty()) {
                Text(etiquette, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.discret,
                    modifier = Modifier.padding(top = 5.dp).clip(RoundedCornerShape(10.dp)).background(c.surface2).padding(horizontal = 9.dp, vertical = 3.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Icone(Ic.Chevron, c.tresDiscret)
    }
}

/** Un carré arrondi teinté qui porte une icône ou un dessin (40 dp). */
@Composable
fun CarreIcone(modifier: Modifier = Modifier, taille: Dp = 40.dp, teinte: Color? = null, contenu: @Composable BoxScope.() -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(taille).clip(RoundedCornerShape(12.dp)).background((teinte ?: mc.p).copy(alpha = 0.14f)),
        content = contenu,
    )
}
