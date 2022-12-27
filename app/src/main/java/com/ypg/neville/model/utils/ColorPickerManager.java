package com.ypg.neville.model.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;
import com.ypg.neville.MainActivity;
import com.ypg.neville.R;


public class ColorPickerManager {

    public static  int Color;


    /**
     * Muestra un cuadro de selección de colores
     * @param context
     * @param color Color por defecto que será seleccionado
     * @param key_Preferences key en el archivo de preferencia donde será almacenado el color
     */
    public static void  ShowColorPicker(Context context,  int color, String key_Preferences, String title){

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title);

        View view = LayoutInflater.from(context).inflate(R.layout.color_picker,null);
        ColorPickerView colorPickerView = view.findViewById(R.id.color_picker);
        AlphaSlideBar alphaSlideBar = view.findViewById(R.id.alphaSlideBar);
        BrightnessSlideBar brightnessSlideBar = view.findViewById(R.id.brightnessSlide);

        Button button = view.findViewById(R.id.colorpicker_btn_ok);

        colorPickerView.attachAlphaSlider(alphaSlideBar);
        colorPickerView.attachBrightnessSlider(brightnessSlideBar);



        //estableciendo un color inicial
        if (color != 0 ){
            colorPickerView.setInitialColorRes(color);
        }

        alert.setView(view);
        AlertDialog alertDialog = alert.create();

        colorPickerView.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                    button.setBackgroundColor(color);
                    ColorPickerManager.Color = color;
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key_Preferences, ColorPickerManager.Color).apply();

                if(MainActivity.mainActivityThis != null){
                    MainActivity.mainActivityThis.AuxSetColorBar(ColorPickerManager.Color);
                }
                alertDialog.dismiss();
            }
        });


        alertDialog.show();

    }

}
