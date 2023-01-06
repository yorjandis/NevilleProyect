package com.ypg.neville.model.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.utils.GetFromRepo;
import com.ypg.neville.model.utils.utilsFields;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


//Clase que se encarga de realizar operaciones específicas y de ayuda a la BD
public class utilsDB {

//RESTAURANDO EL CONTENIDO DE LAS BD :::::::::::::::

    /**
     * Pasa las frases (inbuilt) de xml  a la tabla frases de sqlite
     * @param context
     */
    public  static void yor_populateFraseTable(Context context){

        DBManager dbManager = new DBManager(context).open();

        dbManager.DeleteTable(DatabaseHelper.T_Frases);
        dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Frases);

        String [] listFrases = GetFromRepo.getFrasesFromXML(context); //Fuente de datos
        String [] temp;


        for (int i = 0 ; i < listFrases.length; ++i){

            temp = listFrases[i].split("::");
            utilsDB.insertNewFrase(context,temp[1],temp[0],"","1");
        }

        dbManager.close();
    }


    /**
     * Pasa la información de los videos de xml a la tabla videos en sqlite
     * @param context
     */
    public static  void yor_populateVideosTable(Context context ){

        DBManager dbManager = new DBManager(context);
        dbManager.open();

        dbManager.DeleteTable(DatabaseHelper.T_Videos);
        dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Videos);

        String[] temp;
        String[] arrayVideoYoutube = GetFromRepo.getUrlsVideosFromXML(context); //fuente de datos

        ContentValues contentValues = new ContentValues();

        for (int i = 0 ; i < arrayVideoYoutube.length; ++i){

            temp = arrayVideoYoutube[i].split("::");

            contentValues.put(DatabaseHelper.C_videos_link,temp[0]);
            contentValues.put(DatabaseHelper.C_videos_title,temp[1]);
            contentValues.put(DatabaseHelper.C_videos_type,"conf");
            contentValues.put(DatabaseHelper.CC_favorito,"0");
            contentValues.put(DatabaseHelper.CC_nota,"");
            contentValues.put(DatabaseHelper.C_videos_shared,"0");

            dbManager.insert(DatabaseHelper.T_Videos, contentValues);
            contentValues.clear();
        }

        String[] arrayAudioLibrosYoutube = context.getResources().getStringArray(R.array.list_audiolibros); //lista de audiolibros en youtube

        for (int i = 0 ; i < arrayAudioLibrosYoutube.length; ++i){

            temp = arrayAudioLibrosYoutube[i].split("::");

            contentValues.put(DatabaseHelper.C_videos_link,temp[0]);
            contentValues.put(DatabaseHelper.C_videos_title,temp[1]);
            contentValues.put(DatabaseHelper.C_videos_type,"audioLibro");
            contentValues.put(DatabaseHelper.CC_favorito,"0");
            contentValues.put(DatabaseHelper.CC_nota,"");
            contentValues.put(DatabaseHelper.C_videos_shared,"0");

            dbManager.insert(DatabaseHelper.T_Videos, contentValues);
            contentValues.clear();

        }

        String[] arrayVideosGreggYoutube = context.getResources().getStringArray(R.array.list_gregg_videos); //lista de videos de greeg en youtube

        for (int i = 0 ; i < arrayVideosGreggYoutube.length; ++i){

            temp = arrayVideosGreggYoutube[i].split("::");

            contentValues.put(DatabaseHelper.C_videos_link,temp[0]);
            contentValues.put(DatabaseHelper.C_videos_title,temp[1]);
            contentValues.put(DatabaseHelper.C_videos_type,"gregg");
            contentValues.put(DatabaseHelper.CC_favorito,"0");
            contentValues.put(DatabaseHelper.CC_nota,"");
            contentValues.put(DatabaseHelper.C_videos_shared,"0");

            dbManager.insert(DatabaseHelper.T_Videos, contentValues);
            contentValues.clear();
        }

    }


    /**
     * Pasa el contenido de las conferencias inbuilt a la tabla conf
     * @param context
     * @throws IOException
     */
    public static void yor_populateConfTable(Context context) throws IOException {

        DBManager dbManager = new DBManager(context).open();

        dbManager.DeleteTable(DatabaseHelper.T_Conf);
        dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Conf);

        ContentValues contentValues = new ContentValues();

        String[] fileList = GetFromRepo.getConfListFromAssets(context);


        for (int i = 0; i < fileList.length; ++i){
            contentValues.put(DatabaseHelper.C_conf_title, fileList[i].replace(".txt",""));
            contentValues.put(DatabaseHelper.C_conf_link,fileList[i]);
            contentValues.put(DatabaseHelper.CC_favorito, "0");
            contentValues.put(DatabaseHelper.CC_nota, "");
            contentValues.put(DatabaseHelper.C_conf_shared, "0");

            dbManager.insert(DatabaseHelper.T_Conf, contentValues);
        }

        dbManager.close();

    }



    /**
     * Restaurar automaticamente la información de las tablas si no existe
     * @param context
     * @return
     */
    public static boolean RestoreDBInfo(Context context){
        boolean result = false;
        DBManager dbManager = new DBManager(context).open();
        Cursor cursor;

        //Determinando si las tablas contienen datos:
        cursor = dbManager.ejectSQLRawQuery("SELECT * FROM " + DatabaseHelper.T_Frases + ";");
        if (cursor.getCount() == 0){
            yor_populateFraseTable(context); //tabla de frases
            cursor.close();
            result = true;
        }

        cursor = dbManager.ejectSQLRawQuery("SELECT * FROM " + DatabaseHelper.T_Conf + ";");
        if (cursor.getCount() == 0){
            try {
                yor_populateConfTable(context); //Tabla de conferencias
            } catch (IOException e) {
                e.printStackTrace();
            }
            cursor.close();
            result = true;
        }
        cursor = dbManager.ejectSQLRawQuery("SELECT * FROM " + DatabaseHelper.T_Videos + ";");
        if (cursor.getCount() == 0){
            yor_populateVideosTable(context); //Tabla de videos
            cursor.close();
            result = true;
        }

        dbManager.close();

        return  result;

    }


    /**
     * Actualiza el contenido de las tablas audios_ext, videos_ext a partir del almacenamiento externo. lo hace en un hilo separado
     * @param context
     */
    public static void popularDB_Repo(Context context) {

        AtomicInteger countAudios = new AtomicInteger();
        AtomicInteger countVideos = new AtomicInteger();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler  handler = new Handler(context.getMainLooper()); //handle al UI thread
        executor.execute(() -> {

            DBManager dbManager = new DBManager(context).open();
            ContentValues contentValues = new ContentValues();

            //Purgando el contenido:
            dbManager.DeleteTable(DatabaseHelper.T_Repo);
            dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Repo);

            String Dir_path_videos = Environment.getExternalStorageDirectory().toString()+ File.separator + utilsFields.REPO_DIR_ROOT + File.separator + utilsFields.REPO_DIR_VIDEOS;
            String Dir_path_audios = Environment.getExternalStorageDirectory().toString()+ File.separator + utilsFields.REPO_DIR_ROOT + File.separator + utilsFields.REPO_DIR_AUDIOS;
            File f_videos = new File(Dir_path_videos);
            File f_audios = new File(Dir_path_audios);

            File[] files_videos = f_videos.listFiles();
            File[] files_audios = f_audios.listFiles();
            String[] temp;

            //Populando tabla Videos
            if (files_videos != null && files_videos.length > 0) {
                        countVideos.addAndGet(files_videos.length);

                for (int i = 0; i <= files_videos.length - 1; i++) {
                    temp = files_videos[i].toString().split("/");
                    contentValues.put("title",temp[temp.length-1] );
                    contentValues.put("link", temp[temp.length-1]);
                    contentValues.put("type", "video");
                    dbManager.insert(DatabaseHelper.T_Repo, contentValues);
                }
            }
            contentValues.clear();
            //Populando tabla Audios
            if (files_audios != null && files_audios.length > 0){

                countAudios.addAndGet(files_audios.length);

                for (int i = 0; i <= files_audios.length - 1; i++) {
                    temp = files_audios[i].toString().split("/");
                    contentValues.put("title",  temp[temp.length-1] );
                    contentValues.put("link", temp[temp.length-1] );
                    contentValues.put("type", "audio");
                    dbManager.insert(DatabaseHelper.T_Repo, contentValues);
                }
                contentValues.clear();
                dbManager.close();
            }

            //UI thread: informa del resultado

            //Chequeando si es un contexto UI válido
            if (context instanceof MainActivity){
                MainActivity mainActivity = (MainActivity) context;
                if(mainActivity.isFinishing()){return;} //sale
            }

            handler.post(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(context, "Se actualizaron en total: " + countAudios + " audios " + countVideos + " videos", Toast.LENGTH_SHORT).show();

                }
            });

        });


    }




