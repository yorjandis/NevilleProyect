package com.ypg.neville.Ui.frag

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.MediaController
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DBManager
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.adapter.MyListAdapterItemsList
import com.ypg.neville.model.utils.balloon.HelpBalloon
import com.ypg.neville.model.utils.utilsFields
import java.io.File
import java.io.IOException
import java.util.LinkedList

class frag_listado : Fragment() {

    private lateinit var playerView: YouTubePlayerView
    private var player: YouTubePlayer? = null
    private lateinit var videoView: VideoView
    private lateinit var listView: ListView
    private var myListAdapterItemsList: MyListAdapterItemsList? = null
    private lateinit var mostrar_opciones: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var spinnerFilter: Spinner
    private lateinit var searchView: SearchView
    private lateinit var searchViewConf: SearchView
    private lateinit var ayudaContextual: ImageButton

    private var listado: MutableList<String> = LinkedList()
    private val listadoUrlVideos: MutableList<String> = LinkedList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_listado, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.frag_listado_list1)
        playerView = view.findViewById(R.id.fraglist_videoplayer)
        mostrar_opciones = view.findViewById(R.id.text_fraglist_showoptions)
        linearLayout = view.findViewById(R.id.layout_fraglistado_option)
        spinnerFilter = view.findViewById(R.id.spinner_fraglistado)
        searchView = view.findViewById(R.id.searchView_fraglistado)
        searchViewConf = view.findViewById(R.id.searchView_conf_fraglistado)
        videoView = view.findViewById(R.id.fraglist_videoView)
        ayudaContextual = view.findViewById(R.id.frag_listado_ayuda)

        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("help_inline", true)) {
            ayudaContextual.visibility = View.VISIBLE
        } else {
            ayudaContextual.visibility = View.GONE
        }
        ayudaContextual.setOnClickListener { ShowAyudaContextual(requireContext()) }

        playerView.layoutParams.height = 0
        playerView.requestLayout()
        playerView.visibility = View.VISIBLE

        val navController = Navigation.findNavController(view)

        playerView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
            override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
            }
        })

        if (elementLoaded.contains("play_youtube")) {
            mostrar_opciones.visibility = View.INVISIBLE
            ManagerPlayVideo(urlPath)
        } else if (elementLoaded.contains("play_video_repo")) {
            mostrar_opciones.visibility = View.INVISIBLE
            ManagerPlayVideo(urlPath)
        } else {
            mostrar_opciones.visibility = View.VISIBLE
            GenerarListado()
        }

        if (elementLoaded.equals("conf", ignoreCase = true)) {
            searchViewConf.visibility = View.VISIBLE
        } else {
            searchViewConf.visibility = View.GONE
        }

        myListAdapterItemsList = MyListAdapterItemsList(requireContext(), R.layout.row_list_item, listado)
        listView.adapter = myListAdapterItemsList

        mostrar_opciones.setOnClickListener {
            if (mostrar_opciones.text.toString().contains("Mostrar Opciones")) {
                linearLayout.visibility = View.VISIBLE
                mostrar_opciones.setText(R.string.ocultar_opciones)
            } else {
                linearLayout.visibility = View.GONE
                mostrar_opciones.setText(R.string.mostrar_opciones)
            }
        }

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, l: Long) {
                val itemSelected = adapterView.selectedItem.toString()
                val dbManager = DBManager(requireContext()).open()
                myListAdapterItemsList?.clear()

                when (itemSelected) {
                    "Todas" -> {
                        val sql = when (elementLoaded) {
                            "conf" -> "SELECT title FROM ${DatabaseHelper.T_Conf};"
                            "video_conf" -> "SELECT title FROM ${DatabaseHelper.T_Videos};"
                            "video_ext" -> "SELECT title FROM ${DatabaseHelper.T_Repo} WHERE ${DatabaseHelper.C_repo_type}='video';"
                            "audio_ext" -> "SELECT title FROM ${DatabaseHelper.T_Repo} WHERE ${DatabaseHelper.C_repo_type}='audio';"
                            "video_gredd" -> "SELECT title FROM ${DatabaseHelper.T_Videos} WHERE ${DatabaseHelper.C_videos_type}='gregg';"
                            else -> ""
                        }
                        if (sql.isNotEmpty()) {
                            val cursor = dbManager.ejectSQLRawQuery(sql)
                            if (cursor.moveToFirst()) {
                                listado.clear()
                                do {
                                    listado.add(cursor.getString(0))
                                } while (cursor.moveToNext())
                            }
                            cursor.close()
                        }
                    }
                    "Favoritos" -> {
                        val cursor = when (elementLoaded) {
                            "conf" -> dbManager.getListado("Conferencias favoritas")
                            "video_conf" -> dbManager.getListado("Videos inbuilt favoritos")
                            "video_ext" -> dbManager.getListado("Videos offline favoritos")
                            "audio_ext" -> dbManager.getListado("Audios offline favoritos")
                            "video_gredd" -> dbManager.getListado("Videos gregg favoritos")
                            else -> null
                        }
                        if (cursor != null && cursor.moveToFirst()) {
                            listado.clear()
                            listadoUrlVideos.clear()
                            do {
                                listado.add(cursor.getString(1))
                                if (cursor.columnCount > 2) {
                                    listadoUrlVideos.add(cursor.getString(2))
                                }
                            } while (cursor.moveToNext())
                            cursor.close()
                        }
                    }
                    "Con notas" -> {
                        val cursor = when (elementLoaded) {
                            "conf" -> dbManager.getListado("Conferencias con notas")
                            "video_conf" -> dbManager.getListado("Videos inbuilt con notas")
                            else -> null
                        }
                        if (cursor != null && cursor.moveToFirst()) {
                            listado.clear()
                            do {
                                listado.add(cursor.getString(1))
                            } while (cursor.moveToNext())
                            cursor.close()
                        }
                    }
                }
                myListAdapterItemsList?.notifyDataSetChanged()
                dbManager.close()
                searchView.queryHint = "Buscar en títulos(${listado.size} elementos)"
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        searchView.setOnSearchClickListener { searchView.queryHint = "Buscar en títulos(${listado.size} elementos)" }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(texto: String): Boolean = true
            override fun onQueryTextChange(texto: String): Boolean {
                myListAdapterItemsList?.filter?.filter(texto)
                return true
            }
        })

        searchView.setOnCloseListener {
            spinnerFilter.performClick()
            true
        }

        searchViewConf.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    try {
                        myListAdapterItemsList?.clear()
                        listado = Utils.searchInConf(requireContext(), query).toMutableList()
                        myListAdapterItemsList?.addAll(listado)
                        myListAdapterItemsList?.notifyDataSetChanged()
                    } catch (e: IOException) {
                        Toast.makeText(requireContext(), "No se pudo realizar la búsqueda", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    myListAdapterItemsList?.clear()
                    listado = utilsDB.loadConferenciaList(requireContext()).toMutableList()
                    myListAdapterItemsList?.addAll(listado)
                    myListAdapterItemsList?.notifyDataSetChanged()
                }
                return true
            }
        })

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val selectedItemText = parent.getItemAtPosition(position) as String
            playerView.visibility = View.GONE
            videoView.visibility = View.GONE

            when (elementLoaded) {
                "video_gredd", "video_conf", "video_book" -> {
                    if (Utils.isConnection(requireContext())) {
                        val dbManager = DBManager(requireContext()).open()
                        playerView.visibility = View.VISIBLE
                        utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                        playVideo(dbManager.getDbInfoFromItem(selectedItemText, DatabaseHelper.T_Videos))
                        dbManager.close()
                        handlefavState()
                    }
                }
                "conf" -> {
                    frag_content_WebView.elementLoaded = "conf"
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    frag_content_WebView.urlPath = "file:///android_asset/conf/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "preguntas" -> {
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    frag_content_WebView.urlPath = "file:///android_asset/preg/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "citasConferencias" -> {
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    frag_content_WebView.urlPath = "file:///android_asset/cita/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "ayudas" -> {
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    frag_content_WebView.urlPath = "file:///android_asset/ayuda/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "video_ext" -> {
                    if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("play_video_background", false)) {
                        Utils.playInStreaming(requireContext(), utilsFields.REPO_DIR_VIDEOS, selectedItemText)
                    } else {
                        videoView.visibility = View.VISIBLE
                        playRepo(utilsFields.REPO_DIR_VIDEOS, selectedItemText)
                    }
                }
                "audio_ext" -> {
                    if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("play_audio_background", false)) {
                        Utils.playInStreaming(requireContext(), utilsFields.REPO_DIR_AUDIOS, selectedItemText)
                    } else {
                        videoView.visibility = View.VISIBLE
                        playRepo(utilsFields.REPO_DIR_AUDIOS, selectedItemText)
                    }
                }
            }
        }
    }

    private fun ManagerPlayVideo(urlVideo: String) {
        if (urlVideo.isNotEmpty()) {
            Handler(Looper.getMainLooper()).postDelayed({
                when (elementLoaded) {
                    "play_youtube", "video_conf" -> {
                        playerView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE
                        playVideo(urlVideo)
                    }
                    "video_ext", "play_video_repo" -> {
                        playerView.visibility = View.GONE
                        videoView.visibility = View.VISIBLE
                        playRepo(utilsFields.REPO_DIR_VIDEOS, urlVideo)
                    }
                }
            }, 1300)
        }
    }

    private fun playVideo(urlVideo: String) {
        playerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        playerView.requestLayout()
        player?.loadVideo(urlVideo, 0f)

        MainActivity.mainActivityThis?.let {
            if (!it.isFinishing) {
                it.ic_toolsBar_fav.visibility = View.VISIBLE
                val favstate = utilsDB.readFavState(requireContext(), DatabaseHelper.T_Videos, DatabaseHelper.C_videos_link, utilsFields.ID_Str_row_ofElementLoad)
                it.setFavColor(favstate)
            }
        }
    }

    private fun playRepo(RepoDir: String, urlVideo: String) {
        utilsFields.ID_Str_row_ofElementLoad = urlVideo
        videoView.setVideoPath("/sdcard/${utilsFields.REPO_DIR_ROOT}/$RepoDir/$urlVideo")
        videoView.setMediaController(MediaController(requireContext()))
        videoView.requestFocus()
        videoView.start()

        MainActivity.mainActivityThis?.let {
            it.ic_toolsBar_fav.visibility = View.VISIBLE
            val favstate = utilsDB.readFavState(requireContext(), DatabaseHelper.T_Repo, DatabaseHelper.C_repo_title, utilsFields.ID_Str_row_ofElementLoad)
            it.setFavColor(favstate)
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.mainActivityThis?.let {
            it.ic_toolsBar_fav.setColorFilter(requireContext().resources.getColor(R.color.black, null))
            it.ic_toolsBar_fav.visibility = View.GONE
            it.ic_toolsBar_frase_add.visibility = View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        playerView.layoutParams.height = 0
        playerView.requestLayout()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.release()
    }

    private fun GenerarListado() {
        val utils = Utils(requireContext())
        when (elementLoaded) {
            "conf" -> listado = utilsDB.loadConferenciaList(requireContext()).toMutableList()
            "preguntas" -> try { utils.listFilesInAssets("preg", listado) } catch (e: IOException) { e.printStackTrace() }
            "citasConferencias" -> try { utils.listFilesInAssets("cita", listado) } catch (e: IOException) { e.printStackTrace() }
            "video_conf" -> utilsDB.LoadVideoList(requireContext(), "conf", listadoUrlVideos, listado)
            "video_book" -> utilsDB.LoadVideoList(requireContext(), "audioLibro", listadoUrlVideos, listado)
            "ayudas" -> try { utils.listFilesInAssets("ayuda", listado) } catch (e: IOException) { e.printStackTrace() }
            "video_gredd" -> utilsDB.LoadVideoList(requireContext(), "gregg", listadoUrlVideos, listado)
            "video_ext" -> if (utilsDB.LoadRepoFromDB(requireContext(), "video", listado) == 0) Utils.loadRepo(requireContext())
            "audio_ext" -> if (utilsDB.LoadRepoFromDB(requireContext(), "audio", listado) == 0) Utils.loadRepo(requireContext())
        }
    }

    private fun handlefavState() {
        val favState = utilsDB.readFavState(requireContext(), DatabaseHelper.T_Videos, DatabaseHelper.C_videos_title, utilsFields.ID_Str_row_ofElementLoad)
        MainActivity.mainActivityThis?.setFavColor(favState)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun ShowAyudaContextual(context: Context) {
        val helpBalloon = HelpBalloon(requireContext())
        val balloon1 = helpBalloon.buildFactory("Añadir un apunte", viewLifecycleOwner)
        val balloon2 = helpBalloon.buildFactory("filtro de listado", viewLifecycleOwner)
        val balloon3 = helpBalloon.buildFactory("listado de elementos. Toque un elemento para abrirlo", viewLifecycleOwner)

        balloon1
            .relayShowAlignBottom(balloon2, mostrar_opciones)
            .relayShowAlignTop(balloon3, listView)

        MainActivity.mainActivityThis?.let {
            balloon1.showAlignBottom(it.ic_toolsBar_nota_add)
        }
    }

    companion object {
        @JvmField
        var elementLoaded = ""
        @JvmField
        var urlPath = ""
        @JvmField
        var extension = ""
        @JvmField
        var urlDirAssets = ""
    }
}
