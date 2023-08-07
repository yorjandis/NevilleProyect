package com.ypg.neville.model.utils;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.db.utilsDB;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Clase de métodos estáticos  que obtendrá datos de diversa fuentes (internas y externas)
public class GetFromRepo {

Context context;


    public GetFromRepo(Context context) {
        this.context = context;
    }



    //Obtiene la lista de frases desde una página web: Devuelve el arreglo de frases
        
    public static void getFrasesFromWeb(Context context){

        if (Utils.isConnection(context)) {
            ExecutorService exec = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    String url = "https://projectsypg.mozello.com/productos/neville/frases-de-neville/";
                    long result = 0; //Almacena el valor de resultado de la operación de insertar a BD
                    long noFrasesAnadidas = 0; //número de frases añadidas correctamente a la BD
                    long noFrasesConError = 0; //número de frases que NO han podido importarse a la BD
                    boolean error = false;
                    try {
                        Document document = Jsoup.connect(url).maxBodySize(0).get();

                        String ele = document.select("div.yor").outerHtml(); //devuelve el código html
                        ele = ele.replace("<p class=\"", ""); //quitando parte de la linea
                        ele = ele.replace("</p>", ""); //Quitando el final de las lineas
                        ele = ele.replace("</div>", ""); //quitando la última cadena

                        String[] arrayFrase = ele.split("\\n"); //diviendo el texto en líneas


                        for (int i = 1; i < arrayFrase.length; ++i) {

                            String[] temp = arrayFrase[i].split("\">"); //obteniendo: autor  y  texto de frase (index:0 autor; index:1 texto frase)

                          result =  utilsDB.insertNewFrase(context, temp[1], temp[0], "","1"); //Inserta una frase a la BD

                            //Obteniendo estadísticas del resultado
                            if (result > 0){
                                noFrasesAnadidas += 1;
                            }else{
                                noFrasesConError += 1;
                            }

                        }
                    } catch (IOException ignored) {
                        error = true;
                    }

                    //UI thread
                    boolean error2 = error;
                    long finalResult = noFrasesAnadidas;
                    long finalResultError = noFrasesConError;

                   // UI thread
                    //Chequeando si es un contexto UI válido
                    if (context instanceof  MainActivity){
                        MainActivity mainActivity = (MainActivity) context;
                        if(mainActivity.isFinishing()){return;}
                    }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                if(error2){
                                    Toast.makeText(context, "Se produjo un error, código: 001", Toast.LENGTH_SHORT).show();
                                }else{

                                    if (finalResult > 0){
                                        Toast.makeText(context, "Se han añadido: "+ finalResult + " Frases nuevas", Toast.LENGTH_SHORT).show();
                                    }

                                    if (finalResultError > 0 ){
                                        Toast.makeText(context, "No se han añadido: "+ finalResultError + " Frases", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            }
                        });

                }

            });
        }else { //Si no existe conección a internet: 

            Toast.makeText(context, "Error al importar frases. No se detecta conección internet", Toast.LENGTH_SHORT).show();
        }




    }//fin de method


    //Obtiene un arreglo con la lista de frases del fichero xml interno
    public static String[] getFrasesFromXML(Context context){
       return  context.getResources().getStringArray(R.array.list_frases);
    }

    //Obtiene un arreglo con la lista de url de videos de youtube del fichero xml interno

    public static String[] getUrlsVideosFromXML(Context context){
        return context.getResources().getStringArray(R.array.listvideos); //lista de videos en youtube
    }

    //Obtiene un arrglo con la lista de conferencias del directorio Assets interno
    public static String[] getConfListFromAssets(Context context) throws IOException {
        return  context.getAssets().list("conf");

    }


}
