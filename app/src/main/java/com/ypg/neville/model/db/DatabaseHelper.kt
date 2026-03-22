package com.ypg.neville.model.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_Frases) // Crear la tabla frases
        db.execSQL(CREATE_TABLE_Videos) // crear la tabla de Videos Internos
        db.execSQL(CREATE_TABLE_Repo) // crear la tabla de Videos Externos
        db.execSQL(CREATE_TABLE_Conf) // crear la tabla de Videos
        db.execSQL(CREATE_TABLE_Apuntes) // crear la tabla de notas
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_Frases")
        db.execSQL("DROP TABLE IF EXISTS $T_Videos")
        db.execSQL("DROP TABLE IF EXISTS $T_Repo")
        db.execSQL("DROP TABLE IF EXISTS $T_Conf")
        db.execSQL("DROP TABLE IF EXISTS $T_Apuntes")
        onCreate(db)
    }

    companion object {
        // Información de la BD:
        const val DB_NAME = "neville.db" // nombre de la base de datos
        const val DB_VERSION = 1

        // Tablas
        const val T_Frases = "frases" // Almacena las frases in-built y personales
        const val T_Videos = "videos" // Almacena los recursos de videos: internos
        const val T_Repo = "repo" // Almacena los recursos de videos: externos
        const val T_Conf = "conf" // Almacena los recursos de videos: internos y externos
        const val T_Apuntes = "notas" // Almacena las notas personales y asociadas a frases, conferencias, videos, etc

        // Campos Comunes
        const val CC_id = "id"
        const val CC_favorito = "fav"
        const val CC_nota = "nota"

        // Columnas: TABLE_Frases
        const val C_frases_frase = "frase" // texto de la frase
        const val C_frases_autor = "autor" // nombre del autor
        const val C_frases_fuente = "fuente" // text "la conferencia o audio del que se extrajo la frase...
        const val C_frases_in_built = "inbuild" // 0-1 Si es una frase incorporada por el desarrollador o por el usuario
        const val C_frases_shared = "shared" // int..>= 1: Las veces que se ha compartido la frase en la comunidad

        // Columnas: TABLE_Videos (lista de videos inbuilt)
        const val C_videos_title = "title" // text
        const val C_videos_link = "link" // text
        const val C_videos_type = "type" // text (indica la procedencia del video:conf,audio_libro,otro)
        const val C_videos_shared = "shared" // int increment int... Las veces que se ha compartido el video en la comunidad

        // Columnas: TABLE_Repo
        const val C_repo_title = "title" // text
        const val C_repo_link = "link" // text
        const val C_repo_type = "type" // text (indica la procedencia del video:conf,audio_libro,otro)
        const val C_repo_shared = "shared" // int increment int... Las veces que se ha compartido el video en la comunidad

        // Columnas: TABLE_Conf (lista de las conferencias inbuilt)
        const val C_conf_title = "title" // text Título de la conferencia
        const val C_conf_link = "link" // text Dirección del fichero en assets
        const val C_conf_shared = "shared" // int increment int... Las veces que se ha compartido el video en la comunidad

        // Columnas: Tabla notas(solamente para apuntes personales):
        const val C_apunte_title = "title" // titulo de la nota

        // Schema Table frases:
        const val CREATE_TABLE_Frases = ("create table " + T_Frases + "("
                + CC_id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + C_frases_frase + " TEXT NOT NULL UNIQUE, " // yor este campo no se repite
                + C_frases_autor + " TEXT, "
                + C_frases_fuente + " TEXT DEFAULT '', "
                + CC_favorito + " TEXT DEFAULT '0', "
                + CC_nota + " TEXT DEFAULT '', "
                + C_frases_in_built + " TEXT, "
                + C_frases_shared + " TEXT );")

        // Schema Table Videos:
        const val CREATE_TABLE_Videos = ("create table " + T_Videos + "("
                + CC_id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + C_videos_title + " TEXT NOT NULL UNIQUE, "
                + C_videos_link + " TEXT NOT NULL UNIQUE, "
                + C_videos_type + " TEXT NOT NULL, "
                + CC_favorito + " TEXT DEFAULT '0', "
                + CC_nota + " TEXT DEFAULT '', "
                + C_videos_shared + " TEXT DEFAULT '' );")

        // Schema Table Repo (videos y audios) offline:
        const val CREATE_TABLE_Repo = ("create table " + T_Repo + "("
                + CC_id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + C_repo_title + " TEXT NOT NULL UNIQUE, "
                + C_repo_link + " TEXT NOT NULL UNIQUE, "
                + C_repo_type + " TEXT NOT NULL, "
                + CC_favorito + " TEXT DEFAULT '0', "
                + CC_nota + " TEXT DEFAULT '', "
                + C_repo_shared + " TEXT DEFAULT '' );")

        // Schema Table Conf:
        const val CREATE_TABLE_Conf = ("create table " + T_Conf + "("
                + CC_id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + C_conf_title + " TEXT NOT NULL UNIQUE, "
                + C_conf_link + " TEXT NOT NULL UNIQUE, "
                + CC_favorito + " TEXT DEFAULT '0',"
                + CC_nota + " TEXT DEFAULT '',"
                + C_conf_shared + " TEXT DEFAULT '');")

        // Schema Table Apuntes:
        const val CREATE_TABLE_Apuntes = ("create table " + T_Apuntes + "("
                + CC_id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + C_apunte_title + " TEXT NOT NULL UNIQUE, "
                + CC_nota + " TEXT DEFAULT '');")
    }
}
