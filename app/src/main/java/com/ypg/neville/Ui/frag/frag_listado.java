package com.ypg.neville.Ui.frag;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.skydoves.balloon.Balloon;
import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.db.DBManager;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.model.utils.Utils;
import com.ypg.neville.model.utils.adapter.MyListAdapterItemsList;
import com.ypg.neville.model.utils.balloon.HelpBalloon;
import com.ypg.neville.model.utils.utilsFields;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


//Muestra un listado de elementos que puede ser una listado de archivos de texto/html 0 de videos a mostrar en frag_content_WebView
//incorpora un reproductor de medios para los elementos de tipo videos/audios

public class frag_listado extends Fragment {

    YouTubePlayerView playerView;
    YouTubePlayer player;
    VideoView videoView;
    ListView listView;
    MyListAdapterItemsList myListAdapterItemsList;
    TextView mostrar_opciones;
    LinearLayout linearLayout;
    Spinner spinnerFilter;
    SearchView searchView, searchViewConf;
    ImageButton ayudaContextual;

    Utils utils = new Utils(getContext());

    //Campos estáticos de información
    public static String elementLoaded          = "";  //Elemento actualmente listado/a cargar:
    public static String urlPath                = "";  //almacena la url a cargar en el navegador
    public static String extension              = "";  //almacena la extension del fichero a cargar (.html | .txt)
    public static String urlDirAssets           = "";  //almacena la carpeta en el directorio  Assets donde se encuentra el elemento a cargar


    //Variables de la clase
    private  List<String> listado = new LinkedList<>();  //El listado de los elementos
    private  List<String> listadoUrlVideos = new LinkedList<>(); //listado de url videos tomados de xml file



