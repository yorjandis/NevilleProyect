package com.ypg.neville.feature.transformation.ui

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.feature.transformation.domain.TransformationCatalog
import com.ypg.neville.feature.transformation.domain.TransformationConfiguration
import com.ypg.neville.feature.transformation.domain.TransformationDayEntry
import com.ypg.neville.feature.transformation.domain.TransformationExample
import com.ypg.neville.feature.transformation.domain.TransformationJournal
import com.ypg.neville.feature.transformation.domain.TransformationScore
import com.ypg.neville.feature.transformation.domain.TransformationState
import com.ypg.neville.feature.transformation.domain.currentDay
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val ProtocolBackground = Brush.verticalGradient(listOf(Color(0xFF292651), Color(0xFF315D75), Color(0xFF40918A)))
private val ProtocolViolet = Color(0xFF7058B5)
private val ProtocolBlue = Color(0xFF347CA3)
private val ProtocolMint = Color(0xFF278B79)
private val ProtocolInk = Color(0xFF211F2B)
private val ProtocolSecondary = Color(0xFF625F6D)
private val ProtocolButtonBackground = Color(0xFFB8E3F6)
private val ProtocolButtonBackgroundDisabled = Color(0xFFD8EAF2)
private val ProtocolButtonContent = Color(0xFF101820)
private val ProtocolFieldBackground = Color(0xFFE3F3FA)

