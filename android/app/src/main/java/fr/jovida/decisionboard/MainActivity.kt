package fr.jovida.decisionboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.jovida.decisionboard.ui.DebateScreen
import fr.jovida.decisionboard.ui.DebateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: DebateViewModel = viewModel()
                DebateScreen(vm)
            }
        }
    }
}
