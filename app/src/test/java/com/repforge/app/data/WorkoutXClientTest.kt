package com.repforge.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutXClientTest {
    @Test fun parsesBareArray() {
        val parsed = WorkoutXClient.parseExercises("""[{"id":"0001","name":"3/4 sit-up","target":"abs","equipment":"body weight"}]""")
        assertEquals(listOf(RemoteExercise("0001", "3/4 sit-up", "abs", "body weight")), parsed)
    }

    @Test fun parsesWrappedArrayAndSkipsIncompleteItems() {
        val parsed = WorkoutXClient.parseExercises("""{"data":[{"id":7,"name":"squat"},{"name":"no id"},"junk"]}""")
        assertEquals(listOf(RemoteExercise("7", "squat", "", "")), parsed)
    }

    @Test fun unknownShapeYieldsNoResults() {
        assertEquals(emptyList<RemoteExercise>(), WorkoutXClient.parseExercises("""{"message":"ok"}"""))
    }
}
