package com.ypg.neville.model.utils.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;

import com.ypg.neville.R;
import com.ypg.neville.Ui.frag.frag_list_info;
import com.ypg.neville.model.db.DBManager;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.utils.QRManager;
import com.ypg.neville.model.utils.UiModalWindows;
import com.ypg.neville.model.utils.utilsFields;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


//Adaptador para list en frag_listInfo
public class MyListAdapterList_info extends ArrayAdapter<String> {


    private final int layout;
    List<String>  listado = new LinkedList<>();


    public MyListAdapterList_info(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.layout = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MyListAdapterList_info.ViewHolder mainViewholder = null;

        if(convertView == null){
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent, false);
            MyListAdapterList_info.ViewHolder viewHolder = new MyListAdapterList_info.ViewHolder(); //Clase que contendrá los elementos de cada file del listview
            viewHolder.text = convertView.findViewById(R.id.row_list_info_text_conf);
            viewHolder.textAutor = convertView.findViewById(R.id.row_list_info_text_autor);
            viewHolder.ic_del = convertView.findViewById(R.id.row_list_info_ic_del);
            viewHolder.ic_share = convertView.findViewById(R.id.row_list_info_ic_shared);

            viewHolder.text.setText(getItem(position));




            viewHolder.ic_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String spinnerText = frag_list_info.spinnerStatic.getSelectedItem().toString();

                    if (spinnerText.contains("Frases")){
                        String texto = "f&&"+viewHolder.text.getText().toString()+"&&";
                        QRManager.ShowQRDialog(getContext(),texto,"Compartir Frase", null);

                    }else if(spinnerText.contains("Apuntes")){
                        //Mostrarel cuadro de diálogo de Apuntes
                        UiModalWindows.ApunteManager(getContext(),viewHolder.text.getText().toString(),null, true);
                    }


                }
            });


            //Eliminar un item de la lista
            viewHolder.ic_del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (    //Listado de elementos que no se pueden eliminar de la BD
                            Objects.equals(utilsFields.spinnerListInfoItemSelected, "Frases inbuilt") ||
                            Objects.equals(utilsFields.spinnerListInfoItemSelected, "Frases inbuilt favoritas") ||
                            Objects.equals(utilsFields.spinnerListInfoItemSelected, "Frases inbuilt con notas") ||
                             Objects.equals(utilsFields.spinnerListInfoItemSelected, "Conferencias favoritas") ||
                            Objects.equals(utilsFields.spinnerListInfoItemSelected, "Conferencias con notas") ||
                            Objects.equals(utilsFields.spinnerListInfoItemSelected, "Videos inbuilt favoritos") ||
                            Objects.equals(utilsFields.spinnerListInfoItemSelected, "Videos inbuilt con notas") ){

                        return;
                    }

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("¿Eliminando elemento en: " + utilsFields.spinnerListInfoItemSelected +"?" );
                            builder.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    DBManager dbManager = new DBManager(getContext());
                                    Cursor cursor;
                                    dbManager.open();

                                    listado.clear();


                                    switch (utilsFields.spinnerListInfoItemSelected){
                                        case "Frases personales":
                                        case "Frases favoritas personales":
                                        case "Frases personales con notas":
                                            dbManager.delete_ForIdStr(DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, viewHolder.text.getText().toString());
                                            break;
                                        case "Apuntes":
                                            dbManager.delete_ForIdStr(DatabaseHelper.T_Apuntes, DatabaseHelper.C_apunte_title, viewHolder.text.getText().toString());
                                            break;
                                    }

                                    //Actualizando el listado
                                     cursor =  dbManager.getListado(utilsFields.spinnerListInfoItemSelected); //Realiza la consulta
                                    if(cursor.moveToFirst()){
                                        do {
                                          listado.add(cursor.getString(1)); //yor aqui esta el error
                                        } while (cursor.moveToNext());
                                    }

                                    clear(); //Limpia el ArrayAdapter
                                    addAll(listado); //Adiciona al ArrayAdapter el listado
                                    notifyDataSetChanged(); //Notifica al ArrayAdapter de los cambios

                                    dbManager.close();

                                }
                            });

                    builder.show();


                }
            });




            convertView.setTag(viewHolder);

        }else{
            mainViewholder = (MyListAdapterList_info.ViewHolder) convertView.getTag();
            mainViewholder.text.setText(getItem(position));


        }

        return convertView;
    }


    //Clase utilitaria que mantendrá los elementos clickeable de cada fila
    private class ViewHolder {
        TextView text, textAutor;
        AppCompatImageView ic_del;
        AppCompatImageView ic_share;

    }



}
