package com.repforge.app.data

import android.content.Context
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.text.Normalizer

data class CatalogExercise(
    val id: String,
    val name: String,
    val nameEn: String,
    val muscle: String,
    val secondary: List<String>,
    val equipment: String
) {
    internal val nameKey = ExerciseCatalog.normalize(name)
    internal val haystack = ExerciseCatalog.normalize("$name $nameEn $muscle $equipment")

    /** Kind to use when this exercise is added to a sheet. */
    val defaultKind: String get() = ExerciseCatalog.inferKind(name, nameEn, muscle, equipment)
}

class ExerciseCatalog(private val context: Context) {
    private val mutex = Mutex()
    @Volatile private var cached: List<CatalogExercise>? = null

    suspend fun all(): List<CatalogExercise> = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            context.assets.open(ASSET).bufferedReader().use { parse(it.readText()) }
        }.also { cached = it }
    }

    companion object {
        private const val ASSET = "exercises.json"
        private val MARKS = Regex("\\p{M}+")

        fun normalize(value: String): String =
            MARKS.replace(Normalizer.normalize(value.trim(), Normalizer.Form.NFD), "").lowercase()

        // Isometric holds (normalized, accent-free) logged by time instead of reps.
        private val HOLDS = listOf("prancha", "plank", "isometri", "hold", "pendurar", "dead hang", "wall sit", "na parede", "sustentacao")

        /** Timed holds first (a plank is bodyweight too), then cardio, then bodyweight; anything else is plain reps. */
        fun inferKind(name: String, nameEn: String = "", muscle: String = "", equipment: String = ""): String {
            val names = normalize("$name $nameEn")
            return when {
                HOLDS.any { it in names } -> KIND_TIMED
                muscle == "Cardio" -> KIND_CARDIO
                equipment == "Peso corporal" -> KIND_BODYWEIGHT
                else -> KIND_REPS
            }
        }

        fun parse(raw: String): List<CatalogExercise> {
            val array = try { JSONArray(raw) } catch (_: JSONException) { return emptyList() }
            val seen = HashSet<String>()
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    val muscle = item.optString("muscle").trim()
                    val equipment = item.optString("equipment").trim()
                    if (id.isEmpty() || name.isEmpty() || muscle.isEmpty() || equipment.isEmpty() || !seen.add(id)) continue
                    val secondary = item.optJSONArray("secondary")?.let { values ->
                        (0 until values.length()).map { values.optString(it) }.filter { it.isNotBlank() }
                    }.orEmpty()
                    add(CatalogExercise(id, name, item.optString("nameEn").trim(), muscle, secondary, equipment))
                }
            }
        }

        fun search(items: List<CatalogExercise>, query: String, muscle: String? = null, equipment: String? = null): List<CatalogExercise> {
            val words = normalize(query).split(' ').filter { it.isNotEmpty() }
            val phrase = words.joinToString(" ")
            val ranked = items.mapNotNull { item ->
                if (muscle != null && item.muscle != muscle) return@mapNotNull null
                if (equipment != null && item.equipment != equipment) return@mapNotNull null
                if (words.isEmpty()) return@mapNotNull item to 0
                if (!words.all { item.haystack.contains(it) }) return@mapNotNull null
                val tokens = item.haystack.split(' ')
                val rank = when {
                    item.nameKey.startsWith(phrase) -> 0
                    words.all { word -> tokens.any { it.startsWith(word) } } -> 1
                    else -> 2
                }
                item to rank
            }
            return ranked.sortedWith(compareBy({ it.second }, { it.first.nameKey })).map { it.first }
        }
    }
}
