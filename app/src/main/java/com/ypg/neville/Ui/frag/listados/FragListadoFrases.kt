package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.room.FraseEntity
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.FraseContextActions
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.ui.theme.ContextMenuShape

class FragListadoFrases : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                ListadoFrasesScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE }
        runCatching { MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun ListadoFrasesScreen() {
        val context = LocalContext.current
        val hostActivity = MainActivity.currentInstance()
        val subscriptionState by SubscriptionManager.uiState.collectAsState()
        val hasPremium = subscriptionState.isActive && subscriptionState.isEntitlementVerified
        val allItems = remember { mutableStateListOf<FraseEntity>() }
        var query by remember { mutableStateOf("") }
        var selectedSource by remember { mutableStateOf("Todos") }
        var showSourceMenu by remember { mutableStateOf(false) }
        var showCreateDialog by remember { mutableStateOf(false) }

        fun reload() {
            allItems.clear()
            allItems.addAll(utilsDB.getAllFrases(context))
        }

        LaunchedEffect(Unit) { reload() }

        val sourceOptions = remember(allItems) {
            val knownAuthorOptions = listOf("Neville", "Bruce", "Gregg", "Joe")
            val dynamicAuthors = allItems
                .map { it.autor.trim() }
                .filter {
                    it.isNotBlank() &&
                        !it.equals("Salud", ignoreCase = true) &&
                        !it.equals("Otros", ignoreCase = true) &&
                        !it.contains("neville", ignoreCase = true) &&
                        !it.contains("bruce", ignoreCase = true) &&
                        !it.contains("gregg", ignoreCase = true) &&
                        !it.contains("joe", ignoreCase = true)
                }
                .distinct()
                .sorted()
            listOf("Todos") +
                knownAuthorOptions +
                listOf("Otros", "Salud") +
                dynamicAuthors +
                listOf("Personales", "Favoritas", "Con nota")
        }

        fun isSourcePremium(source: String): Boolean {
            if (hasPremium) return false
            return source in setOf("Bruce", "Gregg", "Joe", "Otros", "Salud")
        }

        fun sourceLabel(source: String): String {
            return if (isSourcePremium(source)) "$source (Premium)" else source
        }

        fun isFrasePremium(item: FraseEntity): Boolean {
            if (hasPremium) return false
            if (item.personalState() == "1") return false
            return when (item.categoria.uppercase()) {
                "SALUD", "OTROS" -> true
                "AUTOR" -> !item.autor.contains("neville", ignoreCase = true)
                else -> false
            }
        }

        val queryLower = query.trim().lowercase()
        val filtered = allItems.filter { item ->
            val bySource = when (selectedSource) {
                "Todos" -> true
                "Neville" -> item.autor.contains("neville", ignoreCase = true)
                "Bruce" -> item.autor.contains("bruce", ignoreCase = true)
                "Gregg" -> item.autor.contains("gregg", ignoreCase = true)
                "Joe" -> item.autor.contains("joe", ignoreCase = true)
                "Otros" -> item.categoria == "OTROS"
                "Salud" -> item.categoria == "SALUD"
                "Personales" -> item.personalState() == "1"
                "Favoritas" -> item.favState() == "1"
                "Con nota" -> item.nota.trim().isNotEmpty()
                else -> item.autor.equals(selectedSource, ignoreCase = true)
            }

            val bySearch = queryLower.isBlank() ||
                item.frase.lowercase().contains(queryLower) ||
                item.nota.lowercase().contains(queryLower)

            bySource && bySearch && !isFrasePremium(item)
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_note),
                        contentDescription = "Nueva frase"
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Listado de Frases", fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por frase o nota") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showSourceMenu = true },
                        label = { Text("Fuente/Filtro: ${sourceLabel(selectedSource)}") },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                    DropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false },
                        shape = ContextMenuShape
                    ) {
                        sourceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sourceLabel(option)) },
                                onClick = {
                                    if (isSourcePremium(option)) {
                                        hostActivity?.showSubscriptionPaywall(
                                            "La categoría \"$option\" forma parte de la suscripción anual."
                                        )
                                    } else {
                                        selectedSource = option
                                    }
                                    showSourceMenu = false
                                }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { "${it.id}-${it.assetKey}-${it.frase.hashCode()}" }) { item ->
                        var showMenu by remember(item.id) { mutableStateOf(false) }
                        val interactionSource = remember { MutableInteractionSource() }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {},
                                    onLongClick = { showMenu = true }
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    FraseOptionsMenu(
                                        expanded = showMenu,
                                        onDismiss = { showMenu = false },
                                        onConvertirNota = {
                                            val result = FraseContextActions.convertirFraseEnNota(context, item.frase)
                                            if (result.ok) {
                                                Toast.makeText(
                                                    context,
                                                    "Nota creada: ${result.titulo}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo crear la nota",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onCargarLienzo = {
                                            FraseContextActions.cargarFraseEnLienzo(context, item.frase)
                                        },
                                        onCompartirSistema = {
                                            FraseContextActions.compartirFraseSistema(
                                                context = context,
                                                frase = item.frase,
                                                autor = item.autor,
                                                fuente = item.fuente
                                            )
                                        },
                                        onAbrirNotaFrase = {
                                            FraseContextActions.abrirNotaDeFrase(context, item.frase)
                                        },
                                        onCrearNuevaFrase = {
                                            showCreateDialog = true
                                        }
                                    )
                                }

                                Text(text = item.frase, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "<${item.autor}>",
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.nota.isNotBlank()) {
                                    Text(
                                        text = "Nota: ${item.nota}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.personalState() == "1") Tag("Personal", Color(0xFF0E7A2F))
                                    if (item.favState() == "1") Tag("Favorita", Color(0xFFC28A00))
                                    if (item.nota.isNotBlank()) Tag("Con nota", Color(0xFF2457A6))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CrearFraseDialog(
                onDismiss = { showCreateDialog = false },
                onSave = { frase, autor, fuente ->
                    if (frase.trim().isBlank()) {
                        Toast.makeText(context, "Debe escribir una frase", Toast.LENGTH_SHORT).show()
                    } else {
                        val result = utilsDB.insertNewFrase(
                            context,
                            frase.trim(),
                            autor.trim(),
                            fuente.trim(),
                            "0"
                        )
                        if (result < 0) {
                            Toast.makeText(context, "No se pudo crear la frase", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Frase creada", Toast.LENGTH_SHORT).show()
                            showCreateDialog = false
                            reload()
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun Tag(text: String, color: Color) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }

    @Composable
    private fun CrearFraseDialog(
        onDismiss: () -> Unit,
        onSave: (frase: String, autor: String, fuente: String) -> Unit
    ) {
        var frase by remember { mutableStateOf("") }
        var autor by remember { mutableStateOf("") }
        var fuente by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth(0.96f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text("Nueva frase personal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frase,
                        onValueChange = { frase = it },
                        label = { Text("Frase") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 6,
                        maxLines = 8
                    )
                    OutlinedTextField(
                        value = autor,
                        onValueChange = { autor = it },
                        label = { Text("Autor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = fuente,
                        onValueChange = { fuente = it },
                        label = { Text("Fuente") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onSave(frase, autor, fuente) }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}