//FIN :::::::::


    /**
     * Recrea la tabla repo de contenido offline: solo si  ha cambiado
     * @param context
     */
        public static void indexOffLineMedios(Context context){

            int md5Result = dirCheckSum();

            //Si se ha producido un error o no ha habido cambios en el directorio repo, sale.
            if (md5Result==0){
                return;
            }

            //Comparando con el último valor almacenado en Preferences
            int temp = PreferenceManager.getDefaultSharedPreferences(context).getInt("dir_checksum",0);

            if (temp == 0){ //Es la primera vez: se almacena el valor y se intenta cargar el contenido
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("dir_checksum",md5Result).apply();
                try {
                    utilsDB.popularDB_Repo(context);
                }catch (Exception ignored){}


            }else { //Se compara el valor
                if (temp != md5Result){ // son diferentes. se intenta actualizar el contenido
                    try {
                        utilsDB.popularDB_Repo(context);
                    }catch (Exception ignored){}
                }
            }

        }


    /**
     * Devuelve un checksum (entero) conjunto de los directorios repo videos y audios
     * @return
     */
        private static int  dirCheckSum() {
            int md5_dirVideos = 0;
            int md5_dirAudios = 0;
            boolean resultError = false;

            //Obteniendo un checksum de todo el directorio
            try {
                File   folderVideos = new File(utilsFields.PATH_ROOT_REPO + utilsFields.REPO_DIR_VIDEOS);
                File[] files_videos  = folderVideos.listFiles();

                if (files_videos.length > 0){
                    for (int i = 0; i<files_videos.length; i++)
                    {
                        md5_dirVideos += files_videos[i].hashCode();
                    }
                }
            }catch (Exception ignored){
                resultError = true;
            }

            //Obteniendo un checksum de todo el directorio
            try {
                File   folderAudios = new File(utilsFields.PATH_ROOT_REPO + utilsFields.REPO_DIR_VIDEOS);
                File[] filesAudios  = folderAudios.listFiles();
                if (filesAudios.length > 0){
                    for (int i = 0; i<filesAudios.length; i++)
                    {
                        md5_dirAudios += filesAudios[i].hashCode();
                    }
                }
            }catch (Exception ignored){
                resultError = true;
            }

            /////////////////////////////////////////////////////////


            //Si ha sucedido un error. Por ejemplo, que no se encuentren los directorios repo, sale del todo y devuelve 0
            if (resultError){
                return 0;
            }

            /////////////////////////////////////////////////////////

            return Math.abs(md5_dirVideos) + Math.abs(md5_dirAudios);

        }


    /**
     * Devuelve la lista de medios repo de la BD
     * @param context
     * @param DirRepo Directorio donde están los medios
     * @param lista  lista que contendrá los medios
     * @return retorna la cantidad de elementos encontrados
     */
    public static int LoadRepoFromDB(Context context, String DirRepo, List<String> lista){
        int result = 0;
        DBManager dbManager = new DBManager(context).open();
        Cursor cursor;
        lista.clear();
        cursor = dbManager.ejectSQLRawQuery("SELECT title FROM " + DatabaseHelper.T_Repo + " WHERE "+ DatabaseHelper.C_repo_type + "='"+ DirRepo+"';");
        if (cursor.moveToFirst()){
            result = cursor.getCount();
            String[] temp;
            while (!cursor.isAfterLast()){
                temp = cursor.getString(0).split("/");
                lista.add(temp[temp.length-1]);
                cursor.moveToNext();
            }
            cursor.close();
        }
        dbManager.close();

        return result;

    }

