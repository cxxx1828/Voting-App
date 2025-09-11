package com.nina.dragicevic;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

        // Uzmi podatke iz intenta
        String sessionName = getIntent().getStringExtra("sessionName");
        String sessionDate = getIntent().getStringExtra("sessionDate");

        Log.d(TAG, "=== RESULTS ACTIVITY DEBUG ===");
        Log.d(TAG, "Intent data -> sessionName: [" + sessionName + "], sessionDate: [" + sessionDate + "]");

        // Debug: Prikaži sve što postoji u VOTES tabeli
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor debugCursor = db.rawQuery("SELECT * FROM VOTES", null);

        Log.d(TAG, "Total votes in database: " + debugCursor.getCount());
        Log.d(TAG, "Database content:");

        while (debugCursor.moveToNext()) {
            String dbSessionName = debugCursor.getString(debugCursor.getColumnIndexOrThrow("SessionName"));
            String dbSessionDate = debugCursor.getString(debugCursor.getColumnIndexOrThrow("SessionDate"));
            int yes = debugCursor.getInt(debugCursor.getColumnIndexOrThrow("YesVotes"));
            int no = debugCursor.getInt(debugCursor.getColumnIndexOrThrow("NoVotes"));
            int abstain = debugCursor.getInt(debugCursor.getColumnIndexOrThrow("AbstainVotes"));

            Log.d(TAG, String.format("  Row: SessionName=[%s], SessionDate=[%s], YES=%d, NO=%d, ABSTAIN=%d",
                    dbSessionName, dbSessionDate, yes, no, abstain));
        }
        debugCursor.close();
        db.close();

        if (sessionName != null && sessionDate != null) {
            // PAŽNJA: Zbog obrnutog redosleda u bazi, pokušavamo oba načina

            // Prvo pokušaj normalno
            int[] results = dbHelper.getVoteResults(sessionName, sessionDate);
            Log.d(TAG, "Normal query results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);

            // Ako su svi rezultati 0, pokušaj obrnuto
            if (results[0] == 0 && results[1] == 0 && results[2] == 0) {
                Log.d(TAG, "Normal query returned zeros, trying reversed parameters...");

                // Pokušaj sa obrnutim parametrima (datum kao naziv, naziv kao datum)
                results = dbHelper.getVoteResults(sessionDate, sessionName);
                Log.d(TAG, "Reversed query results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);
            }

            // Ako i dalje nema rezultata, direktan SQL query
            if (results[0] == 0 && results[1] == 0 && results[2] == 0) {
                Log.d(TAG, "Both queries failed, trying direct SQL...");

                SQLiteDatabase directDb = dbHelper.getReadableDatabase();

                // Pokušaj 1: Traži gde je SessionName = sessionDate ILI SessionDate = sessionName
                String query = "SELECT * FROM VOTES WHERE SessionName = ? OR SessionDate = ?";
                Cursor directCursor = directDb.rawQuery(query, new String[]{sessionDate, sessionName});

                if (directCursor.moveToFirst()) {
                    results[0] = directCursor.getInt(directCursor.getColumnIndexOrThrow("YesVotes"));
                    results[1] = directCursor.getInt(directCursor.getColumnIndexOrThrow("NoVotes"));
                    results[2] = directCursor.getInt(directCursor.getColumnIndexOrThrow("AbstainVotes"));

                    Log.d(TAG, "Direct SQL found results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);
                }

                directCursor.close();
                directDb.close();
            }

            // Postavi rezultate na TextView-ove
            t1.setText("Voted Yes: " + results[0]);
            t2.setText("Voted No: " + results[1]);
            t3.setText("Voted Abstain: " + results[2]);

            // Finalni log
            Log.d(TAG, "Final displayed results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);

        } else {
            Log.e(TAG, "Missing session data in intent (null values)");
            t1.setText("Voted Yes: 0");
            t2.setText("Voted No: 0");
            t3.setText("Voted Abstain: 0");
        }

        Log.d(TAG, "=== END RESULTS ACTIVITY DEBUG ===");
    }
}