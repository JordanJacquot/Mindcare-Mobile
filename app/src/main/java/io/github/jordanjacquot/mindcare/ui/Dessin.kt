package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import io.github.jordanjacquot.mindcare.ui.theme.ThemeMindcare
import io.github.jordanjacquot.mindcare.ui.theme.mc
import io.github.jordanjacquot.mindcare.ui.theme.traduire
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

// Les dessins du catalogue (images\*.svg de Mindcare PC) : la même petite partie de SVG que modules\images.ps1 sur PC
// (rect, circle, ellipse, line, polyline, polygon, path, text, et g pour grouper), les mêmes règles (rempli en noir si
// rien n'est dit, sans contour), et les couleurs Néon traduites dans le thème choisi.

/** Le style d'une forme (hérité des groupes « g », comme en SVG). */
data class StyleSvg(val a: Map<String, String>) {
    operator fun get(k: String): String? = a[k]
    fun nombre(k: String, defaut: Float) = a[k]?.toFloatOrNull() ?: defaut
}

sealed interface FormeSvg { val style: StyleSvg }
data class Rect(val x: Float, val y: Float, val l: Float, val h: Float, val rx: Float, override val style: StyleSvg) : FormeSvg
data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float, override val style: StyleSvg) : FormeSvg
data class Ligne(val x1: Float, val y1: Float, val x2: Float, val y2: Float, override val style: StyleSvg) : FormeSvg
data class Chemin(val d: String, override val style: StyleSvg) : FormeSvg
data class Points(val points: List<Float>, val ferme: Boolean, override val style: StyleSvg) : FormeSvg
data class TexteSvg(val texte: String, val x: Float, val y: Float, override val style: StyleSvg) : FormeSvg

class DessinSvg(val largeur: Float, val hauteur: Float, val formes: List<FormeSvg>)

private val Herites = listOf("fill", "stroke", "stroke-width", "opacity", "fill-opacity", "stroke-opacity", "stroke-linecap",
    "stroke-linejoin", "stroke-dasharray", "font-size", "font-weight", "text-anchor")
private val Separateurs = Regex("[\\s,]+")

/** Lit un SVG (null s'il est illisible). Les éléments inconnus sont ignorés, comme sur PC. */
fun lireSvg(entree: InputStream): DessinSvg? = runCatching {
    val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }.newDocumentBuilder().parse(entree)
    val svg = doc.documentElement
    val vb = svg.getAttribute("viewBox").trim().split(Separateurs).map { it.toFloat() }
    val formes = ArrayList<FormeSvg>()
    fun f(e: Element, k: String) = e.getAttribute(k).toFloatOrNull() ?: 0f
    fun parcourir(noeud: Element, herite: Map<String, String>) {
        val enfants = noeud.childNodes
        for (i in 0 until enfants.length) {
            val e = enfants.item(i) as? Element ?: continue
            val h = HashMap(herite)
            for (a in Herites) if (e.hasAttribute(a)) h[a] = e.getAttribute(a)
            val s = StyleSvg(h)
            when (e.tagName.substringAfter(':')) {
                "g" -> parcourir(e, h)
                "rect" -> formes += Rect(f(e, "x"), f(e, "y"), f(e, "width"), f(e, "height"), f(e, "rx"), s)
                "circle" -> formes += Ellipse(f(e, "cx"), f(e, "cy"), f(e, "r"), f(e, "r"), s)
                "ellipse" -> formes += Ellipse(f(e, "cx"), f(e, "cy"), f(e, "rx"), f(e, "ry"), s)
                "line" -> formes += Ligne(f(e, "x1"), f(e, "y1"), f(e, "x2"), f(e, "y2"), s)
                "polyline", "polygon" -> formes += Points(e.getAttribute("points").trim().split(Separateurs).filter { it.isNotEmpty() }.map { it.toFloat() }, e.tagName == "polygon", s)
                "path" -> formes += Chemin(e.getAttribute("d"), s)
                "text" -> formes += TexteSvg(e.textContent, f(e, "x"), f(e, "y"), s)
            }
        }
    }
    parcourir(svg, emptyMap())
    DessinSvg(vb[2], vb[3], formes)
}.getOrNull()

/** Une couleur du dessin (#RRGGBB, Néon) traduite dans le thème ; null : « none » ou absente. */
private fun ThemeMindcare.couleur(valeur: String?, opacite: Float): Color? {
    if (valeur == null || valeur == "none") return null
    val hex = valeur.removePrefix("#")
    val argb = when (hex.length) { 6 -> 0xFF000000 or hex.toLong(16); 8 -> hex.toLong(16); else -> return null }
    val c = traduire(argb)
    return c.copy(alpha = c.alpha * opacite)
}