//FIN ::::::



//PARA FAVORITOS :::::::::::::::::::


    /**
     * Devuelve el estado del campo de favorito
     * @param context
     * @param TableName Nombre de la tabla
     * @param ColumnID Nombre de columna ID del elemento a consultar
     * @param id valor Id del elemento a consultar
     * @return devuelve 1 para si favorito, 0 para no favorito
     */
        public static String readFavState(Context context, String TableName, String ColumnID, String id){

            String result = "";
            DBManager dbManager = new DBManager(context).open();
            Cursor cursor = null;

            cursor = dbManager.ejectSQLRawQuery(" SELECT "+ DatabaseHelper.CC_favorito + " FROM " + TableName + " WHERE " + ColumnID + "='" + id + "';");

            if (cursor.moveToFirst()){
                result = cursor.getString(0);
                cursor.close();
            }
            dbManager.close();
            return  result;
        }

        //Actualiza el estado favorito para frases, conf, videos y audios
        //devuelve el estado actual del campo de favorito (0 inactivo/1 activo/"" error)

    /**
     * Actaliza el valor de favorito (para frases, conf, videos)
     * @param context
     * @param TableName Nombre de la tabla
     * @param ColumnID Nombre de columna ID del elemento a consultar
     * @param id_str Id del elemento a consultar
     * @param id_int Id del elemento a consultar
     * @return
     */
        public static  String  UpdateFavorito(Context context, String TableName, String ColumnID, String  id_str, int id_int) {
            Cursor cursor;
            DBManager dbManager = new DBManager(context).open();
            String result = "";

            if (Objects.equals(id_str, "")){ //Para campos de tipo Int

                cursor = dbManager.ejectSQLRawQuery(" SELECT fav FROM " + TableName + " WHERE " + ColumnID + "=" + id_int + ";");

            }else { //Para ID de tipo String

                cursor = dbManager.ejectSQLRawQuery(" SELECT fav FROM " + TableName + " WHERE " + ColumnID + "='" + id_str + "';");
            }


            ContentValues contentValues = new ContentValues();
            if (cursor.moveToFirst()) {
                if (Objects.equals(cursor.getString(0), "0")) {
                    contentValues.put("fav", "1");
                    result = "1";

                } else if(Objects.equals(cursor.getString(0), "1")){
                    contentValues.put("fav","0");
                    result = "0";
                }

                //Ejecutando la operación de actualización
                if (Objects.equals(id_str, "")){ //Para campos de tipo Int
                    dbManager.update_ForIdInt(TableName, ColumnID, id_int, contentValues);
                }else { //Para ID de tipo String
                    dbManager.update_ForIdStr(TableName, ColumnID, id_str, contentValues);
                }

                cursor.close();
            }

            dbManager.close();

            return result;
        }

