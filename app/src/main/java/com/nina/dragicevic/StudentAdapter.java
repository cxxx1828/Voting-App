package com.nina.dragicevic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class StudentAdapter extends BaseAdapter {

    private Context mContext;

    private ArrayList<Student> mList;

    public StudentAdapter(Context mContext, ArrayList<Student> mList) {
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

    public void addElement(Student el){
        mList.add(el);
        notifyDataSetChanged();
    }

    public void removeElement(Student el){
        mList.remove(el);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_layout, null);
            vh = new ViewHolder();
            vh.ime_prezime = convertView.findViewById(R.id.ime_prezime);
            vh.index = convertView.findViewById(R.id.indeks);
            vh.slika = convertView.findViewById(R.id.slika1);
            vh.checkBox = convertView.findViewById(R.id.check);
            convertView.setTag(vh);
        }

        vh = (ViewHolder) convertView.getTag();

        Student s = (Student) getItem(position);

        vh.ime_prezime.setText(s.getImePrezime());
        vh.slika.setImageResource(s.getSlikaResId());
        vh.index.setText(s.getIndex());


        vh.checkBox.setOnCheckedChangeListener(null); // reset listenera
        vh.checkBox.setChecked(false);



        vh.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new androidx.appcompat.app.AlertDialog.Builder(mContext)
                            .setTitle("Delete student")
                            .setMessage("Are you sure you want to delete " + s.getImePrezime() + "  " + s.getIndex() + "?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", (dialog, which) -> {
                                removeElement(s);
                                Toast.makeText(mContext, s.getImePrezime() + " deleted", Toast.LENGTH_SHORT).show();
                                if (mList.isEmpty()) {
                                    Toast.makeText(mContext, "Empty List!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                // koristi parametar buttonView umesto vh
                                buttonView.setOnCheckedChangeListener(null);
                                buttonView.setChecked(false);
                                buttonView.setOnCheckedChangeListener(this);
                                dialog.dismiss();
                            })
                            .show();
                }
            }
        });


        return convertView;
    }

    private class ViewHolder{
        ImageView slika;
        TextView ime_prezime;
        TextView index;
        CheckBox checkBox;
    }

}
