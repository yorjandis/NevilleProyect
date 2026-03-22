package com.ypg.neville.model.utils.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.ypg.neville.R

// Adaptador para list en frag_list
class MyListAdapterItemsList(context: Context, private val layout: Int, objects: List<String>) :
    ArrayAdapter<String>(context, layout, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(layout, parent, false)
            holder = ViewHolder()
            holder.textConf = view.findViewById(R.id.row_list_text_conf)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        holder.textConf?.text = getItem(position)

        return view!!
    }

    // Clase utilitaria que mantendrá los elementos clickeables de cada fila
    private class ViewHolder {
        var textConf: TextView? = null
    }
}
