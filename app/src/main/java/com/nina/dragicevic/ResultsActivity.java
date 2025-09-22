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

/**
 * ResultsActivity - Aktivnost koja prikazuje rezultate glasanja za određenu sesiju
 * Ova aktivnost se pokreće kada administrator ili student klikne na sesiju da vidi rezultate
 * Kombinuje sinhronizaciju sa serverom i prikaz podataka iz lokalne baze
 * Koristi background threading za HTTP operacije da ne blokira UI
 */
public class ResultsActivity extends AppCompatActivity {
    // Tag za logovanje - omogućava lakše praćenje u Android logovima
    private static final String TAG = "RESULTS_DEBUG";

    // UI komponente za prikaz brojki glasova
    TextView t1;  // Prikazuje broj "YES" glasova
    TextView t2;  // Prikazuje broj "NO" glasova
    TextView t3;  // Prikazuje broj "ABSTAIN" glasova

    // Komponente za rad sa bazom i threading
    DecideItDbHelper dbHelper;   // Helper za SQLite bazu i HTTP komunikaciju
    ExecutorService executor;    // Thread pool za background operacije
    Handler mainHandler;         // Handler za ažuriranje UI-ja sa background thread-a

    /**
     * Poziva se kada Android kreira aktivnost
     * Ovde se inicijalizuje UI i pokreće učitavanje rezultata glasanja
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        // Postavlja layout za ovu aktivnost iz XML datoteke
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
        // Kreira single-thread executor za background HTTP i database operacije
        executor = Executors.newSingleThreadExecutor();
        // Handler za komunikaciju između background thread-a i UI thread-a
        mainHandler = new Handler(Looper.getMainLooper());

        // uzmem session podatke iz intenta
        // Čita podatke o sesiji koji su prosleđeni preko Intent-a
        String sessionName = getIntent().getStringExtra("sessionName");  // Naziv sesije
        String sessionDate = getIntent().getStringExtra("sessionDate");  // Datum sesije
        Log.d(TAG, "ResultsActivity started with sessionName: " + sessionName + ", sessionDate: " + sessionDate);


        // Prikazuje "Loading..." poruke dok se podaci učitavaju
        t1.setText("Loading...");
        t2.setText("Loading...");
        t3.setText("Loading...");

        // Proverava da li su potrebni podaci prosleđeni preko Intent-a
        if (sessionName != null && sessionDate != null) {
            // Ako postoje podaci, pokreće učitavanje rezultata
            loadVoteResults(sessionName, sessionDate);
        } else {
            // Ako nema podataka, loguje grešku i prikazuje default vrednosti
            Log.e(TAG, "Missing session data in intent");
            showDefaultResults();
        }
    }

    /**
     * Poziva se kada se aktivnost uništava
     * Važno za cleanup da se spreče memory leak-ovi
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Gasi executor thread pool ako postoji
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Učitava rezultate glasanja sa servera i ažurira UI
     * Ova metoda se izvršava u background thread-u da ne blokira UI
     * Loads vote results from server and updates UI
     * @param sessionName - naziv sesije za koju se učitavaju rezultati
     * @param sessionDate - datum sesije
     */
    private void loadVoteResults(String sessionName, String sessionDate) {
        Log.d(TAG, "Loading vote results for session: " + sessionName + ", date: " + sessionDate);

        // Pokreće operaciju u background thread-u da ne blokira UI
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: loading vote results");


                // Prvo verifikuje da sesija postoji u lokalnoj bazi
                Session session = dbHelper.getSessionByDate(sessionDate);
                if (session != null) {
                    Log.d(TAG, "Session found: " + session.getNaziv() + " Status: " + session.getAtribut());
                }


                // Dobija rezultate glasanja - ova metoda automatski sinhronizuje sa serverom
                final int[] results = dbHelper.getVoteResults(sessionName, sessionDate);

                Log.d(TAG, "Vote results -> YES: " + results[0] + ", NO: " + results[1] + ", ABSTAIN: " + results[2]);


                // Prebacuje se na main (UI) thread da ažurira interfejs
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Poziva metodu za ažuriranje UI komponenti
                        updateUI(results);
                    }
                });
            }
        });
    }

    /**
     * Ažurira UI sa rezultatima glasanja
     * Ova metoda se izvršava na main (UI) thread-u
     * Updates the UI with vote results
     * @param results - int array sa [yes, no, abstain] brojevima
     */
    private void updateUI(int[] results) {
        Log.d(TAG, "Updating UI with results: YES=" + results[0] + ", NO=" + results[1] + ", ABSTAIN=" + results[2]);

        // Postavlja tekst za svaki TextView sa odgovarajućim brojem glasova
        t1.setText("Voted Yes: " + results[0]);      // Broj YES glasova
        t2.setText("Voted No: " + results[1]);       // Broj NO glasova
        t3.setText("Voted Abstain: " + results[2]);  // Broj ABSTAIN glasova

        Log.d(TAG, "UI updated successfully");
    }

    /**
     * Prikazuje default rezultate kada nema podataka o sesiji
     * Ova metoda se poziva ako Intent ne sadrži potrebne podatke
     * Shows default results when session data is missing
     */
    private void showDefaultResults() {
        Log.d(TAG, "Showing default results due to missing session data");
        // Postavlja sve brojke na 0 kada nema podataka
        t1.setText("Voted Yes: 0");
        t2.setText("Voted No: 0");
        t3.setText("Voted Abstain: 0");
    }

    /**
     * Javna metoda za osvežavanje rezultata
     * Može biti pozvana spolja da se ponovo učitaju najnoviji podaci
     * Public method to refresh results (can be called if needed)
     */
    public void refreshResults() {
        // Ponovo čita podatke iz Intent-a (u slučaju da su se promenili)
        String sessionName = getIntent().getStringExtra("sessionName");
        String sessionDate = getIntent().getStringExtra("sessionDate");

        // Ako postoje podaci, pokreće novo učitavanje
        if (sessionName != null && sessionDate != null) {
            loadVoteResults(sessionName, sessionDate);
        }
    }
}