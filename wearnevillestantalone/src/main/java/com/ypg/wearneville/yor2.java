package com.ypg.wearneville;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.wear.widget.WearableRecyclerView;


public class yor2 extends Activity implements View.OnClickListener {
    
    Button button;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yor2);

        button = findViewById(R.id.btnyor);
        textView = findViewById(R.id.text1);



        button.setOnClickListener(yor2.this);
        textView.setOnClickListener(yor2.this);

        //centrando el texto en el centro de la pantalla
        textView.setText(Utils.frases(getApplicationContext()));

        setParamText();
        
        
    }


    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.btnyor:


                break;
            case R.id.text1:

                textView.setText(Utils.frases(getApplicationContext()));
                //startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;

        }


    }

//centrando el texto en la pantalla:
    private void setParamText(){

        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(getApplicationContext().WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textView.getLayoutParams();
        params.width = size.x;
        textView.setLayoutParams(params);

        textView.setGravity(Gravity.CENTER);
    }
}