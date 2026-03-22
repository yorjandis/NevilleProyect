package com.ypg.neville.Ui.frag

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DBManager
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.adapter.MyListAdapterList_info
import com.ypg.neville.model.utils.balloon.HelpBalloon
import com.ypg.neville.model.utils.utilsFields
import java.util.LinkedList

class frag_list_info : Fragment() {

    var listado: MutableList<String> = LinkedList()
    private lateinit var ayudaContextual: ImageButton
    private lateinit var spinner: Spinner
    private lateinit var list: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_list_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinner = view.findViewById(R.id.frag_listinfo_spinne_filtro)
        list = view.findViewById(R.id.frag_listinfo_list)
        ayudaContextual = view.findViewById(R.id.frag_list_info_ayuda)

        spinnerStatic = spinner

        showHideAyudaContextual()

        ayudaContextual.setOnClickListener { ShowAyudaContextual(requireContext()) }

        val myListAdapterList_info = MyListAdapterList_info(requireContext(), R.layout.row_list_info_item, listado)
        list.adapter = myListAdapterList_info

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val itemtxt = parentView.getItemAtPosition(position).toString()
                utilsFields.spinnerListInfoItemSelected = itemtxt
                myListAdapterList_info.clear()

                val dbManager = DBManager(requireContext()).open()
                val cursor = dbManager.getListado(itemtxt)

                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast) {
                        listado.add(cursor.getString(1))
                        cursor.moveToNext()
                    }
                }
                myListAdapterList_info.notifyDataSetChanged()
                cursor.close()
                dbManager.close()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, _, position, _ ->
            val itemText = parent.getItemAtPosition(position).toString()
            val dbManager = DBManager(requireContext()).open()
            var query = ""

            when (spinner.selectedItem.toString()) {
                "Frases inbuilt favoritas", "Frases personales favoritas", "Frases inbuilt" -> {
                    query = "SELECT nota FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.C_frases_frase}='$itemText';"
                    val cursor = dbManager.ejectSQLRawQuery(query)
                    if (cursor.moveToFirst()) {
                        UiModalWindows.NotaManager(requireContext(), cursor.getString(0), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, itemText)
                    }
                    cursor.close()
                }
                "Conferencias favoritas", "Conferencias con notas" -> {
                    query = "SELECT nota FROM ${DatabaseHelper.T_Conf} WHERE ${DatabaseHelper.C_conf_title}='$itemText';"
                    val cursor = dbManager.ejectSQLRawQuery(query)
                    if (cursor.moveToFirst()) {
                        UiModalWindows.NotaManager(requireContext(), cursor.getString(0), DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, itemText)
                    }
                    cursor.close()
                }
                "Videos inbuilt favoritos", "Videos inbuilt con notas" -> {
                    query = "SELECT nota FROM ${DatabaseHelper.T_Videos} WHERE ${DatabaseHelper.C_videos_title}='$itemText';"
                    val cursor = dbManager.ejectSQLRawQuery(query)
                    if (cursor.moveToFirst()) {
                        UiModalWindows.NotaManager(requireContext(), cursor.getString(0), DatabaseHelper.T_Videos, DatabaseHelper.C_videos_title, itemText)
                    }
                    cursor.close()
                }
                "Videos offline favoritos", "Videos offline con notas" -> {
                    query = "SELECT nota FROM ${DatabaseHelper.T_Repo} WHERE ${DatabaseHelper.C_repo_title}='$itemText';"
                    val cursor = dbManager.ejectSQLRawQuery(query)
                    if (cursor.moveToFirst()) {
                        UiModalWindows.NotaManager(requireContext(), cursor.getString(0), DatabaseHelper.T_Repo, DatabaseHelper.C_repo_title, itemText)
                    }
                    cursor.close()
                }
            }
            dbManager.close()
            true
        }

        list.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view1, position, _ ->
            val itemText = adapterView.getItemAtPosition(position).toString()
            val dbManager = DBManager(requireContext()).open()
            val navController = Navigation.findNavController(view1)

            when (spinner.selectedItem.toString()) {
                "Frases inbuilt favoritas" -> {
                    val alert = AlertDialog.Builder(requireContext())
                    alert.setTitle("¿Dejar de ser favorita?")
                    alert.setPositiveButton("Quitar favorito") { _, _ ->
                        utilsDB.UpdateFavorito(requireContext(), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, itemText, -1)
                        listado.clear()
                        val cursor = dbManager.getListado(utilsFields.spinnerListInfoItemSelected)
                        if (cursor.moveToFirst()) {
                            do {
                                listado.add(cursor.getString(1))
                            } while (cursor.moveToNext())
                        }
                        myListAdapterList_info.clear()
                        myListAdapterList_info.addAll(listado)
                        myListAdapterList_info.notifyDataSetChanged()
                        cursor.close()
                        dbManager.close()
                    }
                    alert.show()
                }
                "Frases personales", "Frases inbuilt con notas", "Frases personales con notas" -> {
                    val query = "SELECT nota FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.C_frases_frase}='$itemText';"
                    val cursor = dbManager.ejectSQLRawQuery(query)
                    if (cursor.moveToFirst()) {
                        UiModalWindows.NotaManager(requireContext(), cursor.getString(0), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, itemText)
                    }
                    cursor.close()
                }
                "Conferencias favoritas", "Conferencias con notas" -> {
                    utilsFields.ID_Str_row_ofElementLoad = itemText
                    frag_content_WebView.extension = ".txt"
                    frag_content_WebView.urlDirAssets = "conf"
                    frag_content_WebView.urlPath = "file:///android_asset/${frag_content_WebView.urlDirAssets}/$itemText${frag_content_WebView.extension}"
                    navController.navigate(R.id.frag_content_webview)
                }
                "Videos inbuilt favoritos", "Videos inbuilt con notas" -> {
                    if (Utils.isConnection(requireContext())) {
                        frag_listado.elementLoaded = "play_youtube"
                        val temp = dbManager.getDbInfoFromItem(itemText, DatabaseHelper.T_Videos)
                        if (temp.isNotEmpty()) {
                            frag_listado.urlPath = temp
                            navController.navigate(R.id.frag_listado)
                        } else {
                            Toast.makeText(context, "Error al cargar el video", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                "Apuntes" -> {
                    UiModalWindows.ApunteManager(requireContext(), itemText, null, true)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            MainActivity.mainActivityThis?.ic_toolsBar_frase_add?.visibility = View.VISIBLE
        } catch (ignored: Exception) {
        }
        try {
            MainActivity.mainActivityThis?.ic_toolsBar_fav?.visibility = View.GONE
        } catch (ignored: Exception) {
        }
    }

    private fun showHideAyudaContextual() {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("help_inline", true)) {
            ayudaContextual.visibility = View.VISIBLE
        } else {
            ayudaContextual.visibility = View.GONE
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun ShowAyudaContextual(context: Context) {
        val helpBalloon = HelpBalloon(requireContext())
        val balloon1 = helpBalloon.buildFactory("Añadir un apunte", viewLifecycleOwner)
        val balloon2 = helpBalloon.buildFactory("Añadir una frase", viewLifecycleOwner)
        val balloon3 = helpBalloon.buildFactory("lista de elementos a filtrar", viewLifecycleOwner)
        val balloon4 = helpBalloon.buildFactory("Listado de elementos. Toque un elemento para abrirlo. Toque largo sobre un elemento para más opciones", viewLifecycleOwner)

        balloon1
            .relayShowAlignBottom(balloon2, MainActivity.mainActivityThis!!.ic_toolsBar_frase_add)
            .relayShowAlignTop(balloon3, spinner)
            .relayShowAlignBottom(balloon4, list)

        balloon1.showAlignBottom(MainActivity.mainActivityThis!!.ic_toolsBar_nota_add)
    }

    companion object {
        @JvmField
        var spinnerStatic: Spinner? = null
    }
}
