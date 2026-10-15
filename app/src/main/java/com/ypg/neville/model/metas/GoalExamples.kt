package com.ypg.neville.model.metas

/**
 * A complete goal configuration that can be loaded into the new-goal form.
 *
 * The catalogue mirrors the iOS GoalExamples implementation. Android currently
 * schedules weekly goals by a number of days rather than named weekdays, so the
 * selected iOS weekdays are represented by [weeklyDaysPerWeek]. Exact weekly
 * times are retained and converted to the closest Android day period when the
 * example is applied.
 */
data class GoalExample(
    val id: String,
    val category: String,
    val emoji: String,
    val accentArgb: Long,
    val title: String,
    val details: String,
    val configurationSummary: String,
    val customUnitLabel: String,
    val executionTargetValue: Double,
    val completionBasis: GoalCompletionBasis,
    val amount: Int,
    val durationValue: Int,
    val durationUnit: TimeUnitType,
    val scheduleType: GoalScheduleType,
    val intervalUnit: TimeUnitType,
    val frequency: Int,
    val weeklyDaysPerWeek: Int,
    val dayPeriod: GoalDayPeriod,
    val weeklyTimeMinutes: Int? = null,
    val specificDateOffsets: List<Int> = emptyList()
) {
    val androidDayPeriod: GoalDayPeriod
        get() = when {
            dayPeriod != GoalDayPeriod.ANYTIME -> dayPeriod
            weeklyTimeMinutes == null -> GoalDayPeriod.ANYTIME
            weeklyTimeMinutes < 12 * 60 -> GoalDayPeriod.MORNING
            weeklyTimeMinutes < 18 * 60 -> GoalDayPeriod.AFTERNOON
            else -> GoalDayPeriod.NIGHT
        }
}

object GoalExamples {
    private fun example(
        id: String,
        category: String,
        emoji: String,
        accentArgb: Long,
        title: String,
        details: String,
        summary: String,
        unitLabel: String,
        target: Double,
        completion: GoalCompletionBasis,
        amount: Int,
        durationValue: Int,
        durationUnit: TimeUnitType,
        schedule: GoalScheduleType,
        interval: TimeUnitType = TimeUnitType.DIAS,
        frequency: Int = 1,
        weeklyDays: Int = 0,
        period: GoalDayPeriod = GoalDayPeriod.ANYTIME,
        weeklyTimeMinutes: Int? = null,
        offsets: List<Int> = emptyList()
    ) = GoalExample(
        id = id,
        category = category,
        emoji = emoji,
        accentArgb = accentArgb,
        title = title,
        details = details,
        configurationSummary = summary,
        customUnitLabel = unitLabel,
        executionTargetValue = target,
        completionBasis = completion,
        amount = amount,
        durationValue = durationValue,
        durationUnit = durationUnit,
        scheduleType = schedule,
        intervalUnit = interval,
        frequency = frequency,
        weeklyDaysPerWeek = weeklyDays.coerceAtLeast(1),
        dayPeriod = period,
        weeklyTimeMinutes = weeklyTimeMinutes,
        specificDateOffsets = offsets
    )

