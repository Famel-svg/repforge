package com.repforge.app.domain.plates

val BAR_OPTIONS_KG = listOf(20.0, 15.0, 10.0)
val DEFAULT_PLATES_KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

/** [perSideKg] is largest first; [loadKg] is what the bar actually weighs loaded, which is below the target when no exact fit exists. */
data class PlateLoad(val perSideKg: List<Double>, val loadKg: Double, val exact: Boolean)

/** Greedy largest-first with unlimited pairs. Works in hundredths of a kg so 1.25 plates never drift. */
fun calculatePlates(targetKg: Double, barKg: Double = 20.0, platesKg: List<Double> = DEFAULT_PLATES_KG): PlateLoad {
    val bar = Math.round(barKg * 100)
    var perSide = (Math.round(targetKg * 100) - bar).coerceAtLeast(0) / 2
    val used = mutableListOf<Double>()
    for (plate in platesKg.filter { it > 0 }.sortedDescending()) {
        val p = Math.round(plate * 100)
        while (perSide >= p) { used += plate; perSide -= p }
    }
    val load = bar + used.sumOf { Math.round(it * 100) } * 2
    return PlateLoad(used, load / 100.0, load == Math.round(targetKg * 100))
}
