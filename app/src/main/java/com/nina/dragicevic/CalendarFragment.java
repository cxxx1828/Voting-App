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
 * CalendarFragment - Fragment koji prikazuje kalendar studentima
 * Omogućava studentima da selektuju datum i pristupu sesiji za glasanje
 * Koristi se u StudentViewActivity kao jedan od tabova
 * A simple {@link Fragment} subclass.
 * Use the {@link CalendarFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CalendarFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // Standardni Fragment parametri za prosleđivanje argumenata
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    // Privatne promenljive za čuvanje prosleđenih parametara
    private String mParam1;
    private String mParam2;


    private DecideItDbHelper dbHelper;   // Helper klasa za SQLite bazu i HTTP komunikaciju
    private ExecutorService executor;    // Thread pool za background operacije
    private Handler mainHandler;         // Handler za komunikaciju sa UI thread-om

    // Obavezan prazan konstruktor za Fragment klase
    public CalendarFragment() {
    }

    /**
     * Factory metoda za kreiranje nove instance Fragment-a
     * Koristi se umesto direktnog pozivanja konstruktora
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1. - Prvi parametar koji se prosleđuje Fragment-u
     * @param param2 Parameter 2. - Drugi parametar koji se prosleđuje Fragment-u
     * @return A new instance of fragment CalendarFragment. - Nova instanca CalendarFragment-a
     */
    // TODO: Rename and change types and number of parameters
    public static CalendarFragment newInstance(String param1, String param2) {
        CalendarFragment fragment = new CalendarFragment();
        // Kreira Bundle objekat za prosleđivanje argumenata
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        // Postavlja argumente na Fragment
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Poziva se kada Android kreira Fragment
     * Ovde se inicijalizuju osnovne komponente
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Čita prosleđene argumente ako postoje
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        dbHelper = new DecideItDbHelper(getContext(), "decideit_v2.db", null, 2);
        // Kreira single-thread executor za background HTTP operacije, da ne kreiramo svaki put novi thread
        executor = Executors.newSingleThreadExecutor();
        // Handler za prebacivanje rezultata sa background thread-a na UI thread
        mainHandler = new Handler(Looper.getMainLooper()); // handler kao poštar koji prenosi poruke između thread-ova
        //mainHandler.post() - "pošalji ovo na Main Thread"
    }

    /**
     * Kreira i vraća View koji će biti prikazan korisniku
     * Glavna metoda za kreiranje UI komponenti
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflatuje (kreira) View iz XML layout datoteke
        View view1 = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Pronalazi CalendarView komponentu iz layout-a
        CalendarView calendarView = view1.findViewById(R.id.calendarView);

        // Postavlja minimalni datum koji se može selektovati na kalendaru
        // 1734220800000L predstavlja timestamp za 15. decembar 2024.
        calendarView.setMinDate(1734220800000L);

        // Odmah po učitavanju Fragment-a sinhronizuje sesije sa servera
        // MAIN UI THREAD poziva metodu
        syncSessionsFromServer();

        // Postavlja event listener za izbor datuma na kalendaru
        //UI Thread - Kalendar klik
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                // Loguje selektovani datum (month je 0-based, zato +1)
                Log.d("CALENDAR_DEBUG", "Date selected -> " + dayOfMonth + "." + (month + 1) + "." + year);

                // Kreira Calendar objekat za selektovani datum
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth);
                // Formatira datum u string format koji koristi aplikacija (dd.MM.yyyy)
                String selectedDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(c.getTime());

                Log.d("CALENDAR_DEBUG", "Formatted selectedDate: " + selectedDate);

                //provera da li vec postoji sesija za datum
                // Dobija sve datume za koje postoje sesije iz lokalne baze
                ArrayList<String> sessionDates = dbHelper.getSessionDates();
                Log.d("CALENDAR_DEBUG", "Loaded " + sessionDates.size() + " session dates from DB");

                // Proverava da li postoji sesija za selektovani datum
                if (sessionDates.contains(selectedDate)) {
                    Log.d("CALENDAR_DEBUG", "Session found for date: " + selectedDate);
                    // Dobija podatke o sesiji za selektovani datum
                    Session session = dbHelper.getSessionByDate(selectedDate);

                    // Kreira Intent za prelazak na DecideActivity (aktivnost za glasanje)
                    Intent intent = new Intent(getActivity(), DecideActivity.class);
                    // Prosleđuje potrebne podatke o sesiji kroz Intent extras
                    intent.putExtra("sessionName", session.getNaziv());
                    intent.putExtra("sessionDate", selectedDate);
                    intent.putExtra("sessionDescription", "Session description");
                    // Pokreće DecideActivity
                    startActivity(intent);
                    Log.d("CALENDAR_DEBUG", "DecideActivity started with sessionName: " +
                            session.getNaziv() + ", sessionDate: " + selectedDate);
                } else {
                    Log.d("CALENDAR_DEBUG", "No session available for date: " + selectedDate);
                    // Ako ne postoji sesija, prikazuje Toast poruku studentu
                    Toast.makeText(getContext(), "No session available for this date", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Vraća kreiran View koji će Android prikazati
        return view1;
    }

    /**
     * Poziva se kada se Fragment uništava
     * Važno za oslobađanje resursa i sprečavanje memory leak-ova
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Gasi executor thread pool ako postoji
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Sinhronizuje sesije sa servera u lokalnu bazu podataka
     * Izvršava se u background thread-u da ne blokira UI
     * Syncs sessions from server to local database
     */
    private void syncSessionsFromServer() {
        Log.d("CALENDAR_DEBUG", "Starting sync sessions from server...");

        // Pokretaje sinhronizaciju u background thread-u
        executor.execute(new Runnable() {  // PREBACUJE NA BACKGROUND THREAD
            // izvršiš ovo u background-u
            @Override
            public void run() {
                Log.d("CALENDAR_DEBUG", "Background thread: syncing sessions from server");

                //sinhronizuje sesije sa servera na db
                // Poziva DbHelper metodu koja šalje GET zahtev serveru
                // i ažurira lokalnu SQLite bazu sa najnovijim podacima
                // SADA SMO NA BACKGROUND THREAD-U
                boolean syncSuccess = dbHelper.syncSessionsFromServer();
                // HTTP GET zahtev ka serveru - može da traje 5 sekundi
                Log.d("CALENDAR_DEBUG", "Sync from server result: " + syncSuccess);

                //updatuje UI na main threadu
                // Prebacuje se na main (UI) thread da prikaže rezultat korisniku
                mainHandler.post(new Runnable() { // PREBACUJE NAZAD NA MAIN THREAD
                    @Override
                    public void run() {
                        // SADA SMO NAZAD NA MAIN THREAD-U
                        // Proverava da li je sinhronizacija uspešna
                        if (syncSuccess) {
                            // Mogu da ažuriram UI - menjam TextView, Toast, itd.
                            Log.d("CALENDAR_DEBUG", "Successfully synced sessions from server");
                            // Uspešna sinhronizacija - nema potrebe da prikazuje poruku korisniku
                        } else {
                            Log.d("CALENDAR_DEBUG", "Failed to sync sessions from server (may be offline)");
                            // Neuspešna sinhronizacija - možda nema internet konekcije
                            // Ali aplikacija i dalje radi sa lokalnim podacima
                        }
                    }
                });
            }
        });
    }

    /**
     * public metoda za osvežavanje sesija
     * Može biti pozvana iz roditeljske aktivnosti (StudentViewActivity)
     * Public method to refresh sessions (can be called from parent activity)
     */
    public void refreshSessions() {
        Log.d("CALENDAR_DEBUG", "Public refreshSessions called");
        // Pokreće novu sinhronizaciju sa serverom
        syncSessionsFromServer();
    }
}