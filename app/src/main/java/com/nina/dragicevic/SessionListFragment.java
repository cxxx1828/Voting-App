package com.nina.dragicevic;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;


public class SessionListFragment extends Fragment {

    ListView lista;
    TextView emptyView;
    SessionAdapter adapter;
    CalendarView k;
    Button d;


    private long selectedDateMillis = 0;
    private int sessionCounter = 1;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public SessionListFragment() { }

    public static SessionListFragment newInstance(String param1, String param2) {
        SessionListFragment fragment = new SessionListFragment();
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

        View view = inflater.inflate(R.layout.fragment_session_list, container, false);


        lista = view.findViewById(R.id.lista4);
        emptyView = view.findViewById(R.id.emptyView2);
        k = view.findViewById(R.id.kal);
        d = view.findViewById(R.id.butn);

        adapter = new SessionAdapter(getContext(), new ArrayList<>());
        lista.setAdapter(adapter);
        lista.setEmptyView(emptyView);


        k.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view1, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth, 0, 0, 0);
                selectedDateMillis = c.getTimeInMillis();
            }
        });

        d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDateMillis == 0) {
                    selectedDateMillis = k.getDate();
                }

                Date chosenDate = new Date(selectedDateMillis);
                String dateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(chosenDate);


                String sessionName = "Session " + sessionCounter++;

                String status;
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                if (selectedDateMillis >= today.getTimeInMillis()) {
                    status = "UPCOMING";
                } else {
                    status = "PAST";
                }


                Session newSession = new Session(sessionName, dateStr, status);
                adapter.addElement(newSession);
            }
        });


        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
                Session clicked = (Session) adapter.getItem(position);

                Intent intent = new Intent(getActivity(), ResultsActivity.class);
                intent.putExtra("sessionName", clicked.getNaziv());
                intent.putExtra("sessionDate", clicked.getDatum());
                intent.putExtra("sessionStatus", clicked.getAtribut());
                startActivity(intent);
            }
        });

        return view;
    }
}