package com.ypg.wearneville;

import android.content.Context;
import android.widget.Toast;

import java.util.Random;

public class Utils {







//Devuelve una frase al azar, tomada de una lista xml
    public  static String  frases(Context context){
        String[] yArray = context.getResources().getStringArray(R.array.list_frases);
        Random r = new Random();
        return yArray[r.nextInt(yArray.length)];
    }


    //Muestra un mensaje
    public static void showmessage(String msg, Context context){

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

    }


}
