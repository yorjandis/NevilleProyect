package com.ypg.neville.ui.frag

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.ui.theme.ContextMenuShape

class HomeFloatingMenuBottomSheet : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Theme_NevilleProyect_CenterDialog)
        val content = ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    var showSheetContent by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { showSheetContent = true }

                    AnimatedVisibility(
                        visible = showSheetContent,
                        enter = scaleIn(
                            initialScale = 0.90f,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 220))
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 6.dp
                        ) {
                            FloatingMenuContent(onNavigate = { destination ->
                                val host = requireActivity() as? MainActivity
                                when (destination) {
                                    "neville" -> host?.openDestinationAsSheet(R.id.frag_neville_goddard)
                                    "joe" -> host?.openDestinationAsSheet(R.id.frag_joe_dispenza)
                                    "gregg" -> host?.openDestinationAsSheet(R.id.frag_gregg)
                                    "bruce" -> host?.openDestinationAsSheet(R.id.frag_bruce_lipton)
                                    "frases" -> host?.openDestinationAsSheet(R.id.frag_listado_frases)
                                    "notas" -> host?.openDestinationAsSheet(R.id.frag_notas)
                                    "enciclopedia" -> {
                                        frag_listado.elementLoaded = "enciclopedia"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "evidencia" -> {
                                        frag_listado.elementLoaded = "evidenciaCientifica"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "reflexiones" -> {
                                        frag_listado.elementLoaded = "reflexiones"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "ayudas" -> {
                                        frag_listado.elementLoaded = "ayudas"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "lienzo" -> host?.openDestinationAsSheet(R.id.frag_lienzo)
                                    "metas" -> host?.openDestinationAsSheet(R.id.frag_metas)
                                    "ajustes" -> host?.openDestinationAsSheet(R.id.fragSetting)
                                    "premium" -> host?.showSubscriptionPaywall()
                                }
                                dismiss()
                            })
                        }
                    }
                }
            }
        }

        dialog.setContentView(content)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setWindowAnimations(R.style.NevilleBottomDialogAnimation)
        }
        return dialog
    }

    @Composable
    private fun FloatingMenuContent(onNavigate: (String) -> Unit) {
        var showRecursos by remember { mutableStateOf(false) }
        var showProductividad by remember { mutableStateOf(false) }
        val hasPremium = SubscriptionManager.hasActiveSubscriptionNow()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onNavigate("neville") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Neville")
                    }
                    Button(onClick = { onNavigate("joe") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Joe Dispenza")
                    }
                    Button(onClick = { onNavigate("gregg") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Gregg Braden")
                    }
                    Button(onClick = { onNavigate("bruce") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Bruce Lipton")
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showRecursos = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Recursos Didácticos")
                        }

                        DropdownMenu(
                            expanded = showRecursos,
                            onDismissRequest = { showRecursos = false },
                            shape = ContextMenuShape
                        ) {
                            DropdownMenuItem(text = { Text("Frases") }, onClick = { showRecursos = false; onNavigate("frases") })
                            DropdownMenuItem(text = { Text("Notas") }, onClick = { showRecursos = false; onNavigate("notas") })
                            DropdownMenuItem(
                                text = { Text(if (hasPremium) "Enciclopedia" else "Enciclopedia (Preview)") },
                                onClick = {
                                    showRecursos = false
                                    onNavigate("enciclopedia")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (hasPremium) "Evidencia Científica" else "Evidencia Científica (Preview)") },
                                onClick = {
                                    showRecursos = false
                                    onNavigate("evidencia")
                                }
                            )
                            DropdownMenuItem(text = { Text("Reflexiones") }, onClick = { showRecursos = false; onNavigate("reflexiones") })
                            DropdownMenuItem(text = { Text("Ayudas") }, onClick = { showRecursos = false; onNavigate("ayudas") })
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showProductividad = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Productividad")
                        }
                        DropdownMenu(
                            expanded = showProductividad,
                            onDismissRequest = { showProductividad = false },
                            shape = ContextMenuShape
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (hasPremium) "Lienzo" else "Lienzo (Premium)") },
                                onClick = { showProductividad = false; onNavigate("lienzo") }
                            )
                            DropdownMenuItem(
                                text = { Text(if (hasPremium) "Metas" else "Metas (Premium)") },
                                onClick = { showProductividad = false; onNavigate("metas") }
                            )
                        }
                    }
                    Button(onClick = { onNavigate("ajustes") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ajuste")
                    }
                    Button(onClick = { onNavigate("premium") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (hasPremium) "Suscripción Activa" else "Versión Extendida")
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "HomeFloatingMenuBottomSheet"
    }
}
