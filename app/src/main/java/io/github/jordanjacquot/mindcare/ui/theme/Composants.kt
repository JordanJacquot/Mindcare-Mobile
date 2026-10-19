package io.github.jordanjacquot.mindcare.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Les pièces de la DA de la suite (interface.xaml de Mindcare PC) : fond de nuit et ses lueurs, panneaux en verre au
// bord lumineux, boutons pilule en dégradé, et le logo en pouls.

/** Le fond de nuit, avec ses trois lueurs : cyan en haut à gauche, violet au milieu, magenta en bas à droite. */
@Composable
fun FondMindcare(modifier: Modifier = Modifier, contenu: @Composable BoxScope.() -> Unit) {
    val c = mc
    val v = c.theme.traduire(0xFF7A3CFF)
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(c.fondHaut, c.fondBas), end = Offset(size.width, size.height)))
                val grand = maxOf(size.width, size.height)
                fun lueur(couleur: Color, x: Float, y: Float, rayon: Float) = drawRect(
                    Brush.radialGradient(listOf(couleur, Color.Transparent), center = Offset(x * size.width, y * size.height), radius = rayon * grand),
                )
                lueur(c.p.copy(alpha = 0.27f), 0.12f, 0.05f, 0.5f)
                lueur(v.copy(alpha = 0.15f), 0.6f, 0.35f, 0.45f)
                lueur(c.t.copy(alpha = 0.27f), 0.92f, 0.9f, 0.5f)
            },
        content = contenu,
    )
}

val FormeCarte = RoundedCornerShape(20.dp)

/** Un panneau en verre (CardBg et CardBorder sur PC) ; « teinte » le colore. */
@Composable
fun Verre(
    modifier: Modifier = Modifier,
    teinte: Color? = null,
    forme: Shape = FormeCarte,
    onClick: (() -> Unit)? = null,
    interieur: PaddingValues = PaddingValues(16.dp),
    contenu: @Composable ColumnScope.() -> Unit,
) {
    val fond = if (teinte != null) Brush.linearGradient(listOf(teinte.copy(alpha = 0.22f), teinte.copy(alpha = 0.06f)))
               else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f)))
    val bord = if (teinte != null) Brush.linearGradient(listOf(teinte.copy(alpha = 0.55f), teinte.copy(alpha = 0.10f)))
               else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.04f)))
    Column(
        modifier
            .clip(forme)
            .background(fond)
            .border(1.dp, bord, forme)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(interieur),
        content = contenu,
    )
}

/** Le bouton principal : une pilule en dégradé cyan vers violet (AccentBg), avec un reflet en haut. */
@Composable
fun BoutonAccent(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, actif: Boolean = true, icone: ImageVector? = null) {
    val c = mc
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        modifier = modifier
            .alpha(if (actif) 1f else 0.45f)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(c.p, c.s)))
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.65f to Color.Transparent))
            .clickable(enabled = actif, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        if (icone != null) { Icon(icone, null, tint = c.fondBas, modifier = Modifier.size(18.dp)); Box(Modifier.size(6.dp)) }
        Text(texte, color = c.fondBas, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}

/** Le bouton discret : une pilule en verre. */
@Composable
fun BoutonPilule(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, couleur: Color? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(texte, color = couleur ?: mc.texte, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}

/** Un bouton rond en dégradé. */
@Composable
fun BoutonRond(onClick: () -> Unit, modifier: Modifier = Modifier, taille: Dp = 48.dp, couleurs: List<Color>? = null, contenu: @Composable BoxScope.() -> Unit) {
    val c = mc
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(taille)
            .clip(CircleShape)
            .background(Brush.linearGradient(couleurs ?: listOf(c.p, c.s)))
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.65f to Color.Transparent))
            .clickable(onClick = onClick),
        content = contenu,
    )
}

/** Le logo : le pouls de Mindcare (le même tracé que dans Mindset) qui se dessine en boucle, dans un anneau qui tourne. */
@Composable
fun LogoMindcare(taille: Dp = 46.dp) {
    val c = mc
    val anim = rememberInfiniteTransition(label = "logo")
    val tour by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "anneau")
    val trace by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "pouls")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(taille)) {
        Canvas(Modifier.fillMaxSize()) {
            val e = size.minDimension * 0.07f
            drawCircle(Brush.radialGradient(listOf(c.p.copy(alpha = 0.25f), Color.Transparent)), radius = size.minDimension / 2)
            drawArc(
                Brush.sweepGradient(listOf(c.p, c.s, c.t, c.p)),
                startAngle = tour, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(e, e), size = androidx.compose.ui.geometry.Size(size.width - 2 * e, size.height - 2 * e),
                style = Stroke(width = e, cap = StrokeCap.Round),
            )
            // Le pouls, sur une grille de 100
            val k = size.minDimension / 100f
            val chemin = androidx.compose.ui.graphics.Path().apply {
                Pouls.forEachIndexed { n, (x, y) -> if (n == 0) moveTo(x * k, y * k) else lineTo(x * k, y * k) }
            }
            val mesure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(chemin, false) }
            val bout = androidx.compose.ui.graphics.Path()
            mesure.getSegment(0f, mesure.length * trace, bout, true)
            val trait = Stroke(width = 7f * k, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            drawPath(chemin, Color.White.copy(alpha = 0.18f), style = trait)
            drawPath(bout, Brush.horizontalGradient(listOf(c.p, c.t)), style = trait)
        }
    }
}

/** Le tracé du pouls (le champ « logo » de Mindcare dans apps.json de Mindset). */
val Pouls = listOf(14f to 52f, 31f to 52f, 38f to 31f, 47f to 73f, 57f to 20f, 66f to 66f, 71f to 52f, 86f to 52f)

/** Une petite étiquette en majuscules, de la couleur donnée (« SANTÉ », « À FAIRE », « OÙ »). */
@Composable
fun Etiquette(texte: String, couleur: Color, modifier: Modifier = Modifier) {
    Text(texte.uppercase(), color = couleur, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.6.sp, modifier = modifier)
}

/** Une ligne de boutons. */
@Composable
fun LigneBoutons(modifier: Modifier = Modifier, contenu: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 4.dp), content = contenu)
}