private sealed interface ProtocolPage {
    data object Home : ProtocolPage
    data object Info : ProtocolPage
    data object Plan : ProtocolPage
    data object Insights : ProtocolPage
    data class Morning(val day: Int) : ProtocolPage
    data object Para : ProtocolPage
    data class Journal(val day: Int) : ProtocolPage
    data object Settings : ProtocolPage
    data class Day(val number: Int) : ProtocolPage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformationProtocolScreen(
    viewModel: TransformationViewModel,
    onClose: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var page: ProtocolPage by remember { mutableStateOf(ProtocolPage.Home) }
    var selectedExample by remember { mutableStateOf<TransformationExample?>(null) }
    BackHandler(enabled = page != ProtocolPage.Home) { page = ProtocolPage.Home }
    val configured = state.configuration != null
    val title = when (val current = page) {
        ProtocolPage.Home -> if (configured) "21 días" else "Transformación"
        ProtocolPage.Info -> "Cómo funciona"
        ProtocolPage.Plan -> "Ruta de 21 días"
        ProtocolPage.Insights -> "Tendencias"
        is ProtocolPage.Morning -> "Práctica guiada"
        ProtocolPage.Para -> "P.A.R.A."
        is ProtocolPage.Journal -> "Diario · Día ${current.day}"
        ProtocolPage.Settings -> "Ajustes"
        is ProtocolPage.Day -> "Día ${current.number}"
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = if (page == ProtocolPage.Home) onClose else ({ page = ProtocolPage.Home })) {
                        Icon(if (page == ProtocolPage.Home) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                actions = {
                    if (page == ProtocolPage.Home) {
                        IconButton(onClick = { page = ProtocolPage.Info }) { Icon(Icons.Rounded.Info, "Información") }
                        if (configured) IconButton(onClick = { page = ProtocolPage.Settings }) { Icon(Icons.Rounded.Settings, "Ajustes") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF292651), titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(Modifier.fillMaxSize().background(ProtocolBackground).padding(padding)) {
            when (val current = page) {
                ProtocolPage.Home -> if (state.configuration == null) {
                    SetupScreen(
                        example = selectedExample,
                        onStart = {
                            if (it.remindersEnabled) onRequestNotificationPermission()
                            viewModel.start(it)
                            selectedExample = null
                        }
                    )
                } else Dashboard(state, onPage = { page = it })
                ProtocolPage.Info -> InformationScreen(
                    hasCycle = configured,
                    onUseExample = {
                        if (configured) viewModel.reset()
                        selectedExample = it
                        page = ProtocolPage.Home
                    }
                )
                ProtocolPage.Plan -> PlanScreen(state) { page = ProtocolPage.Day(it) }
                ProtocolPage.Insights -> InsightsScreen(state)
                is ProtocolPage.Morning -> MorningPractice { viewModel.completeMorning(current.day); page = ProtocolPage.Home }
                ProtocolPage.Para -> ParaPractice(state.configuration?.alternativeBehavior.orEmpty()) { signal, emotion, action, seconds ->
                    viewModel.recordPara(signal, emotion, action, seconds); page = ProtocolPage.Home
                }
                is ProtocolPage.Journal -> JournalScreen(viewModel.entry(current.day)) { viewModel.updateEntry(it); page = ProtocolPage.Home }
                ProtocolPage.Settings -> SettingsScreen(state.configuration!!, onSave = {
                    if (it.remindersEnabled) onRequestNotificationPermission()
                    viewModel.updateConfiguration(it); page = ProtocolPage.Home
                }, onReset = { viewModel.reset(); page = ProtocolPage.Home })
                is ProtocolPage.Day -> DayScreen(
                    entry = viewModel.entry(current.number),
                    day = current.number,
                    onSave = viewModel::updateEntry,
                    onMorning = { page = ProtocolPage.Morning(current.number) },
                    onJournal = { page = ProtocolPage.Journal(current.number) }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(example: TransformationExample?, onStart: (TransformationConfiguration) -> Unit) {
    var patternName by rememberSaveable(example) { mutableStateOf(example?.patternName.orEmpty()) }
    var trigger by rememberSaveable(example) { mutableStateOf(example?.trigger.orEmpty()) }
    var thought by rememberSaveable(example) { mutableStateOf(example?.automaticThought.orEmpty()) }
    var emotion by rememberSaveable(example) { mutableStateOf(example?.emotion.orEmpty()) }
    var oldBehavior by rememberSaveable(example) { mutableStateOf(example?.oldBehavior.orEmpty()) }
    var consequence by rememberSaveable(example) { mutableStateOf(example?.consequence.orEmpty()) }
    var alternative by rememberSaveable(example) { mutableStateOf(example?.alternativeBehavior.orEmpty()) }
    var toleratedEmotion by rememberSaveable(example) { mutableStateOf(example?.toleratedEmotion.orEmpty()) }
    var identity by rememberSaveable(example) { mutableStateOf(example?.identity.orEmpty()) }
    var reminders by rememberSaveable { mutableStateOf(true) }
    var morning by rememberSaveable { mutableIntStateOf(480) }
    var pause by rememberSaveable { mutableIntStateOf(840) }
    var evening by rememberSaveable { mutableIntStateOf(1260) }
    val canStart = listOf(patternName, trigger, oldBehavior, consequence, alternative, identity).all { it.isNotBlank() }

    ProtocolScroll {
        ProtocolCard {
            Text("Un patrón. Una respuesta nueva. 21 días.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ProtocolInk)
            Text("Observar → interrumpir → sustituir → repetir → evaluar", color = ProtocolBlue, fontWeight = FontWeight.SemiBold)
            Text("Elige un comportamiento concreto y observable. La herramienta te acompañará con práctica guiada, P.A.R.A., diario y medición.", color = ProtocolSecondary)
        }
        SectionCard("Cartografía el patrón", "PASO 1 · Trabaja con un solo automatismo durante el ciclo.") {
            ProtocolField("Nombre breve", patternName) { patternName = it }
            ProtocolField("Cuando ocurre…", trigger) { trigger = it }
            ProtocolField("Suelo pensar…", thought) { thought = it }
            ProtocolField("Siento…", emotion) { emotion = it }
            ProtocolField("Y termino haciendo…", oldBehavior) { oldBehavior = it }
            ProtocolField("Lo que produce…", consequence) { consequence = it }
        }
        SectionCard("Diseña la alternativa", "PASO 2 · Debe poder ejecutarse incluso con emoción presente.") {
            ProtocolField("Cuando aparezca, haré…", alternative) { alternative = it }
            ProtocolField("Aunque sienta…", toleratedEmotion) { toleratedEmotion = it }
            ProtocolField("Estoy entrenando para ser una persona que…", identity) { identity = it }
        }
        ProtocolCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, tint = ProtocolViolet)
                Text("Acompañamiento diario", Modifier.weight(1f).padding(start = 10.dp), fontWeight = FontWeight.SemiBold)
                Switch(reminders, { reminders = it })
            }
            if (reminders) {
                TimeRow("Práctica de mañana", morning) { morning = it }
                TimeRow("Pausa P.A.R.A.", pause) { pause = it }
                TimeRow("Diario nocturno", evening) { evening = it }
            }
        }
        ProtocolCard {
            Text("Practica con dificultad manejable. Si aparecen pánico intenso, recuerdos traumáticos intrusivos, desconexión persistente o empeoramiento significativo, detén el ejercicio y busca acompañamiento profesional.", color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            enabled = canStart,
            onClick = {
                onStart(TransformationConfiguration(
                    patternName.trim(), trigger.trim(), thought.trim(), emotion.trim(), oldBehavior.trim(),
                    consequence.trim(), alternative.trim(), toleratedEmotion.trim(), identity.trim(),
                    System.currentTimeMillis(), reminders, morning, pause, evening
                ))
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = protocolButtonColors()
        ) { Text("Comenzar mis 21 días", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Dashboard(state: TransformationState, onPage: (ProtocolPage) -> Unit) {
    val day = state.currentDay()
    val plan = TransformationCatalog.days[day - 1]
    val entry = state.entries.firstOrNull { it.day == day } ?: TransformationDayEntry(day)
    ProtocolScroll {
        ProtocolCard {
            Text("Día $day de 21", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(state.configuration?.patternName.orEmpty(), color = ProtocolViolet, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth(), color = ProtocolMint)
            Text("${state.completedDays}/21 días completos · Cada repetición correcta es evidencia.", color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall)
        }
        ProtocolCard(Modifier.clickable { onPage(ProtocolPage.Day(day)) }) {
            Text(plan.phase.uppercase(), color = ProtocolBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(plan.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(plan.objective, color = ProtocolSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill("Mañana", entry.morningCompleted); StatusPill("Acción", entry.actionCompleted); StatusPill("Cierre", entry.eveningCompleted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTool("Práctica", "21 min", Modifier.weight(1f)) { onPage(ProtocolPage.Morning(day)) }
            QuickTool("P.A.R.A.", "En el momento", Modifier.weight(1f)) { onPage(ProtocolPage.Para) }
            QuickTool("Diario", "Cierre", Modifier.weight(1f)) { onPage(ProtocolPage.Journal(day)) }
        }
        SectionCard("Tu respuesta elegida", "PROTOCOLO PERSONAL") {
            Text(state.configuration?.alternativeFormula.orEmpty(), color = ProtocolInk, fontWeight = FontWeight.Medium)
            HorizontalDivider()
            Text("¿Qué haría ahora la versión de mí que estoy entrenando?", color = ProtocolViolet, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                { onPage(ProtocolPage.Plan) }, Modifier.weight(1f),
                colors = protocolButtonColors(), border = protocolButtonBorder()
            ) { Icon(Icons.Rounded.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text("Ruta completa") }
            OutlinedButton(
                { onPage(ProtocolPage.Insights) }, Modifier.weight(1f),
                colors = protocolButtonColors(), border = protocolButtonBorder()
            ) { Icon(Icons.Rounded.BarChart, null); Spacer(Modifier.width(6.dp)); Text("Tendencias") }
        }
        Text("Versión mínima disponible: reducir la práctica es válido; abandonarla por perfeccionismo no.", color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DayScreen(
    entry: TransformationDayEntry,
    day: Int,
    onSave: (TransformationDayEntry) -> Unit,
    onMorning: () -> Unit,
    onJournal: () -> Unit
) {
    val plan = TransformationCatalog.days[day - 1]
    var notes by rememberSaveable(day, entry.updatedAtMillis) { mutableStateOf(entry.exerciseNotes) }
    var commitment by rememberSaveable(day, entry.updatedAtMillis) { mutableStateOf(entry.commitment) }
    var actionDone by rememberSaveable(day, entry.updatedAtMillis) { mutableStateOf(entry.actionCompleted) }
    ProtocolScroll {
        ProtocolCard {
            Text(plan.phase.uppercase(), color = ProtocolViolet, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(plan.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(plan.objective, color = ProtocolBlue, fontWeight = FontWeight.SemiBold)
            plan.principle?.let { Text(it, color = ProtocolSecondary) }
        }
        SectionCard("Ejercicio principal") {
            Text(plan.exercise)
            plan.prompts.forEach { Text("• $it", color = ProtocolSecondary) }
            ProtocolField("Tus notas", notes, minLines = 4) { notes = it }
        }
        SectionCard("Acción del día", plan.action) {
            ProtocolField("Hoy demostraré mi nueva identidad haciendo…", commitment) { commitment = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(actionDone, { actionDone = it })
                Text(if (actionDone) "Acción realizada" else "Marcar cuando la realices", fontWeight = FontWeight.SemiBold)
            }
        }
        SectionCard("Rutina base") {
            ActionRow("Práctica guiada de mañana", "21 minutos · versión mínima de 10", entry.morningCompleted, onMorning)
            ActionRow("Diario nocturno", "Seis columnas · medición 0–10", entry.eveningCompleted, onJournal)
        }
        ProtocolCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Puntuación de proceso", fontWeight = FontWeight.Bold)
                    Text("Conciencia, pausa, regulación, alternativa y recuperación.", color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Text("${entry.score.total}/10", color = ProtocolViolet, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        Button(
            onClick = { onSave(entry.copy(exerciseNotes = notes, commitment = commitment, actionCompleted = actionDone)) },
            modifier = Modifier.fillMaxWidth(),
            colors = protocolButtonColors()
        ) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(8.dp)); Text("Guardar progreso") }
    }
}

@Composable
private fun JournalScreen(entry: TransformationDayEntry, onSave: (TransformationDayEntry) -> Unit) {
    var situation by rememberSaveable { mutableStateOf(entry.journal.situation) }
    var thought by rememberSaveable { mutableStateOf(entry.journal.thought) }
    var body by rememberSaveable { mutableStateOf(entry.journal.emotionAndBody) }
    var impulse by rememberSaveable { mutableStateOf(entry.journal.impulse) }
    var response by rememberSaveable { mutableStateOf(entry.journal.response) }
    var learning by rememberSaveable { mutableStateOf(entry.journal.learning) }
    var intensity by rememberSaveable { mutableIntStateOf(entry.journal.maximumIntensity) }
    var recoveryMinutes by rememberSaveable { mutableIntStateOf(entry.journal.recoveryMinutes) }
    var score by remember { mutableStateOf(entry.score) }
    ProtocolScroll {
        ProtocolCard {
            Text("No busques una puntuación perfecta.", fontWeight = FontWeight.Bold)
            Text("Registra el episodio más relevante y usa los números para detectar tendencias.", color = ProtocolSecondary)
        }
        SectionCard("Registro del episodio") {
            ProtocolField("1. Situación · ¿Qué ocurrió?", situation) { situation = it }
            ProtocolField("2. Pensamiento · ¿Qué interpretación apareció?", thought) { thought = it }
            ProtocolField("3. Emoción y cuerpo · ¿Qué sentiste y dónde?", body) { body = it }
            ProtocolField("4. Impulso · ¿Qué querías hacer?", impulse) { impulse = it }
            ProtocolField("5. Respuesta · ¿Qué hiciste realmente?", response) { response = it }
            ProtocolField("6. Aprendizaje · ¿Qué prepararás para la próxima vez?", learning) { learning = it }
            StepperRow("Intensidad máxima", intensity, "/10", 0, 10) { intensity = it }
            StepperRow("Recuperación", recoveryMinutes, " min", 0, 720, 5) { recoveryMinutes = it }
        }
        SectionCard("Calidad del proceso", "Cada dimensión aporta hasta 2 puntos.") {
            Metric("Conciencia", score.awareness) { score = score.copy(awareness = it) }
            Metric("Pausa", score.pause) { score = score.copy(pause = it) }
            Metric("Regulación", score.regulation) { score = score.copy(regulation = it) }
            Metric("Conducta alternativa", score.alternativeBehavior) { score = score.copy(alternativeBehavior = it) }
            Metric("Recuperación", score.recovery) { score = score.copy(recovery = it) }
            Text("Total: ${score.total}/10", Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = ProtocolViolet, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Button(onClick = {
            onSave(entry.copy(
                eveningCompleted = true,
                journal = TransformationJournal(situation, thought, body, impulse, response, learning, intensity, recoveryMinutes),
                score = score
            ))
        }, modifier = Modifier.fillMaxWidth(), colors = protocolButtonColors()) { Icon(Icons.Rounded.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Guardar cierre del día") }
    }
}

@Composable
private fun MorningPractice(onComplete: () -> Unit) {
    var minimum by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val routine = if (minimum) TransformationCatalog.minimumRoutine else TransformationCatalog.morningRoutine
    var remaining by rememberSaveable(minimum, step) { mutableIntStateOf(routine[step].minutes * 60) }
    var running by rememberSaveable { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    LaunchedEffect(running, step, minimum) {
        while (running && remaining > 0) { delay(1_000); remaining-- }
        if (running && remaining == 0) {
            if (step < routine.lastIndex) step++ else { running = false; completed = true }
        }
    }
    if (completed) AlertDialog(
        onDismissRequest = {}, title = { Text("Práctica completada") },
        text = { Text("Has entrenado la respuesta nueva. Cuenta la práctica, no la perfección.") },
        confirmButton = { TextButton(onClick = onComplete, colors = protocolTextButtonColors()) { Text("Cerrar") } }
    )
    Column(
        Modifier.fillMaxSize().padding(22.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Versión mínima · 10 minutos", Modifier.weight(1f), color = Color.White)
            Switch(minimum, { minimum = it; step = 0 }, enabled = !running && step == 0)
        }
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(230.dp).background(Color.White.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d:%02d".format(remaining / 60, remaining % 60), color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                Text("Paso ${step + 1} de ${routine.size}", color = Color.White.copy(.78f))
            }
        }
        Text(routine[step].title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(routine[step].detail, color = Color.White.copy(.84f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ running = !running }, Modifier.weight(1f), colors = protocolButtonColors()) {
                Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (running) "Pausar" else "Continuar")
            }
            OutlinedButton(
                { if (step < routine.lastIndex) step++ else { running = false; completed = true } },
                Modifier.weight(1f), colors = protocolButtonColors(), border = protocolButtonBorder()
            ) { Text("Siguiente") }
        }
    }
}

@Composable
private fun ParaPractice(defaultAction: String, onSave: (String, String, String, Int) -> Unit) {
    var signal by rememberSaveable { mutableStateOf("") }
    var emotion by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf(defaultAction) }
    var duration by rememberSaveable { mutableIntStateOf(30) }
    var remaining by rememberSaveable(duration) { mutableIntStateOf(duration) }
    var running by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(running, duration) { while (running && remaining > 0) { delay(1_000); remaining-- }; if (remaining == 0) running = false }
    ProtocolScroll {
        ParaCard("P", "Percibe", "Reconoce la primera señal sin juzgarla.", ProtocolViolet) { ProtocolField("Señal detectada", signal) { signal = it } }
        ParaCard("A", "Aplaza", "No decidas todavía. Crea espacio.", ProtocolBlue) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(30, 60, 90, 120, 300).forEach { seconds ->
                    OutlinedButton(
                        { duration = seconds; remaining = seconds },
                        Modifier.weight(1f),
                        colors = protocolButtonColors(selected = duration == seconds),
                        border = protocolButtonBorder(selected = duration == seconds),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                    ) {
                        Text(if (seconds < 60) "${seconds}s" else "${seconds / 60}m", fontSize = 11.sp)
                    }
                }
            }
            Button({ running = !running }, Modifier.fillMaxWidth(), colors = protocolButtonColors()) { Icon(Icons.Rounded.Timer, null); Spacer(Modifier.width(6.dp)); Text(if (running) "%d:%02d".format(remaining / 60, remaining % 60) else "Iniciar pausa") }
        }
        ParaCard("R", "Regula", "Haz de tres a seis respiraciones lentas y nombra la emoción.", ProtocolMint) { ProtocolField("Hay…", emotion) { emotion = it } }
        ParaCard("A", "Actúa", "Sigue la conducta definida; no improvises bajo activación.", Color(0xFFC16D49)) { ProtocolField("Ahora voy a…", action) { action = it } }
        Button(
            enabled = signal.isNotBlank() && action.isNotBlank(),
            onClick = { onSave(signal, emotion, action, (duration - remaining).coerceAtLeast(0)) },
            modifier = Modifier.fillMaxWidth(),
            colors = protocolButtonColors()
        ) { Text("Registrar esta pausa") }
    }
}

@Composable
private fun PlanScreen(state: TransformationState, onDay: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items(TransformationCatalog.days, key = { it.number }) { plan ->
            val completed = state.entries.firstOrNull { it.day == plan.number }?.isCompleted == true
            Card(
                Modifier.fillMaxWidth().clickable { onDay(plan.number) },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(.9f)),
                shape = RoundedCornerShape(17.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).background(if (completed) ProtocolMint.copy(.18f) else ProtocolViolet.copy(.13f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(if (completed) "✓" else "${plan.number}", color = if (completed) ProtocolMint else ProtocolViolet, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(plan.title, fontWeight = FontWeight.Bold, color = ProtocolInk)
                        Text(plan.phase, color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (plan.number == state.currentDay()) Text("HOY", color = ProtocolViolet, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InsightsScreen(state: TransformationState) {
    val recent = state.entries.sortedBy { it.day }.takeLast(7)
    ProtocolScroll {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("${state.completedDays}", "días completos", Modifier.weight(1f))
            StatCard("${state.paraEvents.size}", "pausas P.A.R.A.", Modifier.weight(1f))
            StatCard("%.1f".format(state.averageScore), "media / 10", Modifier.weight(1f))
        }
        SectionCard("Últimos registros", "Observa la tendencia; no conviertas el número en perfeccionismo.") {
            if (recent.isEmpty()) Text("Completa el cierre nocturno para empezar a ver tu tendencia.", color = ProtocolSecondary)
            else recent.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Día ${it.day}", Modifier.width(58.dp), fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { it.score.total / 10f }, Modifier.weight(1f), color = ProtocolViolet)
                    Text(" ${it.score.total}/10")
                }
            }
        }
        SectionCard("Señales de progreso real") {
            listOf(
                "Detectas antes el patrón.", "La emoción dura menos o gobierna menos tu conducta.",
                "Realizas pausas con menos esfuerzo.", "Te recuperas antes de un desliz.",
                "Haces lo correcto incluso sin motivación."
            ).forEach { Text("✓  $it", color = ProtocolInk) }
        }
    }
}

@Composable
private fun InformationScreen(hasCycle: Boolean, onUseExample: (TransformationExample) -> Unit) {
    ProtocolScroll {
        ProtocolCard {
            Text("Transformación personal", color = ProtocolViolet, fontWeight = FontWeight.Bold)
            Text("Entrena una respuesta nueva durante 21 días", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Una guía práctica para observar un patrón automático, crear una pausa y acumular evidencias de una conducta diferente.", color = ProtocolSecondary)
        }
        SectionCard("El método") {
            Text("1. Observa y cartografía el patrón (días 1–7).")
            Text("2. Interrumpe y sustituye la respuesta (días 8–14).")
            Text("3. Construye y consolida identidad con conducta (días 15–21).")
        }
        SectionCard("Herramientas diarias") {
            Text("• Práctica guiada de 21 minutos, o versión mínima de 10.")
            Text("• P.A.R.A.: Percibe, Aplaza, Regula y Actúa.")
            Text("• Diario nocturno en seis columnas.")
            Text("• Medición de proceso 0–10 y tendencias.")
        }
        SectionCard("Ejemplos listos para usar", if (hasCycle) "Elegir uno reiniciará el ciclo actual." else "Puedes adaptarlos después.") {
            TransformationCatalog.examples.forEach { example ->
                OutlinedButton(
                    { onUseExample(example) }, Modifier.fillMaxWidth(),
                    colors = protocolButtonColors(), border = protocolButtonBorder()
                ) { Text(example.title, Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
            }
        }
        ProtocolCard {
            Text("Esta herramienta es educativa y no sustituye diagnóstico, psicoterapia ni atención médica. Practica con dificultad manejable y busca apoyo profesional si aparece malestar intenso o persistente.", color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsScreen(
    configuration: TransformationConfiguration,
    onSave: (TransformationConfiguration) -> Unit,
    onReset: () -> Unit
) {
    var value by remember(configuration) { mutableStateOf(configuration) }
    var showReset by remember { mutableStateOf(false) }
    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false }, title = { Text("¿Reiniciar el protocolo?") },
        text = { Text("Se borrarán la configuración, los diarios y todas las mediciones de este ciclo.") },
        confirmButton = { TextButton({ showReset = false; onReset() }, colors = protocolTextButtonColors()) { Text("Borrar ciclo y reiniciar") } },
        dismissButton = { TextButton({ showReset = false }, colors = protocolTextButtonColors()) { Text("Cancelar") } }
    )
    ProtocolScroll {
        SectionCard("Protocolo personal") {
            ProtocolField("Nombre del patrón", value.patternName) { value = value.copy(patternName = it) }
            ProtocolField("Desencadenante", value.trigger) { value = value.copy(trigger = it) }
            ProtocolField("Pensamiento automático", value.automaticThought) { value = value.copy(automaticThought = it) }
            ProtocolField("Emoción y cuerpo", value.emotion) { value = value.copy(emotion = it) }
            ProtocolField("Conducta antigua", value.oldBehavior) { value = value.copy(oldBehavior = it) }
            ProtocolField("Consecuencia", value.consequence) { value = value.copy(consequence = it) }
            ProtocolField("Conducta alternativa", value.alternativeBehavior) { value = value.copy(alternativeBehavior = it) }
            ProtocolField("Emoción tolerada", value.toleratedEmotion) { value = value.copy(toleratedEmotion = it) }
            ProtocolField("Identidad en entrenamiento", value.identity) { value = value.copy(identity = it) }
        }
        ProtocolCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recordatorios diarios", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(value.remindersEnabled, { value = value.copy(remindersEnabled = it) })
            }
            if (value.remindersEnabled) {
                TimeRow("Práctica de mañana", value.morningMinuteOfDay) { value = value.copy(morningMinuteOfDay = it) }
                TimeRow("Pausa P.A.R.A.", value.pauseMinuteOfDay) { value = value.copy(pauseMinuteOfDay = it) }
                TimeRow("Diario nocturno", value.eveningMinuteOfDay) { value = value.copy(eveningMinuteOfDay = it) }
            }
        }
        SectionCard("Reglas del ciclo") {
            listOf(
                "Trabaja con hechos, no solo con intenciones.",
                "No intentes eliminar emociones: evita convertirlas automáticamente en conducta.",
                "Practica primero con situaciones pequeñas.", "Un desliz es información, no una identidad.",
                "Sueño, alimentación, fatiga y estrés también forman parte del sistema."
            ).forEach { Text("• $it") }
        }
        Button({ onSave(value) }, Modifier.fillMaxWidth(), colors = protocolButtonColors()) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(6.dp)); Text("Guardar ajustes") }
        OutlinedButton(
            { showReset = true }, Modifier.fillMaxWidth(),
            colors = protocolButtonColors(), border = protocolButtonBorder()
        ) { Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(6.dp)); Text("Reiniciar protocolo") }
    }
}

@Composable
private fun ProtocolScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp), content = content
    )
}

@Composable
private fun ProtocolCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(.92f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) = ProtocolCard {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ProtocolInk)
    subtitle?.let { Text(it, color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall) }
    content()
}

@Composable
private fun ProtocolField(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProtocolButtonContent,
            unfocusedTextColor = ProtocolButtonContent,
            disabledTextColor = ProtocolButtonContent.copy(alpha = 0.55f),
            focusedContainerColor = ProtocolFieldBackground,
            unfocusedContainerColor = ProtocolFieldBackground,
            disabledContainerColor = ProtocolFieldBackground.copy(alpha = 0.65f),
            cursorColor = ProtocolButtonContent,
            focusedBorderColor = ProtocolButtonContent,
            unfocusedBorderColor = ProtocolBlue,
            disabledBorderColor = ProtocolBlue.copy(alpha = 0.5f),
            focusedLabelColor = ProtocolButtonContent,
            unfocusedLabelColor = ProtocolButtonContent,
            disabledLabelColor = ProtocolButtonContent.copy(alpha = 0.55f)
        )
    )
}

@Composable
private fun TimeRow(label: String, minuteOfDay: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable {
            TimePickerDialog(context, { _, hour, minute -> onChange(hour * 60 + minute) }, minuteOfDay / 60, minuteOfDay % 60, true).show()
        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f)); Text("%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60), color = ProtocolViolet, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(text: String, done: Boolean) {
    Text(
        (if (done) "✓ " else "○ ") + text,
        Modifier.background((if (done) ProtocolMint else ProtocolSecondary).copy(.10f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
        color = if (done) ProtocolMint else ProtocolSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun QuickTool(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = ProtocolButtonBackground), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = ProtocolButtonContent, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ProtocolButtonContent.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, done: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow, null, tint = if (done) ProtocolMint else ProtocolViolet)
        Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Metric(title: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row { Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("$value/2", color = ProtocolViolet) }
        Slider(value.toFloat(), { onChange(it.roundToInt()) }, valueRange = 0f..2f, steps = 1)
    }
}

@Composable
private fun StepperRow(label: String, value: Int, suffix: String, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $value$suffix", Modifier.weight(1f))
        TextButton({ onChange((value - step).coerceAtLeast(min)) }, colors = protocolTextButtonColors()) { Text("−") }
        Spacer(Modifier.width(4.dp))
        TextButton({ onChange((value + step).coerceAtMost(max)) }, colors = protocolTextButtonColors()) { Text("+") }
    }
}

@Composable
private fun ParaCard(letter: String, title: String, detail: String, color: Color, content: @Composable ColumnScope.() -> Unit) = ProtocolCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(color, CircleShape), contentAlignment = Alignment.Center) { Text(letter, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
        Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(detail, color = ProtocolSecondary, style = MaterialTheme.typography.bodySmall) }
    }
    content()
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White.copy(.9f)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = ProtocolViolet, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, color = ProtocolSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun protocolButtonColors(selected: Boolean = false) = ButtonDefaults.buttonColors(
    containerColor = if (selected) Color(0xFF91D1EC) else ProtocolButtonBackground,
    contentColor = ProtocolButtonContent,
    disabledContainerColor = ProtocolButtonBackgroundDisabled,
    disabledContentColor = ProtocolButtonContent.copy(alpha = 0.52f)
)

@Composable
private fun protocolTextButtonColors() = ButtonDefaults.textButtonColors(
    containerColor = ProtocolButtonBackground,
    contentColor = ProtocolButtonContent,
    disabledContentColor = ProtocolButtonContent.copy(alpha = 0.52f)
)

private fun protocolButtonBorder(selected: Boolean = false) = BorderStroke(
    width = if (selected) 2.dp else 1.dp,
    color = if (selected) ProtocolButtonContent else ProtocolBlue
)
