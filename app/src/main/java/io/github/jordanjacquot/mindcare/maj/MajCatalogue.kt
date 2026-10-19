package io.github.jordanjacquot.mindcare.maj

import android.content.Context
import io.github.jordanjacquot.mindcare.moteur.Catalogue
import io.github.jordanjacquot.mindcare.moteur.FormatConnu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Le catalogue de pannes à jour : celui de l'appli (recopié de Mindcare PC à la construction), ou le dernier publié avec
 * Mindcare PC (catalogue.zip dans ses versions GitHub, son empreinte « SHA-256 du catalogue » dans les notes). Le plus
 * récent des deux est utilisé ; un catalogue téléchargé n'est gardé que s'il est complet et lisible par cette version
 * de l'appli (sinon, l'appli garde le sien et propose sa propre mise à jour).
 */
class MajCatalogue(private val contexte: Context, private val portee: CoroutineScope) {
    private val dossier = File(contexte.filesDir, "catalogue")
    private val reglages = contexte.getSharedPreferences("catalogue", Context.MODE_PRIVATE)

    /** L'édition du catalogue de l'appli (la version de Mindcare PC d'où il vient, « 0.7.0 »). */
    private val editionAppli: String = runCatching { contexte.assets.open("catalogue/edition.txt").bufferedReader().use { it.readText().trim() } }.getOrDefault("0")

    /** L'édition téléchargée, si elle est plus récente que celle de l'appli. */
    private fun editionTelechargee(): String? = reglages.getString("edition", null)?.takeIf {
        File(dossier, "enquetes.json").exists() && MiseAJour.compare(MiseAJour.numeros(it), MiseAJour.numeros(editionAppli)) > 0
    }

    private val _catalogue = MutableStateFlow(charger())
    val catalogue: StateFlow<Catalogue> = _catalogue

    /** L'édition en service (pour le rapport et les réglages). */
    val edition: String get() = editionTelechargee() ?: editionAppli

    private fun charger(): Catalogue {
        if (editionTelechargee() != null) runCatching { return Catalogue.lire(File(dossier, "enquetes.json").readText()) }
        return Catalogue.lire(contexte.assets.open("catalogue/enquetes.json").bufferedReader().use { it.readText() })
    }

    /** Une image du catalogue en service (images/<nom>.svg). */
    fun image(nom: String): InputStream? {
        if (editionTelechargee() != null) File(dossier, "images/$nom.svg").takeIf { it.exists() }?.let { return it.inputStream() }
        return runCatching { contexte.assets.open("catalogue/images/$nom.svg") }.getOrNull()
    }

    /** Au lancement : une vérification si la dernière date de plus de 6 heures. */
    fun verifierSiBesoin() {
        if (System.currentTimeMillis() - reglages.getLong("derniere", 0L) > 6 * 3600_000L) verifier()
    }

    fun verifier() {
        portee.launch {
            val r = runCatching { withContext(Dispatchers.IO) { telecharger() } }
            reglages.edit().putLong("derniere", System.currentTimeMillis()).apply()
            r.onSuccess { nouvelle -> if (nouvelle != null) { _catalogue.value = charger(); android.util.Log.i("Mindcare", "catalogue $nouvelle téléchargé") } else android.util.Log.i("Mindcare", "catalogue à jour ($edition)") }
            r.onFailure { android.util.Log.w("Mindcare", "catalogue pas mis à jour : ${it.message}") }
        }
    }

    /** Télécharge le dernier catalogue publié s'il est plus récent ; renvoie son édition (null : rien de nouveau). */
    private fun telecharger(): String? {
        val c = MiseAJour.ouvrirHttps("https://api.github.com/repos/$DepotPc/releases?per_page=10")
        val texte = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        val publie = Json.parseToJsonElement(texte).jsonArray.mapNotNull { versionCatalogue(it.jsonObject) }
            .maxWithOrNull { a, b -> MiseAJour.compare(MiseAJour.numeros(a.first), MiseAJour.numeros(b.first)) } ?: return null
        val (edition, url, sha) = publie
        if (MiseAJour.compare(MiseAJour.numeros(edition), MiseAJour.numeros(this.edition)) <= 0) return null

        val zip = File(contexte.cacheDir, "catalogue.zip")
        MiseAJour.ouvrirHttps(url).let { cx -> cx.inputStream.use { e -> zip.outputStream().use { e.copyTo(it) } }; cx.disconnect() }
        check(sha256(zip).equals(sha, ignoreCase = true)) { "empreinte du catalogue différente" }
        val nouveau = File(contexte.filesDir, "catalogue-nouveau").apply { deleteRecursively(); mkdirs() }
        try {
            extraire(zip, nouveau)
            val cat = Catalogue.lire(File(nouveau, "enquetes.json").readText())
            val pb = cat.problemes { File(nouveau, "images/$it.svg").exists() }
            check(pb.isEmpty()) { "catalogue $edition refusé : ${pb.take(3)}" }
            dossier.deleteRecursively()
            check(nouveau.renameTo(dossier)) { "impossible de ranger le catalogue" }
            reglages.edit().putString("edition", edition).apply()
            return edition
        } finally {
            nouveau.deleteRecursively(); zip.delete()
        }
    }

