package io.github.jordanjacquot.mindcare

import io.github.jordanjacquot.mindcare.maj.MiseAJour
import io.github.jordanjacquot.mindcare.ui.theme.Pouls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqueletteTest {
    @Test fun lesVersionsSeComparent() {
        assertTrue(MiseAJour.compare(MiseAJour.numeros("0.2.0-beta"), MiseAJour.numeros("0.1.9")) > 0)
        assertEquals(0, MiseAJour.compare(MiseAJour.numeros("0.1.0-beta"), MiseAJour.numeros("0.1.0")))
    }

    /** Le pouls du logo est celui de Mindcare dans Mindset (apps.json, champ « logo »). */
    @Test fun leLogoEstLeMemeQueDansMindset() {
        val trace = "M" + Pouls.joinToString(" L") { (x, y) -> "${x.toInt()},${y.toInt()}" }
        assertEquals("M14,52 L31,52 L38,31 L47,73 L57,20 L66,66 L71,52 L86,52", trace)
    }
}
