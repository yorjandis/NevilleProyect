package com.ypg.neville.model.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.preference.PreferenceManager;

import com.ypg.neville.R;
import com.ypg.neville.model.db.DBManager;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.db.utilsDB;

import org.jetbrains.annotations.Nullable;

//Esta clase se encarga de mostrar ventanas modales para las operaciones CRUD sobre la BD, además de otras ventanas modales.
public class UiModalWindows {

   static String title_tempp; //Variable de apoyo: almacena el titulo del apunte

    /**
     * Diálogo para adicionar una nueva frase
     * @param pcontext
     * @param contentValues  Conjunto de valores a cargar en los campos
     */
    public static void Add_New_frase(Context pcontext, @Nullable ContentValues contentValues){

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(pcontext, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert);
        builder.setTitle("Adicionar una nueva frase");
        builder.setMessage("Adicione sus propias frases a la biblioteca");
        builder.setIcon(R.drawable.neville);
        builder.setCancelable(false);

        View view = LayoutInflater.from(pcontext).inflate(R.layout.modal_add_frase,null);

        EditText editFrase = (EditText) view.findViewById(R.id.mod_add_frase_edit_frase);
        EditText editAutor = (EditText) view.findViewById(R.id.mod_add_frase_edit_autor);
        EditText editFuente = (EditText) view.findViewById(R.id.mod_add_frase_edit_fuente);
        Button btn_save = (Button) view.findViewById(R.id.mod_add_frase_btn_save);
        Button btn_cancel = (Button) view.findViewById(R.id.mod_add_frase_btn_salir);
        AppCompatImageView img_shared = view.findViewById(R.id.mod_add_frase_shared);



        builder.setView(view);
        AlertDialog alertDialog = builder.create();
       // alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_modal_windows);


        //Si se ha dado parametro contentValues: rellenar los campos:
        if (contentValues != null){
            editFrase.setText(contentValues.getAsString("frase"));
            editAutor.setText(contentValues.getAsString("autor"));
            editFuente.setText(contentValues.getAsString("fuente"));
        }



        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        //Adiciona una nueva frase a la Bd
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                
                //Chequeando valores
                if (!editFrase.getText().toString().trim().isEmpty()){

                   long res =  utilsDB.insertNewFrase(pcontext,editFrase.getText().toString(),
                                                        editAutor.getText().toString(),
                                                        editFuente.getText().toString(),
                                                        "0");
                       if (res < 0 ){
                           Toast.makeText(pcontext, "Error al adicionar la frases", Toast.LENGTH_SHORT).show();
                       }else{
                           Toast.makeText(pcontext, "Frase adicionada con éxito", Toast.LENGTH_SHORT).show();
                       }


                }else{
                    Toast.makeText(pcontext, "Debe establecer el texto de la frase", Toast.LENGTH_SHORT).show();
                }
                //Limpiando los campos
                editFrase.setText("");
                editAutor.setText("");
                editFuente.setText("");
                editFrase.requestFocus();

            }
        });

        img_shared.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!editFrase.getText().toString().trim().isEmpty()){

                    QRManager.ShowQRDialog(pcontext, "f&&" +
                            editFrase.getText().toString() + "&&"  +
                            editAutor.getText().toString() + "&&"  +
                            editFuente.getText().toString(),"Compartir Frase",
                            "Puede utilizar el lector QR para importar frases");
                }else{
                    Toast.makeText(pcontext, "Debe establecer el texto de la frase", Toast.LENGTH_SHORT).show();
                }


            }
        });

        editFrase.requestFocus();


        alertDialog.show();

        InputMethodManager imm = (InputMethodManager)pcontext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
    }


    /**
     * Muestra, guarda, actualiza Apuntes
     * @param context
     * @param titleInDB Si es dado: carga la info del apunte desde la BD
     * @param contentValues Si es dado: Carga los valores contenidos en el contentValue
     * @param isUpdate Si es true, actualiza la info del apunte (solo el apunte no el titulo)
     */
    public static void ApunteManager(Context context,String titleInDB,  @Nullable ContentValues contentValues, boolean isUpdate){

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert);
        builder.setTitle("Apuntes Personales");


        View view = LayoutInflater.from(context).inflate(R.layout.modal_add_notas,null);
        EditText edit_titulo = view.findViewById(R.id.modal_add_nota_edit_title);
        EditText edit_nota =  view.findViewById(R.id.modal_add_nota_edit_nota);
        Button btn_save =  view.findViewById(R.id.modal_add_nota_edit_btnguardar);
        Button btn_cancel =  view.findViewById(R.id.modal_add_nota_edit_btnsalir);
        ImageView img_shared = view.findViewById(R.id.modal_add_nota_shared);

        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        //Cargando un apunte previamente guardado en BD
        if(!titleInDB.isEmpty()){
            //Consultando la BD
            String query = "SELECT * FROM " + DatabaseHelper.T_Apuntes + " WHERE " + DatabaseHelper.C_apunte_title +" = '"+titleInDB+"';";
            DBManager dbManager = new DBManager(context).open();
            Cursor cursor;
            cursor = dbManager.ejectSQLRawQuery(query);
            if(cursor.moveToFirst()){

                edit_titulo.setText(cursor.getString(1));
                edit_nota.setText(cursor.getString(2));
                cursor.close();
            }
            dbManager.close();
        }

        //Cargando un la info de un apunte dado en contentValues:
        if(contentValues != null){
            edit_titulo.setText(contentValues.getAsString("title"));
            edit_nota.setText(contentValues.getAsString("apunte"));
        }

        //Deshabilitando el campo Title si se trata de un update
        if(isUpdate){
            edit_titulo.setEnabled(false);
        }



            //botón save
            btn_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //chequeo de campos
                    if(edit_titulo.getText().toString().trim().isEmpty() || edit_nota.getText().toString().trim().isEmpty()){
                        Toast.makeText(context, "Debe colocar un texto y un título", Toast.LENGTH_LONG).show();

                    }else{

                        //verificando si es un update o un insert
                        if (isUpdate){

                            if(utilsDB.updateApunte(context,edit_titulo.getText().toString().trim(),edit_nota.getText().toString().trim())){
                                Toast.makeText(context, "El apunte ha sido actualizado", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(context, "Error al adicionar el apunte", Toast.LENGTH_SHORT).show();
                            }

                        }else{
                            long res =  utilsDB.insertNewApunte(context,    edit_titulo.getText().toString(),
                                    edit_nota.getText().toString());
                            if (res < 0 ){
                                Toast.makeText(context, "Error al adicionar el apunte", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(context, "El apunte fue adicionado!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }


    /**
     * Muestra/modifica el contenidod de una nota
     * @param context contexto de trabajp
     * @param nota  Texto de la nota
     * @param tableName Nombre de la tabla
     * @param clumn_id Columna ID para filtro
     * @param valor_id  Valor ID para el filtro
     */
    public static void NotaManager(Context context,String nota, String tableName, String clumn_id,String valor_id){

        AlertDialog.Builder builder = new AlertDialog.Builder(context, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert);
        builder.setTitle("Nota asociada");

        View view = LayoutInflater.from(context).inflate(R.layout.modal_add_notas, null);
        view.findViewById(R.id.modal_add_nota_edit_title).setVisibility(View.GONE);
        EditText edi_nota = view.findViewById(R.id.modal_add_nota_edit_nota);
        Button btn_calcel = view.findViewById(R.id.modal_add_nota_edit_btnsalir);
        Button btn_save = view.findViewById(R.id.modal_add_nota_edit_btnguardar);

        builder.setView(view);

        edi_nota.setText(nota);


        AlertDialog alert = builder.create();
        alert.show();

        btn_calcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Actualizando el contenido del campo
               if (utilsDB.updateNota(context,tableName,clumn_id,valor_id,edi_nota.getText().toString().trim())){
                   Toast.makeText(context, "La nota fué actualizada",Toast.LENGTH_LONG).show();
                }else{
                   Toast.makeText(context, "Error al actualizar la nota",Toast.LENGTH_LONG).show();
               }
            }
        });






    }






    //Ventana de Ayuda contextual (Tambien muestra las novedades de la app)
    public static void showAyudaContectual(Context pcontext, String ptitle, String pMessage, String pContenido, boolean showbotonocultarestaayuda, @Nullable Drawable ico){

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(pcontext, R.style.Dialog);
        builder.setTitle(ptitle);
        builder.setMessage(pMessage);
        if (ico != null){
            builder.setIcon(ico);
        }else{
            builder.setIcon(R.drawable.ic_help);
        }

        builder.setCancelable(true);

        View view = LayoutInflater.from(pcontext).inflate(R.layout.layout_ayuda,null);

        TextView text = (TextView) view.findViewById(R.id.layout_ayuda_text);
        Button btn = (Button) view.findViewById(R.id.layout_ayuda_btn_hide);
        Button btn_cerrar = (Button) view.findViewById(R.id.layout_ayuda_btn_cerrar);

        text.setText(pContenido);

        if (!showbotonocultarestaayuda){
            btn.setVisibility(View.INVISIBLE);
        }


        builder.setView(view);
        AlertDialog alertDialog = builder.create();


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(pcontext).edit().putBoolean("help_inline",false).apply();
                alertDialog.dismiss();
            }
        });

        btn_cerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });


alertDialog.show();



    }

}
