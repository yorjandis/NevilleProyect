package com.ypg.neville.Ui.frag;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.model.utils.utilsFields;

import java.util.Timer;
import java.util.TimerTask;


public class frag_content_WebView extends Fragment {



public static String urlPath = "";                      //almacena la url a cargar en el navegador
public static String extension = "";                    //almacena la extension del fichero a cargar (.html | .txt)
public static String urlDirAssets = "";                 //almacena la carpeta en el directorio  Assets donde se encuentra el elemento a cargar


    public  static boolean flag_isPrimeraVez = true; //blblblblblb
    public static String elementLoaded = "";


    WebView webView;

    public frag_content_WebView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_content, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        visibilidadIconos(); //maneja la visibilidad de los iconos en la toolbar


       webView = view.findViewById(R.id.frag_content_webview);

        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setSupportZoom(true);





        //Ajustando el tamaño de fuente adecuadamente, segun sea texto o html
        if (frag_content_WebView.elementLoaded.contains("biografia") || frag_content_WebView.elementLoaded.contains("galeriafotos")) {
            webView.getSettings().setTextZoom(80);
        } else {
            webView.getSettings().setTextZoom(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("fuente_conf","170")));
        }


        webView.setVisibility( View.VISIBLE);
        webView.loadUrl(frag_content_WebView.urlPath);






//Restablece la posición la posicion de la barra de desplazamiento
            if (frag_content_WebView.flag_isPrimeraVez && frag_content_WebView.elementLoaded.contains("conf")
                    && PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("list_start_load","").contains("Ultima_conf_vista"))
            {

                        Timer t = new Timer(false);
                        t.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        int i = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION,"0"));
                                        webView.setScrollY(i);
                                        frag_content_WebView.flag_isPrimeraVez = false;
                                    }
                                });
                            }
                        }, 300); // 300 ms delay before scrolling
            }




        //Almacenando el path de la ultima conferencia cargada
        if (frag_content_WebView.elementLoaded.contains("conf")){
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA,utilsFields.ID_Str_row_ofElementLoad).apply();
        }


        //Comprobando y cargando el estado de favorito para el elemento cargado
            handlefavState();



    } //onViewCreated



    @Override
    public void onStart() {
        super.onStart();
            visibilidadIconos();
    }


    @Override
    public void onStop() {
        super.onStop();
        //Almacenando la posición del scrooll
            try {
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION,String.valueOf(webView.getScrollY())).apply();
            }catch (Exception ignored){}

    }

    //Controla la visibilidad de los iconos
    private void visibilidadIconos(){

        if (frag_content_WebView.elementLoaded.contains("conf") ){

           try{
               MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.VISIBLE);
               MainActivity.mainActivityThis.ic_toolsBar_frase_add.setVisibility(View.VISIBLE);
           } catch (Exception ignored){}

        }else{
            try{
                MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.INVISIBLE);
            } catch (Exception ignored){}
        }
    }


//lee y actualiza el estado de favorito de un elemento cargado
    private void  handlefavState(){
        String favState = "";

        if ("conf".equals(frag_content_WebView.elementLoaded)) {
            favState = utilsDB.readFavState(requireContext(), DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, utilsFields.ID_Str_row_ofElementLoad);
        }

            try {
                MainActivity.mainActivityThis.setFavColor(favState);
            }catch (Exception ignored){}



    }




}