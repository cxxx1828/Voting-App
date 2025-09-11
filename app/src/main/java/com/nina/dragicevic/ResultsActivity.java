package com.nina.dragicevic;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultsActivity extends AppCompatActivity {
    private static final String TAG = "RESULTS_DEBUG";
    TextView t1;
    TextView t2;

    TextView t3;

    DecideItDbHelper dbHelper;


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

        dbHelper = new DecideItDbHelper(this, "decideit.db", null, 1);

        // uzmem session podatke iz intenta
        String sessionName = getIntent().getStringExtra("sessionName");
        String sessionDate = getIntent().getStringExtra("sessionDate");
        Log.d(TAG, "ResultsActivity started with sessionName: " + sessionName + ", sessionDate: " + sessionDate);

        if (sessionName != null && sessionDate != null) {
            // session iz baze
            Session session = dbHelper.getSessionByDate(sessionDate);
            if (session != null) {
                Log.d(TAG, "Session found: " + session.getNaziv() + " Status: " + session.getAtribut());
            }

            // glasovi iz baze
            int[] results = dbHelper.getVoteResults(sessionName, sessionDate);

            Log.d(TAG, "Vote results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);

            t1.setText("Voted Yes: " + results[0]);
            t2.setText("Voted No: " + results[1]);
            t3.setText("Voted Abstain: " + results[2]);
        } else {
            Log.e(TAG, "Missing session data in intent");
            t1.setText("Voted Yes: 0");
            t2.setText("Voted No: 0");
            t3.setText("Voted Abstain: 0");
        }

    }
}