//FIN :::::::::




//PARA FRASES ::::::::::::::::::::::::::::::::::::::::::


    /**
     * Corrige errores ortográficos y gramaticales en la BD
     * @param pcontext
     */
    public static void CorrectOrtogFrases(Context pcontext){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                DBManager dbManager = new DBManager(pcontext).open();
                Cursor cursor;
                String sql;

                sql = "SELECT id,frase FROM " + DatabaseHelper.T_Frases + ";"; //Obtiene todas las frase de la tabla frases
                cursor = dbManager.ejectSQLRawQuery(sql);
                String temp;
                ContentValues contentValues = new ContentValues();
                if (cursor.moveToFirst()){

                    while (!cursor.isAfterLast()){
                        temp = cursor.getString(1);
                        ///Yor poner aquí la sección de reemplazos de cadenas
                        temp = temp.replace(" echos "," hechos "); //Reemplazando texto
                        temp = temp.replace("enanmórate","enamórate"); //Reemplazando texto
                        ///
                        contentValues.put(DatabaseHelper.C_frases_frase,temp);
                        dbManager.update_ForIdStr(DatabaseHelper.T_Frases,DatabaseHelper.C_frases_frase,cursor.getString(1),contentValues);
                        contentValues.clear();
                        cursor.moveToNext();
                    }

                }
                cursor.close();
                dbManager.close();

            }
        }); //thread pool
    }



    /**
    *Adiciona una nueva frase a la BD. Devuelve 1 si la frase ha sido añadida
     @param pcontext contexto
     @param textFrase Texto de la frase
     @param autor Autor de la frase
     @param fuente Fuente de la frase
     @param inbuilt si es una frase inbuilt/personal
     @return  si ok devuelve el id del nuevo registro, -1 de lo contrario
     */
    public static  long insertNewFrase(Context pcontext, String textFrase, String autor, String fuente, String inbuilt) {
        DBManager dbManager = new DBManager(pcontext).open();
        ContentValues contentValues = new ContentValues();
        long result = 0;

            contentValues.put(DatabaseHelper.C_frases_frase, textFrase.trim());
            contentValues.put(DatabaseHelper.C_frases_autor, autor.trim());
            contentValues.put(DatabaseHelper.C_frases_fuente, fuente.trim());
            contentValues.put(DatabaseHelper.CC_favorito, "0");
            contentValues.put(DatabaseHelper.CC_nota, "");
            contentValues.put(DatabaseHelper.C_frases_in_built, inbuilt);
            contentValues.put(DatabaseHelper.C_frases_shared, "0");
        result =  dbManager.insert(DatabaseHelper.T_Frases, contentValues);

        dbManager.close();

        return  result;

    }

