package com.ypg.neville.Ui.frag;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.db.DBManager;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.model.utils.UiModalWindows;
import com.ypg.neville.model.utils.Utils;
import com.ypg.neville.model.utils.adapter.MyListAdapterList_info;
import com.ypg.neville.model.utils.utilsFields;

import java.util.LinkedList;
import java.util.List;


//fragmento que mostrará un listado con las: Favoritos, Notas y Frases
public class frag_list_info extends Fragment {

   public List<String> listado = new LinkedList<>();
    ImageButton ayudaContextual;
    public static  Spinner spinnerStatic;

    public frag_list_info() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_list_info, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner spinner = view.findViewById(R.id.frag_listinfo_spinne_filtro);
        ListView list =  view.findViewById(R.id.frag_listinfo_list);
        ayudaContextual = view.findViewById(R.id.frag_list_info_ayuda);

        spinnerStatic = spinner;


        //Setting: Mostrando/Ocultando la Ayuda contextual
            showHideAyudaContextual();

        ayudaContextual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiModalWindows.showAyudaContectual(getContext(),"Ayuda Contextual","Aprenda como utilizar las nuevas funciones",
                        getString(R.string.ayuda_frag_list_info),true, null);
            }
        });



        //ArrayAdapter<String> adapterListado = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, listado);

        //Adaptador para el ListView de la lista
        MyListAdapterList_info myListAdapterList_info = new MyListAdapterList_info(requireContext(),R.layout.row_list_info_item,listado);

        list.setAdapter(myListAdapterList_info);



        //Nota yor: las acciones de los elementos en cada fila del custom listView es implementada dentro del propio adapter (MyListAdapterList_info)


        //eventos del spinner:
            //Al seleccionar un item en el spinner se muestra la lista filtrada según el criterio dado
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                String itemtxt = parentView.getItemAtPosition(position).toString(); //Almacena la categoria actual del spinner

                utilsFields.spinnerListInfoItemSelected = itemtxt; //Actualizando variable


                myListAdapterList_info.clear();

                   DBManager dbManager = new DBManager(getContext()).open();
                   Cursor cursor;

                  cursor =  dbManager.getListado(itemtxt); //Realiza la consulta

                if(cursor.moveToFirst()){
                    while (!cursor.isAfterLast()) {
                        listado.add(cursor.getString(1));
                        cursor.moveToNext();
                    }

                }

                myListAdapterList_info.notifyDataSetChanged();

                    cursor.close();
                  dbManager.close();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        });



        //Evento OnLongClick de la lista
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                String itemText = parent.getItemAtPosition(position).toString();
                DBManager dbManager = new DBManager(getContext()).open();
                Cursor cursor = null;
                String query = "";
                ContentValues contentValues  = new ContentValues();

                switch (spinner.getSelectedItem().toString()){

                    case "Frases inbuilt favoritas":
                    case "Frases personales favoritas":
                    case "Frases inbuilt":
                        //Cargar la info de la nota
                        query = "SELECT nota FROM " + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.C_frases_frase+"='"+itemText+"';";
                        cursor = dbManager.ejectSQLRawQuery(query);
                        if (cursor != null && cursor.moveToFirst()){
                            UiModalWindows.NotaManager(getContext(),cursor.getString(0),DatabaseHelper.T_Frases,DatabaseHelper.C_frases_frase,itemText);
                        }

                        break;

                    case "Conferencias favoritas":
                    case "Conferencias con notas":
                        //Cargar la info de la nota
                       query = "SELECT nota FROM " + DatabaseHelper.T_Conf + " WHERE " + DatabaseHelper.C_conf_title +"='"+itemText+"';";
                       cursor = dbManager.ejectSQLRawQuery(query);
                        if (cursor != null && cursor.moveToFirst()){
                            UiModalWindows.NotaManager(getContext(),cursor.getString(0),DatabaseHelper.T_Conf,DatabaseHelper.C_conf_title,itemText);
                        }

                        break;
                    case "Videos inbuilt favoritos":
                    case "Videos inbuilt con notas":
                        //Abrir el cuadro de Add_notas
                        query = "SELECT nota FROM " + DatabaseHelper.T_Videos + " WHERE " + DatabaseHelper.C_videos_title +"='"+itemText+"';";
                        cursor = dbManager.ejectSQLRawQuery(query);
                        if (cursor != null && cursor.moveToFirst()){
                            UiModalWindows.NotaManager(getContext(),cursor.getString(0),DatabaseHelper.T_Videos,DatabaseHelper.C_videos_title,itemText);
                        }
                        break;

                    case "Videos offline favoritos":
                    case "Videos offline con notas":
                        //Abrir el cuadro de Add_notas
                        query = "SELECT nota FROM " + DatabaseHelper.T_Repo + " WHERE " + DatabaseHelper.C_repo_title +"='"+itemText+"';";
                        cursor = dbManager.ejectSQLRawQuery(query);
                        if (cursor != null && cursor.moveToFirst()){
                            UiModalWindows.NotaManager(getContext(),cursor.getString(0),DatabaseHelper.T_Repo,DatabaseHelper.C_repo_title,itemText);
                        }
                        break;
                    case "Audios offline favoritos":
                    case "Audios offline con notas":
                        //Abrir el cuadro de Add_notas

                        //Yorjandis: por el momento no implementamos esta lógica
                        break;

                }




               dbManager.close();


                return true;
            }
        });


        //Evento On click de la lista
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {


                String itemText = adapterView.getItemAtPosition(position).toString();
                Bundle urlvideo = new Bundle();
                DBManager dbManager = new DBManager(getContext()).open();

                NavController navController = Navigation.findNavController(view);

                switch (spinner.getSelectedItem().toString()){

                    case "Frases inbuilt favoritas":
                        AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
                        alert.setTitle("¿Dejar de ser favorita?");
                        alert.setPositiveButton("Quitar favorito", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                utilsDB.UpdateFavorito(requireContext(), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase,itemText,-1);

                                listado.clear();

                                //Actualizando el listado
                                Cursor cursor =  dbManager.getListado(utilsFields.spinnerListInfoItemSelected); //Realiza la consulta
                                if(cursor.moveToFirst()){
                                    do {
                                        listado.add(cursor.getString(1));
                                    } while (cursor.moveToNext());
                                }
                                myListAdapterList_info.clear(); //Limpia el ArrayAdapter
                                myListAdapterList_info.addAll(listado); //Adiciona al ArrayAdapter el listado
                                myListAdapterList_info.notifyDataSetChanged(); //Notifica al ArrayAdapter de los cambios
                                   cursor.close();
                                dbManager.close();


                            }
                        });
                        alert.show();
                        break;

                    case "Frases personales":
                    case "Frases inbuilt con notas":
                    case "Frases personales con notas":
                        //Cargar la info de la nota

                       String  query = "SELECT nota FROM " + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.C_frases_frase +"='"+itemText+"';";
                       Cursor cursor = dbManager.ejectSQLRawQuery(query);

                        if (cursor.moveToFirst()){
                            UiModalWindows.NotaManager(requireContext(),cursor.getString(0),DatabaseHelper.T_Frases,DatabaseHelper.C_frases_frase,itemText );
                        }
                        break;

                    case "Conferencias favoritas": //Abrir el texto de la conferencia
                    case "Conferencias con notas": //Abrir el texto de la conferencia
                        utilsFields.ID_Str_row_ofElementLoad = itemText;
                        frag_content_WebView.extension = ".txt";
                        frag_content_WebView.urlDirAssets = "conf";
                        frag_content_WebView.urlPath = "file:///android_asset/" + frag_content_WebView.urlDirAssets +"/"+ itemText + frag_content_WebView.extension;
                        navController.navigate(R.id.frag_content_webview);
                        break;
                    case "Videos inbuilt favoritos": //Abrir el video en el reproductor
                    case "Videos inbuilt con notas":
                        if(!Utils.isConnection(requireContext())){break;}
                        frag_listado.elementLoaded = "video_conf";
                        dbManager.open();
                            urlvideo.putString("urlvideo", dbManager.getDbInfoFromItem(itemText, DatabaseHelper.T_Videos));
                        dbManager.close();
                        navController.navigate(R.id.frag_listado, urlvideo);
                        break;
                    case "Videos offline favoritos": //abrir el video en el reproductor
                    case "Videos offline con notas":
                        frag_listado.elementLoaded = "play_video_repo";
                        frag_listado.urlPath = itemText;
                        navController.navigate(R.id.frag_listado);
                        break;

                    case "Audios offline favoritos":
                    case "Audios offline con notas":
                        frag_listado.elementLoaded = "audio_ext";
                        frag_listado.urlPath = itemText;
                        navController.navigate(R.id.frag_listado);
                        break;
                    case "Apuntes":
                        //Carga la info del apunte
                        UiModalWindows.ApunteManager(getContext(),itemText,null, true);

                        break;

                }

            }
        });


    }


    @Override
    public void onStart() {
        super.onStart();

        try {
            MainActivity.mainActivityThis.ic_toolsBar_frase_add.setVisibility(View.VISIBLE);
        }catch (Exception ignored){}

        try {
            MainActivity.mainActivityThis.ic_toolsBar_fav.setVisibility(View.GONE);
        }catch (Exception ignored){}

    }


    //Muestra/oculta la ayuda contextual:
    private void showHideAyudaContextual(){
        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("help_inline",true)){
            ayudaContextual.setVisibility(View.VISIBLE);
        }else{
            ayudaContextual.setVisibility(View.GONE);
        }

    }

}