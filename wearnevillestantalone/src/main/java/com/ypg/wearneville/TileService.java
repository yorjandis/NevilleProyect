package com.ypg.wearneville;


import android.content.res.Resources;
import android.text.style.TtsSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.wear.tiles.ActionBuilders;
import androidx.wear.tiles.ColorBuilders;
import androidx.wear.tiles.DimensionBuilders;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.ResourceBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TimelineBuilders;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Modifier;

public class TileService extends androidx.wear.tiles.TileService {

    private static final String RESOURCES_VERSION = "1";

    private static  DimensionBuilders.DpProp  PROGRESS_BAR_THICKNESS = DimensionBuilders.dp(6f);


    @NonNull
    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(@NonNull RequestBuilders.TileRequest requestParams) {



        return Futures.immediateFuture(new TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                        .setFreshnessIntervalMillis(2000)
                .setTimeline(new TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(new TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(new LayoutElementBuilders.Layout.Builder()

                                        .setRoot(tappableElement()).build()

                                ).build()
                        ).build()
                ).build()


        );
    }




    //Crear un elemento de layout
    private LayoutElementBuilders.LayoutElement tappableElement() {
        return new LayoutElementBuilders.Text.Builder()
                .setText("Frases de Neville")
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setClickable(new ModifiersBuilders.Clickable.Builder()
                                .setId("foo")
                                .setOnClick(new ActionBuilders.LaunchAction.Builder()
                                        .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                                                .setClassName(MainActivity.class.getName())
                                                .setPackageName(this.getPackageName())
                                                .build()
                                        ).build()
                                ).build()
                        ).build()
                ).build();
    }






    @NonNull
    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onResourcesRequest(@NonNull RequestBuilders.ResourcesRequest requestParams) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        );
    }


}