//FIN :::::::::

    /**
     * Adiciona un nuevo apunte  a la BD
     * @param context contexto de trabajo
     * @param title Título del apunte
     * @param apunte Texto del apunte
     * @return devuelve 1 si se ha añadido el apunte, -1 de lo contrarios
     */
    public static long insertNewApunte(Context context, String title, String apunte){
        long result = 0;

        DBManager dbManager = new DBManager(context).open();
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.C_apunte_title,title.trim() );
        contentValues.put(DatabaseHelper.CC_nota,apunte.trim() );

        result = dbManager.insert(DatabaseHelper.T_Apuntes, contentValues);
        dbManager.close();

        return  result;
    }

    /**
     * Actualiza un apunte. No modifica el título del apunte
     * @param context Contexto de trabajo
     * @param title titulo de la nota
     * @param apunte Contenido del apunte
     * @return true si OK,  false si ha habido un error
     */
    public static boolean updateApunte(Context context, String title, String apunte){
        boolean result = false;
        DBManager dbManager = new DBManager(context).open();
        String query =" UPDATE " + DatabaseHelper.T_Apuntes + " SET " + DatabaseHelper.CC_nota +"='"+ apunte+ "' WHERE " + DatabaseHelper.C_apunte_title + "='" + title + "';";

        try {
            dbManager.ejectSQLCommand(query);
            result = true;
        }catch (SQLException e){
            e.printStackTrace();
        }

        return result;
    }


    /**
     * Actualiza el campo nota en una tabla determinada
     * @param context Contexto de trabajo
     * @param tableName Nombre de la tabla
     * @param columnID Columna ID de la fila a actualizar
     * @param valorID  valor ID de la columna ID
     * @param nota Texto de la nota
     * @return devuelve true si OK, false si error
     */
    public static boolean updateNota(Context context, String tableName, String columnID,String valorID, String nota){
        boolean result = false;
        DBManager dbManager = new DBManager(context).open();
        String query =" UPDATE " + tableName + " SET " + DatabaseHelper.CC_nota +"='"+ nota+ "' WHERE " + columnID  + "='" + valorID + "';";

        try {
            dbManager.ejectSQLCommand(query);
            result = true;
        }catch (SQLException e){
            e.printStackTrace();
        }

        return result;
    }





//PARA CONFERENCIAS ::::::::::::::::::::::::::::::::::::::::::

    /**
     * devuelve  el listado de conferencias a partir de la BD
     * @param pcontext
     * @return devuelve el listado de las conferencias
     */
    public static List<String> loadConferenciaList(Context pcontext) {
        List<String> result = new LinkedList<>();
        Cursor cursor;
        String sql = "SELECT "+DatabaseHelper.C_conf_title + " FROM " + DatabaseHelper.T_Conf;
        DBManager dbManager = new DBManager(pcontext).open();

        cursor = dbManager.ejectSQLRawQuery(sql);


        if (cursor.moveToFirst()){

            while (!cursor.isAfterLast()){

                result.add(cursor.getString(0).replace(".txt",""));

                cursor.moveToNext();
            }
            cursor.close();
        }
        dbManager.close();

        return result;

    }

//FIN :::::::::




//PARA TABLA DE VIDEOS :::::::::::::::::

    //Devuelve la lista de los videos de la BD
    //El parámetro typeOfVideo es el valor del campo type de la tabla video, se utiliza para filtrar el resultado(video conferencias, gregg, etc)

    /**
     * Devuelve el listado de medios de videos de la BD
     * @param context
     * @param typeOfVideo tipo de video a devolver (video_conf, Audio_libros, greeg)
     * @param listaUrl listado de urls de medios a devolver
     * @param lista  listado de titulos de medios a devolver
     */
    public static void LoadVideoList(Context context, String typeOfVideo, List<String> listaUrl, List<String> lista) {

        lista.clear();
        listaUrl.clear();

        DBManager dbManager = new DBManager(context).open();
        Cursor cursor = dbManager.ejectSQLRawQuery("SELECT * FROM "+ DatabaseHelper.T_Videos + " WHERE " + DatabaseHelper.C_videos_type + "='" + typeOfVideo+"';" );

        if (cursor != null && cursor.getCount() > 0){
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                lista.add(cursor.getString(1));
                listaUrl.add(cursor.getString(2));
                cursor.moveToNext();
            }
            cursor.close();
        }

        dbManager.close();
    }

//FIN :::::::::

}
