package com.ypg.neville.Ui.frag;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.skydoves.balloon.Balloon;
import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.db.DBManager;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.model.utils.QRManager;
import com.ypg.neville.model.utils.UiModalWindows;
import com.ypg.neville.model.utils.balloon.HelpBalloon;
import com.ypg.neville.model.utils.utilsFields;

import java.util.Objects;
import java.util.Random;


public class frag_home extends Fragment {

    public static boolean isPrimeracarga = true;

    TextView text_frase, textAutor;
    AppCompatImageView ic_fav, ic_shared;
    private long id_frase; //ALmacena el id de la frase actual
    NavController navController;
    LinearLayout linearLayout_IconosInlineFrases;
    ImageButton ayudaContextual;
    ImageView masinfo;

    public static frag_home frag_home_this; //Referencia a esta clase

    public static String elementLoaded_home = "";

    public static int ShowHelpCount = 0;

    public frag_home() {
        // Required empty public constructor
        frag_home_this = frag_home.this; //referencia a este fragmento
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_home, container, false);

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

         text_frase = view.findViewById(R.id.frag_home_text);
         textAutor = view.findViewById(R.id.frag_home_textautor);
         ic_fav = view.findViewById(R.id.ic_frase_fav);
         ic_shared = view.findViewById(R.id.ic_frase_shared);
         linearLayout_IconosInlineFrases = view.findViewById(R.id.layout_fraghome_icons_inlines_frase);
         ayudaContextual = view.findViewById(R.id.frag_home_ayuda);
         masinfo = view.findViewById(R.id.frag_home_frases_img_abajo);


        navController = Navigation.findNavController(view);
        frag_home.elementLoaded_home = "frases";






