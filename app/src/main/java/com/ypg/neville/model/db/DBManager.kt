package com.ypg.neville.model.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import java.util.LinkedList

/**
 * Manejo de las Operaciones CRUD (Create, Read, Update, Delete)
 */
class DBManager(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    private var database: SQLiteDatabase? = null

    @Throws(SQLException::class)
    fun open(): DBManager {
        dbHelper = DatabaseHelper(context)
        database = dbHelper!!.writableDatabase
        return this
    }

    fun close() {
        dbHelper?.close()
    }

    //================================================================================ Operaciones CRUD

    /**
     * Crear una tabla
     * @param createTableString sqlite command
     */
    fun CreateTable(createTableString: String) {
        database?.execSQL(createTableString)
    }

    /**
     * Eliminar una tabla
     * @param tableName Nombre de la tabla
     */
    fun DeleteTable(tableName: String) {
        database?.execSQL("DROP TABLE IF EXISTS $tableName")
    }

    /**
     * Inserta un nuevo registro a la BD
     * @param tableName nombre de la tabla
     * @param contentValues conjunto de valores a añadir
     * @return devuelve el ID de la fila adicionada, -1 de lo contrario
     */
    fun insert(tableName: String, contentValues: ContentValues): Long {
        return database?.insert(tableName, null, contentValues) ?: -1L
    }

    /**
     * Actualizar un registro de la tabla
     * @param tableName Nombre de la tabla
     * @param ColumnId columna ID para filtro
     * @param id Valor ID para el filtro
     * @param contentValues Cojunto de valores a actualizar
     */
    fun update_ForIdInt(tableName: String, ColumnId: String, id: Long, contentValues: ContentValues) {
        database?.update(tableName, contentValues, "$ColumnId=$id", null)
    }

    /**
     * Actualizar registro (Para campos cuyo ID es un String)
     * @param tableName Nombre de la tabla
     * @param ColumnId columna ID para filtro
     * @param id Valor ID para el filtro
     * @param contentValues Cojunto de valores a actualizar
     */
    fun update_ForIdStr(tableName: String, ColumnId: String, id: String, contentValues: ContentValues) {
        database?.update(tableName, contentValues, "$ColumnId='$id';", null)
    }

    /**
     * Eliminar un registro
     * @param tableName Nombre de la tabla
     * @param ColumnId Columna ID para filtro
     * @param id Valor ID para filtro
     */
    fun delete(tableName: String, ColumnId: String, id: Long) {
        database?.delete(tableName, "$ColumnId=$id", null)
    }

    /**
     * borrar registro (versión para campos id de tipo String)
     * @param tableName Nombre de la tabla
     * @param ColumnId Columna ID para filtro
     * @param id Valor ID para filtro
     */
    fun delete_ForIdStr(tableName: String, ColumnId: String, id: String) {
        database?.delete(tableName, "$ColumnId='$id';", null)
    }

    /**
     * Obtiene datos de la tabla frases:
     * @param tableName Nombre de la tabla
     * @param columns Listado de columnas a obtener
     * @param whereColumna Columna para filtro
     * @param whereValor Valor para filtro
     * @return
     */
    fun fetch(tableName: String, columns: Array<String>, whereColumna: String, whereValor: Array<String>): Cursor? {
        return database?.query(tableName, columns, whereColumna, whereValor, null, null, null)
    }

    /**
     * Ejecutar una operación (que no es una consulta) con un comando SQL
     * @param sql_command sqlite command
     */
    fun ejectSQLCommand(sql_command: String) {
        database?.execSQL(sql_command)
    }

    /**
     * Ejecutar una consulta raw con comando sql
     * @param SQL_command sqlite command
     * @return retorna un cursor
     */
    fun ejectSQLRawQuery(SQL_command: String): Cursor {
        return database?.rawQuery(SQL_command, null) ?: throw SQLException("Database not opened")
    }

    //####################################################   Ejecutar operaciones especificas

    /**
     * Consultas específicas a la BD. Devuelve un objeto Cursor
     * @param filtro sqlite command
     * @return Devuelve un cursor
     */
    fun getListado(filtro: String): Cursor {
        var sqlite = ""

        when (filtro) {
            "Todas las frases" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases};"
            "Frases inbuilt" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.C_frases_in_built}=1 ;"
            "Frases favoritas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.CC_favorito}=1;"
            "Frases inbuilt favoritas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.CC_favorito}=1 AND ${DatabaseHelper.C_frases_in_built}=1;"
            "Frases inbuilt con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE TRIM(${DatabaseHelper.CC_nota},'') !='' AND ${DatabaseHelper.C_frases_in_built}='1';"
            "Frases personales" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.C_frases_in_built}=0 ;"
            "Frases personales favoritas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.CC_favorito}=1 AND ${DatabaseHelper.C_frases_in_built}=0;"
            "Frases personales con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.C_frases_in_built}=0 AND TRIM(${DatabaseHelper.CC_nota},'') !='';"
            "Todas las conf" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Conf};"
            "Conferencias favoritas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Conf} WHERE ${DatabaseHelper.CC_favorito}=1;"
            "Conferencias con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Conf} WHERE TRIM(${DatabaseHelper.CC_nota},'') !='' ;"
            "Videos inbuilt favoritos" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Videos} WHERE ${DatabaseHelper.CC_favorito}=1;"
            "Videos inbuilt con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Videos} WHERE TRIM(${DatabaseHelper.CC_nota},'') !='' ;"
            "Videos offline favoritos" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Repo} WHERE ${DatabaseHelper.CC_favorito}='1' AND ${DatabaseHelper.C_repo_type}='video';"
            "Videos offline con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Repo} WHERE TRIM(${DatabaseHelper.CC_nota},'') !='' AND ${DatabaseHelper.C_repo_type}='video';"
            "Audios offline favoritos" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Repo} WHERE ${DatabaseHelper.CC_favorito}='1' AND ${DatabaseHelper.C_repo_type}='audio';"
            "Audios offline con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Repo} WHERE TRIM(${DatabaseHelper.CC_nota},'') !=''AND ${DatabaseHelper.C_repo_type}='audio';"
            "Apuntes" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Apuntes};"
            "Videos gregg favoritos" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Videos} WHERE ${DatabaseHelper.CC_favorito}='1' AND ${DatabaseHelper.C_repo_type}='gregg';"
            "Videos gregg con notas" -> sqlite = "SELECT * FROM ${DatabaseHelper.T_Videos} WHERE TRIM(${DatabaseHelper.CC_nota},'') !='' AND ${DatabaseHelper.C_repo_type}='gregg';"
        }

        return database?.rawQuery(sqlite, null) ?: throw SQLException("Database not opened")
    }

    /**
     * Obtiene el link de un video a partir de su título (el titulo debe ser ser campo único)
     * @param ptitle título del video
     * @param TableName Nombre de la tabla que contiene el elemento
     * @return devuelve el link del medio
     */
    fun getDbInfoFromItem(ptitle: String, TableName: String): String {
        var result = ""
        val cursor = database?.rawQuery("SELECT link FROM $TableName WHERE title='$ptitle';", null)
        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getString(0)
            cursor.close()
        }
        return result
    }
}
