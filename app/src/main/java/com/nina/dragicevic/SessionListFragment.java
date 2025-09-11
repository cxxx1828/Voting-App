package com.nina.dragicevic;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    DecideItDbHelper dbHelper;


    private long selectedDateMillis = 0; // cuvamo izabrani datum
    private int sessionCounter = 1; // brojac sesija

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "SESSION_LIST_DEBUG";

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

        // Inicijalizacija
        lista = view.findViewById(R.id.lista4);
        emptyView = view.findViewById(R.id.emptyView2);
        k = view.findViewById(R.id.kal);
        d = view.findViewById(R.id.butn);

        lista.setEmptyView(emptyView);
        Log.d(TAG, "ListView and EmptyView initialized");

        dbHelper = new DecideItDbHelper(getContext(), "decideit.db", null, 1);
        adapter = new SessionAdapter(getContext(), new ArrayList<>());
        lista.setAdapter(adapter);

        Log.d(TAG, "Adapter set, calling refreshSessionList()");
        refreshSessionList();


        k.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view1, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth, 0, 0, 0);
                selectedDateMillis = c.getTimeInMillis();
                Log.d(TAG, "Date selected from CalendarView -> " + dayOfMonth + "." + (month + 1) + "." + year);
            }
        });

        // klik na SUBMIT
        d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Submit button clicked");
                if (selectedDateMillis == 0) {
                    Log.d(TAG, "No date manually selected, taking current CalendarView date");
                    selectedDateMillis = k.getDate();
                }

                Date chosenDate = new Date(selectedDateMillis);
                String dateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(chosenDate);
                String sessionName = "Session " + sessionCounter++;
                Log.d(TAG, "Creating new session -> name: " + sessionName + ", date: " + dateStr);

                // u dbhelper radim upcoming ili past
                boolean success = dbHelper.insertSession(dateStr, sessionName, "");

                if (success) {
                    Log.d(TAG, "Session successfully inserted into DB");
                    Toast.makeText(getContext(), "Session added successfully", Toast.LENGTH_SHORT).show();
                    refreshSessionList();
                } else {
                    Log.d(TAG, "Session insertion failed (duplicate date?)");
                    Toast.makeText(getContext(), "Only one session per date allowed", Toast.LENGTH_SHORT).show();
                }


            }
        });


        // klik na item u listi -> ResultsActivity
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
                Session clicked = (Session) adapter.getItem(position);

                Log.d(TAG, "List item clicked -> sessionName: " + clicked.getNaziv() +
                        ", sessionDate: " + clicked.getDatum() +
                        ", sessionStatus: " + clicked.getAtribut());

                Intent intent = new Intent(getActivity(), ResultsActivity.class);
                intent.putExtra("sessionName", clicked.getNaziv());      // NAZIV (npr. "Session 2")
                intent.putExtra("sessionDate", clicked.getDatum());      // DATUM (npr. "21.09.2025")
                intent.putExtra("sessionStatus", clicked.getAtribut());
                startActivity(intent);
            }
        });

        return view;
    }



    private void refreshSessionList() {
        Log.d(TAG, "refreshSessionList START");

        Session[] sessions = dbHelper.readSessions();
        adapter.clear();

        if (sessions != null) {
            Log.d(TAG, "Loaded " + sessions.length + " sessions from DB");
            for (Session session : sessions) {
                Log.d(TAG, "Adding session -> name: " + session.getNaziv() +
                        ", date: " + session.getDatum() +
                        ", status: " + session.getAtribut());
                adapter.addElement(session);
            }
        } else {
            Log.d(TAG, "No sessions found in DB (sessions == null)");
        }

        Log.d(TAG, "refreshSessionList END");
    }
}