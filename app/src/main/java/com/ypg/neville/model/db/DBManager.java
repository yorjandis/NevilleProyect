package com.ypg.neville.model.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedList;
import java.util.List;

/**
 * Manejo de las Operaciones CRUD (Create, Read, Update, Delete)
 */
public class DBManager {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Context context;

    //constructor:
    public DBManager(Context pContext) {
        this.context = pContext;
    }



    public DBManager open() throws SQLException{

        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }


    public void close(){
        dbHelper.close();
    }


    //================================================================================ Operaciones CRUD


    /**
     * Crear una tabla
     * @param createTableString sqlite command
     */
    public void CreateTable(String createTableString){
        database.execSQL(createTableString);
    }


    /**
     * Eliminar una tabla
     * @param tableName Nombre de la tabla
     */
    public void DeleteTable(String tableName ){
    database.execSQL("DROP TABLE IF EXISTS " + tableName);
}


    /**
     * Inserta un nuevo registro a la BD
     * @param tableName nombre de la tabla
     * @param contentValues conjunto de valores a añadir
     * @return devuelve el ID de la fila adicionada, -1 de lo contrario
     */
    public long insert(String tableName, ContentValues contentValues){
       return  database.insert(tableName,null,contentValues);
    }


    /**
     * Actualizar un registro de la tabla
     * @param tableName Nombre de la tabla
     * @param ColumnId columna ID para filtro
     * @param id Valor ID para el filtro
     * @param contentValues Cojunto de valores a actualizar
     */
    public void update_ForIdInt(String tableName, String ColumnId, long id, ContentValues contentValues){
        database.update(tableName, contentValues,ColumnId + "=" + id, null);
    }

    /**
     * Actualizar registro (Para campos cuyo ID es un String)
     * @param tableName Nombre de la tabla
     * @param ColumnId columna ID para filtro
     * @param id Valor ID para el filtro
     * @param contentValues Cojunto de valores a actualizar
     */
    public void update_ForIdStr(String tableName, String ColumnId, String id, ContentValues contentValues){
        database.update(tableName, contentValues,ColumnId + "='" + id +"';", null);
    }


    /**
     * Eliminar un registro
     * @param tableName Nombre de la tabla
     * @param ColumnId Columna ID para filtro
     * @param id Valor ID para filtro
     */
    public void delete(String tableName, String ColumnId, long id){
        database.delete(tableName, ColumnId + "=" + id,null);
    }


    /**
     *borrar registro (versión para campos id de tipo String)
     * @param tableName Nombre de la tabla
     * @param ColumnId Columna ID para filtro
     * @param id Valor ID para filtro
     */
    public void delete_ForIdStr(String tableName, String ColumnId, String id){
        database.delete(tableName, ColumnId + "='" + id+"';",null);
    }



    /**
     * Obtiene datos de la tabla frases:
     * @param tableName Nombre de la tabla
     * @param columns Listado de columnas a obtener
     * @param whereColumna Columna para  filtro
     * @param whereValor Valor para filtro
     * @return
     */
    public Cursor fetch(String tableName, String[] columns, String whereColumna, String[] whereValor){
     Cursor cursor = database.query(tableName, columns, whereColumna, whereValor, null, null, null);
     return cursor;
    }


    /**
     * Ejecutar una operación (que no es una consulta) con un comando SQL
     * @param sql_command sqlite command
     */
    public void ejectSQLCommand(String sql_command){
        database.execSQL(sql_command);
    }

    //

    /**
     * Ejecutar una consulta raw con comando sql
     * @param SQL_command sqlite command
     * @return retorna un cursor
     */
    public  Cursor ejectSQLRawQuery(String SQL_command){
      return database.rawQuery(SQL_command, null);
    }





    //####################################################   Ejecutar operaciones especificas


