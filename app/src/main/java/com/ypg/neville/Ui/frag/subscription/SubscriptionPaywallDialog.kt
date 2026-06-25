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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.subscription.SubscriptionManager

class SubscriptionPaywallDialog : DialogFragment() {

    private data class PremiumFeature(
        val icon: ImageVector,
        val title: String,
        val description: String
    )

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
                                    val launched = host?.let {
                                        SubscriptionManager.launchPurchase(it)
                                    } == true
                                    if (!launched) {
                                        Toast.makeText(
                                            requireContext(),
                                            "No se pudo iniciar la compra. Verifica la conexión con Google Play.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onRestore = {
                                    SubscriptionManager.restorePurchases()
                                    Toast.makeText(
                                        requireContext(),
                                        "Verificando compras...",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
        val hasTrial = state.hasIntroductoryTrial
        val price = state.productPrice ?: "el precio mostrado en Google Play"

        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFD7EFFA),
                                Color(0xFFEBE4F7),
                                Color(0xFFC2DDF3)
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (hasPremium) "Suscripción activa" else "Versión Extendida",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF082F3B)
                    )
                    Text(
                        text = if (hasPremium) {
                            "Ya tienes acceso a la versión extendida"
                        } else {
                            "Accede a todo el contenido premium"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    if (!hasPremium) {
                        Text(
                            text = "Primera semana gratis",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    Color.White.copy(alpha = 0.58f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF083B3B)
                        )
                        Text(
                            text = "Oferta para nuevos suscriptores elegibles.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF173455)
                        )
                    }

                    if (reason.isNotBlank()) {
                        Text(
                            text = reason,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.38f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF173455)
                        )
                    }

                    premiumFeatures.forEachIndexed { index, feature ->
                        FeatureCard(
                            feature = feature,
                            backgroundColor = featureCardColors[index % featureCardColors.size]
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x6622406B), RoundedCornerShape(14.dp))
                        .background(
                            Color.White.copy(alpha = 0.28f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = if (hasTrial) {
                            "7 días gratis; después $price al año"
                        } else {
                            "$price al año"
                        },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1678D3))
                ) {
                    Text(
                        text = when {
                            hasPremium -> "Suscripción activa"
                            hasTrial -> "Empezar prueba gratis"
                            else -> "Suscribirme ahora"
                        },
                        color = if (hasPremium) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!hasPremium) {
                    Text(
                        text = if (hasTrial) {
                            "Al finalizar los 7 días, la suscripción se renovará automáticamente por $price al año hasta que la canceles."
                        } else {
                            "La suscripción se renovará automáticamente cada año hasta que la canceles."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF173455)
                    )
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
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun FeatureCard(feature: PremiumFeature, backgroundColor: Color) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.55f),
                    RoundedCornerShape(10.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color(0xFF0D5757),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0A3B39)
                )
            }
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.82f)
            )
        }
    }

    companion object {
        private val featureCardColors = listOf(
            Color(0xFFDCEFD8),
            Color(0xFFD4E9D1),
            Color(0xFFE4F2DC),
            Color(0xFFCFE5D3)
        )

        private val premiumFeatures = listOf(
            PremiumFeature(
                Icons.Outlined.Star,
                "Contenido exclusivo",
                "Accede a frases y enseñanzas de Joe Dispenza, Bruce Lipton, Gregg Braden y otros autores."
            ),
            PremiumFeature(
                Icons.Outlined.CheckCircle,
                "Metas y transformación personal",
                "Define objetivos, sigue hábitos saludables y utiliza programas prácticos para impulsar tu cambio."
            ),
            PremiumFeature(
                Icons.Outlined.Palette,
                "Lienzo creativo",
                "Crea imágenes con tus citas favoritas para compartirlas o usarlas como tarjetas de enfoque."
            ),
            PremiumFeature(
                Icons.Outlined.WbSunny,
                "Ritual Matutino",
                "Diseña cada día con intención y lleva un registro del progreso de tu transformación."
            ),
            PremiumFeature(
                Icons.Outlined.CalendarMonth,
                "Agenda",
                "Organiza tus actividades y tareas, y añade recordatorios para no olvidar lo importante."
            ),
            PremiumFeature(
                Icons.Outlined.Favorite,
                "Coherencia cardio-cerebral",
                "Sigue una guía visual para sincronizar corazón y cerebro y entrar en un estado de coherencia."
            ),
            PremiumFeature(
                Icons.Outlined.Spa,
                "Presencia Consciente",
                "Registra pequeños momentos de despertar durante el día y vuelve al presente con un solo toque."
            ),
            PremiumFeature(
                Icons.Outlined.Spa,
                "Espacio de Calma",
                "Disfruta de esferas relajantes, música, frases y fondos inmersivos."
            ),
            PremiumFeature(
                Icons.Outlined.Notifications,
                "Recordatorios inteligentes",
                "Programa avisos para tareas y prácticas como meditar, agradecer o revisar tus metas."
            ),
            PremiumFeature(
                Icons.AutoMirrored.Outlined.MenuBook,
                "Enciclopedia",
                "Consulta conocimientos prácticos sobre neurociencia, meditación, hábitos y temas relacionados."
            ),
            PremiumFeature(
                Icons.Outlined.Science,
                "Evidencia científica",
                "Explora investigaciones que respaldan estas enseñanzas en resúmenes fáciles de consultar."
            ),
            PremiumFeature(
                Icons.Outlined.Psychology,
                "Anclas Emocionales",
                "Utiliza una herramienta cognitiva guiada para afrontar momentos de estrés."
            ),
            PremiumFeature(
                Icons.Outlined.Mic,
                "Notas de voz",
                "Graba y reproduce ideas o reflexiones sin necesidad de escribirlas."
            ),
            PremiumFeature(
                Icons.Outlined.Event,
                "Resumen Semanal",
                "Consulta estadísticas generales y conserva un histórico de tu evolución."
            ),
            PremiumFeature(
                Icons.Outlined.Lock,
                "Notas protegidas",
                "Protege tus notas privadas mediante autenticación biométrica."
            ),
            PremiumFeature(
                Icons.Outlined.ContentPaste,
                "Menú «Pegar en»",
                "Convierte rápidamente el texto seleccionado en una nota, un lienzo o una frase."
            ),
            PremiumFeature(
                Icons.Outlined.Edit,
                "Frases de salud",
                "Amplía tu biblioteca con frases de salud y contenidos de autores adicionales."
            )
        )

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
