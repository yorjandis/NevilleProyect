package com.ypg.neville.model.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.zxing.integration.android.IntentIntegrator;
import com.ypg.neville.MainActivity;
import com.ypg.neville.R;

import org.jetbrains.annotations.Nullable;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

/**
Clase que se encarga de manejar la funcionalidad de código QR
 */
public class QRManager {

   public static  boolean Request_Code = false; //para OnactivityResult


    /**
     * Dialogo que genera el código QR generado
     @param context contexto
     @param textoQR texto de qr
     @param title título del diálogo
     @param message mensaje del diálogo
     */
public static void ShowQRDialog(Context context, String textoQR, String title, @Nullable String message){

    //Determinar el número de caracteres para saber si se puede convertir a QR:
    if (textoQR.toCharArray().length > 4000){
        Toast.makeText(context, "Se ha sobrepasado el límite de texto para QR", Toast.LENGTH_SHORT).show();
        return;
    }

    AlertDialog.Builder alert = new AlertDialog.Builder(context);
    alert.setTitle(title);
    String msg = message != null ? message : "";
    alert.setMessage(msg);

    View view = LayoutInflater.from(context).inflate(R.layout.dialog_qr, null, false);

    ImageView imageView = view.findViewById(R.id.qr_image_view);
    ImageView image_shared = view.findViewById(R.id.qr_image_shared);

    imageView.setImageBitmap(generarQR(textoQR,300));

    alert.setView(view);

    AlertDialog alertDialog = alert.create();
        alertDialog.show();

    image_shared.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, textoQR);
            context.startActivity(Intent.createChooser(intent, title));
        }
    });

}


    /**
     * Inicializa el lector de QR
     */
    public static void launch_QRRead(){
        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.mainActivityThis);
        intentIntegrator.setPrompt("neville - Leyendo Código QR");
        intentIntegrator.setBarcodeImageEnabled(true);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        intentIntegrator.setBeepEnabled(false);
        QRManager.Request_Code = true;
        intentIntegrator.initiateScan();

    }




    /**
     *Genera un bitmap de QR del texto pasado como parametro
     @param text_P   Texto del código QR
     @param dimension Dimension ancho/alto de la imagen generada
     */
    private static Bitmap generarQR(String text_P, int dimension){
        QRGEncoder qrgEncoder = new QRGEncoder(text_P, null, QRGContents.Type.TEXT, dimension);
        qrgEncoder.setColorBlack(Color.WHITE);
        qrgEncoder.setColorWhite(Color.BLACK);

        // qrgEncoder.setColorWhite(R.color.black);
        try {
            // Getting QR-Code as Bitmap
            // Setting Bitmap to ImageView
            Bitmap bitmap = qrgEncoder.getBitmap();
            return bitmap;
        } catch (Exception e) {
            return  null;
        }

    }


}
