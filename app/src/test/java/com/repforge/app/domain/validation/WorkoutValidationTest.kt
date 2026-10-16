package com.repforge.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutValidationTest {
    @Test fun acceptsPositiveSetWithZeroWeight() {
        assertNull(validateSetInput(3, 10, 0.0))
    }

    @Test fun rejectsMissingValues() {
        assertEquals(AppError.SetMissing, validateSetInput(null, 10, 20.0))
    }

    @Test fun rejectsNonPositiveReps() {
        assertEquals(AppError.RepsNotPositive, validateSetInput(3, 0, 20.0))
    }

    @Test fun rejectsNegativeWeight() {
        assertEquals(AppError.WeightNegative, validateSetInput(3, 10, -1.0))
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
        assertEquals(AppError.BodyWeightRange, validateBodyWeight(19.9))
        assertEquals(AppError.BodyWeightRange, validateBodyWeight(400.1))
        assertEquals(AppError.BodyWeightMissing, validateBodyWeight(null))
    }
}
