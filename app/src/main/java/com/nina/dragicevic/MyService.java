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
    private static final long NOTIFICATION_TIME = 483 * 60 * 1000; // 483 min

    private MyBinder binder = null;
    private Thread monitoringThread = null;
    private DecideItDbHelper dbHelper;
    private boolean isRunning = false; 

    public MyService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Kreiram servis - poziva se prvi put");

        dbHelper = new DecideItDbHelper(this, "decideit_v2.db", null, 2);

        createNotificationChannels();

        monitoringThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Pokretam thread koji prati sesije");

                while (isRunning) {
                    try {
                        Thread.sleep(CHECK_INTERVAL);

                        if (isRunning) {
                            Log.d(TAG, "Proveravaam sesije za notifikacije...");
                            checkSessionsForNotification();

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


        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        if (!isRunning) {
            isRunning = true;

            if (binder != null) {
                binder.setServiceStatus(true);
            }

            if (!monitoringThread.isAlive()) {
                Log.d(TAG, "Pokretam thread za praćenje sesija");
                monitoringThread.start();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Aktivnost se povezuje na servis");

        if (binder == null) {
            binder = new MyBinder();
        }

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Aktivnost se isključuje sa servisa - ali servis nastavlja da radi");
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Uništavam servis");

        isRunning = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }

        stopForeground(true);
    }

   
    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, StudentViewActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("DecideIT Service")
                .setContentText("Pratim sesije za notifikacije")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true) 
                .setPriority(NotificationCompat.PRIORITY_LOW) 
                .build();
    }

   
    private void checkSessionsForNotification() {
        try {
            Session[] sessions = dbHelper.readSessions();

            if (sessions == null || sessions.length == 0) {
                Log.d(TAG, "Nema sesija u bazi podataka");
                return;
            }

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
                    Date sessionDate = dateFormat.parse(session.getDatum());

                    if (sessionDate != null) {
                        Calendar sessionStart = Calendar.getInstance();
                        sessionStart.setTime(sessionDate);
                        sessionStart.set(Calendar.HOUR_OF_DAY, 0);
                        sessionStart.set(Calendar.MINUTE, 0);
                        sessionStart.set(Calendar.SECOND, 0);
                        sessionStart.set(Calendar.MILLISECOND, 0);

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

                        if (timeUntilStart > 0) {
                            Log.d(TAG, "Sesija počinje za: " + (timeUntilStart / 1000 / 60) + " minuta");
                        } else if (timeUntilEnd > 0) {
                            Log.d(TAG, "Sesija je AKTIVNA - završava se za: " + (timeUntilEnd / 1000 / 60) + " minuta");
                        } else {
                            Log.d(TAG, "Sesija je ISTEKLA pre " + (Math.abs(timeUntilEnd) / 1000 / 60) + " minuta");
                        }

                       
                        if (timeUntilEnd > 0 && timeUntilEnd <= NOTIFICATION_TIME) {
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

  
    private void sendSessionNotification(Session session) {
        Log.d(TAG, "Šaljem notifikaciju za sesiju: " + session.getNaziv());

        Intent intent = new Intent(this, DecideActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("sessionName", session.getNaziv());
        intent.putExtra("sessionDate", session.getDatum());
        intent.putExtra("sessionDescription", "Session description");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Sesija uskoro ističe!")
                .setContentText(session.getNaziv() + " - vreme za glasanje uskoro ističe")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Sesija '" + session.getNaziv() + "' ističe za približno 15 minuta. Kliknite da glasate."))
                .setPriority(NotificationCompat.PRIORITY_HIGH) 
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) 
                .setDefaults(NotificationCompat.DEFAULT_ALL); 

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notifikacija uspešno poslata");
        }
    }

    
    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            NotificationChannel sessionChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Session Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            sessionChannel.setDescription("Notifikacije o sesijama koje uskoro ističu");
            sessionChannel.enableVibration(true); 
            sessionChannel.enableLights(true);   

            NotificationChannel foregroundChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Drži servis za praćenje sesija aktivnim");
            foregroundChannel.setShowBadge(false);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(sessionChannel);
                notificationManager.createNotificationChannel(foregroundChannel);
                Log.d(TAG, "Notifikacioni kanali su kreirani");
            }
        }
    }
}
