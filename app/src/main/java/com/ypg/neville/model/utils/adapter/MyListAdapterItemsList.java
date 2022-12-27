package com.ypg.neville.model.utils.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ypg.neville.R;

import java.util.List;



//Adaptador para list en frag_list
public  class MyListAdapterItemsList extends ArrayAdapter<String> {

        private final int layout;

    public MyListAdapterItemsList(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.layout = resource;
    }

    @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder mainViewholder = null;

            if(convertView == null){
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                ViewHolder viewHolder = new ViewHolder(); //Clase que contendrá los elementos de cada file del listview
                viewHolder.textConf = convertView.findViewById(R.id.row_list_text_conf);

                viewHolder.textConf.setText(getItem(position));

                convertView.setTag(viewHolder);

            }else{
                mainViewholder = (ViewHolder) convertView.getTag();
                mainViewholder.textConf.setText(getItem(position));
            }

            return convertView;
        }



    //Clase utilitaria que mantendrá los elementos clickeable de cada fila
    private class ViewHolder {
        TextView textConf;
    }



}





