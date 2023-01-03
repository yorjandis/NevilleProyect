package com.ypg.neville.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.crashlytics.internal.common.CrashlyticsBackgroundWorker;
import com.ypg.neville.services.serviceStreaming;

public class myReceiver extends BroadcastReceiver {

    public static final  String ACTION_SIGNAL = "com.ypg.neville.action.streaming.signal";

    @Override
    public void onReceive(Context context, Intent intent) {




            if (intent.getAction().contains(myReceiver.ACTION_SIGNAL)){

                String action = intent.getStringExtra("action");

                if (action != null && !action.isEmpty()){
                    switch ( intent.getStringExtra("action")){

                        case "stop":
                            if (serviceStreaming.mserviseThis != null){
                                context.stopService(new Intent(context, serviceStreaming.class));
                            }
                            break;

                        case "pause":
                            if (serviceStreaming.mserviseThis != null){
                                serviceStreaming.mserviseThis.pauseMediaP();
                            }
                            break;

                        case "resume":
                            if (serviceStreaming.mserviseThis != null) {
                                serviceStreaming.mserviseThis.startMediaP();
                            }
                            break;

                        case "Yorjandis":

                            break;
                    }

                }else{
                    FirebaseCrashlytics.getInstance().log("esto es un ejemplo Yorjandis");
                }



            }

    }


}