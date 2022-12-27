package com.ypg.neville.model.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Información de la BD:
    static final String DB_NAME = "neville.db";  //nombre de la base de datos
    static final int DB_VERSION = 1;
    private Context context;


    //Constructor
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;

    }

    // Tablas
    public static final String T_Frases         = "frases"; //Almacena las frases in-built y personales
    public static final String T_Videos         = "videos"; //Almacena los recursos de videos: internos
    public static final String T_Repo           = "repo"; //Almacena los recursos de videos: externos
    public static final String T_Conf           = "conf";   //Almacena los recursos de videos: internos y externos
    public static final String T_Apuntes        = "notas";  //Almacena las notas personales y asociadas a frases, conferencias, videos, etc


    //Campos Comunes
    public static final String CC_id = "id";
    public static final String CC_favorito = "fav";
    public static final String CC_nota = "nota";




    // Columnas: TABLE_Frases
       // public static final String C_frases_ID          = "id";         //index auntoinc
        public static final String C_frases_frase       = "frase";      //texto de la frase
        public static final String C_frases_autor       = "autor";      //nombre del autor
        public static final String C_frases_fuente      = "fuente";     //text "la conferencia o audio del que se extrajo la frase, también puede ser "propia"
        //public static final String C_frases_favorito    = "fav";        //0-1 Si se ha marcado como frase favorita
        //public static final String C_frases_nota        = "nota";       //text   nota asociada a la frase
        public static final String C_frases_in_built    = "inbuild";    //0-1 Si es una frase incorporada por el desarrollador o por el usuario
        public static final String C_frases_shared      = "shared";   //int..>= 1:  Las veces que se ha compartido la frase en la comunidad



    // Columnas: TABLE_Videos (lista de videos inbuilt)
        //public static final String C_videos_ID          = "id";         //index auntoinc
        public static final String C_videos_title       = "title";      //text
        public static final String C_videos_link        = "link";       //text
        public static final String C_videos_type        = "type";       //text (indica la procedencia del video:conf,audio_libro,otro)
        //public static final String C_videos_favorito    = "fav";        //int 0-1 Si se ha marcado como frase favorita
        //public static final String C_videos_nota        = "nota";        //text   nota asociada al video
        public static final String C_videos_shared      = "shared";   //int increment int... Las veces que se ha compartido el video en la comunidad

    // Columnas: TABLE_Videos (lista de videos inbuilt)
        //public static final String C_videos_ID          = "id";         //index auntoinc
        public static final String C_repo_title       = "title";      //text
        public static final String C_repo_link        = "link";       //text
        public static final String C_repo_type        = "type";       //text (indica la procedencia del video:conf,audio_libro,otro)
        //public static final String C_videos_favorito    = "fav";        //int 0-1 Si se ha marcado como frase favorita
        //public static final String C_videos_nota        = "nota";        //text   nota asociada al video
        public static final String C_repo_shared      = "shared";   //int increment int... Las veces que se ha compartido el video en la comunidad

    // Columnas: TABLE_Conf (lista de las conferencias inbuilt)
        //public static final String C_conf_ID          = "id";           //index auntoinc
        public static final String C_conf_title       = "title";        // text Título de la conferencia
        public static final String C_conf_link        = "link";         //text Dirección del fichero en assets
        //public static final String C_conf_favorito    = "fav";          //int 0-1 si es marcado como favorito
        //public static final String C_conf_nota        = "nota";         //int 0-1 si tiene una nota asociada
        public static final String C_conf_shared      = "shared";   //int increment int... Las veces que se ha compartido el video en la comunidad



    //Columnas: Tabla notas(solamente para apuntes personales):
        //public static String C_notas_ID                 = "id";         //id de la nota
        public static String C_apunte_title              = "title";      //titulo de la nota
        //public static String C_notas_nota               = "nota";       //Texto de la nota



    //Schema Table frases:
    public static final String CREATE_TABLE_Frases = "create table "  + T_Frases + "("
            + CC_id                 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + C_frases_frase        + " TEXT NOT NULL UNIQUE, " //yor este campo no se repite
            + C_frases_autor        + " TEXT, "
            + C_frases_fuente       + " TEXT DEFAULT '', "
            + CC_favorito           + " TEXT DEFAULT '0', "
            + CC_nota               + " TEXT DEFAULT '', "
            + C_frases_in_built     + " TEXT, "
            + C_frases_shared       + " TEXT );";



    //Schema Table Videos:
    public static final String CREATE_TABLE_Videos = "create table "  + T_Videos + "("
            + CC_id                 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + C_videos_title        + " TEXT NOT NULL UNIQUE, "
            + C_videos_link         + " TEXT NOT NULL UNIQUE, "
            + C_videos_type         + " TEXT NOT NULL, "
            + CC_favorito           + " TEXT DEFAULT '0', "
            + CC_nota               + " TEXT DEFAULT '', "
            + C_videos_shared       + " TEXT DEFAULT '' );";

    //Schema Table Repo (videos y audios) offline:
    public static final String CREATE_TABLE_Repo = "create table "  + T_Repo + "("
            + CC_id                 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + C_repo_title          + " TEXT NOT NULL UNIQUE, "
            + C_repo_link           + " TEXT NOT NULL UNIQUE, "
            + C_repo_type           + " TEXT NOT NULL, "
            + CC_favorito           + " TEXT DEFAULT '0', "
            + CC_nota               + " TEXT DEFAULT '', "
            + C_repo_shared         + " TEXT DEFAULT '' );";



    //Schema Table Conf:
    public static final String CREATE_TABLE_Conf= "create table "  + T_Conf + "("
            + CC_id                 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + C_conf_title          + " TEXT NOT NULL UNIQUE, "
            + C_conf_link           + " TEXT NOT NULL UNIQUE, "
            + CC_favorito           + " TEXT DEFAULT '0',"
            + CC_nota               + " TEXT DEFAULT '',"
            + C_conf_shared         + " TEXT DEFAULT '');";




    //Schema Table Apuntes:
    public static final String CREATE_TABLE_Apuntes = "create table "  + T_Apuntes + "("
            + CC_id                 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + C_apunte_title         + " TEXT NOT NULL UNIQUE, "
            + CC_nota               + " TEXT DEFAULT '');";


//____________________________________________________________________________________________________________________________






    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_Frases);            //Crear la tabla frases
        db.execSQL(CREATE_TABLE_Videos);            //crear la tabla de Videos Internos
        db.execSQL(CREATE_TABLE_Repo);         //crear la tabla de Videos Externos
        db.execSQL(CREATE_TABLE_Conf);              //crear la tabla de Videos
        db.execSQL(CREATE_TABLE_Apuntes);             //crear la tabla de notas

    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + T_Frases);
        db.execSQL("DROP TABLE IF EXISTS " + T_Videos);
        db.execSQL("DROP TABLE IF EXISTS " + T_Repo);
        db.execSQL("DROP TABLE IF EXISTS " + T_Conf);
        db.execSQL("DROP TABLE IF EXISTS " + T_Apuntes);

        onCreate(db);


    }
}
