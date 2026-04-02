package com.ypg.neville.model.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
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
            MaterialTheme {
                val selected = remember { mutableIntStateOf(if (color != 0) color else 0xFF444444.toInt()) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    AndroidView(factory = { ctx ->
                        ColorPickerView(ctx).apply {
                            val alpha = AlphaSlideBar(ctx)
                            val bright = BrightnessSlideBar(ctx)
                            attachAlphaSlider(alpha)
                            attachBrightnessSlider(bright)
                            setColorListener(ColorListener { selectedColor, _ ->
                                selected.intValue = selectedColor
                                colorValue = selectedColor
                            })
                        }
                    })

                    Button(onClick = {
                        PreferenceManager.getDefaultSharedPreferences(context).edit {
                            putInt(keyPreferences, selected.intValue)
                        }
                        MainActivity.currentInstance()?.auxSetColorBar(selected.intValue)
                        alertDialog.dismiss()
                    }) {
                        Text("Aplicar")
                    }
                }
            }
        }

        alertDialog.show()
    }
}
