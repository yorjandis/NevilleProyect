package com.ypg.neville.model.utils.balloon

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonHighlightAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.ypg.neville.R

/**
 * Clase que se encarga de mostrar la ayuda contextual
 */
class HelpBalloon(private val context: Context) {

    /**
     * Construye un objeto Balloon
     * @param message Mensaje para mostrar
     * @return retorna un objeto de tipo Balloon
     */
    fun buildFactory(message: String, lifecycleOwner: LifecycleOwner?): Balloon {
        return Balloon.Builder(context)
            .setArrowSize(10)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowPosition(0.5f)
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(BalloonSizeSpec.WRAP)
            .setPadding(6)
            .setTextSize(18f)
            .setCornerRadius(4f)
            .setAlpha(0.9f)
            .setText(message)
            .setTextColor(ContextCompat.getColor(context, R.color.black))
            .setTextIsHtml(false)
            .setIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_tips))
            .setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setLifecycleOwner(lifecycleOwner) // Para adecuar el objeto al ciclo de vida del propietario
            .setBalloonHighlightAnimation(BalloonHighlightAnimation.SHAKE)
            .build()
    }
}