    /** Une version de Mindcare PC avec son catalogue : (édition, adresse du zip, empreinte), ou null. */
    private fun versionCatalogue(o: kotlinx.serialization.json.JsonObject): Triple<String, String, String>? {
        val tag = o["tag_name"]?.jsonPrimitive?.content ?: return null
        if (!tag.matches(Regex("v\\d+\\.\\d+\\.\\d+"))) return null
        if (o["draft"]?.jsonPrimitive?.boolean == true || o["prerelease"]?.jsonPrimitive?.boolean == true) return null
        val asset = (o["assets"] as? JsonArray)?.map { it.jsonObject }?.firstOrNull { it["name"]?.jsonPrimitive?.content == "catalogue.zip" } ?: return null
        val sha = Regex("SHA-256 du catalogue\\s*:\\s*([0-9a-fA-F]{64})").find(o["body"]?.jsonPrimitive?.content ?: "")?.groupValues?.get(1) ?: return null
        val url = asset["browser_download_url"]?.jsonPrimitive?.content ?: return null
        if (!url.startsWith("https://github.com/$DepotPc/releases/download/")) return null
        return Triple(tag.removePrefix("v"), url, sha)
    }

    companion object {
        const val DepotPc = "JordanJacquot/Mindcare"
        private val NomImage = Regex("images/[a-z0-9-]+\\.svg")

        /** Seuls enquetes.json et images/<nom>.svg sont sortis du zip (jamais un chemin qui sortirait du dossier). */
        fun extraire(zip: File, vers: File) {
            ZipInputStream(zip.inputStream()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val nom = e.name.replace('\\', '/')
                    if (!e.isDirectory && (nom == "enquetes.json" || NomImage.matches(nom))) {
                        val f = File(vers, nom); f.parentFile?.mkdirs()
                        f.outputStream().use { z.copyTo(it) }
                    }
                }
            }
            check(File(vers, "enquetes.json").exists()) { "pas de enquetes.json dans le catalogue" }
        }

        fun sha256(f: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { e -> val t = ByteArray(1 shl 16); while (true) { val n = e.read(t); if (n < 0) break; md.update(t, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Ce qui empêcherait le téléphone de se servir d'un catalogue (une liste vide : il est bon) : un format trop récent pour
 * cette version de l'appli, une famille, un suspect, un test ou une image qui manque.
 */
fun Catalogue.problemes(imageExiste: (String) -> Boolean): List<String> {
    val pb = ArrayList<String>()
    if (version > FormatConnu) pb += "format $version trop récent (cette appli lit jusqu'au $FormatConnu)"
    if (pannes.isEmpty()) pb += "aucune panne"
    familles.forEach { f -> if (f.texte("Icone").isNotEmpty() && !imageExiste(f.texte("Icone"))) pb += "icône ${f.texte("Icone")} manquante" }
    for (p in pannes) {
        val id = p.texte("Id")
        if (famille(p.texte("Famille")) == null) pb += "$id : famille inconnue"
        p.liste("Suspects").forEach { if (suspect(it) == null) pb += "$id : suspect $it inconnu" }
        p.objets("Etapes").forEach { e -> e.texte("Ref").takeIf { it.isNotEmpty() }?.let { if (test(it) == null) pb += "$id : test $it inconnu" } }
        etapes(p).forEach { e -> e.texte("Image").takeIf { it.isNotEmpty() }?.let { if (!imageExiste(it)) pb += "$id : image $it manquante" } }
    }
    return pb
}