/** Un dessin du catalogue (images/<nom>.svg), qui s'adapte à la place qu'on lui donne, sans se déformer. */
@Composable
fun Dessin(nom: String, modifier: Modifier = Modifier) {
    val contexte = LocalContext.current
    val catalogue = (contexte.applicationContext as io.github.jordanjacquot.mindcare.MindcareApp).majCatalogue
    val edition = catalogue.edition
    val dessin = remember(nom, edition) { runCatching { catalogue.image(nom)?.use { lireSvg(it) } }.getOrNull() } ?: return
    val theme = mc.theme
    val mesureur = rememberTextMeasurer()
    Canvas(modifier) {
        val k = minOf(size.width / dessin.largeur, size.height / dessin.hauteur)
        val dx = (size.width - dessin.largeur * k) / 2
        val dy = (size.height - dessin.hauteur * k) / 2
        withTransform({ translate(dx, dy); scale(k, k, Offset.Zero) }) {
            decoupe(0f, 0f, dessin.largeur, dessin.hauteur) {
                for (forme in dessin.formes) dessiner(forme, theme) { t, x, y, s ->
                    val style = TextStyle(fontSize = (s.nombre("font-size", 16f) / density).sp, fontWeight = if (s["font-weight"] == "bold") FontWeight.Bold else FontWeight.Normal)
                    val mesure = mesureur.measure(t, style)
                    val gauche = when (s["text-anchor"]) { "middle" -> x - mesure.size.width / 2f; "end" -> x - mesure.size.width; else -> x }
                    val couleur = theme.couleur(s["fill"] ?: "#000000", s.nombre("fill-opacity", 1f) * s.nombre("opacity", 1f)) ?: return@dessiner
                    translate(gauche, y - mesure.firstBaseline) { drawText(mesure, couleur) }
                }
            }
        }
    }
}

private fun DrawScope.decoupe(l: Float, t: Float, r: Float, b: Float, bloc: DrawScope.() -> Unit) =
    withTransform({ clipRect(l, t, r, b) }) { bloc() }

private fun DrawScope.dessiner(forme: FormeSvg, theme: ThemeMindcare, texte: DrawScope.(String, Float, Float, StyleSvg) -> Unit) {
    val s = forme.style
    val opacite = s.nombre("opacity", 1f)
    // Comme en SVG : rempli en noir si rien n'est dit, sans contour
    val remplissage = theme.couleur(s["fill"] ?: "#000000", s.nombre("fill-opacity", 1f) * opacite)
    val contour = theme.couleur(s["stroke"], s.nombre("stroke-opacity", 1f) * opacite)
    val ep = s.nombre("stroke-width", 1f)
    val tirets = s["stroke-dasharray"]?.trim()?.split(Separateurs)?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.isNotEmpty() }
    val trait = Stroke(
        width = ep,
        cap = if (s["stroke-linecap"] == "round") StrokeCap.Round else StrokeCap.Butt,
        join = if (s["stroke-linejoin"] == "round") StrokeJoin.Round else StrokeJoin.Miter,
        pathEffect = tirets?.let { PathEffect.dashPathEffect((if (it.size % 2 == 1) it + it else it).toFloatArray()) },
    )
    fun peindre(remplir: DrawScope.(Color) -> Unit, tracer: DrawScope.(Color) -> Unit) {
        remplissage?.let { remplir(it) }
        contour?.let { tracer(it) }
    }
    when (forme) {
        is Rect -> peindre(
            { drawRoundRect(it, Offset(forme.x, forme.y), Size(forme.l, forme.h), CornerRadius(forme.rx), style = Fill) },
            { drawRoundRect(it, Offset(forme.x, forme.y), Size(forme.l, forme.h), CornerRadius(forme.rx), style = trait) })
        is Ellipse -> peindre(
            { drawOval(it, Offset(forme.cx - forme.rx, forme.cy - forme.ry), Size(2 * forme.rx, 2 * forme.ry), style = Fill) },
            { drawOval(it, Offset(forme.cx - forme.rx, forme.cy - forme.ry), Size(2 * forme.rx, 2 * forme.ry), style = trait) })
        is Ligne -> contour?.let { drawLine(it, Offset(forme.x1, forme.y1), Offset(forme.x2, forme.y2), ep, trait.cap, trait.pathEffect) }
        is Chemin -> {
            val p = PathParser().parsePathString(forme.d).toPath()
            peindre({ drawPath(p, it, style = Fill) }, { drawPath(p, it, style = trait) })
        }
        is Points -> {
            val p = Path()
            forme.points.chunked(2).filter { it.size == 2 }.forEachIndexed { i, (x, y) -> if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
            if (forme.ferme) p.close()
            peindre({ drawPath(p, it, style = Fill) }, { drawPath(p, it, style = trait) })
        }
        is TexteSvg -> texte(forme.texte, forme.x, forme.y, s)
    }
}