         //Seting: Mostrando/Ocultando la Ayuda contextual
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("help_inline",true)){
            ayudaContextual.setVisibility(View.VISIBLE);
        }else{
            ayudaContextual.setVisibility(View.GONE);
        }
        ayudaContextual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frag_home.ShowHelpCount = 0;
                ShowAyudaContextual(MainActivity.mainActivityThis);

               // UiModalWindows.showAyudaContectual(getContext(),"Ayuda Contextual","Aprenda como utilizar las nuevas funciones",
                      //  getString(R.string.ayuda_frag_home),true, null);
            }
        });


         //Setting: Aplicando tamaño de fuente a frases
        text_frase.setTextSize(Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("fuente_frase","28")));

        //Setting: Aplicando el color de fuente de las frases
        int temp_color = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("color_letra_frases", 0);
        if (temp_color != 0){
            text_frase.setTextColor(temp_color);
        }

        //Setting: Chequeando si se muestran/ocultan los iconos inlines en las frases
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("hide_frase_controles", false)){
            linearLayout_IconosInlineFrases.setVisibility(View.INVISIBLE);
            masinfo.setImageResource(R.drawable.ic_abajo);
        }else{
            linearLayout_IconosInlineFrases.setVisibility(View.VISIBLE);
            masinfo.setImageResource(R.drawable.ic_arriba);
        }

        // Setting: determinando si el fragment se ha iniciado la primera vez
        if (isPrimeracarga){
            Init();
           isPrimeracarga = false;
        }else {
            Loadfrases(false);
        }




        //expandir o colapsar la información de la frase (Consulta en Setting):
        masinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (linearLayout_IconosInlineFrases.getVisibility() == View.INVISIBLE){
                    linearLayout_IconosInlineFrases.setVisibility(View.VISIBLE);
                    masinfo.setImageResource(R.drawable.ic_arriba);
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("hide_frase_controles", false).apply();
                }else {
                    linearLayout_IconosInlineFrases.setVisibility(View.INVISIBLE);
                    masinfo.setImageResource(R.drawable.ic_abajo);
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("hide_frase_controles", true).apply();
                }
            }
        });



        //Onclick del texto en frases
        text_frase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("list_start_load","Nada").contains("Frase_fav_azar")){
                    Loadfrases(true); //Carga una frase favorita  al azar
                }else{
                    Loadfrases(false);
                }


            }
        });

        //onLong click sobre el texto para abrir el cuadro de adicionar una nota asociada
        text_frase.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DBManager dbManager = new DBManager(getContext()).open();
                String  query = "SELECT nota FROM " + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.C_frases_frase +"='"+text_frase.getText().toString()+"';";
                Cursor cursor = dbManager.ejectSQLRawQuery(query);

               if (cursor.moveToFirst()){
                   UiModalWindows.NotaManager(getContext(),cursor.getString(0),DatabaseHelper.T_Frases,DatabaseHelper.C_frases_frase, text_frase.getText().toString());
               }
                cursor.close();
                dbManager.close();
                return true;
            }
        });
        
        

        ic_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String result = utilsDB.UpdateFavorito(requireContext(), DatabaseHelper.T_Frases,DatabaseHelper.CC_id,"", (int) id_frase);

                //Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();

                if (!Objects.equals(result, "")){
                    setFavColor(result);
                }



            }
        });

        //Comparte el texto de una frase
        ic_shared.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, text.getText().toString());
                startActivity(Intent.createChooser(intent, "Compartir frase"));*/

                String autor = textAutor.getText().toString().replace("<","");
                autor = autor.replace(">","");

                QRManager.ShowQRDialog(getContext(),"f&&"+ text_frase.getText().toString()+ "&&" + autor + "&&","Compartir Frase","Puede utilizar el lector QR para importar frases");
            }
        });

    }


    @Override
    public void onStart() {
        super.onStart();

        /*utilsFields.elementLoaded = "frases"; //Actualiza el campo de informacion
        utilsFields.ID_row_ofElementLoad = (int) id_frase;*/

        try {
            MainActivity.mainActivityThis.ic_toolsBar_frase_add.setVisibility(View.VISIBLE);
        }catch (Exception ignored){}

        try {
            MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.GONE);
        }catch (Exception ignored){}


    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            MainActivity.mainActivityThis.ic_toolsBar_frase_add.setVisibility(View.GONE);
        }catch (Exception ignored){}

        try {
            MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.GONE);
        }catch (Exception ignored){}


        //restear las variables antes de abandonar el fragment
        utilsFields.ID_row_ofElementLoad = -1;

    }



    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);


    }



    //Aplicando la configuración en setting

    private void Init(){

        String temp = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("list_start_load","");

        //Pone un valor por defecto
        if (temp.isEmpty()){
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("list_start_load","Frase_azar").apply();
        }


        switch (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("list_start_load","")){

            case "Ultima_frase_vista":
                String idUltimaFrase = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE,"0");

                String sql = "SELECT * FROM " + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.CC_id + "=" + Integer.parseInt(idUltimaFrase);
                DBManager dbManager = new DBManager(getContext()).open();
                Cursor cursor;
                cursor =  dbManager.ejectSQLRawQuery(sql);

                if (cursor.moveToFirst()){
                    text_frase.setText(cursor.getString(1));
                    textAutor.setText("<" + cursor.getString(2)+">");

                    id_frase = cursor.getInt(0);        //Almacena el id de la frase actual (para operaciones crud)
                    utilsFields.ID_row_ofElementLoad = cursor.getInt(0);    //Almacena el id de la frase actual (para operaciones crud)
                    utilsFields.ID_Str_row_ofElementLoad = cursor.getString(1); // (para operaciones crud)
                    setFavColor(cursor.getString(4));
                }
                break;
            case "Frase_azar":
                Loadfrases(false); //Cargar una frase aleatoria
                break;
            case  "Frase_fav_azar":
                Loadfrases(true); //Cargar una frase aleatoria
                break;

            case "Conf_azar":
                    LoadConfAzar(false);
                break;
            case "Conf_fav_azar":
                LoadConfAzar(true);
                break;
            case "Ultima_conf_vista":
                utilsFields.ID_Str_row_ofElementLoad = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA,"");
                if (!utilsFields.ID_Str_row_ofElementLoad.isEmpty()){
                    frag_content_WebView.extension = ".txt";
                    frag_content_WebView.urlDirAssets = "conf";
                    frag_content_WebView.urlPath = "file:///android_asset/" + frag_content_WebView.urlDirAssets +"/"+ utilsFields.ID_Str_row_ofElementLoad + frag_content_WebView.extension;
                    navController.navigate(R.id.frag_content_webview);
                }else {
                    Toast.makeText(getContext(), "Debe cargar al menos una conferencia en Texto", Toast.LENGTH_SHORT).show();
                   frag_listado.elementLoaded = "conf";
                   // MainActivity.mainActivityThis.bottomNavigationView.setItemSelected(R.id.bottom_menu_conf, true);
                    navController.navigate(R.id.frag_listado);
                }
                
                break;

        }
    }




    //Carga una frase aleatoria
    private  void Loadfrases(boolean isfav){
    //ArrayList<String> tagList = new ArrayList();
    //String[] someArray = getResources().getStringArray(R.array.list_frases);
    //Random r = new Random();
    //text.setText(someArray[r.nextInt(someArray.length)]);

        DBManager dbManager = new DBManager(getContext()).open();
        Cursor cursor;
        //Determina si cargar lista de frases favoritas o lista de frases totales
        if (isfav){
            cursor = dbManager.getListado("Frases favoritas");
        }else {
            cursor = dbManager.getListado("Todas las frases");
        }


        if (cursor.moveToFirst()){
                 int randomNumber = 0;

                Random random = new Random();
                if (cursor.getCount() > 1){
                     randomNumber = random.nextInt(cursor.getCount());
                }

                cursor.move(randomNumber);
                text_frase.setText(cursor.getString(1));
                textAutor.setText("<"+cursor.getString(2)+">");

                id_frase = cursor.getInt(0);        //Almacena el id de la frase actual (para operaciones crud)
                utilsFields.ID_row_ofElementLoad = cursor.getInt(0);    //Almacena el id de la frase actual (para operaciones crud)
                utilsFields.ID_Str_row_ofElementLoad = cursor.getString(1); // (para operaciones crud)

            //Almacenando el id de la ultima frase mostrada
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE,String.valueOf(cursor.getInt(0))).apply();

                setFavColor(cursor.getString(4));

            cursor.close();
        }else{
            //Muestra un mensaje de que no hay registros para cargar e intenta cargar la lista de frases inbuilt
            Toast.makeText(getContext(),"No hay frase para mostrar. Cargando frases inbuilt", Toast.LENGTH_SHORT).show();
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("list_start_load","Frase_azar").apply();
        }

    dbManager.close();

    }


    //Carga una conferencia al azar
    private void LoadConfAzar(boolean isfav){
        DBManager dbManager = new DBManager(getContext()).open();
        Cursor cursor;

        if (isfav){
            cursor = dbManager.getListado("Conferencias favoritas");
        }else{
            cursor = dbManager.getListado("Todas las conf");
        }


            if (cursor.moveToFirst()){
                int randomNumber = 0;

                Random random = new Random();
                if (cursor.getCount() > 1){
                    randomNumber = random.nextInt(cursor.getCount());
                }

                cursor.move(randomNumber);

                utilsFields.ID_Str_row_ofElementLoad = cursor.getString(1);
                frag_content_WebView.extension = ".txt";
                frag_content_WebView.urlDirAssets = "conf";

                frag_content_WebView.urlPath = "file:///android_asset/" + frag_content_WebView.urlDirAssets +"/"+ cursor.getString(1) + frag_content_WebView.extension;
                navController.navigate(R.id.frag_content_webview);
            }else{
                Toast.makeText(getContext(),"No hay Conferencia favorita para mostrar. Cargando Conferencias inbuilt", Toast.LENGTH_SHORT).show();
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("list_start_load","Conf_azar").apply();
            }

    }


    /**
     * Establece el color del icono del favorito
     * @param fav_state
     */
    private void setFavColor(String fav_state){
        if(Objects.equals(fav_state, "1")) {
            ic_fav.setColorFilter(getContext().getResources().getColor(R.color.fav_active,null));
            animate(ic_fav);
        }
        else{
            ic_fav.setColorFilter(getContext().getResources().getColor(R.color.fav_inactive, null));
        }

    }

    /**
     * Anima un icono
     * @param view
     */
    private void animate(View view){
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("scaleX", 1.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1.3f));
        scaleDown.setDuration(300);
        scaleDown.setAutoCancel(false);
        scaleDown.setRepeatCount(3);
        scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

         scaleDown.start();
    }


    /**
     * Muestra la ayuda contextual para los elementos
     * @param context contexto de trabajo
     */
    @SuppressLint("SuspiciousIndentation")
    private void ShowAyudaContextual(Context context){

        HelpBalloon helpBalloon = new HelpBalloon(getContext());
        linearLayout_IconosInlineFrases.setVisibility(View.VISIBLE);
        Balloon balloon1, balloon2, balloon3,balloon4,balloon5, balloon6;

        balloon1 = helpBalloon.buildFactory("Añadir un apunte");
        balloon2 = helpBalloon.buildFactory("Añadir una frase");
        balloon3 = helpBalloon.buildFactory("toque largo sobre frase para añadir una nota asociada");
        balloon4 = helpBalloon.buildFactory("Marca la frase como favorita");
        balloon5 = helpBalloon.buildFactory("Compartir la frase");
        balloon6 = helpBalloon.buildFactory("Mostrar/ocultar los iconos");

                balloon1
                .relayShowAlignBottom(balloon2, MainActivity.mainActivityThis.ic_toolsBar_frase_add)
                .relayShowAlignTop(balloon3, text_frase)
                .relayShowAlignBottom(balloon4, ic_fav)
                .relayShowAlignBottom(balloon5, ic_shared)
                .relayShowAlignTop(balloon6, masinfo);

        balloon1.showAlignBottom(MainActivity.mainActivityThis.ic_toolsBar_nota_add);


    }

}