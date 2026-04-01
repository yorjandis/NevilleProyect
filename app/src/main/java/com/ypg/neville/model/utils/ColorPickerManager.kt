package com.ypg.neville.model.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
import com.ypg.neville.MainActivity
import com.ypg.neville.R

object ColorPickerManager {

    @JvmField
    var colorValue: Int = 0

    /**
     * Muestra un cuadro de selección de colores
     * @param context
     * @param color Color por defecto que será seleccionado
     * @param keyPreferences key en el archivo de preferencia donde será almacenado el color
     * @param title título del diálogo
     */
    @JvmStatic
    fun showColorPicker(context: Context, color: Int, keyPreferences: String, title: String) {
        val alert = AlertDialog.Builder(context)
        alert.setTitle(title)

        val view = LayoutInflater.from(context).inflate(R.layout.color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_picker)
        val alphaSlideBar = view.findViewById<AlphaSlideBar>(R.id.alphaSlideBar)
        val brightnessSlideBar = view.findViewById<BrightnessSlideBar>(R.id.brightnessSlide)
        val button = view.findViewById<Button>(R.id.colorpicker_btn_ok)

        colorPickerView.attachAlphaSlider(alphaSlideBar)
        colorPickerView.attachBrightnessSlider(brightnessSlideBar)

        // estableciendo un color inicial
        if (color != 0) {
            colorPickerView.setInitialColorRes(color)
        }

        alert.setView(view)
        val alertDialog = alert.create()

        colorPickerView.setColorListener(ColorListener { selectedColor, _ ->
            button.setBackgroundColor(selectedColor)
            colorValue = selectedColor
        })

        button.setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putInt(keyPreferences, colorValue)
            }
            MainActivity.currentInstance()?.AuxSetColorBar(colorValue)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
