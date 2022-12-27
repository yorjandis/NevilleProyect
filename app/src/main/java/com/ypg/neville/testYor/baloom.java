package com.ypg.neville.testYor;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowPositionRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;
import com.skydoves.balloon.OnBalloonClickListener;
import com.ypg.neville.MainActivity;
import com.ypg.neville.R;

import java.util.List;

public class baloom {


   public static List<View> list;
    public static Balloon balloon;




    //Frases
    public static void showBallon(Context context, View view){

         balloon = new Balloon.Builder(context)
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(65)
                .setTextSize(15f)
                .setCornerRadius(4f)
                .setAlpha(0.9f)
                .setText("<a href='http://www.google.com'>Esto es solo un ejemplo</a>")
                .setTextColor(ContextCompat.getColor(context, R.color.white))
                .setTextIsHtml(true)
                .setIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_help))
                .setBackgroundColor(ContextCompat.getColor(context, R.color.fav_inactive))
                .setOnBalloonClickListener(new OnBalloonClickListener() {
                    @Override
                    public void onBalloonClick(@NonNull View view) {
                        balloon.dismiss();
                        baloom.showBallon(context, MainActivity.mainActivityThis.ic_toolsBar_nota_add );

                    }
                })
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setLifecycleOwner(MainActivity.mainActivityThis)
                .build();

        balloon.showAlignBottom(view);

    }


    public static void showBallon2(Context context, View view){

        Balloon balloon = new Balloon.Builder(context)
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(65)
                .setTextSize(15f)
                .setCornerRadius(4f)
                .setAlpha(0.9f)
                .setText("<a href='http://www.google.com'>Esto es solo un ejemplo</a>")
                .setTextColor(ContextCompat.getColor(context, R.color.white))
                .setTextIsHtml(true)
                .setIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_help))
                .setBackgroundColor(ContextCompat.getColor(context, R.color.fav_inactive))
                .setOnBalloonClickListener(new OnBalloonClickListener() {
                    @Override
                    public void onBalloonClick(@NonNull View view) {

                    }
                })
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setLifecycleOwner(MainActivity.mainActivityThis)
                .build();

        balloon.showAlignBottom(view);

    }



}
