package com.repforge.app.data

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class RemoteExercise(val id: String, val name: String, val target: String, val equipment: String)

class WorkoutXClient(private val context: Context, private val proxyOrigin: String = DEFAULT_PROXY) {
    suspend fun search(name: String): List<RemoteExercise> = withContext(Dispatchers.IO) {
        require(name.isNotBlank()) { "Informe um exercício para buscar." }
        val path = "/v1/exercises/name/${URLEncoder.encode(name.trim(), "UTF-8")}"
        val connection = (URL(proxyOrigin + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-RepForge-Install", installId())
        }
        try {
            val status = connection.responseCode
            if (status == 429) error("Limite de buscas atingido.")
            if (status !in 200..299) error("Busca WorkoutX indisponível ($status).")
            parse(connection.inputStream.bufferedReader().readText())
        } finally { connection.disconnect() }
    }

    private fun installId(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    private fun parse(raw: String): List<RemoteExercise> {
        val values = if (raw.trimStart().startsWith("[")) {
            JSONArray(raw)
        } else {
            val root = JSONObject(raw)
            root.optJSONArray("data") ?: root.optJSONArray("results") ?: root.optJSONArray("exercises") ?: JSONArray()
        }
        return buildList {
            for (index in 0 until values.length()) {
                val item = values.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                if (id.isNotBlank() && name.isNotBlank()) add(RemoteExercise(id, name, item.optString("target"), item.optString("equipment")))
            }
        }
    }

    companion object { const val DEFAULT_PROXY = "https://repforge-workoutx-proxy.repforge-rafael.workers.dev" }
}
