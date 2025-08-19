package com.nina.dragicevic;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StudentListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StudentListFragment extends Fragment {

    ListView lista;

    TextView emptyView;
    ImageView slika;
    StudentAdapter adapter;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public StudentListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StudentListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StudentListFragment newInstance(String param1, String param2) {
        StudentListFragment fragment = new StudentListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_student_list, container, false);


        lista = view.findViewById(R.id.lista);
        emptyView = view.findViewById(R.id.emptyView);


        lista.setEmptyView(emptyView);


        ArrayList<Student> studenti = new ArrayList<>();
        studenti.add(new Student(R.drawable.male_student, "Marinko Maric", "RA123/2016", false));
        studenti.add(new Student(R.drawable.female_student, "Ana Anić", "RA25/2022", false));
        studenti.add(new Student(R.drawable.male_student, "Jovan Jovanović", "RA3/2022", false));
        studenti.add(new Student(R.drawable.female_student, "Milica Milić", "RA55/2021", false));
        studenti.add(new Student(R.drawable.male_student, "Petar Petrović", "RA101/2019", false));
        studenti.add(new Student(R.drawable.female_student, "Marija Marić", "RA202/2020", false));
        studenti.add(new Student(R.drawable.male_student, "Nikola Nikolić", "RA303/2018", false));
        studenti.add(new Student(R.drawable.female_student, "Jelena Jelić", "RA404/2021", false));
        studenti.add(new Student(R.drawable.male_student, "Aleksandar Aleksić", "RA505/2022", false));
        studenti.add(new Student(R.drawable.female_student, "Ivana Ivanović", "RA606/2020", false));
        studenti.add(new Student(R.drawable.male_student, "Miloš Milošević", "RA707/2017", false));
        studenti.add(new Student(R.drawable.female_student, "Sofija Sofić", "RA808/2019", false));
        studenti.add(new Student(R.drawable.male_student, "Vuk Vukić", "RA909/2021", false));
        studenti.add(new Student(R.drawable.female_student, "Katarina Katić", "RA100/2022", false));
        studenti.add(new Student(R.drawable.male_student, "Luka Lukić", "RA111/2020", false));
        studenti.add(new Student(R.drawable.female_student, "Tamara Tomić", "RA222/2021", false));
        studenti.add(new Student(R.drawable.male_student, "Stefan Stanković", "RA333/2019", false));
        studenti.add(new Student(R.drawable.female_student, "Dragana Dragić", "RA444/2022", false));
        studenti.add(new Student(R.drawable.male_student, "Marko Matić", "RA555/2018", false));
        studenti.add(new Student(R.drawable.female_student, "Natalija Novaković", "RA666/2020", false));
        studenti.add(new Student(R.drawable.male_student, "Filip Filipović", "RA777/2021", false));
        studenti.add(new Student(R.drawable.female_student, "Jovana Jović", "RA888/2019", false));
        studenti.add(new Student(R.drawable.male_student, "Nenad Nenadić", "RA999/2017", false));
        studenti.add(new Student(R.drawable.female_student, "Milena Milićević", "RA112/2022", false));
        studenti.add(new Student(R.drawable.male_student, "Bogdan Bogdanović", "RA223/2016", false));
        studenti.add(new Student(R.drawable.female_student, "Teodora Tešić", "RA334/2020", false));
        studenti.add(new Student(R.drawable.male_student, "Žarko Živković", "RA445/2019", false));
        studenti.add(new Student(R.drawable.female_student, "Sara Sarić", "RA556/2021", false));




        adapter = new StudentAdapter(getContext(), studenti);
        lista.setAdapter(adapter);

        return view;

    }
}