package com.ypg.neville.model.utils;


//Listener que comprueba y notifica cuando existe una nueva actualización de la app en play store

import android.content.Context;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class myListener_In_App_Update {

    In_mylistener listener; //interfaz
    Context context;

    //Constructor
    public myListener_In_App_Update(Context pcontext) {
        context = pcontext;
        this.listener = null; //Inicializa el listener a null.
       // doTask();
    }

    //Clase interna que representa la interfaz con un método abstracto
    public interface In_mylistener {
        void onUpdateAvailable(Boolean pUpdateAvailable); //esta es la variable que contendrá el resultado
    }



    //permite establecer el listener al objeto de la clase. Este método permite asiganr el listener y devolver el resultado de la operación asincrónica
    public void setMylistener(In_mylistener plistener){
        this.listener = plistener;
        doTask();
    }

//Método que hará el trabajo (asincrónicamente - en un nuevo hilo de ejecución)
    private void doTask(){
        if (this.listener != null){

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
                    appUpdateManager.getAppUpdateInfo().addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
                        @Override
                        public void onSuccess(AppUpdateInfo appUpdateInfo) {

                            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ){

                                listener.onUpdateAvailable(true);

                            }//else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED){ //Se ha descargado una actualizacion}
                            else{
                                listener.onUpdateAvailable(false);

                            }
                        }
                    });
                }
            });
        }
    }

}