    public frag_listado() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Obteniendo el tipo de elemento en el listado:
        try {
            //getArguments().getString()
        }catch (Exception ignored){}

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.frag_listado, container, false);

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle url = new Bundle();

        listView            = view.findViewById(R.id.frag_listado_list1);
        playerView          = view.findViewById(R.id.fraglist_videoplayer);
        mostrar_opciones    = view.findViewById(R.id.text_fraglist_showoptions);
        linearLayout        = view.findViewById(R.id.layout_fraglistado_option);
        spinnerFilter       = view.findViewById(R.id.spinner_fraglistado);
        searchView          = view.findViewById(R.id.searchView_fraglistado);
        searchViewConf      = view.findViewById(R.id.searchView_conf_fraglistado);
        videoView           = view.findViewById(R.id.fraglist_videoView);
        ayudaContextual     = view.findViewById(R.id.frag_listado_ayuda);


        //Seting: Mostrando/Ocultando la Ayuda contextual
            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("help_inline",true)){
                ayudaContextual.setVisibility(View.VISIBLE);
            }else{
                ayudaContextual.setVisibility(View.GONE);
            }
            ayudaContextual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
               ShowAyudaContextual(getContext());
                }
            });


        playerView.getLayoutParams().height = 0;
        playerView.requestLayout();
        playerView.setVisibility(View.VISIBLE);


        NavController navController = Navigation.findNavController(view);

        playerView.getYouTubePlayerWhenReady(new YouTubePlayerCallback() {
            @Override
            public void onYouTubePlayer(@NonNull YouTubePlayer youTubePlayer) {
                player = youTubePlayer;
            }

        });


        //Determinar si se reproduce un video al iniciar o se carga un listado de items de medios

        if (frag_listado.elementLoaded.contains("play_youtube")){ // Reproduce un video de youtube
            mostrar_opciones.setVisibility(View.INVISIBLE);
            ManagerPlayVideo(frag_listado.urlPath);

        }else if (frag_listado.elementLoaded.contains("play_video_repo")){ //reproduce un video externo (En la carpeta de almacenamiento de los videos)
            mostrar_opciones.setVisibility(View.INVISIBLE);
            ManagerPlayVideo(frag_listado.urlPath);

        }else{ //Cargando un listado
            mostrar_opciones.setVisibility(View.VISIBLE);
            //Genera el listado a cargar
            GenerarListado();

        }

        //Habilitar la búsqueda dentro de conferencias si se ha cargado la lista de conf

        if (elementLoaded.equalsIgnoreCase("conf")){
            searchViewConf.setVisibility(View.VISIBLE);
        }else{
            searchViewConf.setVisibility(View.GONE);
        }


        myListAdapterItemsList = new MyListAdapterItemsList(getContext(),R.layout.row_list_item,listado);
        listView.setAdapter(myListAdapterItemsList);


        //Yor estas lineas hacen que los elementos de la lisda puedan seleccionarse independientemente
        /*listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setSelector(R.color.fav_active);*/


        //Muestra/oculta el panel de opciones
        mostrar_opciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mostrar_opciones.getText().toString().contains("Mostrar Opciones")){
                    linearLayout.setVisibility(View.VISIBLE);
                    mostrar_opciones.setText(R.string.ocultar_opciones);
                }else{
                    linearLayout.setVisibility(View.GONE);
                    mostrar_opciones.setText(R.string.mostrar_opciones);
                }
            }
        });

            //Espinner para mostrar opciones de filtrado
            spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {


                    String itemSelected = adapterView.getSelectedItem().toString();

                    DBManager dbManager = new DBManager(getContext()).open();
                    Cursor cursor;
                    myListAdapterItemsList.clear();
                    listView.setAdapter(myListAdapterItemsList);


                    //Filtrar en la lista de conferencias
                    switch (itemSelected){

                        case ("Todas"):
                                if (Objects.equals(elementLoaded, "conf")){
                                    String sql = "SELECT title FROM "+DatabaseHelper.T_Conf+";";
                                    cursor = dbManager.ejectSQLRawQuery(sql);
                                    if (cursor.moveToFirst()){
                                        listado.clear();
                                        do {
                                            listado.add(cursor.getString(0));
                                        }while (cursor.moveToNext());

                                    }
                                    cursor.close();
                                }else if (Objects.equals(elementLoaded, "video_conf")){
                                    String sql = "SELECT title FROM "+DatabaseHelper.T_Videos+";";
                                    cursor = dbManager.ejectSQLRawQuery(sql);
                                    if (cursor.moveToFirst()){
                                        listado.clear();
                                        do {
                                            listado.add(cursor.getString(0));
                                        }while (cursor.moveToNext());
                                    }
                                    cursor.close();
                                }else if (Objects.equals(elementLoaded, "video_ext")){
                                    String sql = "SELECT title FROM "+ DatabaseHelper.T_Repo +" WHERE "+ DatabaseHelper.C_repo_type + "='video';";
                                    cursor = dbManager.ejectSQLRawQuery(sql);
                                    if (cursor.moveToFirst()){
                                        listado.clear();
                                        do {
                                            listado.add(cursor.getString(0));
                                        }while (cursor.moveToNext());
                                    }
                                    cursor.close();
                                }else if (Objects.equals(elementLoaded, "audio_ext")){
                                    String sql = "SELECT title FROM "+ DatabaseHelper.T_Repo +" WHERE "+ DatabaseHelper.C_repo_type + "='audio';";
                                    cursor = dbManager.ejectSQLRawQuery(sql);
                                    if (cursor.moveToFirst()){
                                        listado.clear();
                                        do {
                                            listado.add(cursor.getString(0));
                                        }while (cursor.moveToNext());
                                    }
                                    cursor.close();
                                }else if (Objects.equals(elementLoaded, "video_gredd")){
                                    String sql = "SELECT title FROM "+ DatabaseHelper.T_Videos +" WHERE "+ DatabaseHelper.C_videos_type + "='gregg';";
                                    cursor = dbManager.ejectSQLRawQuery(sql);
                                    if (cursor.moveToFirst()){
                                        listado.clear();
                                        do {
                                            listado.add(cursor.getString(0));
                                        }while (cursor.moveToNext());
                                    }
                                    cursor.close();

                                }
                                break;

                        case ("Favoritos"):
                            if (Objects.equals(elementLoaded, "conf")){

                                cursor = dbManager.getListado("Conferencias favoritas");

                                if (cursor.moveToFirst()){
                                    listado.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                    }while (cursor.moveToNext());
                                }

                                //Toast.makeText(getContext(), String.valueOf(listado.size()), Toast.LENGTH_SHORT).show();

                                cursor.close();

                            }else if (Objects.equals(elementLoaded, "video_conf")){
                                cursor = dbManager.getListado("Videos inbuilt favoritos");

                                if (cursor.moveToFirst()) {
                                    listado.clear();
                                    listadoUrlVideos.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                        listadoUrlVideos.add(cursor.getString(2));
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();

                            }else if (Objects.equals(elementLoaded, "video_ext")){
                                cursor = dbManager.getListado("Videos offline favoritos");
                                if (cursor.moveToFirst()) {
                                    listado.clear();
                                    listadoUrlVideos.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                        listadoUrlVideos.add(cursor.getString(2));
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();
                            }else if (Objects.equals(elementLoaded, "audio_ext")){
                                cursor = dbManager.getListado("Audios offline favoritos");
                                if (cursor.moveToFirst()) {
                                    listado.clear();
                                    listadoUrlVideos.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                        listadoUrlVideos.add(cursor.getString(2));
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();
                            }else if (Objects.equals(elementLoaded, "video_gredd")){
                                cursor = dbManager.getListado("Videos gregg favoritos");
                                if (cursor.moveToFirst()) {
                                    listado.clear();
                                    listadoUrlVideos.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                        listadoUrlVideos.add(cursor.getString(2));
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();

                            }

                            break;
                        case ("Con notas"): // Yorj: Solo las conferencias llevan notas asociadas
                            if (Objects.equals(elementLoaded, "conf")){
                                cursor = dbManager.getListado("Conferencias con notas");

                                if (cursor.moveToFirst()) {
                                    listado.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();

                            }
                            if (Objects.equals(elementLoaded, "video_conf")){
                                cursor = dbManager.getListado("Videos inbuilt con notas");

                                if (cursor.moveToFirst()) {
                                    listado.clear();
                                    do {
                                        listado.add(cursor.getString(1));
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();

                            }

                            break;

                    }

                    myListAdapterItemsList.notifyDataSetChanged();

                    dbManager.close();
                    //actualizando la informacion
                    searchView.setQueryHint("Buscar en títulos("+listado.size() + " elementos)");


                } //onItemSelected

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            //SearchView: filtra el listado de acuerdo al texto introducido
            searchView.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    searchView.setQueryHint("Buscar en títulos("+listado.size() + " elementos)");
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String texto) {

                    return true;
                }

                @Override
                public boolean onQueryTextChange(String texto) {
                    myListAdapterItemsList.getFilter().filter(texto); //aqui ponemos el filtro de búsqueda (en el adaptador del ListView

                    return true;
                }
            });

            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                        spinnerFilter.callOnClick();
                    return true;
                }
            });


            //Busqueda dentro de las conferencias
            searchViewConf.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {

                    if (!query.isEmpty()){

                        try {
                            myListAdapterItemsList.clear();
                            listado =  Utils.searchInConf(requireContext(), query);
                            myListAdapterItemsList.addAll(listado);
                            myListAdapterItemsList.notifyDataSetChanged();

                        } catch (IOException e) {
                            Toast.makeText(getContext(),"No se pudo realizar la búsqueda", Toast.LENGTH_SHORT).show();
                        }
                    }


                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {

                    if (newText.isEmpty()){
                        myListAdapterItemsList.clear();
                        listado = utilsDB.loadConferenciaList(getContext());
                        myListAdapterItemsList.addAll(listado);
                        myListAdapterItemsList.notifyDataSetChanged();
                    }
                    return true;
                }
            });



        //evento onClic  listado
        listView.setOnItemClickListener((AdapterView<?> parent, View view1, int position, long id) -> {

            String selectedItemText = (String) parent.getItemAtPosition(position);

            playerView.setVisibility(View.GONE);
            videoView.setVisibility(View.GONE);

            switch (frag_listado.elementLoaded){

            //Para Listados en la tabla Videos (video_conf,video_book, video_gregg)
                case "video_gredd":
                case "video_conf":
                case "video_book":
                    if (!Utils.isConnection(getContext())){break;} //chequeando si existe conexión
                    DBManager dbManager = new DBManager(getContext()).open();
                    playerView.setVisibility(View.VISIBLE);
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText;
                    playVideo(dbManager.getDbInfoFromItem(selectedItemText, DatabaseHelper.T_Videos));
                    dbManager.close();

                    handlefavState();
                    break;

            //Para Texto
                case "conf":
                    frag_content_WebView.elementLoaded = "conf";
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText;
                    frag_content_WebView.urlPath = "file:///android_asset/conf/"+ selectedItemText + ".txt";
                    navController.navigate(R.id.frag_content_webview);
                    break;
                case "preguntas":
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText;
                    frag_content_WebView.urlPath = "file:///android_asset/preg/"+ selectedItemText + ".txt";
                    navController.navigate(R.id.frag_content_webview);
                    break;
                case "citasConferencias":
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText;
                    frag_content_WebView.urlPath = "file:///android_asset/cita/"+ selectedItemText + ".txt";
                    navController.navigate(R.id.frag_content_webview);
                    break;
                case "ayudas":
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText;
                    frag_content_WebView.urlPath = "file:///android_asset/ayuda/"+ selectedItemText + ".txt";
                    navController.navigate(R.id.frag_content_webview);
                    break;

            //Para videos offline
                case "video_ext":

                    //Setting: Verificando si se reproduce en el reproductor interno (videoView) o en streaming (servicio)
                    if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("play_video_background",false)){
                        Utils.playInStreaming(getContext(), utilsFields.REPO_DIR_VIDEOS, selectedItemText);
                    }else{
                        videoView.setVisibility(View.VISIBLE);
                        playRepo(utilsFields.REPO_DIR_VIDEOS, selectedItemText);
                    }
                    break;
            //Para audios offline
                case "audio_ext":
                    //Setting: Verificando si se reproduce en el reproductor interno (videoView) o en streaming (servicio)
                    if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("play_audio_background",false)){
                        Utils.playInStreaming(getContext(), utilsFields.REPO_DIR_AUDIOS, selectedItemText);
                    }else{
                        videoView.setVisibility(View.VISIBLE);
                        playRepo(utilsFields.REPO_DIR_AUDIOS, selectedItemText);
                    }
                    break;

            }


        });



    } // onViewCreated


    /**
     * Administra la reproducción de un medio (video youtube, audio/video externo).
     * Crea un hilo de ejecución con temporizador
     * @param urlVideo url del medio a reproducir
     */
    private void ManagerPlayVideo(@NonNull String urlVideo){
        if (!urlVideo.isEmpty()){
            Handler h =new Handler() ; //Configurando un hilo de ejecución con una espera de 1300 milisegundos
            h.postDelayed(new Runnable() {
                public void run() {

                    switch (frag_listado.elementLoaded){
                        case"play_youtube":
                        case"video_conf":
                            playerView.setVisibility(View.VISIBLE);
                            videoView.setVisibility(View.GONE);
                            playVideo(urlVideo);
                         break;

                        case "video_ext":
                        case "play_video_repo":
                            playerView.setVisibility(View.GONE);
                            videoView.setVisibility(View.VISIBLE);
                            playRepo(utilsFields.REPO_DIR_VIDEOS, urlVideo);
                            break;

                        case "audio_ext":
                            // Iniciar el  servicio Streaming

                            break;

                    }

                }
            }, 1300); //1.3 segundos
        }

    }


    //Reproduce video inbuilt de Youtube (Utiliza el reproductor playView)
    private void playVideo(String urlVideo){

            playerView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            playerView.requestLayout();

            if(player != null){

                //Carga automáticamente un video, si se es dado el parámetro
                player.loadVideo(urlVideo, 0);}

            try {

                if (MainActivity.mainActivityThis.isFinishing()){return;}// chequeando el estado del ciclo de vida de MainActivity

                MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.VISIBLE);


            }catch (Exception ignored){}



            //leyendo el estado de los favoritos
            Utils utils = new Utils(getContext());
            String favstate = "";
            favstate =   utilsDB.readFavState(requireContext(), DatabaseHelper.T_Videos,DatabaseHelper.C_videos_link,utilsFields.ID_Str_row_ofElementLoad );

           try {
               if (MainActivity.mainActivityThis.isFinishing()){return;}// chequeando el estado del ciclo de vida de MainActivity

               MainActivity.mainActivityThis.setFavColor(favstate);
           }catch (Exception ignored){}




    }


    //Reproduce video offline, en el almacenamiento externo (Utiliza el reproductor VideoView)
    private void playRepo(String RepoDir, String urlVideo){
        utilsFields.ID_Str_row_ofElementLoad = urlVideo;
        videoView.setVideoPath(File.separator + "sdcard" + File.separator + utilsFields.REPO_DIR_ROOT + File.separator + RepoDir + File.separator + urlVideo);
        videoView.setMediaController(new MediaController(getContext()));
        videoView.requestFocus();
        videoView.start();

        //leyendo el estado de los favoritos

        try {
            MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.VISIBLE);
        }catch (Exception ignored){}

        Utils utils = new Utils(getContext());
        String favstate = "";
        favstate =   utilsDB.readFavState(requireContext(), DatabaseHelper.T_Repo,DatabaseHelper.C_repo_title,utilsFields.ID_Str_row_ofElementLoad );
       try {
           MainActivity.mainActivityThis.setFavColor(favstate);
       }catch (Exception ignored){}

    }





    @Override
    public void onStart() {
        super.onStart();

        MainActivity.mainActivityThis.ic_toolsBar_fav.setColorFilter(requireContext().getResources().getColor(R.color.black, null));
        MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.GONE);
        MainActivity.mainActivityThis.ic_toolsBar_frase_add.setVisibility(View.VISIBLE);

    }

    @Override
    public void onStop() {
        super.onStop();
        playerView.getLayoutParams().height = 0;
        playerView.requestLayout();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (playerView != null){ playerView.release();}
    }


    //Generando el listado a partir de la información dada
        private void GenerarListado(){
            Utils utils = new Utils(getContext());

            switch (frag_listado.elementLoaded){
                case "conf":
                    listado = utilsDB.loadConferenciaList(getContext());
                    break;
                case "preguntas":
                    try { utils.listFilesInAssets("preg", listado);} catch (IOException e) {e.printStackTrace();}
                    break;
                case "citasConferencias":
                    try { utils.listFilesInAssets("cita", listado);} catch (IOException e) {e.printStackTrace();}
                    break;
                case "video_conf":
                    utilsDB.LoadVideoList(requireContext(), "conf", listadoUrlVideos, listado);
                    break;
                case "video_book":
                    utilsDB.LoadVideoList(requireContext(),"audioLibro", listadoUrlVideos, listado);
                    break;
                case "ayudas":
                    try { utils.listFilesInAssets("ayuda", listado);} catch (IOException e) {e.printStackTrace();}
                    break;
                case "video_gredd":
                    utilsDB.LoadVideoList(requireContext(),"gregg", listadoUrlVideos, listado);
                    break;
                case "video_ext":
                    if (utilsDB.LoadRepoFromDB(requireContext(), "video", listado) == 0){
                        Utils.loadRepo(requireContext());
                    }
                    break;
                case "audio_ext":
                   if(utilsDB.LoadRepoFromDB(requireContext(), "audio", listado)==0){
                       Utils.loadRepo(requireContext());
                   }
                    break;
            }

        }

    //lee y actualiza el estado de favorito de un elemento cargado
    private void  handlefavState(){
        String favState = "";

            favState = utilsDB.readFavState(requireContext(), DatabaseHelper.T_Videos, DatabaseHelper.C_videos_title, utilsFields.ID_Str_row_ofElementLoad);

        try {
            MainActivity.mainActivityThis.setFavColor(favState);
        }catch (Exception ignored){}



    }


    /**
     * Muestra la ayuda contextual para los elementos
     * @param context contexto de trabajo
     */
    @SuppressLint("SuspiciousIndentation")
    private void ShowAyudaContextual(Context context){

        HelpBalloon helpBalloon = new HelpBalloon(getContext());

        Balloon balloon1, balloon2, balloon3,balloon4,balloon5;

        balloon1 = helpBalloon.buildFactory("Añadir un apunte",getViewLifecycleOwner());
        balloon2 = helpBalloon.buildFactory("filtro de listado",getViewLifecycleOwner());
        balloon3 = helpBalloon.buildFactory("listado de elementos. Toque un elemento para abrirlo",getViewLifecycleOwner());


        balloon1
                .relayShowAlignBottom(balloon2, mostrar_opciones)
                .relayShowAlignTop(balloon3, listView);

// show sequentially customListBalloon-customProfileBalloon-customTagBalloon
        balloon1.showAlignBottom(MainActivity.mainActivityThis.ic_toolsBar_nota_add);


    }



}