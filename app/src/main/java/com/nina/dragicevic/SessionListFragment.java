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

/**
 * SessionListFragment - Fragment koji prikazuje listu sesija i omogućava kreiranje novih
 * Ovo je jedan od najvažnijih Fragment-ova u aplikaciji
 * Koristi se u AdminActivity-ju kao jedan od tabova koji admin može da bira
 * Implementira kompletnu sinhronizaciju sa serverom i lokalne operacije nad sesijama
 * Kombinuje prikaz postojećih sesija sa mogućnošću kreiranja novih
 */
public class SessionListFragment extends Fragment {

    // =================== UI KOMPONENTE ===================
    ListView lista;
    TextView emptyView;
    SessionAdapter adapter;
    CalendarView k;
    Button d;

    // =================== BAZA I THREADING KOMPONENTE ===================
    DecideItDbHelper dbHelper;   // Helper klasa koja kombinuje SQLite database operacije sa HTTP komunikacijom
    ExecutorService executor;    // Thread pool koji omogućava izvršavanje HTTP zahteva u background thread-u
    Handler mainHandler;         // Handler koji omogućava komunikaciju između background thread-a i main UI thread-a

    // =================== STANJE FRAGMENT-A ===================
    private long selectedDateMillis = 0;  // Čuva vreme selektovanog datuma u milisekundama (0 znači da nije selektovan)
    private int sessionCounter = 1;       // Brojač koji se uvećava za auto-generisanje naziva sesija ("Session 1", "Session 2", itd.)


    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "SESSION_LIST_DEBUG";

    private String mParam1;  // Prvi parametar koji može biti prosleđen Fragment-u kroz Bundle
    private String mParam2;  // Drugi parametar koji može biti prosleđen Fragment-u kroz Bundle

    /**
     * Prazan konstruktor - OBAVEZAN za sve Fragment klase
     * Android zahteva da Fragment klase imaju prazan konstruktor
     * Nikada ne treba direktno pozivati ovaj konstruktor - koristiti newInstance() metodu
     */
    public SessionListFragment() { }

