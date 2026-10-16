package com.repforge.app.domain.progression

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RulesTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int) = LocalDate.of(2026, 3, day).atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun e(day: Int, reps: Int, kg: Double, sec: Int? = null, kind: String = KIND_REPS) =
        HistoryEntry(1, "X", "A", 1, reps, kg, at(day), kind = kind, durationSec = sec)
    private fun s(rule: String, kind: String, vararg h: HistoryEntry) = suggestNext(rule, kind, h.toList(), zone)

    @Test fun effectiveRulePrefersExerciseThenSheetThenDouble() {
        assertEquals(RULE_LINEAR, effectiveRule(RULE_LINEAR, RULE_NONE))
        assertEquals(RULE_NONE, effectiveRule(null, RULE_NONE))
        assertEquals(RULE_DOUBLE, effectiveRule("junk", null))
    }

    @Test fun noneAndCardioSuggestNothing() {
        assertNull(s(RULE_NONE, KIND_REPS, e(1, 12, 40.0)))
        assertNull(s(RULE_DOUBLE, KIND_CARDIO, e(1, 1, 0.0, 600)))
        assertNull(s(RULE_LINEAR, KIND_REPS))
    }

    @Test fun linearRaisesBy2_5AfterSuccess() {
        val r = s(RULE_LINEAR, KIND_REPS, e(1, 8, 40.0), e(1, 9, 40.0))!!
        assertEquals(ProgressionKind.RaiseLoad, r.kind)
        assertEquals(42.5, r.weightKg, 0.0)
    }

    @Test fun linearLightLoadRaisesBy1_25() {
        assertEquals(11.25, s(RULE_LINEAR, KIND_REPS, e(1, 8, 10.0))!!.weightKg, 0.0)
    }

    @Test fun linearMissedRepsKeepsLoad() {
        val r = s(RULE_LINEAR, KIND_REPS, e(1, 8, 40.0), e(1, 6, 40.0))!!
        assertEquals(ProgressionKind.KeepLoad, r.kind)
        assertEquals(40.0, r.weightKg, 0.0)
    }

    @Test fun linearTwoFailuresStillKeep() {
        assertEquals(ProgressionKind.KeepLoad, s(RULE_LINEAR, KIND_REPS, e(1, 6, 40.0), e(2, 7, 40.0))!!.kind)
    }

    @Test fun linearDeloadsTenPercentAfterThreeFailures() {
        val r = s(RULE_LINEAR, KIND_REPS, e(1, 6, 100.0), e(2, 7, 100.0), e(3, 5, 100.0))!!
        assertEquals(ProgressionKind.Deload, r.kind)
        assertEquals(90.0, r.weightKg, 0.0)
    }

    @Test fun linearSuccessBreaksFailureStreak() {
        val r = s(RULE_LINEAR, KIND_REPS, e(1, 6, 100.0), e(2, 6, 100.0), e(3, 8, 100.0), e(4, 6, 100.0))!!
        assertEquals(ProgressionKind.KeepLoad, r.kind)
    }

    @Test fun doubleRuleDelegatesToRangeLogic() {
        assertEquals(ProgressionKind.RaiseLoad, s(RULE_DOUBLE, KIND_REPS, e(1, 12, 40.0))!!.kind)
        assertEquals(ProgressionKind.KeepLoad, s(RULE_DOUBLE, KIND_REPS, e(1, 5, 40.0))!!.kind)
        assertEquals(ProgressionKind.AddRep, s(RULE_DOUBLE, KIND_REPS, e(1, 10, 40.0))!!.kind)
    }

    @Test fun doubleDeloadsAfterThreeDecliningSessions() {
        assertEquals(ProgressionKind.Deload, s(RULE_DOUBLE, KIND_REPS, e(3, 8, 50.0), e(2, 9, 50.0), e(1, 10, 50.0))!!.kind)
    }

    @Test fun bodyweightAddsRepsThenExtraLoad() {
        val add = s(RULE_DOUBLE, KIND_BODYWEIGHT, e(1, 9, 0.0, kind = KIND_BODYWEIGHT))!!
        assertEquals(ProgressionKind.AddRep, add.kind)
        assertEquals(10, add.reps)
        val extra = s(RULE_LINEAR, KIND_BODYWEIGHT, e(1, 12, 0.0, kind = KIND_BODYWEIGHT))!!
        assertEquals(ProgressionKind.ExtraLoad, extra.kind)
        assertEquals(EXTRA_LOAD_START_KG, extra.weightKg, 0.0)
    }

    @Test fun bodyweightBelowRangeNeverAddsLoad() {
        val r = s(RULE_DOUBLE, KIND_BODYWEIGHT, e(1, 4, 0.0, kind = KIND_BODYWEIGHT))!!
        assertEquals(ProgressionKind.KeepLoad, r.kind)
        assertEquals(0.0, r.weightKg, 0.0)
    }

    @Test fun weightedBodyweightUsesNormalRules() {
        assertEquals(ProgressionKind.RaiseLoad, s(RULE_DOUBLE, KIND_BODYWEIGHT, e(1, 12, 5.0, kind = KIND_BODYWEIGHT))!!.kind)
    }

    @Test fun timeFirstSessionSetsBaselinePlusFive() {
        val r = s(RULE_TIME, KIND_TIMED, e(1, 1, 0.0, 30, KIND_TIMED))!!
        assertEquals(35, r.durationSec)
    }

    @Test fun timeHitMovesTargetUp() {
        val r = s(RULE_TIME, KIND_TIMED, e(1, 1, 0.0, 30, KIND_TIMED), e(2, 1, 0.0, 36, KIND_TIMED))!!
        assertEquals(ProgressionKind.AddTime, r.kind)
        assertEquals(41, r.durationSec)
    }

    @Test fun timeMissKeepsTarget() {
        val r = s(RULE_TIME, KIND_TIMED, e(1, 1, 0.0, 30, KIND_TIMED), e(2, 1, 0.0, 32, KIND_TIMED))!!
        assertEquals(ProgressionKind.KeepTime, r.kind)
        assertEquals(35, r.durationSec)
    }

    @Test fun timeUsesWeakestSetAndTimedIgnoresDoubleRule() {
        val r = s(RULE_DOUBLE, KIND_TIMED, e(1, 1, 0.0, 30, KIND_TIMED), e(2, 1, 0.0, 50, KIND_TIMED), e(2, 1, 0.0, 34, KIND_TIMED))!!
        assertEquals(ProgressionKind.KeepTime, r.kind)
        assertEquals(35, r.durationSec)
    }

    @Test fun timedWithRuleNoneIsSilent() {
        assertNull(s(RULE_NONE, KIND_TIMED, e(1, 1, 0.0, 30, KIND_TIMED)))
    }
}
