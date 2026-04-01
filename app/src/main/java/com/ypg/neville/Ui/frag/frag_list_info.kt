package com.ypg.neville.ui.frag

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.UiModalWindows
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

        showHideAyudaContextual()

        ayudaContextual.setOnClickListener { showAyudaContextual() }

        val myListAdapterListInfo = MyListAdapterList_info(
            requireContext(),
            R.layout.row_list_info_item,
            listado
        ) { spinner.selectedItem?.toString().orEmpty() }
        list.adapter = myListAdapterListInfo

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val itemtxt = parentView.getItemAtPosition(position).toString()
                utilsFields.spinnerListInfoItemSelected = itemtxt
                myListAdapterListInfo.clear()
                listado.clear()
                listado.addAll(utilsDB.getListadoTitles(requireContext(), itemtxt))
                myListAdapterListInfo.notifyDataSetChanged()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, _, position, _ ->
            val itemText = parent.getItemAtPosition(position).toString()

            when (spinner.selectedItem.toString()) {
                "Frases inbuilt favoritas", "Frases personales favoritas", "Frases inbuilt" -> {
                    UiModalWindows.NotaManager(requireContext(), utilsDB.getFraseNota(requireContext(), itemText), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, itemText)
                }
                "Conferencias favoritas", "Conferencias con notas" -> {
                    UiModalWindows.NotaManager(requireContext(), utilsDB.getConfNota(requireContext(), itemText), DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, itemText)
                }
            }
            true
        }

        list.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view1, position, _ ->
            val itemText = adapterView.getItemAtPosition(position).toString()
            val navController = view1.findNavController()

            when (spinner.selectedItem.toString()) {
                "Frases inbuilt favoritas" -> {
                    val alert = AlertDialog.Builder(requireContext())
                    alert.setTitle("¿Dejar de ser favorita?")
                    alert.setPositiveButton("Quitar favorito") { _, _ ->
                        utilsDB.UpdateFavorito(requireContext(), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, itemText, -1)
                        listado.clear()
                        listado.addAll(utilsDB.getListadoTitles(requireContext(), utilsFields.spinnerListInfoItemSelected))
                        myListAdapterListInfo.clear()
                        myListAdapterListInfo.addAll(listado)
                        myListAdapterListInfo.notifyDataSetChanged()
                    }
                    alert.show()
                }
                "Frases personales", "Frases inbuilt con notas", "Frases personales con notas" -> {
                    UiModalWindows.NotaManager(requireContext(), utilsDB.getFraseNota(requireContext(), itemText), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, itemText)
                }
                "Conferencias favoritas", "Conferencias con notas" -> {
                    utilsFields.ID_Str_row_ofElementLoad = itemText
                    FragContentWebView.extension = ".txt"
                    FragContentWebView.urlDirAssets = "conf"
                    FragContentWebView.urlPath = "file:///android_asset/${FragContentWebView.urlDirAssets}/$itemText${FragContentWebView.extension}"
                    navController.navigate(R.id.frag_content_webview)
                }
                "Apuntes" -> {
                    UiModalWindows.ApunteManager(requireContext(), itemText, null, true)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MainActivity.currentInstance()?.ic_toolsBar_frase_add?.visibility = View.VISIBLE }
        runCatching { MainActivity.currentInstance()?.ic_toolsBar_fav?.visibility = View.GONE }
    }

    private fun showHideAyudaContextual() {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("help_inline", true)) {
            ayudaContextual.visibility = View.VISIBLE
        } else {
            ayudaContextual.visibility = View.GONE
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showAyudaContextual() {
        val helpBalloon = HelpBalloon(requireContext())
        val balloon1 = helpBalloon.buildFactory("Añadir un apunte", viewLifecycleOwner)
        val balloon2 = helpBalloon.buildFactory("Añadir una frase", viewLifecycleOwner)
        val balloon3 = helpBalloon.buildFactory("lista de elementos a filtrar", viewLifecycleOwner)
        val balloon4 = helpBalloon.buildFactory("Listado de elementos. Toque un elemento para abrirlo. Toque largo sobre un elemento para más opciones", viewLifecycleOwner)

        balloon1
            .relayShowAlignBottom(balloon2, MainActivity.currentInstance()!!.ic_toolsBar_frase_add)
            .relayShowAlignTop(balloon3, spinner)
            .relayShowAlignBottom(balloon4, list)

        balloon1.showAlignBottom(MainActivity.currentInstance()!!.ic_toolsBar_nota_add)
    }
}
