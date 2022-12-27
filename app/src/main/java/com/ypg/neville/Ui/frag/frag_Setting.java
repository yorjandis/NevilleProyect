package com.ypg.neville.Ui.frag;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ypg.neville.MainActivity;
import com.ypg.neville.R;
import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.model.utils.ColorPickerManager;
import com.ypg.neville.model.utils.GetFromRepo;
import com.ypg.neville.model.utils.UiModalWindows;

public class frag_Setting extends PreferenceFragmentCompat {



    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);



        //Establecer el tema de la app:
        findPreference("tema").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

                getActivity().startActivity(new Intent(getContext(), MainActivity.class));
                getActivity().finish();
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);


                return true;
            }
        });


        //Tamaño de fuente:

        findPreference("fuente_frase").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

                //Extrayendo los valores numericos del texto dado (si aplicamos un caracter nos da error)
                String numberOnly= newValue.toString().replaceAll("[^0-9]", "");
                if (numberOnly.isEmpty()){numberOnly = "28";}
                if (Integer.parseInt(numberOnly) >= 40 ){numberOnly = "28";}
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("fuente_frase",numberOnly).apply();

                return false;
            }
        });

        findPreference("fuente_conf").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

                //Extrayendo los valores numericos del texto dado (si aplicamos un caracter nos da error)
                String numberOnly= newValue.toString().replaceAll("[^0-9]", "");
                if (numberOnly.isEmpty()){numberOnly = "170";}
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("fuente_conf",numberOnly).apply();
                return false;
            }
        });


        //Color de los marcos de la app
        findPreference("color_marcos").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                ColorPickerManager.ShowColorPicker(getContext(), 0, "color_marcos", "Color de Marcos" );
                return false;
            }
        });

        //Color del texto de las frases
        findPreference("color_letra_frases").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                ColorPickerManager.ShowColorPicker(getContext(), 0, "color_letra_frases", "Color de letra en frases" );
                return false;
            }
        });




        //LLeva a la página de proyecto, sección donar:
        findPreference("donar").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://projectsypg.mozello.com/donar/")));
                return false;
            }
        });

        //Permite indexar el contenido off-line manualmente:
        findPreference("index_offline").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {

                AlertDialog.Builder aler = new AlertDialog.Builder(getContext());
                aler.setTitle("Indexar el contenido off-line");
                aler.setMessage("Se actualizará el índice de medios off-line de videos y audios en el almacenamiento externo. Utilizar si ha cambiado el contenido en esta carpeta");
                aler.setPositiveButton("Indexar Contenido", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        utilsDB.indexOffLineMedios(requireContext());
                    }
                });

                aler.show();



                return false;
            }
        });

        //Actualizar el listado de frases con la información desde la web

        findPreference("update_frases_from_web").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {

                GetFromRepo.getFrasesFromWeb(requireContext());

                Toast.makeText(requireContext(), "El compendio de frases se esta actualizando", Toast.LENGTH_SHORT).show();

                return false;
            }
        });



        //LLeva a la página de proyecto, sección escribir comentario:
        findPreference("write_comment").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://projectsypg.mozello.com/contacto/")));
                return false;
            }
        });
        //LLeva a la página de proyecto:
        findPreference("web_site").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://projectsypg.mozello.com/")));
                return false;
            }
        });

        //Lleva al sitio web en google play, para escribir una reseña
        findPreference("resena_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Uri uri = Uri.parse("market://details?id=" + getContext().getPackageName());
                Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(myAppLinkToMarket);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), " unable to find market app", Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

        //Muestra el mensaje con las novedades:
        findPreference("show_news").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                UiModalWindows.showAyudaContectual(getContext(), "Novedades","Que hay de nuevo?", getActivity().getString(R.string.news),false, getActivity().getDrawable(R.drawable.neville));
                return false;
            }
        });


    }

}
