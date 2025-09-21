package com.nina.dragicevic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CalendarFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CalendarFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private DecideItDbHelper dbHelper;
    private ExecutorService executor;
    private Handler mainHandler;

    public CalendarFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CalendarFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CalendarFragment newInstance(String param1, String param2) {
        CalendarFragment fragment = new CalendarFragment();
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
        dbHelper = new DecideItDbHelper(getContext(), "decideit.db", null, 2);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view1 = inflater.inflate(R.layout.fragment_calendar, container, false);

        CalendarView calendarView = view1.findViewById(R.id.calendarView);

        calendarView.setMinDate(1734220800000L);

        // Sync sessions from server when fragment loads
        syncSessionsFromServer();

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Log.d("CALENDAR_DEBUG", "Date selected -> " + dayOfMonth + "." + (month + 1) + "." + year);
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth);
                String selectedDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(c.getTime());

                Log.d("CALENDAR_DEBUG", "Formatted selectedDate: " + selectedDate);

                //provera da li vec postoji sesija za datum
                ArrayList<String> sessionDates = dbHelper.getSessionDates();
                Log.d("CALENDAR_DEBUG", "Loaded " + sessionDates.size() + " session dates from DB");

                if (sessionDates.contains(selectedDate)) {
                    Log.d("CALENDAR_DEBUG", "Session found for date: " + selectedDate);
                    Session session = dbHelper.getSessionByDate(selectedDate);

                    Intent intent = new Intent(getActivity(), DecideActivity.class);
                    intent.putExtra("sessionName", session.getNaziv());
                    intent.putExtra("sessionDate", selectedDate);
                    intent.putExtra("sessionDescription", "Session description");
                    startActivity(intent);
                    Log.d("CALENDAR_DEBUG", "DecideActivity started with sessionName: " +
                            session.getNaziv() + ", sessionDate: " + selectedDate);
                } else {
                    Log.d("CALENDAR_DEBUG", "No session available for date: " + selectedDate);
                    Toast.makeText(getContext(), "No session available for this date", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Syncs sessions from server to local database
     */
    private void syncSessionsFromServer() {
        Log.d("CALENDAR_DEBUG", "Starting sync sessions from server...");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d("CALENDAR_DEBUG", "Background thread: syncing sessions from server");

                // Sync sessions from server to local database
                boolean syncSuccess = dbHelper.syncSessionsFromServer();
                Log.d("CALENDAR_DEBUG", "Sync from server result: " + syncSuccess);

                // Update UI on main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (syncSuccess) {
                            Log.d("CALENDAR_DEBUG", "Successfully synced sessions from server");
                        } else {
                            Log.d("CALENDAR_DEBUG", "Failed to sync sessions from server (may be offline)");
                        }
                    }
                });
            }
        });
    }

    /**
     * Public method to refresh sessions (can be called from parent activity)
     */
    public void refreshSessions() {
        Log.d("CALENDAR_DEBUG", "Public refreshSessions called");
        syncSessionsFromServer();
    }
}