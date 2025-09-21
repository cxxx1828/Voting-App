package com.nina.dragicevic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultsActivity extends AppCompatActivity {
    private static final String TAG = "RESULTS_DEBUG";
    TextView t1;
    TextView t2;
    TextView t3;

    DecideItDbHelper dbHelper;
    ExecutorService executor;
    Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_results);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        t1 = findViewById(R.id.tekst1);
        t2 = findViewById(R.id.tekst2);
        t3 = findViewById(R.id.tekst3);

        dbHelper = new DecideItDbHelper(this, "decideit_v2.db", null, 1);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // uzmem session podatke iz intenta
        String sessionName = getIntent().getStringExtra("sessionName");
        String sessionDate = getIntent().getStringExtra("sessionDate");
        Log.d(TAG, "ResultsActivity started with sessionName: " + sessionName + ", sessionDate: " + sessionDate);

        // Show loading state
        t1.setText("Loading...");
        t2.setText("Loading...");
        t3.setText("Loading...");

        if (sessionName != null && sessionDate != null) {
            loadVoteResults(sessionName, sessionDate);
        } else {
            Log.e(TAG, "Missing session data in intent");
            showDefaultResults();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Loads vote results from server and updates UI
     */
    private void loadVoteResults(String sessionName, String sessionDate) {
        Log.d(TAG, "Loading vote results for session: " + sessionName + ", date: " + sessionDate);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: loading vote results");

                // First verify session exists
                Session session = dbHelper.getSessionByDate(sessionDate);
                if (session != null) {
                    Log.d(TAG, "Session found: " + session.getNaziv() + " Status: " + session.getAtribut());
                }

                // Get vote results (this method now includes HTTP sync)
                final int[] results = dbHelper.getVoteResults(sessionName, sessionDate);

                Log.d(TAG, "Vote results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);

                // Update UI on main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateUI(results);
                    }
                });
            }
        });
    }

    /**
     * Updates the UI with vote results
     */
    private void updateUI(int[] results) {
        Log.d(TAG, "Updating UI with results: YES=" + results[0] + ", NO=" + results[1] + ", ABSTAIN=" + results[2]);

        t1.setText("Voted Yes: " + results[0]);
        t2.setText("Voted No: " + results[1]);
        t3.setText("Voted Abstain: " + results[2]);

        Log.d(TAG, "UI updated successfully");
    }

    /**
     * Shows default results when session data is missing
     */
    private void showDefaultResults() {
        Log.d(TAG, "Showing default results due to missing session data");
        t1.setText("Voted Yes: 0");
        t2.setText("Voted No: 0");
        t3.setText("Voted Abstain: 0");
    }

    /**
     * Public method to refresh results (can be called if needed)
     */
    public void refreshResults() {
        String sessionName = getIntent().getStringExtra("sessionName");
        String sessionDate = getIntent().getStringExtra("sessionDate");

        if (sessionName != null && sessionDate != null) {
            loadVoteResults(sessionName, sessionDate);
        }
    }
}