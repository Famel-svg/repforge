package com.repforge.app.domain.templates

/** [name]/[target] are catalog (pt-BR) keys; the UI localizes them. */
data class TemplateExercise(val name: String, val target: String)

data class WorkoutTemplate(
    val id: String,
    val exercises: List<TemplateExercise>,
    /** Localized by the UI when the sheet is created; display names live in string resources keyed by [id]. */
    val sheetName: String = ""
)

private fun ex(name: String, target: String) = TemplateExercise(name, target)

val StarterTemplates: List<WorkoutTemplate> = listOf(
    WorkoutTemplate(
        "full-body",
        listOf(
            ex("Agachamento livre", "Pernas"),
            ex("Supino reto com halteres", "Peito"),
            ex("Remada curvada com barra", "Costas"),
            ex("Desenvolvimento com halteres sentado", "Ombros"),
            ex("Leg press 45°", "Pernas"),
            ex("Rosca direta com barra", "Bíceps"),
            ex("Tríceps pulley com corda", "Tríceps"),
            ex("Prancha", "Abdômen")
        )
    ),
    WorkoutTemplate(
        "abc-a",
        listOf(
            ex("Supino reto com barra", "Peito"),
            ex("Supino inclinado com halteres", "Peito"),
            ex("Voador / Peck deck", "Peito"),
            ex("Desenvolvimento com halteres sentado", "Ombros"),
            ex("Elevação lateral com halteres", "Ombros"),
            ex("Tríceps pulley com corda", "Tríceps"),
            ex("Tríceps testa com barra W", "Tríceps")
        )
    ),
    WorkoutTemplate(
        "abc-b",
        listOf(
            ex("Puxada frontal", "Costas"),
            ex("Remada curvada com barra", "Costas"),
            ex("Remada baixa", "Costas"),
            ex("Crucifixo invertido na máquina", "Ombros"),
            ex("Rosca direta com barra", "Bíceps"),
            ex("Rosca martelo", "Bíceps")
        )
    ),
    WorkoutTemplate(
        "abc-c",
        listOf(
            ex("Agachamento livre", "Quadríceps"),
            ex("Leg press 45°", "Quadríceps"),
            ex("Cadeira extensora", "Quadríceps"),
            ex("Mesa flexora", "Posterior de coxa"),
            ex("Stiff com barra", "Posterior de coxa"),
            ex("Panturrilha em pé na máquina", "Panturrilhas")
        )
    ),
    WorkoutTemplate(
        "upper",
        listOf(
            ex("Supino reto com barra", "Peito"),
            ex("Puxada frontal", "Costas"),
            ex("Desenvolvimento com halteres sentado", "Ombros"),
            ex("Remada baixa", "Costas"),
            ex("Rosca direta com barra", "Bíceps"),
            ex("Tríceps pulley com corda", "Tríceps")
        )
    ),
    WorkoutTemplate(
        "lower",
        listOf(
            ex("Agachamento livre", "Quadríceps"),
            ex("Leg press 45°", "Quadríceps"),
            ex("Stiff com barra", "Posterior de coxa"),
            ex("Mesa flexora", "Posterior de coxa"),
            ex("Cadeira abdutora", "Glúteos"),
            ex("Panturrilha em pé na máquina", "Panturrilhas")
        )
    )
)
