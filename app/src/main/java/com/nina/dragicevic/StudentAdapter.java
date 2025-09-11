package com.nina.dragicevic;

import android.content.Context;
import android.util.Log;
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
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

public class StudentAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Student> mList;
    private StudentListFragment fragment;

    public StudentAdapter(Context mContext, ArrayList<Student> mList, StudentListFragment fragment) {
        this.mContext = mContext;
        this.mList = mList;
        this.fragment = fragment;
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

    public void clear(){
        mList.clear();
        notifyDataSetChanged();
    }

    public void update(Student[] students) {
        mList.clear();
        if(students != null) {
            for(Student student : students) {
                mList.add(student);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_layout, null);
            vh = new ViewHolder();
            vh.ime = convertView.findViewById(R.id.ime_prezime);
            vh.index = convertView.findViewById(R.id.indeks);
            vh.slika = convertView.findViewById(R.id.slika1);
            vh.checkBox = convertView.findViewById(R.id.check);
            vh.prezime = convertView.findViewById(R.id.surname);
            convertView.setTag(vh);
        }

        vh = (ViewHolder) convertView.getTag();
        Student s = (Student) getItem(position);

        if (s != null) {
            vh.ime.setText(s.getIme());
            vh.slika.setImageResource(s.getSlikaResId());
            vh.index.setText(s.getIndex());
            vh.prezime.setText(s.getPrezime());

            vh.checkBox.setOnCheckedChangeListener(null);
            vh.checkBox.setChecked(false);

            vh.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("Delete Student")
                                .setMessage("Are you sure you want to delete " + s.getIme() + " " + s.getPrezime() + " (Index: " + s.getIndex() + ")?")
                                .setCancelable(false)
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    if (fragment != null) {
                                        fragment.deleteStudent(s);
                                        Toast.makeText(mContext, s.getIme() + " " + s.getPrezime() + " deleted successfully", Toast.LENGTH_SHORT).show();
                                    }
                                    Log.e("STUDENT_ADAPTER", "Fragment is null, cannot delete from database!");
                                    Toast.makeText(mContext, s.getIme() + " " + s.getPrezime() + " deleted successfully", Toast.LENGTH_SHORT).show();
                                    if (mList.isEmpty()) {
                                        Toast.makeText(mContext, "Student list is now empty", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    buttonView.setOnCheckedChangeListener(null);
                                    buttonView.setChecked(false);
                                    buttonView.setOnCheckedChangeListener(this);
                                    dialog.dismiss();
                                })
                                .show();
                    }
                }
            });
        }

        return convertView;
    }

    private class ViewHolder{
        ImageView slika;
        TextView ime;
        TextView prezime;
        TextView index;
        CheckBox checkBox;
    }
}