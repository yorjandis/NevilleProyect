package com.ypg.neville.model.utils;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.ypg.neville.R;
import com.ypg.neville.model.db.utilsDB;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Clase de métodos estaticos  que obtendra datos de diversa fuentes
public class GetFromRepo {

Context context;


    public GetFromRepo(Context context) {
        this.context = context;
    }



    //Obtiene la lista de frases desde una página web: Devuelve el arreglo de frases
        //Nota: si no existe conección a internet procesa las frases que tiene en internamente en el xml
    public static void getFrasesFromWeb(Context context){
        

        if (Utils.isConnection(context)) {
            ExecutorService exec = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            exec.execute(new Runnable() {

                @Override
                public void run() {
                    String url = "https://projectsypg.mozello.com/productos/neville/frases-de-neville/";
                    long noFrasesNuevas = 0;
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

                           noFrasesNuevas += utilsDB.insertNewFrase(context, temp[1], temp[0], "","1"); //Inserta una frase a la BD


                        }
                    } catch (IOException ignored) {
                        error = true;
                    }

                    //UI thread
                    boolean error2 = error;
                    long finalResult = noFrasesNuevas;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            if(error2){
                                Toast.makeText(context, "Se produjo un error, código: 001", Toast.LENGTH_SHORT).show();
                            }else{

                                if (finalResult < 0){
                                    Toast.makeText(context, "No existe frases nuevas para añadir", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(context, "Se han añadido: "+ finalResult + " Frases nuevas", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                    });

                }

            });
        }else { //Si no existe conección a internet: utiliza la lista de frases del fichero xml interno

            String [] arrayFrases = GetFromRepo.getFrasesFromXML(context);
            String[] temp;
            for (int i = 1; i < arrayFrases.length; ++i) {
                temp = arrayFrases[i].split("::");
                utilsDB.insertNewFrase(context, temp[1], temp[2],"","1"); //Inserta una frase a la BD
            }
        }




    }//fin de method


    //Obtiene la lista de frases del fichero xml interno
    public static String[] getFrasesFromXML(Context context){
       return  context.getResources().getStringArray(R.array.list_frases);
    }

    //Obtiene la lista de url de videos de youtube del fichero xml interno

    public static String[] getUrlsVideosFromXML(Context context){
        return context.getResources().getStringArray(R.array.listvideos); //lista de videos en youtube
    }

    //Obtiene la lista de conferencias del directorio Assets interno
    public static String[] getConfListFromAssets(Context context) throws IOException {
        return  context.getAssets().list("conf");

    }


}
