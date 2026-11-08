package fr.jovida.decisionboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private val DOMAINS = listOf("carriere" to "Carrière", "finances" to "Finances", "elections" to "Élections")
private val PHASES = mapOf("ouverture" to "1 · Ouverture", "croise" to "2 · Débat croisé", "arbitre" to "3 · Recommandation")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebateScreen(vm: DebateViewModel) {
    var question by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Decision Board") }) }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            OutlinedTextField(
                value = question, onValueChange = { question = it },
                label = { Text("Quelle décision dois-tu prendre ?") },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DOMAINS.forEach { (id, label) ->
                    FilterChip(selected = id in vm.domains, onClick = { vm.toggleDomain(id) }, label = { Text(label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = vm.useFinvault, onClick = { vm.useFinvault = !vm.useFinvault }, label = { Text("FinVault") })
                FilterChip(selected = vm.useMaillage, onClick = { vm.useMaillage = !vm.useMaillage }, label = { Text("Maillage") })
            }
            Button(onClick = { vm.start(question) }, enabled = !vm.running && question.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.running) "Débat en cours…" else "Lancer le débat")
            }
            vm.status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (vm.running) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(vm.events.toList()) { EventCard(it) }
            }
        }
    }
}

@Composable
private fun EventCard(e: JSONObject) {
    when (e.optString("type")) {
        "phase" -> Text(PHASES[e.optString("phase")] ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        "erreur" -> Text("⚠ ${e.optString("agent")} : ${e.optString("message")}", color = MaterialTheme.colorScheme.error)
        "agent" -> {
            val d = e.getJSONObject("data")
            val isArbitre = e.optString("phase") == "arbitre"
            Card(colors = if (isArbitre) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()) {
                SelectionContainer {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(e.optString("nom"), fontWeight = FontWeight.Bold)
                        when (e.optString("phase")) {
                            "ouverture" -> Ouverture(d)
                            "croise" -> Croise(d)
                            else -> Arbitre(d)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Ouverture(d: JSONObject) {
    if (d.optBoolean("hors_perimetre")) { Text("Hors périmètre pour cette question."); return }
    Text(d.optString("position"), fontWeight = FontWeight.Medium)
    d.optJSONArray("arguments").forEachObj { Text("• ${it.optString("titre")} (${it.optInt("force")}/5) — ${it.optString("contenu")}") }
    d.optJSONArray("questions").forEachObj { Text("❓ → ${it.optString("a")} : ${it.optString("question")}", style = MaterialTheme.typography.bodySmall) }
}

@Composable private fun Croise(d: JSONObject) {
    d.optJSONArray("reponses").forEachObj { Text("↩ ${it.optString("a")} : ${it.optString("reponse")}") }
    d.optJSONArray("jugements").forEachObj {
        Text("⚖ ${it.optString("agent")} — ${it.optInt("pertinence")}/10 (${it.optString("verdict")}) : ${it.optString("commentaire")}", style = MaterialTheme.typography.bodySmall)
    }
    Text("Position révisée : ${d.optString("position_revisee")}", fontWeight = FontWeight.Medium)
    Text("Recommandation : ${d.optString("recommandation_partielle")}")
}

@Composable private fun Arbitre(d: JSONObject) {
    Text(d.optString("recommandation"), style = MaterialTheme.typography.titleMedium)
    Text("Confiance : ${d.optString("confiance")}")
    section("Raisons", d.optJSONArray("raisons"))
    d.optJSONArray("arbitrages").forEachObj { Text("⚖ ${it.optString("sujet")} → ${it.optString("tranche_en_faveur_de")} : ${it.optString("pourquoi")}", style = MaterialTheme.typography.bodySmall) }
    section("Risques", d.optJSONArray("risques"))
    section("Ce qui ferait changer d'avis", d.optJSONArray("ce_qui_ferait_changer_davis"))
    section("Prochaines étapes", d.optJSONArray("prochaines_etapes"))
    section("À vérifier avant d'agir", d.optJSONArray("points_a_verifier"))
}

@Composable private fun section(title: String, arr: JSONArray?) {
    if (arr == null || arr.length() == 0) return
    Text(title, fontWeight = FontWeight.Bold)
    for (i in 0 until arr.length()) Text("• ${arr.optString(i)}")
}

@Composable private fun JSONArray?.forEachObj(block: @Composable (JSONObject) -> Unit) {
    if (this == null) return
    for (i in 0 until length()) optJSONObject(i)?.let { block(it) }
}
