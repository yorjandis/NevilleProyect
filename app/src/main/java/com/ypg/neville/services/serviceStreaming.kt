package com.ypg.neville.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.myReceiver;
import com.ypg.neville.model.utils.utilsFields;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class serviceStreaming extends Service {

    private MediaPlayer mediaPlayer;
    private ExecutorService exec;

    public static serviceStreaming mserviseThis = null;

    //Para Notificaciones
    public static final String CHANEL_ID = "neville1";
    public static final int NOTIFICATION_ID = 1221;


    @Override
    public void onCreate() {
        serviceStreaming.mserviseThis = serviceStreaming.this;
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        //Enviando un mensaje al reseiver (Para hacer una prueba yor)
        Intent intent11 = new Intent();
        intent11.setAction("com.ypg.neville.action.streaming.signal");
        intent11.putExtra("action", "Yorjandis");
        getApplicationContext().sendBroadcast(intent11);

        if (intent.getAction().contains("play_medio")){


            try{
                if(!exec.isTerminated()){
                    exec.shutdownNow();
                }

            }catch (Exception ignored){}


            exec = Executors.newSingleThreadExecutor();
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(intent.getStringExtra("file")));

                    createNotificacion(intent.getStringExtra("title"), mediaPlayer.getDuration()/60000);

                    mediaPlayer.setLooping(false);


                    //Setting: establece el valor de repetición
                    if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("play_video_playloop",false)){
                        mediaPlayer.setLooping(true);
                    }else{
                        mediaPlayer.setLooping(false);
                    }

                    mediaPlayer.start();

                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {

                        }
                    });
                }
            });

        }

        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        if(mediaPlayer != null){
            mediaPlayer.release();
        }
        exec.shutdownNow();
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    private void createNotificationChanel(){
        //Chequeando si la versión de Adroid es Oreo o superior:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(serviceStreaming.CHANEL_ID,"Foreground Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

    }


    /**
     * Crea una notificación para la reproducción en streaming
     * @param titulo título de la notificación
     * @param duraMinutos valor que indica la duración de la pista
     */
    private void createNotificacion(String titulo, long duraMinutos){

        createNotificationChanel();
        Intent intent_openActivity = new Intent(this, MainActivity.class);
        intent_openActivity. setAction("yor");
        PendingIntent pendingIntent_open = PendingIntent.getActivity(this,5,intent_openActivity, PendingIntent.FLAG_IMMUTABLE);

        Intent intent_Stop = new Intent(this, myReceiver.class);
        intent_Stop.setAction(myReceiver.ACTION_SIGNAL);
        intent_Stop.putExtra("action","stop");
        PendingIntent pendingIntent_stop = PendingIntent.getBroadcast(this, 0,intent_Stop,PendingIntent.FLAG_IMMUTABLE);

        Intent intent_pause = new Intent(this, myReceiver.class);
        intent_pause.setAction(myReceiver.ACTION_SIGNAL);
        intent_pause.putExtra("action","pause");
        PendingIntent pendingIntent_pause = PendingIntent.getBroadcast(this, 1,intent_pause,PendingIntent.FLAG_IMMUTABLE);

        Intent intent_resume = new Intent(this, myReceiver.class);
        intent_resume.setAction(myReceiver.ACTION_SIGNAL);
        intent_resume.putExtra("action","resume");
        PendingIntent pendingIntent_resume = PendingIntent.getBroadcast(this, 2,intent_resume,PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this,serviceStreaming.CHANEL_ID )
                .setContentTitle("Neville - play ("+ duraMinutos +" minutos)")
                .setContentText(titulo)
                .setSmallIcon(R.drawable.ic_item)
                .setContentIntent(pendingIntent_open)
                .addAction(R.drawable.ic_item, "Detener",pendingIntent_stop)
                .addAction(R.drawable.ic_item,"Pausar",pendingIntent_pause)
                .addAction(R.drawable.ic_item,"Resumir",pendingIntent_resume)
                .build();


        startForeground(serviceStreaming.NOTIFICATION_ID, notification);

    }

    /**
     * Actualiza el contenido de la notificación
     * @param context Contexto de trabajo
     * @param titulo Nuevo título
     * @param duraMinutos valor que indica la duración de la pista
     */
    public static void updateNotification(Context context, String titulo, long duraMinutos){

        Intent intent_openActivity = new Intent(context, MainActivity.class);
        intent_openActivity. setAction("yor");
        PendingIntent pendingIntent_open = PendingIntent.getActivity(context,5,intent_openActivity, PendingIntent.FLAG_IMMUTABLE);

        Intent intent_Stop = new Intent(context, myReceiver.class);
        intent_Stop.setAction(myReceiver.ACTION_SIGNAL);
        intent_Stop.putExtra("action","stop");
        PendingIntent pendingIntent_stop = PendingIntent.getBroadcast(context, 0,intent_Stop,PendingIntent.FLAG_IMMUTABLE);

        Intent intent_pause = new Intent(context, myReceiver.class);
        intent_pause.setAction(myReceiver.ACTION_SIGNAL);
        intent_pause.putExtra("action","pause");
        PendingIntent pendingIntent_pause = PendingIntent.getBroadcast(context, 1,intent_pause,PendingIntent.FLAG_IMMUTABLE);

        Intent intent_resume = new Intent(context, myReceiver.class);
        intent_resume.setAction(myReceiver.ACTION_SIGNAL);
        intent_resume.putExtra("action","resume");
        PendingIntent pendingIntent_resume = PendingIntent.getBroadcast(context, 2,intent_resume,PendingIntent.FLAG_IMMUTABLE);

        Notification notificationupdate = new NotificationCompat.Builder(context,serviceStreaming.CHANEL_ID )
                .setContentTitle("Neville - play ("+ duraMinutos +" minutos)")
                .setContentText(titulo)
                .setSmallIcon(R.drawable.ic_item)
                .setContentIntent(pendingIntent_open)
                .addAction(R.drawable.ic_item, "Detener",pendingIntent_stop)
                .addAction(R.drawable.ic_item,"Pausar",pendingIntent_pause)
                .addAction(R.drawable.ic_item,"Resumir",pendingIntent_resume)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(serviceStreaming.NOTIFICATION_ID, notificationupdate);



    }


