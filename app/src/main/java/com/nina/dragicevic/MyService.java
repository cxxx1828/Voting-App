package com.nina.dragicevic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyService extends Service {


    private static final String TAG = "MY_SERVICE";
    private static final String CHANNEL_ID = "SESSION_NOTIFICATION_CHANNEL";
    private static final String FOREGROUND_CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    private static final int FOREGROUND_NOTIFICATION_ID = 1002;
    private static final long CHECK_INTERVAL = 60 * 1000;


    private MyBinder binder = null;
    private Thread monitoringThread = null;
    private DecideItDbHelper dbHelper;
    private boolean isRunning = false; //aktivnost servisa

    public MyService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Kreiram servis - poziva se prvi put");

        dbHelper = new DecideItDbHelper(this, "decideit_v2.db", null, 2);

        // Pravim kanale za notifikacije (potrebno za Android 8+)
        createNotificationChannels();

        //  thread koji će u pozadini stalno da proverava sesije
        monitoringThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Pokretam thread koji prati sesije");

                // petlja dok je servis aktivan
                while (isRunning) {
                    try {
                        // Čekam 1 minut između provera
                        Thread.sleep(CHECK_INTERVAL);

                        // Ako je servis još uvek aktivan, proverim sesije
                        if (isRunning) {
                            Log.d(TAG, "Proveravaam sesije za notifikacije...");
                            checkSessionsForNotification();

                            // Ažuriram brojač provera u binderu debuggggg
                            if (binder != null) {
                                int currentCount = binder.getCheckedSessionsCount();
                                binder.setCheckedSessionsCount(currentCount + 1);
                            }
                        }

                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread je prekinut");
                        break;
                    }
                }

                Log.d(TAG, "Thread za praćenje sesija je završen");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Pokrećem servis kao foreground - neće ga sistem ubiti");

        // Pokrećem kao foreground service da ga Android ne ubije
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        // Aktiviram monitoring sesija
        if (!isRunning) {
            isRunning = true;

            // Obavešatavam binder da je servis aktivan
            if (binder != null) {
                binder.setServiceStatus(true);
            }

            // Pokrećem monitoring thread ako već nije pokrenut
            if (!monitoringThread.isAlive()) {
                Log.d(TAG, "Pokretam thread za praćenje sesija");
                monitoringThread.start();
            }
        }

        // START_STICKY znači da će Android ponovo pokrenuti servis ako ga ubije
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Aktivnost se povezuje na servis");

        // Kreiram binder ako ne postoji
        if (binder == null) {
            binder = new MyBinder();
        }

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Aktivnost se isključuje sa servisa - ali servis nastavlja da radi");
        // Ne zaustavljam servis kada se aktivnost odspoji
        return false; // false = neću pozivati onRebind
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Uništavam servis");

        // Zaustavljam monitoring
        isRunning = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }

        // Uklanjam foreground notifikaciju
        stopForeground(true);
    }

    /**
     * Kreiram notifikaciju koja drži servis živ u pozadini
     * Android zahteva da foreground servisi imaju notifikaciju
     */
    private Notification createForegroundNotification() {
        // Intent koji će otvoriti glavnu aktivnost kada korisnik tapne notifikaciju
        Intent notificationIntent = new Intent(this, StudentViewActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("DecideIT Service")
                .setContentText("Pratim sesije za notifikacije")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Korisnik ne može da je ukloni swipe-om
                .setPriority(NotificationCompat.PRIORITY_LOW) // Niska prioriteta da ne smeta
                .build();
    }

    /**
     * Glavna metoda koja proverava sve sesije u bazi i šalje notifikacije
     * za one koje uskoro isteknu
     */
    private void checkSessionsForNotification() {
        try {
=            Session[] sessions = dbHelper.readSessions();

            if (sessions == null || sessions.length == 0) {
                Log.d(TAG, "Nema sesija u bazi podataka");
                return;
            }

            // Uzimam trenutno vreme
            long currentTime = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

            Log.d(TAG, "=== PROVERAVAM SVE SESIJE ===");
            Log.d(TAG, "Trenutno vreme: " + fullDateFormat.format(new Date(currentTime)));

            for (Session session : sessions) {
                Log.d(TAG, "--- Sesija: " + session.getNaziv() + " ---");
                Log.d(TAG, "Datum sesije: " + session.getDatum());
                Log.d(TAG, "Status sesije: " + session.getAtribut());

                try {
                    // Parsiranje datuma sesije
                    Date sessionDate = dateFormat.parse(session.getDatum());

                    if (sessionDate != null) {
                        // Postavljam početak sesije na 00:00:00
                        Calendar sessionStart = Calendar.getInstance();
                        sessionStart.setTime(sessionDate);
                        sessionStart.set(Calendar.HOUR_OF_DAY, 0);
                        sessionStart.set(Calendar.MINUTE, 0);
                        sessionStart.set(Calendar.SECOND, 0);
                        sessionStart.set(Calendar.MILLISECOND, 0);

                        // Postavljam kraj sesije na 23:59:59
                        Calendar sessionEnd = Calendar.getInstance();
                        sessionEnd.setTime(sessionDate);
                        sessionEnd.set(Calendar.HOUR_OF_DAY, 23);
                        sessionEnd.set(Calendar.MINUTE, 59);
                        sessionEnd.set(Calendar.SECOND, 59);
                        sessionEnd.set(Calendar.MILLISECOND, 999);

                        long sessionStartTime = sessionStart.getTimeInMillis();
                        long sessionEndTime = sessionEnd.getTimeInMillis();
                        long timeUntilStart = sessionStartTime - currentTime;
                        long timeUntilEnd = sessionEndTime - currentTime;

                        Log.d(TAG, "Početak sesije: " + fullDateFormat.format(new Date(sessionStartTime)));
                        Log.d(TAG, "Kraj sesije:   " + fullDateFormat.format(new Date(sessionEndTime)));

                        // Logika za određivanje stanja sesije
                        if (timeUntilStart > 0) {
                            Log.d(TAG, "Sesija počinje za: " + (timeUntilStart / 1000 / 60) + " minuta");
                        } else if (timeUntilEnd > 0) {
                            Log.d(TAG, "Sesija je AKTIVNA - završava se za: " + (timeUntilEnd / 1000 / 60) + " minuta");
                        } else {
                            Log.d(TAG, "Sesija je ISTEKLA pre " + (Math.abs(timeUntilEnd) / 1000 / 60) + " minuta");
                        }

                        // KLJUČNA LOGIKA: šaljem notifikaciju ako je sesija aktivna i ostalo je manje od 75 minuta
                        // Fokusiram se na vreme
                        if (timeUntilEnd > 0 && timeUntilEnd <= 75 * 60 * 1000) { // 75 minuta = 1 sat i 15 minuta
                            Log.d(TAG, "Vreme do kraja: " + (timeUntilEnd / 1000 / 60) + " minuta");
                            Log.d(TAG, ">>> ŠALJEM NOTIFIKACIJU za sesiju: " + session.getNaziv() + " <<<");
                            sendSessionNotification(session);
                        }

                        Log.d(TAG, "");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Greška pri parsiranju datuma sesije: " + session.getDatum(), e);
                }
            }

            Log.d(TAG, "=== ZAVRŠENA PROVERA SESIJA ===");

        } catch (Exception e) {
            Log.e(TAG, "Greška pri proveravanju sesija", e);
        }
    }

    /**
     * Šalje notifikaciju korisniku o sesiji koja uskoro ističe
     * Direktno otvara DecideActivity
     */
    private void sendSessionNotification(Session session) {
        Log.d(TAG, "Šaljem notifikaciju za sesiju: " + session.getNaziv());

        // Kreiram intent koji direktno otvara aktivnost za glasanje
        Intent intent = new Intent(this, DecideActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("sessionName", session.getNaziv());
        intent.putExtra("sessionDate", session.getDatum());
        intent.putExtra("sessionDescription", "Session description");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Kreiram notifikaciju sa visokim prioritetom
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Sesija uskoro ističe!")
                .setContentText(session.getNaziv() + " - vreme za glasanje uskoro ističe")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Sesija '" + session.getNaziv() + "' ističe za približno 15 minuta. Kliknite da glasate."))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Visok prioritet da korisnik odmah vidi
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // Automatski se uklanja kada korisnik tapne
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Zvuk, vibracija, svetlo

        // Šaljem notifikaciju preko NotificationManagera
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notifikacija uspešno poslata");
        }
    }

    /**
     * Kreiram kanale za notifikacije (potrebno za Android 8.0+)
     * Jedan kanal za sesijske notifikacije, drugi za foreground servis
     */
    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Kanal za notifikacije o sesijama - visok prioritet
            NotificationChannel sessionChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Session Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            sessionChannel.setDescription("Notifikacije o sesijama koje uskoro ističu");
            sessionChannel.enableVibration(true); // Omogućavam vibraciju
            sessionChannel.enableLights(true);    // Omogućavam LED svetlo

            // Kanal za foreground servis - nizak prioritet da ne smeta
            NotificationChannel foregroundChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Drži servis za praćenje sesija aktivnim");
            foregroundChannel.setShowBadge(false); // Ne prikazuje badge na ikoni aplikacije

            // Registrujem kanale u sistemu
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(sessionChannel);
                notificationManager.createNotificationChannel(foregroundChannel);
                Log.d(TAG, "Notifikacioni kanali su kreirani");
            }
        }
    }
}