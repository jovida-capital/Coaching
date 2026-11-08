package fr.jovida.decisionboard.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.jovida.decisionboard.data.ApiClient
import fr.jovida.decisionboard.data.AppBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DebateViewModel(app: Application) : AndroidViewModel(app) {
    val events = mutableStateListOf<JSONObject>()
    var running by mutableStateOf(false); private set
    var status by mutableStateOf<String?>(null); private set

    var useFinvault by mutableStateOf(true)
    var useMaillage by mutableStateOf(true)
    val domains = mutableStateListOf("carriere", "finances", "elections")

    fun toggleDomain(id: String) { if (id in domains) domains.remove(id) else domains.add(id) }

    fun start(question: String) {
        if (running || question.isBlank() || domains.isEmpty()) return
        running = true; events.clear(); status = null
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                // Lecture locale, puis envoi des seules données résumées/filtrées
                val fin = if (useFinvault) withContext(Dispatchers.IO) { AppBridge.readAll(ctx, AppBridge.FINVAULT_URIS) } else null
                val mai = if (useMaillage) withContext(Dispatchers.IO) { AppBridge.readAll(ctx, AppBridge.MAILLAGE_URIS) } else null
                status = listOfNotNull(fin?.error, mai?.error).joinToString(" · ").ifEmpty { null }
                ApiClient.streamDebate(question.trim(), domains.toList(), fin?.data, mai?.data) { evt ->
                    viewModelScope.launch { events.add(evt) }
                }
            } catch (e: Exception) {
                status = "Erreur : ${e.message}"
            } finally {
                running = false
            }
        }
    }
}
