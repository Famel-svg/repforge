package com.repforge.app.domain.templates

data class TemplateExercise(val name: String, val target: String)

data class WorkoutTemplate(
    val id: String,
    val group: String,
    val label: String,
    val sheetName: String,
    val exercises: List<TemplateExercise>
)

private fun ex(name: String, target: String) = TemplateExercise(name, target)

val StarterTemplates: List<WorkoutTemplate> = listOf(
    WorkoutTemplate(
        "full-body", "Full body", "Full body iniciante", "Full body iniciante",
        listOf(
            ex("Agachamento livre", "Pernas"),
            ex("Supino reto com halteres", "Peito"),
            ex("Remada curvada", "Costas"),
            ex("Desenvolvimento com halteres", "Ombros"),
            ex("Leg press 45°", "Pernas"),
            ex("Rosca direta", "Bíceps"),
            ex("Tríceps corda", "Tríceps"),
            ex("Prancha", "Abdômen")
        )
    ),
    WorkoutTemplate(
        "abc-a", "ABC", "A — Peito, Tríceps e Ombros", "ABC — A (Peito/Tríceps/Ombros)",
        listOf(
            ex("Supino reto", "Peito"),
            ex("Supino inclinado com halteres", "Peito"),
            ex("Crucifixo na máquina", "Peito"),
            ex("Desenvolvimento com halteres", "Ombros"),
            ex("Elevação lateral", "Ombros"),
            ex("Tríceps corda", "Tríceps"),
            ex("Tríceps testa", "Tríceps")
        )
    ),
    WorkoutTemplate(
        "abc-b", "ABC", "B — Costas e Bíceps", "ABC — B (Costas/Bíceps)",
        listOf(
            ex("Puxada frontal", "Costas"),
            ex("Remada curvada", "Costas"),
            ex("Remada baixa", "Costas"),
            ex("Crucifixo inverso", "Ombros"),
            ex("Rosca direta", "Bíceps"),
            ex("Rosca martelo", "Bíceps")
        )
    ),
    WorkoutTemplate(
        "abc-c", "ABC", "C — Pernas", "ABC — C (Pernas)",
        listOf(
            ex("Agachamento livre", "Quadríceps"),
            ex("Leg press 45°", "Quadríceps"),
            ex("Cadeira extensora", "Quadríceps"),
            ex("Mesa flexora", "Posterior de coxa"),
            ex("Stiff", "Posterior de coxa"),
            ex("Panturrilha em pé", "Panturrilhas")
        )
    ),
    WorkoutTemplate(
        "upper", "Upper/Lower", "Upper — Parte superior", "Upper (Superiores)",
        listOf(
            ex("Supino reto", "Peito"),
            ex("Puxada frontal", "Costas"),
            ex("Desenvolvimento com halteres", "Ombros"),
            ex("Remada baixa", "Costas"),
            ex("Rosca direta", "Bíceps"),
            ex("Tríceps corda", "Tríceps")
        )
    ),
    WorkoutTemplate(
        "lower", "Upper/Lower", "Lower — Parte inferior", "Lower (Inferiores)",
        listOf(
            ex("Agachamento livre", "Quadríceps"),
            ex("Leg press 45°", "Quadríceps"),
            ex("Stiff", "Posterior de coxa"),
            ex("Mesa flexora", "Posterior de coxa"),
            ex("Cadeira abdutora", "Glúteos"),
            ex("Panturrilha em pé", "Panturrilhas")
        )
    )
)
