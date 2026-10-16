package com.repforge.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.ZoneOffset

class CsvImportTest {
    private val utc = ZoneOffset.UTC

    @Test
    fun parsesStrongWithQuotesLbsAndBadRow() {
        val csv = "﻿Date,Workout Name,Exercise Name,Set Order,Weight,Weight Unit,Reps,Notes\n" +
            "2024-05-01 18:30:00,\"Push, A\",\"Bench \"\"Flat\"\"\",1,100,lb,5,\"line1\nline2\"\n" +
            "2024-05-01 18:30:00,\"Push, A\",\"Bench \"\"Flat\"\"\",2,60,kg,8,\n" +
            "2024-05-01 18:30:00,\"Push, A\",Plank,1,0,kg,,\n" +
            "not-a-date,\"Push, A\",Squat,1,50,kg,5,\n"
        val result = parseCsvWorkoutsWithStats(csv, utc)
        assertEquals(2, result.skippedRows)
        val sheet = result.sheets.single()
        assertEquals("Push, A", sheet.name)
        val bench = sheet.exercises.single()
        assertEquals("Bench \"Flat\"", bench.name)
        assertEquals(listOf(5, 8), bench.entries.map { it.reps })
        assertEquals(45.359237, bench.entries[0].weightKg, 1e-6)
        assertEquals(60.0, bench.entries[1].weightKg, 1e-6)
        assertEquals(1714588200000L, bench.entries[0].recordedAt)
    }

    @Test
    fun parsesHevyWithSemicolonAndLbs() {
        val csv = "title;start_time;exercise_title;set_index;set_type;weight_lbs;reps\r\n" +
            "Legs;1 May 2024, 18:30;Squat;0;warmup;45,5;10\r\n" +
            "Legs;1 May 2024, 18:30;Squat;1;normal;100;5\r\n" +
            "Legs;1 May 2024, 18:30;Lunge;0;normal;20;12\r\n"
        val sheet = parseCsvWorkouts(csv, utc).single()
        assertEquals(listOf("Squat", "Lunge"), sheet.exercises.map { it.name })
        assertEquals(listOf(0, 1), sheet.exercises.map { it.position })
        assertEquals(2, sheet.exercises[0].entries.size)
        assertEquals(100 * 0.45359237, sheet.exercises[0].entries[1].weightKg, 1e-6)
        assertEquals(1714588200000L, sheet.updatedAt)
    }

    @Test
    fun parsesHevyKg() {
        val csv = "title,start_time,exercise_title,set_index,set_type,weight_kg,reps\n" +
            "Pull,\"1 May 2024, 18:30\",Row,0,normal,50,8\n"
        assertEquals(50.0, parseCsvWorkouts(csv, utc).single().exercises.single().entries.single().weightKg, 1e-6)
    }

    @Test
    fun rejectsUnknownHeader() {
        val error = assertThrows(IllegalArgumentException::class.java) { parseCsvWorkouts("a,b,c\n1,2,3\n", utc) }
        assertEquals("Formato CSV não reconhecido (use exportação do Strong ou Hevy).", error.message)
    }
}
