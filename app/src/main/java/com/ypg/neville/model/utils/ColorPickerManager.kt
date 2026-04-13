package com.ypg.neville.model.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.MainActivity

object ColorPickerManager {

    @JvmField
    var colorValue: Int = 0

    @JvmStatic
    fun showColorPicker(context: Context, color: Int, keyPreferences: String, title: String) {
        val compose = ComposeView(context)
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(compose)
            .create()

        compose.setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                val initialColor = if (color != 0) color else 0xFF5A6270.toInt()
                val initial = Color(initialColor)
                val red = remember { mutableFloatStateOf(initial.red * 255f) }
                val green = remember { mutableFloatStateOf(initial.green * 255f) }
                val blue = remember { mutableFloatStateOf(initial.blue * 255f) }
                val selected = remember { mutableIntStateOf(initialColor) }

                fun recomputeColor() {
                    val r = red.floatValue.toInt().coerceIn(0, 255)
                    val g = green.floatValue.toInt().coerceIn(0, 255)
                    val b = blue.floatValue.toInt().coerceIn(0, 255)
                    selected.intValue = android.graphics.Color.rgb(r, g, b)
                    colorValue = selected.intValue
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .background(Color(selected.intValue), RoundedCornerShape(10.dp))
                    )

                    ChannelSlider("Rojo", red.floatValue) {
                        red.floatValue = it
                        recomputeColor()
                    }
                    ChannelSlider("Verde", green.floatValue) {
                        green.floatValue = it
                        recomputeColor()
                    }
                    ChannelSlider("Azul", blue.floatValue) {
                        blue.floatValue = it
                        recomputeColor()
                    }

                    Button(onClick = {
                        DbPreferences.default(context).edit {
                            putInt(keyPreferences, selected.intValue)
                        }
                        if (keyPreferences == "color_marcos") {
                            MainActivity.currentInstance()?.auxSetColorBar(selected.intValue)
                        }
                        alertDialog.dismiss()
                    }) {
                        Text("Aplicar")
                    }
                }
            }
        }

        alertDialog.show()
    }

    @androidx.compose.runtime.Composable
    private fun ChannelSlider(
        name: String,
        value: Float,
        onValueChange: (Float) -> Unit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name)
                Text(value.toInt().toString())
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..255f
            )
        }
    }
}
