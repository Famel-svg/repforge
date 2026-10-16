package com.repforge.app.data

import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
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
        assertEquals(listOf("warmup", "normal"), sheet.exercises[0].entries.map { it.setType })
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
        assertEquals(AppError.CsvFormat, (error as AppException).error)
    }

    @Test
    fun mapsStrongSetOrderMarkersAndFlagsPossiblePounds() {
        val csv = "Date,Workout Name,Exercise Name,Set Order,Weight,Reps\n" +
            "2024-05-01 18:30:00,A,Squat,W,60,10\n" +
            "2024-05-01 18:30:00,A,Squat,1,350,5\n" +
            "2024-05-01 18:30:00,A,Squat,D,300,5\n" +
            "2024-05-01 18:30:00,A,Squat,F,320,2\n"
        val result = parseCsvWorkoutsWithStats(csv, utc)
        assertEquals(listOf("warmup", "normal", "drop", "failure"), result.sheets.single().exercises.single().entries.map { it.setType })
        assertEquals(true, result.maybePounds)
    }

    @Test
    fun importsSecondsAsTimedAndDistanceAsCardio() {
        val csv = "Date,Workout Name,Exercise Name,Set Order,Weight,Weight Unit,Reps,Distance,Seconds\n" +
            "2024-05-01 18:30:00,Core,Plank,1,0,kg,,0,45\n" +
            "2024-05-01 18:30:00,Core,Plank,2,0,kg,0,0,60\n" +
            "2024-05-01 18:30:00,Core,Run,1,0,kg,,5000,1500\n" +
            "2024-05-01 18:30:00,Core,Squat,1,50,kg,5,0,0\n" +
            "2024-05-01 18:30:00,Core,Empty,1,0,kg,,0,0\n"
        val result = parseCsvWorkoutsWithStats(csv, utc)
        assertEquals(1, result.skippedRows)
        val byName = result.sheets.single().exercises.associateBy { it.name }
        val plank = byName.getValue("Plank")
        assertEquals("timed", plank.kind)
        assertEquals(listOf(45, 60), plank.entries.map { it.durationSec })
        assertEquals(listOf(1, 1), plank.entries.map { it.reps })
        val run = byName.getValue("Run")
        assertEquals("cardio", run.kind)
        assertEquals(5.0, run.entries.single().distanceKm!!, 1e-9)
        assertEquals(1500, run.entries.single().durationSec)
        assertEquals("reps", byName.getValue("Squat").kind)
        assertEquals(null, byName.getValue("Squat").entries.single().durationSec)
    }

    @Test
    fun importsHevyDurationAndDistanceColumns() {
        val csv = "title,start_time,exercise_title,set_index,set_type,weight_kg,reps,distance_km,duration_seconds\n" +
            "\"Day\",\"1 May 2024, 18:30\",Plank,0,normal,,,,50\n" +
            "\"Day\",\"1 May 2024, 18:30\",Bike,0,normal,,,12.5,1800\n"
        val exercises = parseCsvWorkouts(csv, utc).single().exercises.associateBy { it.name }
        assertEquals("timed", exercises.getValue("Plank").kind)
        assertEquals("cardio", exercises.getValue("Bike").kind)
        assertEquals(12.5, exercises.getValue("Bike").entries.single().distanceKm!!, 1e-9)
    }
}
