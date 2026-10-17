package ca.alexlittle.dose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ca.alexlittle.dose.ui.navigation.DoseNavHost
import ca.alexlittle.dose.ui.theme.DoseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoseTheme {
                DoseNavHost()
            }
        }
    }
}
