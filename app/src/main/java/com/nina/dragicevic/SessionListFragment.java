package com.nina.dragicevic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SessionListFragment extends Fragment {

    ListView lista;
    TextView emptyView;
    SessionAdapter adapter;
    CalendarView k;
    Button d;
    DecideItDbHelper dbHelper;
    ExecutorService executor;
    Handler mainHandler;

    private long selectedDateMillis = 0;
    private int sessionCounter = 1;

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

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_session_list, container, false);

        lista = view.findViewById(R.id.lista4);
        emptyView = view.findViewById(R.id.emptyView2);
        k = view.findViewById(R.id.kal);
        d = view.findViewById(R.id.butn);

        lista.setEmptyView(emptyView);
        Log.d(TAG, "ListView and EmptyView initialized");

        dbHelper = new DecideItDbHelper(getContext(), "decideit_v2.db", null, 1);
        adapter = new SessionAdapter(getContext(), new ArrayList<>());
        lista.setAdapter(adapter);

        Log.d(TAG, "Adapter set, calling syncAndRefreshSessionList()");
        syncAndRefreshSessionList();

        k.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view1, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth, 0, 0, 0);
                selectedDateMillis = c.getTimeInMillis();
                Log.d(TAG, "Date selected from CalendarView -> " + dayOfMonth + "." + (month + 1) + "." + year);
            }
        });

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

                // Show loading state
                d.setEnabled(false);
                d.setText("Creating...");

                // Create session on server in background thread
                createSessionOnServer(dateStr, sessionName, "");
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
                intent.putExtra("sessionName", clicked.getNaziv());
                intent.putExtra("sessionDate", clicked.getDatum());
                intent.putExtra("sessionStatus", clicked.getAtribut());
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Syncs sessions from server and refreshes the list
     */
    private void syncAndRefreshSessionList() {
        Log.d(TAG, "syncAndRefreshSessionList START");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: syncing sessions from server");

                // Sync sessions from server to local database
                boolean syncSuccess = dbHelper.syncSessionsFromServer();
                Log.d(TAG, "Sync from server result: " + syncSuccess);

                // Read sessions from local database (updated with server data)
                final Session[] sessions = dbHelper.readSessions();

                // Update UI on main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.clear();

                        if (sessions != null) {
                            Log.d(TAG, "Loaded " + sessions.length + " sessions from local DB");
                            for (Session session : sessions) {
                                Log.d(TAG, "Adding session -> name: " + session.getNaziv() +
                                        ", date: " + session.getDatum() +
                                        ", status: " + session.getAtribut());
                                adapter.addElement(session);
                            }
                        } else {
                            Log.d(TAG, "No sessions found in local DB");
                        }

                        Log.d(TAG, "syncAndRefreshSessionList END");
                    }
                });
            }
        });
    }

    /**
     * Creates a new session on server and refreshes the list
     */
    private void createSessionOnServer(String date, String sessionName, String description) {
        Log.d(TAG, "createSessionOnServer START -> date: " + date + ", name: " + sessionName);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: creating session on server");

                // Create session on server (which also updates local database)
                boolean success = dbHelper.createSessionOnServer(date, sessionName, description);

                // Update UI on main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Reset button state
                        d.setEnabled(true);
                        d.setText(getString(R.string.submit));

                        if (success) {
                            Log.d(TAG, "Session successfully created on server");
                            Toast.makeText(getContext(), "Session added successfully", Toast.LENGTH_SHORT).show();

                            // Refresh the session list to show the new session
                            refreshSessionListFromLocal();
                        } else {
                            Log.d(TAG, "Failed to create session on server");
                            Toast.makeText(getContext(), "Failed to create session. Check network connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * Refreshes the session list from local database only (faster for UI updates)
     */
    private void refreshSessionListFromLocal() {
        Log.d(TAG, "refreshSessionListFromLocal START");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final Session[] sessions = dbHelper.readSessions();

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.clear();

                        if (sessions != null) {
                            Log.d(TAG, "Refreshed " + sessions.length + " sessions from local DB");
                            for (Session session : sessions) {
                                adapter.addElement(session);
                            }
                        }

                        Log.d(TAG, "refreshSessionListFromLocal END");
                    }
                });
            }
        });
    }

    /**
     * Public method to refresh sessions (can be called from parent activity)
     */
    public void refreshSessions() {
        Log.d(TAG, "Public refreshSessions called");
        syncAndRefreshSessionList();
    }
}