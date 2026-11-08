package fr.jovida.decisionboard.data

import fr.jovida.decisionboard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    /** Appelle le backend et remet chaque événement NDJSON au fur et à mesure. */
    suspend fun streamDebate(
        question: String,
        domains: List<String>,
        finvault: JSONArray?,
        maillage: JSONArray?,
        onEvent: (JSONObject) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("question", question)
            .put("domains", JSONArray(domains))
            .put("appData", JSONObject().apply {
                finvault?.let { put("finvault", it) }
                maillage?.let { put("maillage", it) }
            })
        val conn = (URL("${BuildConfig.BACKEND_URL}/debate").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("content-type", "application/json")
            setRequestProperty("authorization", "Bearer ${BuildConfig.APP_TOKEN}")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode != 200) throw IllegalStateException("Serveur : ${conn.responseCode}")
            conn.inputStream.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach { onEvent(JSONObject(it)) }
            }
        } finally {
            conn.disconnect()
        }
    }
}
