package io.github.jordanjacquot.mindcare.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jordanjacquot.mindcare.ui.theme.Separation
import io.github.jordanjacquot.mindcare.ui.theme.mc

/**
 * Le cadre d'un écran du téléphone : la barre du haut, une seule colonne qui défile, et le pied (les réponses ou les
 * grands boutons, toujours visibles en bas, sous le pouce).
 */
@Composable
fun EcranTelephone(
    barre: @Composable () -> Unit,
    pied: (@Composable ColumnScope.() -> Unit)? = null,
    defilement: ScrollState = rememberScrollState(),
    contenu: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        barre()
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(defilement).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = contenu,
        )
        if (pied != null) {
            Separation()
            Column(
                Modifier.fillMaxWidth().background(mc.fondBas.copy(alpha = 0.88f)).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = pied,
            )
        }
    }
}
