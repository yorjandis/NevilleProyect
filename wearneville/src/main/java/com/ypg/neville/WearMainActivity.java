package com.ypg.neville;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import com.ypg.neville.databinding.ActivityWearMainBinding;

import java.util.ArrayList;
import java.util.Random;

public class WearMainActivity extends Activity {

    private TextView mTextView, title;
    private ActivityWearMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityWearMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mTextView = binding.text;
        title = binding.texttitle;


        frases();

        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frases();
            }
        });

        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    }


    public  void frases(){
        ArrayList<String> tagList = new ArrayList();
        //String[] someArray = getResources().getStringArray(R.array.listfrases);
        Random r = new Random();
      //  mTextView.setText(someArray[r.nextInt(someArray.length)]);

    }
}