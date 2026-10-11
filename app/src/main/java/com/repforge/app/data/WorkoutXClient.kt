package com.repforge.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RemoteExercise(val id: String, val name: String, val target: String, val equipment: String)

class WorkoutXClient(private val settings: SettingsStore, private val proxyOrigin: String = DEFAULT_PROXY) {
    /** Name wins over body part, which wins over equipment: the API filters by one criterion per call. */
    suspend fun search(name: String, bodyPart: String? = null, equipment: String? = null): List<RemoteExercise> = withContext(Dispatchers.IO) {
        val path = when {
            name.isNotBlank() -> "/v1/exercises/name/${encode(name)}"
            bodyPart != null -> "/v1/exercises/bodyPart/${encode(bodyPart)}"
            equipment != null -> "/v1/exercises/equipment/${encode(equipment)}"
            else -> "/v1/exercises"
        }
        try {
            val connection = (URL("$proxyOrigin$path?limit=$LIMIT").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-RepForge-Install", settings.installId())
            }
            try {
                val status = connection.responseCode
                if (status == 429) throw IllegalStateException("Limite de buscas atingido. Tente novamente amanhã.")
                if (status !in 200..299) throw IllegalStateException("Busca WorkoutX indisponível ($status).")
                parseExercises(connection.inputStream.bufferedReader().use { it.readText() }).take(LIMIT)
            } finally { connection.disconnect() }
        } catch (_: IOException) {
            throw IllegalStateException("Sem conexão — verifique sua internet.")
        } catch (_: JSONException) {
            throw IllegalStateException("Resposta inesperada da WorkoutX.")
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")

    companion object {
        const val DEFAULT_PROXY = "https://repforge-workoutx-proxy.repforge-rafael.workers.dev"
        private const val LIMIT = 20

        /** Portuguese label to WorkoutX API value. */
        val BODY_PARTS = linkedMapOf(
            "Costas" to "back", "Cardio" to "cardio", "Peito" to "chest", "Antebraços" to "lower arms",
            "Pernas (inferior)" to "lower legs", "Pescoço" to "neck", "Ombros" to "shoulders", "Braços" to "upper arms",
            "Pernas (superior)" to "upper legs", "Abdômen" to "waist"
        )
        val EQUIPMENT = linkedMapOf(
            "Assistido" to "assisted", "Barra" to "barbell", "Peso corporal" to "body weight", "Bosu" to "bosu ball",
            "Polia" to "cable", "Halter" to "dumbbell", "Elíptica" to "elliptical machine", "Barra W" to "ez barbell",
            "Kettlebell" to "kettlebell", "Máquina alavanca" to "leverage machine", "Bola medicinal" to "medicine ball",
            "Faixa elástica" to "resistance band", "Rolo" to "roller", "Corda" to "rope", "Máquina Smith" to "smith machine",
            "Bola de estabilidade" to "stability ball", "Com peso" to "weighted"
        )

        fun parseExercises(raw: String): List<RemoteExercise> {
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
    }
}
