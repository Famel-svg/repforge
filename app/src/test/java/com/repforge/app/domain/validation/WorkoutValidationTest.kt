package com.repforge.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutValidationTest {
    @Test fun acceptsPositiveSetWithZeroWeight() {
        assertNull(validateSetInput(3, 10, 0.0))
    }

    @Test fun rejectsMissingValues() {
        assertEquals("Preencha séries, repetições e carga.", validateSetInput(null, 10, 20.0)?.message)
    }

    @Test fun rejectsNonPositiveReps() {
        assertEquals("Repetições devem ser maiores que zero.", validateSetInput(3, 0, 20.0)?.message)
    }

    @Test fun rejectsNegativeWeight() {
        assertEquals("Carga não pode ser negativa.", validateSetInput(3, 10, -1.0)?.message)
    }
}
