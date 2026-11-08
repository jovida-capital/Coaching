package fr.jovida.decisionboard.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lecture des données de FinVault et Maillage via ContentProvider.
 *
 * ⚠️ Les authorities et chemins ci-dessous sont des PLACEHOLDERS : remplace-les par ceux
 * réellement exposés par tes deux applications (voir README, section « Côté FinVault / Maillage »).
 */
object AppBridge {
    // TODO: authorities réelles
    val FINVAULT_URIS = listOf(
        "content://fr.jovida.finvault.provider/summary",
        "content://fr.jovida.finvault.provider/accounts_overview",
    )
    val MAILLAGE_URIS = listOf(
        "content://fr.jovida.maillage.provider/contacts_summary",
    )

    private const val MAX_ROWS = 200

    // Colonnes jamais transmises au backend, quel que soit ce que l'autre appli expose.
    private val SENSITIVE = Regex("(iban|bic|rib|account_?n|numero|number|password|secret|token|pin|cvv|ssn|email|phone|tel)", RegexOption.IGNORE_CASE)

    data class ReadResult(val data: JSONArray?, val error: String?)

    fun readAll(context: Context, uris: List<String>): ReadResult {
        val out = JSONArray()
        var lastError: String? = null
        for (u in uris) {
            try {
                val cursor = context.contentResolver.query(Uri.parse(u), null, null, null, null)
                if (cursor == null) { lastError = "Fournisseur introuvable : $u"; continue }
                cursor.use { c ->
                    val rows = JSONArray()
                    val keep = (0 until c.columnCount).filter { !SENSITIVE.containsMatchIn(c.getColumnName(it)) }
                    var n = 0
                    while (c.moveToNext() && n < MAX_ROWS) {
                        val row = JSONObject()
                        for (i in keep) {
                            val name = c.getColumnName(i)
                            when (c.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_INTEGER -> row.put(name, c.getLong(i))
                                android.database.Cursor.FIELD_TYPE_FLOAT -> row.put(name, c.getDouble(i))
                                android.database.Cursor.FIELD_TYPE_NULL -> {}
                                else -> row.put(name, c.getString(i))
                            }
                        }
                        rows.put(row); n++
                    }
                    out.put(JSONObject().put("source", Uri.parse(u).lastPathSegment).put("lignes", rows))
                }
            } catch (e: SecurityException) {
                lastError = "Permission refusée pour $u"
            } catch (e: Exception) {
                lastError = "Lecture impossible : $u"
            }
        }
        return ReadResult(if (out.length() > 0) out else null, lastError)
    }
}
