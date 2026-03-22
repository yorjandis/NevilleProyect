package com.ypg.neville.Ui.frag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.ypg.neville.R;


//representa el contenido sobre gregg draden
public class frag_gregg extends Fragment {


    public frag_gregg() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_gregg, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Html html;

        Button btn_libros = view.findViewById(R.id.frag_gregg_libros);
        Button btn_videos = view.findViewById(R.id.frag_gregg_videos);
        TextView text_youtube = view.findViewById(R.id.frag_gregg_youtube);
        TextView text_website = view.findViewById(R.id.frag_gregg_website);

        text_website.setText(Html.fromHtml(getString(R.string.app_name)));


        btn_libros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent ii = new Intent(Intent.ACTION_VIEW);
                ii.setData(Uri.parse("https://drive.google.com/file/d/1_fTTJDpyTSqOtZ4shdbsXeEQSVWqpO_a/view?usp=sharing"));
                startActivity(ii);
            }
        });

        btn_videos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frag_listado.elementLoaded ="video_gredd";
                Navigation.findNavController(view).navigate(R.id.frag_listado);
            }
        });

        text_youtube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://www.youtube.com/c/GreggBradenOfficial"));
                startActivity(i);
            }
        });

        text_website.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://www.greggbraden.com/about-gregg-braden/"));
                startActivity(i);
            }
        });


    }




}