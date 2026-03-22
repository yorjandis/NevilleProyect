package com.ypg.neville.Ui.frag

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.ypg.neville.R

// representa el contenido sobre gregg braden
class frag_gregg : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_gregg, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn_libros = view.findViewById<Button>(R.id.frag_gregg_libros)
        val btn_videos = view.findViewById<Button>(R.id.frag_gregg_videos)
        val text_youtube = view.findViewById<TextView>(R.id.frag_gregg_youtube)
        val text_website = view.findViewById<TextView>(R.id.frag_gregg_website)

        text_website.text = Html.fromHtml(getString(R.string.app_name))

        btn_libros.setOnClickListener {
            val ii = Intent(Intent.ACTION_VIEW)
            ii.data = Uri.parse("https://drive.google.com/file/d/1_fTTJDpyTSqOtZ4shdbsXeEQSVWqpO_a/view?usp=sharing")
            startActivity(ii)
        }

        btn_videos.setOnClickListener {
            frag_listado.elementLoaded = "video_gredd"
            Navigation.findNavController(view).navigate(R.id.frag_listado)
        }

        text_youtube.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://www.youtube.com/c/GreggBradenOfficial")
            startActivity(i)
        }

        text_website.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://www.greggbraden.com/about-gregg-braden/")
            startActivity(i)
        }
    }
}
