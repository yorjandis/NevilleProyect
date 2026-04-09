package com.ypg.neville.ui.frag

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.subscription.SubscriptionManager

class SubscriptionPaywallDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val reason = arguments?.getString(ARG_REASON).orEmpty()
        val host = requireActivity() as? MainActivity

        return Dialog(requireContext(), R.style.Theme_NevilleProyect_CenterDialog).apply {
            setCanceledOnTouchOutside(true)
            setContentView(
                ComposeView(requireContext()).apply {
                    setContent {
                        com.ypg.neville.ui.theme.NevilleTheme {
                            PaywallContent(
                                reason = reason,
                                onClose = { dismiss() },
                                onSubscribe = {
                                    val launched = host?.let { SubscriptionManager.launchPurchase(it) } == true
                                    if (!launched) {
                                        Toast.makeText(
                                            requireContext(),
                                            "No se pudo iniciar la compra. Verifica publicación y Product ID.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onRestore = {
                                    SubscriptionManager.restorePurchases()
                                    Toast.makeText(requireContext(), "Verificando compras...", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @Composable
    private fun PaywallContent(
        reason: String,
        onClose: () -> Unit,
        onSubscribe: () -> Unit,
        onRestore: () -> Unit
    ) {
        val state by SubscriptionManager.uiState.collectAsState()
        val hasPremium = state.isActive
        val price = state.productPrice ?: "Anual en Google Play"

        val title = if (hasPremium) "Suscripción activa" else "Versión Extendida"
        val subtitle = if (hasPremium) {
            "Ya tienes acceso a todo el contenido premium."
        } else {
            "Desbloquea toda la experiencia con una suscripción anual muy asequible"
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF6FBFF),
                                Color(0xFFE7F1FF),
                                Color(0xFFC2DDF3)
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }

                if (reason.isNotBlank()) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureItem("Conocimientos y frases de autores destacados relacionados con las enseñanzas de Neville")
                    FeatureItem("Frases de Salud y otros autores")
                    FeatureItem("Notas protegidas por biometría")
                    FeatureItem("Metas: seguimiento de hábitos saludables y empoderadores")
                    FeatureItem("Lienzo: crea imágenes con citas para compartir")
                    FeatureItem("Enciclopedia: base de conocimientos")
                    FeatureItem("Evidencia Científica: investigaciones de apoyo")
                    FeatureItem("Menú 'Pegar en': portar texto a Notas, Lienzo y Frases")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x6622406B), RoundedCornerShape(14.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "Precio: $price / año",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF173455)
                    )
                }

                Button(
                    onClick = onSubscribe,
                    enabled = !hasPremium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A4D8F))
                ) {
                    Text(if (hasPremium) "Suscripción activa" else "Suscribirme ahora", color = Color.Black)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                        Text("Restaurar", color = Color.Black)
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Cerrar", color = Color.Black)
                    }
                }

                Text(
                    text = "Las enseñanzas de Neville seguirán siendo gratis, nada cambiará eso.",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    @Composable
    private fun FeatureItem(text: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDFD9DBE2), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF1A4D8F), CircleShape)
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }

    companion object {
        private const val ARG_REASON = "arg_reason"
        const val TAG = "SubscriptionPaywallDialog"

        fun newInstance(reason: String?): SubscriptionPaywallDialog {
            return SubscriptionPaywallDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_REASON, reason)
                }
            }
        }
    }
}
