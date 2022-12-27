package com.ypg.wearneville;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.wear.remote.interactions.RemoteActivityHelper;

import com.ypg.wearneville.databinding.ActivityMainBinding;

import java.util.concurrent.Executor;

public class MainActivity extends Activity   {

    private TextView mTextView;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mTextView = binding.text;

        mTextView.setText(Utils.frases(getApplicationContext()));


        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                
                mTextView.setText(Utils.frases(getApplicationContext()));


                startActivity(new Intent(getApplicationContext(), yor2.class));




            }
        });

    }












    private void showmsg(){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Yorjandis");
        alertDialog.setMessage("Esto es solo un ejemplo");
        alertDialog.create();
        alertDialog.setPositiveButton("Salir", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Utils.showmessage("Yorjandis", getApplicationContext());
            }
        });
        alertDialog.setNegativeButton("yor", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Utils.showmessage("perez", getApplicationContext());
            }

        });

        alertDialog.show();

    }
    
    
    

}