package com.ypg.wearneville

import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TimelineBuilders
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class TileService : androidx.wear.tiles.TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return Futures.immediateFuture(TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(2000)
            .setTimeline(TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(LayoutElementBuilders.Layout.Builder()
                        .setRoot(tappableElement()).build()
                    ).build()
                ).build()
            ).build()
        )
    }

    // Crear un elemento de layout
    private fun tappableElement(): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Text.Builder()
            .setText("Frases de Neville")
            .setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setClickable(ModifiersBuilders.Clickable.Builder()
                    .setId("foo")
                    .setOnClick(ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(ActionBuilders.AndroidActivity.Builder()
                            .setClassName(MainActivity::class.java.name)
                            .setPackageName(this.packageName)
                            .build()
                        ).build()
                    ).build()
                ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        )
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
        private val PROGRESS_BAR_THICKNESS = DimensionBuilders.dp(6f)
    }
}
