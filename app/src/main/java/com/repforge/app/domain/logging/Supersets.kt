package com.repforge.app.domain.logging

/**
 * Superset groups are stored per exercise (null = none) and only mean something for consecutive exercises
 * sharing an id. All functions work on the group ids in display order.
 */

/** Keeps only runs of 2+ consecutive equal ids; a lone exercise loses its group and a repeated id in a later run gets a fresh one. */
fun normalizeGroups(groups: List<Int?>): List<Int?> {
    val result = groups.toMutableList()
    var nextId = (groups.filterNotNull().maxOrNull() ?: 0) + 1
    val seen = mutableSetOf<Int>()
    var i = 0
    while (i < groups.size) {
        val g = groups[i]
        var j = i
        while (g != null && j + 1 < groups.size && groups[j + 1] == g) j++
        if (g != null) {
            val id = if (g in seen) nextId++ else g
            seen += id
            for (k in i..j) result[k] = if (j > i) id else null
        }
        i = j + 1
    }
    return result
}

/** Links [index] with the next exercise, merging any group the next one already had. */
fun joinWithNext(groups: List<Int?>, index: Int): List<Int?> {
    if (index !in 0 until groups.lastIndex) return groups
    val id = groups[index] ?: groups[index + 1] ?: ((groups.filterNotNull().maxOrNull() ?: 0) + 1)
    val old = groups[index + 1]
    return normalizeGroups(groups.mapIndexed { i, g -> if (i == index || i == index + 1 || (old != null && g == old)) id else g })
}

fun separateAt(groups: List<Int?>, index: Int): List<Int?> =
    if (index !in groups.indices) groups else normalizeGroups(groups.toMutableList().also { it[index] = null })

/** The rest timer only runs after the last exercise of a superset (or a regular exercise). */
fun isGroupEnd(groups: List<Int?>, index: Int): Boolean = groups[index] == null || groups.getOrNull(index + 1) != groups[index]