    /**
     * Consultas específicas a la BD. Devuelve un objeto Cursor
     * @param filtro sqlite command
     * @return Devuelve un cursor
     */
    public Cursor getListado(String filtro){

        List<String> listado = new LinkedList<>();
        String sqlite = "";
        Cursor cursor;

        switch (filtro){
            //Para Frases:
            case "Todas las frases": //listar todas las Fases
                sqlite = "SELECT * FROM "+DatabaseHelper.T_Frases+";";
                break;
            case  "Frases inbuilt"://listar solo las frases incorporadas
                sqlite = "SELECT * FROM " + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.C_frases_in_built + "=1 ;";
                break;
            case "Frases favoritas"://listar todas las frases favoritas (incorporadas + personales)
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.CC_favorito + "=1;";
                break;
            case  "Frases inbuilt favoritas": //listar solo las frases favoritas incorporadas
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.CC_favorito + "=1 AND "  + DatabaseHelper.C_frases_in_built + "=1;" ;
                break;
            case "Frases inbuilt con notas":
                sqlite = "SELECT * FROM " + DatabaseHelper.T_Frases + " WHERE TRIM(" + DatabaseHelper.CC_nota + ",'') !='' AND " + DatabaseHelper.C_frases_in_built+"='1';" ;
                break;
            case  "Frases personales":
                sqlite = "SELECT * FROM " + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.C_frases_in_built + "=0 ;";
                break;
            case "Frases personales favoritas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.CC_favorito + "=1 AND "  + DatabaseHelper.C_frases_in_built + "=0;" ;
                break;
            case "Frases personales con notas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Frases + " WHERE " + DatabaseHelper.C_frases_in_built + "=0 AND TRIM(" + DatabaseHelper.CC_nota + ",'') !='';";
                break;
            //Para conferencias
            case "Todas las conf":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Conf+";";
                break;
            case "Conferencias favoritas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Conf + " WHERE " + DatabaseHelper.CC_favorito + "=1;";
                break;
            case "Conferencias con notas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Conf + " WHERE TRIM(" + DatabaseHelper.CC_nota + ",'') !='' ;";
                break;
            //Para videos inbuilt
            case "Videos inbuilt favoritos":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Videos + " WHERE " + DatabaseHelper.CC_favorito + "=1;" ;
                break;
            case "Videos inbuilt con notas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Videos + " WHERE TRIM(" + DatabaseHelper.CC_nota + ",'') !='';";
                break;

            //Para videos off line
            case "Videos offline favoritos":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Repo + " WHERE " + DatabaseHelper.CC_favorito + "='1' AND "+  DatabaseHelper.C_repo_type +"='video';";
                break;
            case "Videos offline con notas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Repo + " WHERE TRIM(" + DatabaseHelper.CC_nota + ",'') !='' AND "+  DatabaseHelper.C_repo_type +"='video';";
                break;
            //Para audios off line
            case "Audios offline favoritos":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Repo + " WHERE " + DatabaseHelper.CC_favorito + "='1' AND "+  DatabaseHelper.C_repo_type +"='audio';";
                break;
            case "Audios offline con notas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Repo + " WHERE TRIM(" + DatabaseHelper.CC_nota + ",'') !=''AND "+  DatabaseHelper.C_repo_type +"='audio';";
                break;

            //Para Apuntes:
            case "Apuntes":
                sqlite = "SELECT * FROM " + DatabaseHelper.T_Apuntes + ";";
                break;

            //videos greeg
            case "Videos gregg favoritos":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Videos + " WHERE " + DatabaseHelper.CC_favorito + "='1' AND "+  DatabaseHelper.C_repo_type +"='gregg';";
                break;
            case "Videos gregg con notas":
                sqlite = "SELECT * FROM "  + DatabaseHelper.T_Videos + " WHERE TRIM(" + DatabaseHelper.CC_nota + ",'') !='' AND "+  DatabaseHelper.C_repo_type +"='gregg';";
                break;
        }

            cursor =  database.rawQuery(sqlite,null);

        return cursor;
    }


    /**
     Obtiene el link de un video a partir de su título (el titulo debe ser ser campo único)
     @param  ptitle título del video
     @param TableName Nombre de la tabla que contiene el elemento
     @return devuelve el link del medio
    */
    public  String getDbInfoFromItem(String ptitle, String TableName){
        String result = "";
        Cursor cursor;

        cursor = database.rawQuery("SELECT link FROM " + TableName +" WHERE title='" + ptitle + "';", null);
        if (cursor.moveToFirst()){
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }





}
