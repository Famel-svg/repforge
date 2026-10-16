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

    @Test fun parsesDecimalCommaAndPoint() {
        assertEquals(80.5, parseWeightKg(" 80,5 ")!!, 0.001)
        assertEquals(80.5, parseWeightKg("80.5")!!, 0.001)
    }

    @Test fun rejectsNonNumericWeight() {
        assertNull(parseWeightKg("abc"))
        assertNull(parseWeightKg(""))
        assertNull(parseWeightKg("NaN"))
    }

    @Test fun bodyWeightRange() {
        assertNull(validateBodyWeight(20.0))
        assertNull(validateBodyWeight(400.0))
        assertNull(validateBodyWeight(82.5))
        assertEquals("Peso deve ficar entre 20 e 400 kg.", validateBodyWeight(19.9)?.message)
        assertEquals("Peso deve ficar entre 20 e 400 kg.", validateBodyWeight(400.1)?.message)
        assertEquals("Informe o peso em kg.", validateBodyWeight(null)?.message)
    }
}