////  LISTADO DE METODOS PUBLICOS ///

    /**
     * detiene el servicio
     */
        public void serviceStop(){
            try {
                mediaPlayer.release();
                exec.shutdownNow();
                exec.shutdownNow();
            }catch (Exception ignored){
            }

            stopSelf();

        }

    /**
     * Detiene el reproductor
     */
    public void stopMediaP(){
            try{
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    mediaPlayer.stop();
            }
            catch (Exception ignored){

            }
        }

    /**
     * Pausa el reproductor
     */
    public void pauseMediaP(){

            if (mediaPlayer != null && mediaPlayer.isPlaying() ){
                mediaPlayer.pause();
            }
        }

    /**
     * Inicia el reproductor
     */
    public void startMediaP(){
            if (mediaPlayer != null){
                if (mediaPlayer.isPlaying()){mediaPlayer.stop();}
                mediaPlayer.start();
            }
        }

    /**
     * Establece una ruta de un medio para el reproductor
     * @param source
     */
    public  void SetDataSourceMediaP(String source){

            try {
                mediaPlayer.setDataSource(source);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    /**
     * Libera el objero del reprocutor
     */
    public void releaseMediaP(){

        try{
            if (mediaPlayer != null){
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }

        }catch (Exception ignored){}

        }

    /**
     * Estalece la reproducción continua del medio
     * @param flag
     */
    public  void loopingMediaP(boolean flag){

            if (mediaPlayer != null){
                mediaPlayer.setLooping(flag);
            }

        }

    /**
     * Obtiene la duración de reproducción
     * @return
     */
    public int getDurationMediaP(){
        int result = 0;
         if (mediaPlayer != null ){
             result = mediaPlayer.getDuration();
         }
        return  result;
        }



    /**
     * Reproduce un nuevo archivo. El servicio debe estar activo. Es útil para reproducir listas de medios
     * @param DirRepo nombre del directorio repo
     * @param file nombre del archivo a reproducir
     * @param plooping indica si se reproduce continuamente
     */
        public  void playMediaP(String DirRepo, String file, boolean plooping){

                if(mediaPlayer != null ){

                    if (mediaPlayer.isPlaying()){mediaPlayer.stop();}
                    mediaPlayer.reset();

                String source = File.separator + "sdcard" + File.separator + utilsFields.REPO_DIR_ROOT + File.separator + DirRepo + File.separator  + file;


                    try {
                        mediaPlayer.setDataSource(source);

                        try {
                            mediaPlayer.prepare();

                            mediaPlayer.setLooping(plooping);

                            updateNotification(getBaseContext(), source, mediaPlayer.getDuration()/60000);
                            mediaPlayer.start();

                        }catch (Exception ignored){}

                    }catch (Exception ignored){}
                }
        }

}
