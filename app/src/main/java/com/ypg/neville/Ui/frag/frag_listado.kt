package com.ypg.neville.ui.frag

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.adapter.MyListAdapterItemsList
import com.ypg.neville.model.utils.balloon.HelpBalloon
import com.ypg.neville.model.utils.utilsFields
import java.io.IOException
import java.util.LinkedList

class frag_listado : Fragment() {

    private lateinit var listView: ListView
    private var myListAdapterItemsList: MyListAdapterItemsList? = null
    private lateinit var mostrar_opciones: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var spinnerFilter: Spinner
    private lateinit var searchView: SearchView
    private lateinit var searchViewConf: SearchView
    private lateinit var ayudaContextual: ImageButton

    private var listado: MutableList<String> = LinkedList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_listado, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.frag_listado_list1)
        mostrar_opciones = view.findViewById(R.id.text_fraglist_showoptions)
        linearLayout = view.findViewById(R.id.layout_fraglistado_option)
        spinnerFilter = view.findViewById(R.id.spinner_fraglistado)
        searchView = view.findViewById(R.id.searchView_fraglistado)
        searchViewConf = view.findViewById(R.id.searchView_conf_fraglistado)
        ayudaContextual = view.findViewById(R.id.frag_listado_ayuda)

        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("help_inline", true)) {
            ayudaContextual.visibility = View.VISIBLE
        } else {
            ayudaContextual.visibility = View.GONE
        }
        ayudaContextual.setOnClickListener { showAyudaContextual() }

        if (elementLoaded.equals("conf", ignoreCase = true)) {
            mostrar_opciones.visibility = View.VISIBLE
            searchViewConf.visibility = View.VISIBLE
            spinnerFilter.visibility = View.VISIBLE
        } else {
            mostrar_opciones.visibility = View.GONE
            linearLayout.visibility = View.GONE
            searchViewConf.visibility = View.GONE
            spinnerFilter.visibility = View.GONE
        }

        GenerarListado()

        val navController = view.findNavController()

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
                if (!elementLoaded.equals("conf", ignoreCase = true)) return

                val itemSelected = adapterView.selectedItem.toString()
                myListAdapterItemsList?.clear()

                when (itemSelected) {
                    "Todas" -> {
                        listado.clear()
                        listado.addAll(utilsDB.getAllConfTitles(requireContext()))
                    }
                    "Favoritos" -> {
                        listado.clear()
                        listado.addAll(utilsDB.getListadoTitles(requireContext(), "Conferencias favoritas"))
                    }
                    "Con notas" -> {
                        listado.clear()
                        listado.addAll(utilsDB.getListadoTitles(requireContext(), "Conferencias con notas"))
                    }
                }
                myListAdapterItemsList?.notifyDataSetChanged()
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
            if (elementLoaded.equals("conf", ignoreCase = true)) {
                spinnerFilter.performClick()
            }
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
                    } catch (_: IOException) {
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

            when (elementLoaded) {
                "conf" -> {
                    FragContentWebView.elementLoaded = "conf"
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    FragContentWebView.urlPath = "file:///android_asset/conf/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "preguntas" -> {
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    FragContentWebView.urlPath = "file:///android_asset/preg/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "citasConferencias" -> {
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    FragContentWebView.urlPath = "file:///android_asset/cita/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                "ayudas" -> {
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    FragContentWebView.urlPath = "file:///android_asset/ayuda/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.currentInstance()?.let {
            it.ic_toolsBar_fav.setColorFilter(requireContext().resources.getColor(R.color.black, null))
            it.ic_toolsBar_fav.visibility = if (elementLoaded.equals("conf", ignoreCase = true)) View.VISIBLE else View.GONE
            it.ic_toolsBar_frase_add.visibility = View.VISIBLE
        }
    }

    private fun GenerarListado() {
        val utils = Utils(requireContext())
        when (elementLoaded) {
            "conf" -> listado = utilsDB.loadConferenciaList(requireContext()).toMutableList()
            "preguntas" -> try { utils.listFilesInAssets("preg", listado) } catch (e: IOException) { e.printStackTrace() }
            "citasConferencias" -> try { utils.listFilesInAssets("cita", listado) } catch (e: IOException) { e.printStackTrace() }
            "ayudas" -> try { utils.listFilesInAssets("ayuda", listado) } catch (e: IOException) { e.printStackTrace() }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showAyudaContextual() {
        val helpBalloon = HelpBalloon(requireContext())
        val balloon1 = helpBalloon.buildFactory("Añadir un apunte", viewLifecycleOwner)
        val balloon2 = helpBalloon.buildFactory("filtro de listado", viewLifecycleOwner)
        val balloon3 = helpBalloon.buildFactory("listado de elementos. Toque un elemento para abrirlo", viewLifecycleOwner)

        balloon1
            .relayShowAlignBottom(balloon2, mostrar_opciones)
            .relayShowAlignTop(balloon3, listView)

        MainActivity.currentInstance()?.let {
            balloon1.showAlignBottom(it.ic_toolsBar_nota_add)
        }
    }

    companion object {
        @JvmField
        var elementLoaded = ""
    }
}
