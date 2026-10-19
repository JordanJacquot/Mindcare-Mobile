package io.github.jordanjacquot.mindcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jordanjacquot.mindcare.ui.MindcareNavigation
import io.github.jordanjacquot.mindcare.ui.theme.FondMindcare
import io.github.jordanjacquot.mindcare.ui.theme.MindcareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barres du téléphone transparentes, icônes claires : le fond de nuit va jusqu'en haut
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val app = application as MindcareApp
        setContent {
            val theme by app.theme.collectAsStateWithLifecycle()
            MindcareTheme(theme) {
                FondMindcare { MindcareNavigation(theme, app::choisirTheme) }
            }
        }
    }
}
