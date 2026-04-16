package com.ypg.neville.feature.emotionalanchors.domain

data class BreathingTechnique(
    val id: String,
    val name: String,
    val pattern: String,
    val guide: String,
    val shortEffect: String
)

object BreathingTechniques {
    val quickStressRelief: List<BreathingTechnique> = listOf(
        BreathingTechnique(
            id = "physiological_sigh",
            name = "Suspiro fisiológico",
            pattern = "2 inhalaciones nasales + 1 exhalación larga por boca",
            guide = "Haz una inhalación nasal normal, añade una segunda inhalación corta para expandir pulmón y suelta una exhalación larga por la boca. Repite 3 a 5 veces.",
            shortEffect = "Reduce activación en segundos y corta picos de ansiedad."
        ),
        BreathingTechnique(
            id = "coherent_4_6",
            name = "Respiración coherente 4-6",
            pattern = "Inhala 4s y exhala 6s",
            guide = "Respira por la nariz 4 segundos y exhala 6 segundos, suave y continuo. Mantén entre 1 y 3 minutos.",
            shortEffect = "Baja frecuencia cardiaca y mejora sensación de control."
        ),
        BreathingTechnique(
            id = "box_4_4_4_4",
            name = "Caja 4-4-4-4",
            pattern = "Inhala 4s, pausa 4s, exhala 4s, pausa 4s",
            guide = "Sigue cuatro fases iguales de 4 segundos. Completa de 4 a 6 ciclos.",
            shortEffect = "Ordena mente y foco cuando hay sobrecarga mental."
        ),
        BreathingTechnique(
            id = "relax_4_7_8",
            name = "4-7-8",
            pattern = "Inhala 4s, sostén 7s, exhala 8s",
            guide = "Haz ciclos lentos manteniendo el patrón 4-7-8. Empieza con 3 o 4 rondas.",
            shortEffect = "Disminuye hiperactivación y favorece calma rápida."
        ),
        BreathingTechnique(
            id = "long_exhale_4_8",
            name = "Exhalación larga 4-8",
            pattern = "Inhala 4s, exhala 8s",
            guide = "Respira cómodo, priorizando exhalación más larga que inhalación. Practica 1 a 2 minutos.",
            shortEffect = "Activa sistema parasimpático y reduce tensión corporal."
        )
    )
}
