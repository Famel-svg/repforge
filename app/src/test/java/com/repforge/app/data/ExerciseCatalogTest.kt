package com.repforge.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    private val raw = """
        [
          {"id":"supino-reto-halteres","name":"Supino reto com halteres","nameEn":"Dumbbell bench press","muscle":"Peito","secondary":["Tríceps","Ombros"],"equipment":"Halteres"},
          {"id":"supino-reto-barra","name":"Supino reto","nameEn":"Barbell bench press","muscle":"Peito","secondary":["Tríceps"],"equipment":"Barra"},
          {"id":"triceps-pulley","name":"Tríceps pulley","nameEn":"Triceps pushdown","muscle":"Tríceps","secondary":[],"equipment":"Polia"},
          {"id":"rosca-direta","name":"Rosca direta","nameEn":"Barbell curl","muscle":"Bíceps","secondary":["Antebraço"],"equipment":"Barra"},
          {"id":"agachamento","name":"Agachamento livre","nameEn":"Back squat","muscle":"Quadríceps","secondary":["Glúteos"],"equipment":"Barra"},
          {"id":"sem-nome","nameEn":"No name","muscle":"Peito","secondary":[],"equipment":"Barra"},
          {"id":"","name":"Sem id","nameEn":"No id","muscle":"Peito","secondary":[],"equipment":"Barra"},
          {"id":"supino-reto-barra","name":"Duplicado","nameEn":"Dup","muscle":"Peito","secondary":[],"equipment":"Barra"},
          "lixo",
          {"id":"sem-sec","name":"Prancha","nameEn":"Plank","muscle":"Abdômen","equipment":"Peso corporal"}
        ]
    """.trimIndent()
    private val items = ExerciseCatalog.parse(raw)

    private fun names(query: String, muscle: String? = null, equipment: String? = null) =
        ExerciseCatalog.search(items, query, muscle, equipment).map { it.name }

    @Test fun parsesValidItemsAndSkipsMalformed() {
        assertEquals(listOf("supino-reto-halteres", "supino-reto-barra", "triceps-pulley", "rosca-direta", "agachamento", "sem-sec"), items.map { it.id })
        assertEquals(listOf("Tríceps", "Ombros"), items[0].secondary)
        assertTrue(items.last().secondary.isEmpty())
    }

    @Test fun invalidJsonYieldsEmptyList() {
        assertTrue(ExerciseCatalog.parse("not json").isEmpty())
        assertTrue(ExerciseCatalog.parse("{}").isEmpty())
    }

    @Test fun emptyQueryReturnsEverythingAlphabetical() {
        val all = names("")
        assertEquals(items.size, all.size)
        assertEquals("Agachamento livre", all.first())
    }

    @Test fun ignoresCaseAndAccents() {
        assertEquals(listOf("Tríceps pulley"), names("TRICEPS"))
        assertEquals(listOf("Tríceps pulley"), names("tríceps"))
    }

    @Test fun matchesAllWordsInAnyOrder() {
        assertEquals(listOf("Supino reto com halteres"), names("supino halter"))
        assertEquals(listOf("Supino reto com halteres"), names("halter supino"))
    }

    @Test fun matchesEnglishNameMuscleAndEquipment() {
        assertEquals(listOf("Rosca direta"), names("curl"))
        assertEquals(listOf("Rosca direta"), names("biceps"))
        assertEquals(listOf("Tríceps pulley"), names("polia"))
    }

    @Test fun filtersByMuscleAndEquipment() {
        assertEquals(listOf("Supino reto", "Supino reto com halteres"), names("", muscle = "Peito"))
        assertEquals(listOf("Supino reto com halteres"), names("", muscle = "Peito", equipment = "Halteres"))
        assertEquals(listOf("Agachamento livre", "Rosca direta", "Supino reto"), names("", equipment = "Barra"))
    }

    @Test fun ranksNamePrefixThenWordPrefixThenSubstring() {
        val fixture = ExerciseCatalog.parse(
            """[
            {"id":"a","name":"Remada com barra","nameEn":"x","muscle":"Costas","secondary":[],"equipment":"Barra"},
            {"id":"b","name":"Barra fixa","nameEn":"x","muscle":"Costas","secondary":[],"equipment":"Peso corporal"},
            {"id":"c","name":"Levantamento","nameEn":"x","muscle":"Costas","secondary":[],"equipment":"Subbarra"}
            ]"""
        )
        assertEquals(listOf("Barra fixa", "Remada com barra", "Levantamento"), ExerciseCatalog.search(fixture, "barra").map { it.name })
    }

    @Test fun noMatchReturnsEmpty() {
        assertTrue(names("zzzz").isEmpty())
        assertTrue(names("supino agachamento").isEmpty())
    }

    @Test fun infersKindFromEquipmentMuscleAndHolds() {
        assertEquals("timed", ExerciseCatalog.inferKind("Prancha", "Plank", "Abdômen", "Peso corporal"))
        assertEquals("timed", ExerciseCatalog.inferKind("Hollow hold", "Hollow body hold", "Abdômen", "Peso corporal"))
        assertEquals("timed", ExerciseCatalog.inferKind("Pendurar na barra", "Dead hang", "Antebraço", "Peso corporal"))
        assertEquals("cardio", ExerciseCatalog.inferKind("Corrida", "Run", "Cardio", "Peso corporal"))
        assertEquals("bodyweight", ExerciseCatalog.inferKind("Flexão", "Push-up", "Peito", "Peso corporal"))
        assertEquals("reps", ExerciseCatalog.inferKind("Supino reto", "Bench press", "Peito", "Barra"))
        assertEquals("timed", ExerciseCatalog.inferKind("Minha PRANCHA lateral"))
    }

    @Test fun shippedCatalogDefaultsLookRight() {
        val catalog = ExerciseCatalog.parse(java.io.File("src/main/assets/exercises.json").readText())
        val byId = catalog.associateBy { it.id }
        assertEquals("timed", byId.getValue("prancha").defaultKind)
        assertEquals("timed", byId.getValue("agachamento-na-parede").defaultKind)
        assertEquals("reps", byId.getValue("hang-clean").defaultKind)
        assertTrue(catalog.filter { it.muscle == "Cardio" }.all { it.defaultKind == "cardio" })
    }
}
