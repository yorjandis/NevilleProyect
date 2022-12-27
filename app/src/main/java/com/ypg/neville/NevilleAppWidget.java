package com.ypg.neville;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.ypg.neville.model.db.DBManager;
import com.ypg.neville.model.db.DatabaseHelper;

import java.util.Random;

/**
 * Implementation of App Widget functionality.
 */
public class NevilleAppWidget extends AppWidgetProvider {

    private static final String WIDGET_CLICK_ACTION    = "automaticWidgetSyncButtonClick1";
    private static int widgetid;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.neville_widget);
        views.setTextViewText(R.id.widget_text, frases(context));
        widgetid = appWidgetId;
        Intent intent = new Intent(context, NevilleAppWidget.class);
        intent.setAction(WIDGET_CLICK_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);


        Intent intent1 = new Intent(context,MainActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(intent1.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pendingIntent1 = PendingIntent.getActivity(context,0,intent1,PendingIntent.FLAG_IMMUTABLE);



        views.setOnClickPendingIntent(R.id.widget_text, pendingIntent );
        views.setOnClickPendingIntent(R.id.widget_text_home, pendingIntent1);




        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            widgetid = appWidgetId;
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (WIDGET_CLICK_ACTION.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews;
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.neville_widget);
            remoteViews.setTextViewText(R.id.widget_text, frases(context));

            appWidgetManager.updateAppWidget(widgetid, remoteViews);
        }



    }

    //Obtiene una frase al azar
    private static String  frases(Context context){
        DBManager dbManager = new DBManager(context).open();
        String sql = "SELECT frase FROM " + DatabaseHelper.T_Frases + ";";
        String result = "Inmaginar crea la realidad";
        Cursor cursor;

        cursor = dbManager.ejectSQLRawQuery(sql);
        Random random = new Random();

        if (cursor.moveToFirst()){
            cursor.move(random.nextInt(cursor.getCount()));
            result = cursor.getString(0);
        }
        return result;
    }




}