    /**
     * Factory metoda za kreiranje nove instance Fragment-a sa parametrima
     * Ovo je preporučeni način kreiranja Fragment-a umesto direktnog pozivanja konstruktora
     * Koristi Bundle sistem za prosleđivanje argumenata što je Android best practice
     * @param param1 Prvi parametar koji se prosleđuje Fragment-u
     * @param param2 Drugi parametar koji se prosleđuje Fragment-u
     * @return Nova instanca SessionListFragment-a sa prosleđenim argumentima
     */
    public static SessionListFragment newInstance(String param1, String param2) {
        SessionListFragment fragment = new SessionListFragment();
        Bundle args = new Bundle();

        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Lifecycle metoda koja se poziva kada Android kreira Fragment
     * Ova metoda se poziva pre onCreateView() i koristi se za inicijalizaciju
     * Ovde se inicijalizuju komponente koje ne zavise od UI
     * @param savedInstanceState Bundle sa sačuvanim stanjem (ako je Fragment bio uništen pa ponovno kreiran)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Kreira single-thread executor za background operacije
        // Ovo je ključno jer HTTP zahtevi ne smeju da se izvršavaju na main (UI) thread-u
        // Single thread znači da će se operacije izvršavati redom, jedna po jedna
        executor = Executors.newSingleThreadExecutor();
        // Kreira Handler povezan sa main thread-om (UI thread)
        // Ovaj handler omogućava background thread-ovima da pošalju rezultate UI-ju
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Lifecycle metoda koja kreira i vraća View koji će biti prikazan korisniku
     * Ova metoda je srce Fragment-a - ovde se kreira celokupan UI
     * @param inflater LayoutInflater za kreiranje View-a iz XML layout-a
     * @param container ViewGroup u koji će biti umetnut Fragment (obično Activity)
     * @param savedInstanceState Bundle sa sačuvanim stanjem UI komponenti
     * @return View objekat koji predstavlja UI Fragment-a
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate (kreira) View iz XML layout datoteke
        // fragment_session_list.xml sadrži definiciju UI komponenti
        View view = inflater.inflate(R.layout.fragment_session_list, container, false);

        // =================== INICIJALIZACIJA UI KOMPONENTI ===================

        lista = view.findViewById(R.id.lista4);
        emptyView = view.findViewById(R.id.emptyView2);
        k = view.findViewById(R.id.kal);
        d = view.findViewById(R.id.butn);

        // Povezuje emptyView sa ListView-om
        // Kada je lista prazna, automatski će se prikazati emptyView umesto liste
        lista.setEmptyView(emptyView);
        Log.d(TAG, "ListView and EmptyView initialized");

        // =================== INICIJALIZACIJA DATABASE I ADAPTER KOMPONENTI ===================
        dbHelper = new DecideItDbHelper(getContext(), "decideit_v2.db", null, 1);
        // Kreira adapter sa praznom ArrayList-om - podaci će biti dodani kasnije kada se učitaju
        adapter = new SessionAdapter(getContext(), new ArrayList<>());
        // Povezuje adapter sa ListView-om - od sada adapter kontroliše šta se prikazuje u listi
        lista.setAdapter(adapter);

        Log.d(TAG, "Adapter set, calling syncAndRefreshSessionList()");
        // Odmah po kreiranju pokušava da sinhronizuje sesije sa servera i prikaže ih
        syncAndRefreshSessionList();


        // Postavlja listener koji se poziva kada admin selektuje datum na kalendaru
        k.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view1, int year, int month, int dayOfMonth) {
                // Kreira Calendar objekat za precizno rukovanje datumom
                Calendar c = Calendar.getInstance();
                // Postavlja datum na selektovani
                // Postavlja vreme na 00:00:00 da izbegne probleme sa vremenskim zonama
                c.set(year, month, dayOfMonth, 0, 0, 0);
                // Konvertuje datum u milisekunde i čuva za kasnije korišćenje
                selectedDateMillis = c.getTimeInMillis();
                // Loguje selektovani datum (month+1 jer je mesec 0-based)
                Log.d(TAG, "Date selected from CalendarView -> " + dayOfMonth + "." + (month + 1) + "." + year);
            }
        });

        // Postavlja listener koji se poziva kada admin klikne Submit dugme za kreiranje nove sesije
        d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Submit button clicked");
                // Proverava da li je admin eksplicitno selektovao datum na kalendaru
                if (selectedDateMillis == 0) {
                    Log.d(TAG, "No date manually selected, taking current CalendarView date");
                    // Ako nije, uzima trenutni datum prikazan na kalendaru
                    selectedDateMillis = k.getDate();
                }

                // Konvertuje timestamp u Date objekat za lakše formatiranje
                Date chosenDate = new Date(selectedDateMillis);
                // Formatira datum u string format koji koristi aplikacija (dd.MM.yyyy)
                String dateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(chosenDate);
                // Auto-generiše naziv sesije koristeći brojač (Session 1, Session 2, itd.)
                String sessionName = "Session " + sessionCounter++;
                Log.d(TAG, "Creating new session -> name: " + sessionName + ", date: " + dateStr);

                // =================== LOADING STATE ===================
                // Onemogućava dugme da spreči admin-a da klikne više puta pre nego što se završi prvi zahtev
                d.setEnabled(false);
                // Menja tekst dugmeta da prikaže da se operacija izvršava
                d.setText("Creating...");

                // Pokreće kreiranje sesije na serveru u background thread-u
                // Prosleđuje datum, naziv i prazan opis
                createSessionOnServer(dateStr, sessionName, "");
            }
        });

        // =================== EVENT LISTENER ZA KLIK NA SESIJU U LISTI ===================
        // Postavlja listener koji se poziva kada admin klikne na postojeću sesiju u listi
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
                // Dobija Session objekat iz adapter-a na osnovu pozicije klika
                Session clicked = (Session) adapter.getItem(position);

                Log.d(TAG, "List item clicked -> sessionName: " + clicked.getNaziv() +
                        ", sessionDate: " + clicked.getDatum() +
                        ", sessionStatus: " + clicked.getAtribut());

                // Kreira Intent za prelazak na ResultsActivity (aktivnost koja prikazuje rezultate glasanja)
                Intent intent = new Intent(getActivity(), ResultsActivity.class);
                // Prosleđuje podatke o selektovanoj sesiji preko Intent extras
                intent.putExtra("sessionName", clicked.getNaziv());    // Naziv sesije
                intent.putExtra("sessionDate", clicked.getDatum());    // Datum sesije
                intent.putExtra("sessionStatus", clicked.getAtribut()); // Status (UPCOMING/PAST)
                // Pokreće ResultsActivity
                startActivity(intent);
            }
        });

        // Vraća kompletno konfigurisani View koji će Android prikazati korisniku
        return view;
    }

    /**
     * Lifecycle metoda koja se poziva kada se Fragment uništava
     * KRITIČNO VAŽNO za sprečavanje memory leak-ova i oslobađanje resursa
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Gasi executor thread pool ako postoji
        // Ovo sprečava da background thread-ovi nastave sa radom nakon što je Fragment uništen
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Glavna metoda koja sinhronizuje sesije sa servera i osvežava UI listu
     * Ova metoda kombinuje HTTP komunikaciju sa lokalnom database operacijom
     * Ključna je za održavanje konzistentnosti između servera i lokalne aplikacije
     */
    private void syncAndRefreshSessionList() {
        Log.d(TAG, "syncAndRefreshSessionList START");

        // Pokreće celokupnu operaciju u background thread-u da ne blokira UI
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: syncing sessions from server");

                // KORAK 1: Sinhronizacija sa serverom
                // Šalje HTTP GET zahtev serveru (/sessions endpoint) i ažurira lokalnu SQLite bazu
                boolean syncSuccess = dbHelper.syncSessionsFromServer();
                Log.d(TAG, "Sync from server result: " + syncSuccess);

                // KORAK 2: Čitanje iz lokalne baze
                // Čita sve sesije iz lokalne baze (koja je sada ažurirana najnovijim podacima sa servera)
                final Session[] sessions = dbHelper.readSessions();

                // KORAK 3: Ažuriranje UI-ja na main thread-u
                // Background thread ne može direktno da menja UI - mora koristiti Handler
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Briše sve postojeće sesije iz adapter-a da počne ispočetka
                        adapter.clear();

                        // Proverava da li su učitane sesije sa servera/baze
                        if (sessions != null) {
                            Log.d(TAG, "Loaded " + sessions.length + " sessions from local DB");
                            // Prolazi kroz sve sesije i dodaje ih u adapter
                            for (Session session : sessions) {
                                Log.d(TAG, "Adding session -> name: " + session.getNaziv() +
                                        ", date: " + session.getDatum() +
                                        ", status: " + session.getAtribut());
                                // Dodavanje u adapter automatski ažurira ListView prikaz
                                adapter.addElement(session);
                            }
                        } else {
                            Log.d(TAG, "No sessions found in local DB");
                            // Ako nema sesija, lista će ostati prazna i prikazaće se emptyView
                        }

                        Log.d(TAG, "syncAndRefreshSessionList END");
                    }
                });
            }
        });
    }

    /**
     * Kreira novu sesiju na serveru i lokalno ažurira prikaz
     * Ova metoda je srce funkcionalnosti kreiranja novih sesija
     * @param date Datum sesije u dd.MM.yyyy formatu
     * @param sessionName Naziv sesije (auto-generisan kao "Session N")
     * @param description Opis sesije (trenutno se uvek prosleđuje prazan string)
     */
    private void createSessionOnServer(String date, String sessionName, String description) {
        Log.d(TAG, "createSessionOnServer START -> date: " + date + ", name: " + sessionName);

        // Pokreće HTTP POST operaciju u background thread-u
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: creating session on server");

                // KLJUČNA OPERACIJA: Šalje POST zahtev serveru da kreira novu sesiju
                // Ova metoda automatski:
                // 1. Konvertuje datum u ISO format za server
                // 2. Šalje HTTP POST zahtev na /session endpoint
                // 3. Prima odgovor sa server ID-jem nove sesije
                // 4. Čuva sesiju u lokalnu bazu sa server ID-jem
                boolean success = dbHelper.createSessionOnServer(date, sessionName, description);

                // Vraća se na main thread da ažurira UI sa rezultatom
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // =================== RESET DUGMETA ===================
                        // Vraća dugme u normalno stanje nezavisno od ishoda
                        d.setEnabled(true);
                        d.setText(getString(R.string.submit)); // Vraća originalni tekst

                        if (success) {
                            Log.d(TAG, "Session successfully created on server");
                            // Prikazuje Toast poruku admin-u o uspešnom kreiranju
                            Toast.makeText(getContext(), "Session added successfully", Toast.LENGTH_SHORT).show();

                            // Osvežava listu SAMO iz lokalne baze (brže od ponovnog server zahteva)
                            // Lokalna baza je već ažurirana u createSessionOnServer() metodi
                            refreshSessionListFromLocal();
                        } else {
                            Log.d(TAG, "Failed to create session on server");
                            // Prikazuje Toast poruku o grešci sa sugestijom za rešavanje
                            Toast.makeText(getContext(), "Failed to create session. Check network connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * Brža verzija refresh-a koja čita SAMO iz lokalne baze (bez HTTP zahteva)
     * Koristi se nakon operacija gde je lokalna baza već ažurirana (npr. kreiranje nove sesije)
     * Značajno brža od syncAndRefreshSessionList() jer preskače server komunikaciju
     */
    private void refreshSessionListFromLocal() {
        Log.d(TAG, "refreshSessionListFromLocal START");

        // Background thread samo za SQLite operacije (bez HTTP)
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Čita sesije SAMO iz lokalne SQLite baze
                final Session[] sessions = dbHelper.readSessions();

                // Ažurira UI na main thread-u
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Briše postojeći sadržaj adapter-a
                        adapter.clear();

                        // Dodaje sve sesije iz lokalne baze
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
     * Javna metoda koja omogućava spoljašnjim komponentama da pokrenu osvežavanje
     * Može biti pozvana iz parent Activity-ja ili drugih komponenti
     * Pokreće PUNU sinhronizaciju (uključujući server zahtev)
     */
    public void refreshSessions() {
        Log.d(TAG, "Public refreshSessions called");
        // Poziva punu sinhronizaciju koja uključuje server komunikaciju
        syncAndRefreshSessionList();
    }
}