package com.nina.dragicevic;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SessionAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Session> mList;

    public SessionAdapter(Context mContext, ArrayList<Session> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        if(position < 0 || position >= mList.size()){
            return null;
        } else {
            return mList.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addElement(Session el){
        mList.add(el);
        notifyDataSetChanged();
    }

    public void removeElement(Session el){
        mList.remove(el);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.row_layout2, parent, false);
            vh = new ViewHolder();
            vh.naziv = convertView.findViewById(R.id.naziv);
            vh.datum = convertView.findViewById(R.id.datum);
            vh.atribut = convertView.findViewById(R.id.atribut);
            convertView.setTag(vh);
        }
        vh = (ViewHolder) convertView.getTag();


        Session s = (Session) getItem(position);
        if (s != null) {
            vh.naziv.setText(s.getNaziv());
            vh.datum.setText(s.getDatum());
            vh.atribut.setText(s.getAtribut());

            switch (s.getAtribut()) {
                case "PAST":
                    vh.atribut.setTextColor(Color.RED);
                    break;
                case "UPCOMING":
                    vh.atribut.setTextColor(Color.BLUE);
                    break;
                default:
                    vh.atribut.setTextColor(Color.BLACK);
                    break;
            }
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView datum;
        TextView naziv;
        TextView atribut;
    }
}