    val all: List<GoalExample> = listOf(
        example(
            "meditacion-diaria", "Bienestar", "🧠", 0xFF8E44AD, "Meditar 10 minutos",
            "Reservar un momento tranquilo cada mañana para respirar y empezar el día con más claridad.",
            "Cada día · mañana · durante 30 días", "minutos", 10.0, GoalCompletionBasis.DURATION, 30, 30,
            TimeUnitType.DIAS, GoalScheduleType.INTERVAL, period = GoalDayPeriod.MORNING
        ),
        example(
            "caminar-cinco-km", "Salud", "🚶", 0xFF2EAD67, "Caminar 5 km",
            "Completar una caminata a buen ritmo para mejorar la resistencia y despejar la mente.",
            "Lunes, miércoles y viernes · tarde · 36 veces", "km", 5.0, GoalCompletionBasis.EXECUTIONS, 36, 30,
            TimeUnitType.DIAS, GoalScheduleType.WEEKLY, weeklyDays = 3, period = GoalDayPeriod.AFTERNOON
        ),
        example(
            "leer-libros", "Aprendizaje", "📚", 0xFF4F5BD5, "Leer 20 páginas",
            "Avanzar cada noche en un libro y sustituir unos minutos de pantalla por lectura.",
            "Cada día · noche · 25 veces", "páginas", 20.0, GoalCompletionBasis.EXECUTIONS, 25, 30,
            TimeUnitType.DIAS, GoalScheduleType.INTERVAL, period = GoalDayPeriod.NIGHT
        ),
        example(
            "entrenamiento-fuerza", "Deporte", "🏋️", 0xFFF08A24, "Entrenar fuerza 45 minutos",
            "Realizar una sesión completa de fuerza con calentamiento, ejercicios principales y vuelta a la calma.",
            "Martes, jueves y sábado · 18:30 · 24 veces", "minutos", 45.0, GoalCompletionBasis.EXECUTIONS, 24, 30,
            TimeUnitType.DIAS, GoalScheduleType.WEEKLY, weeklyDays = 3, weeklyTimeMinutes = 18 * 60 + 30
        ),
        example(
            "ahorro-mensual", "Finanzas", "€", 0xFF3AAE88, "Ahorrar 100 € al mes",
            "Separar el ahorro nada más empezar el mes y mantenerlo fuera de la cuenta de gastos cotidianos.",
            "Cada mes · 100 euros · 12 veces", "euros", 100.0, GoalCompletionBasis.EXECUTIONS, 12, 1,
            TimeUnitType.ANIOS, GoalScheduleType.INTERVAL, interval = TimeUnitType.MESES
        ),
        example(
            "llamar-familia", "Relaciones", "☎️", 0xFFE85B91, "Llamar a mi familia",
            "Dedicar una conversación sin prisas a ponerse al día y cuidar el vínculo con las personas importantes.",
            "Domingos · tarde · 12 veces", "llamada", 1.0, GoalCompletionBasis.EXECUTIONS, 12, 3,
            TimeUnitType.MESES, GoalScheduleType.WEEKLY, weeklyDays = 1, period = GoalDayPeriod.AFTERNOON
        ),
        example(
            "beber-agua", "Salud", "💧", 0xFF20A9C9, "Beber 8 vasos de agua",
            "Registrar al final del día que se alcanzó una hidratación suficiente, repartida a lo largo de la jornada.",
            "Cada día · 8 vasos · durante 30 días", "vasos", 8.0, GoalCompletionBasis.DURATION, 30, 30,
            TimeUnitType.DIAS, GoalScheduleType.INTERVAL
        ),
        example(
            "ordenar-casa", "Hogar", "🏠", 0xFF95613C, "Ordenar una zona de casa",
            "Elegir un cajón, armario o rincón, retirar lo innecesario y dejarlo organizado de forma sostenible.",
            "Sábados · mañana · 8 zonas", "zona", 1.0, GoalCompletionBasis.EXECUTIONS, 8, 2,
            TimeUnitType.MESES, GoalScheduleType.WEEKLY, weeklyDays = 1, period = GoalDayPeriod.MORNING
        ),
        example(
            "estudiar-idioma", "Aprendizaje", "🔤", 0xFF2F80ED, "Estudiar un idioma 30 minutos",
            "Combinar vocabulario, comprensión y conversación en una sesión breve pero constante.",
            "De lunes a viernes · 20:00 · durante 3 meses", "minutos", 30.0, GoalCompletionBasis.DURATION, 60, 3,
            TimeUnitType.MESES, GoalScheduleType.WEEKLY, weeklyDays = 5, weeklyTimeMinutes = 20 * 60
        ),
        example(
            "escritura-creativa", "Creatividad", "✍️", 0xFF198F8A, "Escribir 500 palabras",
            "Producir un primer borrador sin editar para avanzar de forma constante en un relato, diario o proyecto.",
            "Cada 2 días · mañana · 30 veces", "palabras", 500.0, GoalCompletionBasis.EXECUTIONS, 30, 2,
            TimeUnitType.MESES, GoalScheduleType.INTERVAL, frequency = 2, period = GoalDayPeriod.MORNING
        ),
        example(
            "desconexion-digital", "Bienestar", "📵", 0xFFD64C4C, "Desconectar del móvil 2 horas",
            "Dejar el teléfono fuera de alcance para disfrutar de una tarde semanal sin redes ni notificaciones.",
            "Domingos · 17:00 · 12 veces", "horas", 2.0, GoalCompletionBasis.EXECUTIONS, 12, 3,
            TimeUnitType.MESES, GoalScheduleType.WEEKLY, weeklyDays = 1, weeklyTimeMinutes = 17 * 60
        ),
        example(
            "revisiones-preventivas", "Organización", "🗓️", 0xFF6E7781, "Completar mis revisiones preventivas",
            "Programar tres hitos para pedir cita, realizar la revisión y comprobar o archivar los resultados.",
            "3 fechas específicas · en 7, 30 y 60 días", "revisión", 1.0, GoalCompletionBasis.EXECUTIONS, 3, 2,
            TimeUnitType.MESES, GoalScheduleType.SPECIFIC_DATES, period = GoalDayPeriod.MORNING, offsets = listOf(7, 30, 60)
        ),
        example(
            "pausa-visual", "Salud digital", "👁️", 0xFF20A9C9, "Descansar la vista 20 segundos",
            "Apartar la mirada de la pantalla y enfocar un punto lejano para reducir la fatiga visual durante una jornada intensa.",
            "Cada 30 minutos · 20 segundos · 12 veces", "segundos", 20.0, GoalCompletionBasis.EXECUTIONS, 12, 1,
            TimeUnitType.DIAS, GoalScheduleType.INTERVAL, interval = TimeUnitType.MINUTOS, frequency = 30
        ),
        example(
            "pausa-activa", "Salud", "🤸", 0xFF2EAD67, "Hacer una pausa activa de 5 minutos",
            "Levantarse, movilizar hombros y cadera y caminar un poco para romper periodos largos de sedentarismo.",
            "Cada 2 horas · 5 minutos · 8 veces", "minutos", 5.0, GoalCompletionBasis.EXECUTIONS, 8, 1,
            TimeUnitType.DIAS, GoalScheduleType.INTERVAL, interval = TimeUnitType.HORAS, frequency = 2
        ),
        example(
            "revision-semanal", "Organización", "✅", 0xFF4F5BD5, "Hacer una revisión semanal",
            "Revisar compromisos, celebrar avances, cerrar pendientes y elegir las tres prioridades de la siguiente semana.",
            "Cada semana · 30 minutos · durante 3 meses", "minutos", 30.0, GoalCompletionBasis.DURATION, 12, 3,
            TimeUnitType.MESES, GoalScheduleType.INTERVAL, interval = TimeUnitType.SEMANAS
        ),
        example(
            "salida-naturaleza", "Bienestar", "🌿", 0xFF2EAD67, "Pasar 90 minutos en la naturaleza",
            "Salir a un parque, bosque o entorno natural sin prisas, caminar y prestar atención consciente al entorno.",
            "Cada 2 semanas · 90 minutos · durante 6 meses", "minutos", 90.0, GoalCompletionBasis.DURATION, 13, 6,
            TimeUnitType.MESES, GoalScheduleType.INTERVAL, interval = TimeUnitType.SEMANAS, frequency = 2
        ),
        example(
            "limpieza-digital-mensual", "Organización", "🗂️", 0xFF6E7781, "Ordenar 50 archivos digitales",
            "Eliminar duplicados, archivar documentos útiles y vaciar descargas para mantener un espacio digital manejable.",
            "Cada mes · 50 archivos · 6 veces", "archivos", 50.0, GoalCompletionBasis.EXECUTIONS, 6, 6,
            TimeUnitType.MESES, GoalScheduleType.INTERVAL, interval = TimeUnitType.MESES
        ),
        example(
            "chequeo-anual", "Salud", "🩺", 0xFFD64C4C, "Realizar un chequeo preventivo anual",
            "Revisar con un profesional qué controles corresponden según la edad y los antecedentes, y dar seguimiento a los resultados.",
            "Cada año · 1 chequeo · durante 3 años", "chequeo", 1.0, GoalCompletionBasis.DURATION, 3, 3,
            TimeUnitType.ANIOS, GoalScheduleType.INTERVAL, interval = TimeUnitType.ANIOS
        ),
        example(
            "diario-gratitud", "Bienestar", "💗", 0xFFE85B91, "Escribir 3 motivos de gratitud",
            "Anotar experiencias concretas, personas o pequeños detalles positivos para entrenar una mirada más equilibrada del día.",
            "Lunes, miércoles y viernes · noche · durante 8 semanas", "motivos", 3.0, GoalCompletionBasis.DURATION, 24, 8,
            TimeUnitType.SEMANAS, GoalScheduleType.WEEKLY, weeklyDays = 3, period = GoalDayPeriod.NIGHT
        ),
        example(
            "luz-natural", "Sueño", "☀️", 0xFFF08A24, "Tomar 15 minutos de luz natural",
            "Salir al exterior al empezar el día para apoyar el ritmo circadiano, el estado de alerta y el descanso nocturno.",
            "Cada día · mañana · durante 6 semanas", "minutos", 15.0, GoalCompletionBasis.DURATION, 42, 6,
            TimeUnitType.SEMANAS, GoalScheduleType.INTERVAL, period = GoalDayPeriod.MORNING
        ),
        example(
            "planificar-comidas", "Nutrición", "🍽️", 0xFF3AAE88, "Planificar 5 comidas saludables",
            "Elegir platos sencillos, comprobar ingredientes y preparar una lista de compra para reducir decisiones impulsivas.",
            "Domingos · 11:00 · durante 3 meses", "comidas", 5.0, GoalCompletionBasis.DURATION, 13, 3,
            TimeUnitType.MESES, GoalScheduleType.WEEKLY, weeklyDays = 1, weeklyTimeMinutes = 11 * 60
        ),
        example(
            "yoga-matutino", "Movimiento", "🧘", 0xFF8E44AD, "Practicar yoga 25 minutos",
            "Combinar movilidad, respiración y posturas básicas para comenzar el día con el cuerpo activo y la mente centrada.",
            "Martes y jueves · 07:00 · durante 8 semanas", "minutos", 25.0, GoalCompletionBasis.DURATION, 16, 8,
            TimeUnitType.SEMANAS, GoalScheduleType.WEEKLY, weeklyDays = 2, weeklyTimeMinutes = 7 * 60
        ),
        example(
            "trabajo-profundo", "Productividad", "⏱️", 0xFF2F80ED, "Completar 90 minutos de trabajo profundo",
            "Trabajar en una prioridad importante con notificaciones silenciadas, una tarea definida y sin cambiar de contexto.",
            "De lunes a viernes · 09:00 · 40 sesiones", "minutos", 90.0, GoalCompletionBasis.EXECUTIONS, 40, 8,
            TimeUnitType.SEMANAS, GoalScheduleType.WEEKLY, weeklyDays = 5, weeklyTimeMinutes = 9 * 60
        ),
        example(
            "cocinar-cenas-saludables", "Nutrición", "🥕", 0xFFF08A24, "Cocinar una cena saludable",
            "Preparar una cena basada en alimentos poco procesados, con verdura, proteína y una ración adecuada.",
            "Lunes, miércoles y domingo · noche · 24 cenas", "cena", 1.0, GoalCompletionBasis.EXECUTIONS, 24, 8,
            TimeUnitType.SEMANAS, GoalScheduleType.WEEKLY, weeklyDays = 3, period = GoalDayPeriod.NIGHT
        ),
        example(
            "rutina-sueno", "Sueño", "🌙", 0xFF4F5BD5, "Completar una rutina de sueño de 30 minutos",
            "Bajar la luz, dejar pantallas, preparar el día siguiente y realizar una actividad tranquila antes de acostarse.",
            "Cada día · noche · durante 6 semanas", "minutos", 30.0, GoalCompletionBasis.DURATION, 42, 6,
            TimeUnitType.SEMANAS, GoalScheduleType.INTERVAL, period = GoalDayPeriod.NIGHT
        ),
        example(
            "reflexion-terapeutica", "Autoconocimiento", "💬", 0xFF198F8A, "Realizar 4 sesiones de reflexión guiada",
            "Usar cada sesión para identificar patrones, preparar preguntas, registrar aprendizajes y acordar una acción concreta.",
            "4 fechas específicas · tarde · cada 2 semanas", "sesión", 1.0, GoalCompletionBasis.EXECUTIONS, 4, 2,
            TimeUnitType.MESES, GoalScheduleType.SPECIFIC_DATES, period = GoalDayPeriod.AFTERNOON, offsets = listOf(7, 21, 35, 49)
        ),
        example(
            "revision-valores", "Crecimiento personal", "🧭", 0xFF8E44AD, "Revisar mis valores y prioridades",
            "Responder por escrito qué importa ahora, qué está recibiendo mi tiempo y qué ajuste concreto quiero hacer.",
            "4 fechas específicas · noche · en 1, 3, 6 y 12 meses", "revisión", 1.0, GoalCompletionBasis.EXECUTIONS, 4, 1,
            TimeUnitType.ANIOS, GoalScheduleType.SPECIFIC_DATES, period = GoalDayPeriod.NIGHT, offsets = listOf(30, 90, 180, 365)
        ),
        example(
            "aprendizaje-primeros-auxilios", "Aprendizaje", "➕", 0xFFD64C4C, "Completar un curso de primeros auxilios",
            "Dividir el aprendizaje en cuatro hitos: teoría básica, práctica, repaso de emergencias y evaluación final.",
            "4 fechas específicas · cualquier momento · en 2 meses", "módulo", 1.0, GoalCompletionBasis.EXECUTIONS, 4, 2,
            TimeUnitType.MESES, GoalScheduleType.SPECIFIC_DATES, offsets = listOf(7, 21, 42, 60)
        ),
        example(
            "acto-amabilidad", "Relaciones", "🤲", 0xFFE85B91, "Realizar un acto consciente de amabilidad",
            "Ofrecer ayuda, agradecer de forma específica o dedicar atención plena a alguien sin esperar nada a cambio.",
            "Martes y viernes · tarde · durante 10 semanas", "acto", 1.0, GoalCompletionBasis.DURATION, 20, 10,
            TimeUnitType.SEMANAS, GoalScheduleType.WEEKLY, weeklyDays = 2, period = GoalDayPeriod.AFTERNOON
        ),
        example(
            "voluntariado", "Propósito", "👥", 0xFF2F80ED, "Dedicar 2 horas a una causa",
            "Colaborar de forma periódica con una iniciativa local alineada con los propios valores y necesidades de la comunidad.",
            "Cada mes · 2 horas · 6 veces", "horas", 2.0, GoalCompletionBasis.EXECUTIONS, 6, 6,
            TimeUnitType.MESES, GoalScheduleType.INTERVAL, interval = TimeUnitType.MESES
        )
    )
}
