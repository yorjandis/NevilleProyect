package com.ypg.neville.model.utils;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.services.serviceStreaming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

//Clase para operaciones generales

public   class Utils {

    private Context context;




    //constructor
    public Utils(Context pcontext) {
        this.context = pcontext;
    }

    //Muestra una notificación en la barra de tareas

    /**
     *
     * @param bigtext texto grande
     * @param intent intent
     */
    @TargetApi(Build.VERSION_CODES.O)
    public void show_Notification(String bigtext, Intent intent) {

        NotificationChannel notificationChannel = new NotificationChannel(utilsFields.NOTIFICACION_CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, utilsFields.NOTIFICACION_CHANNEL_ID)
                .setContentTitle("")
                //.setContentText("Contenido")
                .setContentIntent(pendingIntent)
                .setStyle(new Notification.BigTextStyle().bigText(bigtext))
                .addAction(android.R.drawable.sym_action_chat, "Actualizar", pendingIntent)
                .setChannelId(utilsFields.NOTIFICACION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(utilsFields.NOTIFICACION_ID, notification);
    }


    /**
     * Chequea si existe conexión a internet. Envia un mensaje de NO haber conexión
     * @param contextP Contexto
     * @return true si existe conexión, false de lo contrario
     */
    public static boolean isConnection(Context contextP) {
        boolean isconeccion = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) contextP.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        isconeccion = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if(!isconeccion){
            Toast.makeText(contextP, "Contenido no accesible sin conexión", Toast.LENGTH_SHORT).show();
        }

        return isconeccion;

    }




    //Enviar un email al desarrollador (No usado)
   /* public void sendEmailToDeveploper() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "projectsypg@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android APP - " + context.getPackageName().toString());
        context.startActivity(Intent.createChooser(emailIntent, "Neville - Enviar un email al Desarrolador"));
    }*/




    // Lista los ficheros en el directorio assets y sus carpetas
    //el parámetro dir puede ser "" para la raiz de assets o el nombre de una subcarpeta como "config"
    public void listFilesInAssets(String dir, List <String> lista) throws IOException {

        lista.clear(); //limpia el arreglo de String
        String[] fileList = context.getAssets().list(dir);

        for (int i = 0; i < fileList.length; ++i){
            lista.add(fileList[i].replace(".txt",""));
        }
    }





    //Inicia el servicio de streaming reproduciendo un medio en particular (solo para medios offline)
    public static void playInStreaming(Context context, String repoDir, String ItemName){
        Intent intent = new Intent(context, serviceStreaming.class);

        context.getApplicationContext().stopService(intent); //detiene el servicio

        intent.setAction("play_medio");
        intent.putExtra("file",utilsFields.PATH_ROOT_REPO + repoDir + File.separator + ItemName);
        intent.putExtra("title", ItemName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(intent);
        }else {
            context.startService(intent);
        }
    }



//Carga el contenido de los directorios externos. Genera el listado para los archivos en las carpetas "videos" y "audios"
    public  static void loadRepo(Context context){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Indexar el contenido offline");
        alert.setMessage(" Utilize esta opción Si ha cambiado el contenido en la carpeta NevilleRepo en su almacenamiento externo");
        alert.setPositiveButton("Actualizar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                utilsDB.popularDB_Repo(context); //Actualiza la tabla repo con el nuevo contenido en las carpetas del sistema
                Toast.makeText(context, "El contenido se ha actualizado", Toast.LENGTH_SHORT).show();
            }
        });
        alert.show();
    }



    // Devuelve una cadena legible  de elementos que pueden tener una nota asociada
    public  static String getElementLoad(String key){
        switch (key){
            case "frases":
                return  "Esta frase";
            case "conf":
                return "Esta Conferencia";
            case "video_conf":
                return "Este Video Conferencia";
            case "video_gredd":
                return "Video gregg";
            case "video_book":
                return "Este Audio libro";
            case "video_ext":
                return "Este video offline";
            case "audio_ext":
                return "Este Audio offline";

        }
        return  "";
    }



    //Crear la estructura de directorios
    public static void CrearDirectoriosRepo(Context pcontext){
        //Creando la estructura de directorios en el almacenamiento externo, Si no existe
        String path = Environment.getExternalStoragePublicDirectory(utilsFields.REPO_DIR_ROOT) + File.separator + utilsFields.REPO_DIR_VIDEOS;
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()){
                Toast.makeText(pcontext, "Directorio Videos NO creado", Toast.LENGTH_SHORT).show();
            }
        }
        String path2 = Environment.getExternalStoragePublicDirectory(utilsFields.REPO_DIR_ROOT) + File.separator + utilsFields.REPO_DIR_AUDIOS;
        File file2 = new File(path2);
        if (!file2.exists()) {
            if (!file2.mkdirs()){
                Toast.makeText(pcontext, "Directorio Audios NO creado", Toast.LENGTH_SHORT).show();
            }
        }
    }



    //devuelve una lista de conferencias en Assets que contiene una subcadena de texto
        //devuelve una lista con las conferencias que tienen el texto
    public static List<String> searchInConf(Context pcontext, String string) throws IOException {

        String[] listFiles = pcontext.getAssets().list("conf");
        InputStream is;
        String confText;
        int size;
        byte[] buffer;
        List<String> lista = new LinkedList<>();


        for (int i = 0; i < listFiles.length; ++i){

            is =  pcontext.getAssets().open("conf" + File.separator + listFiles[i]);

            size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
            confText = new String(buffer);

            if (confText.toLowerCase().contains(string.toLowerCase())){
                lista.add(listFiles[i].replace(".txt",""));
            }
        }

        return lista;

    }




}
