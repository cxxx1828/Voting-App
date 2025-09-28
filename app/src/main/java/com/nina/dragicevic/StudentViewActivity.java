package com.nina.dragicevic;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentViewActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "STUDENT_VIEW_DEBUG";

    Button b1, b2;
    ImageView logo;

    String username1, name, surname;


    private MyBinder serviceBinder = null;
    private boolean isServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_view);

        b1 = findViewById(R.id.buttonp);
        b2 = findViewById(R.id.buttonc);

        b1.setBackgroundColor(getColor(R.color.purple));
        b2.setBackgroundColor(getColor(R.color.blue));

        username1 = getIntent().getStringExtra("username");
        name = getIntent().getStringExtra("name");
        surname = getIntent().getStringExtra("surname");

        String fullName = name + " " + surname;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Proverava da li treba direktno pokazati kalendar (iz notifikacije)
        boolean showCalendar = getIntent().getBooleanExtra("showCalendar", false);
        String sessionDate = getIntent().getStringExtra("sessionDate");
        String sessionName = getIntent().getStringExtra("sessionName");

        if (showCalendar) {
            Log.d(TAG, "Opening calendar from notification for session: " + sessionName + " on " + sessionDate);

            CalendarFragment calendarFragment = CalendarFragment.newInstance("1", "2");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment1, calendarFragment)
                    .commit();
            b2.setBackgroundColor(getColor(R.color.purple));
            b1.setBackgroundColor(getColor(R.color.blue));

            Toast.makeText(this, "Sesija '" + sessionName + "' uskoro ističe!", Toast.LENGTH_LONG).show();
        } else {
            ProfileFragment profileFragment = ProfileFragment.newInstance(username1, "2");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment1, profileFragment)
                    .commit();
        }

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileFragment pFragment = ProfileFragment.newInstance(fullName, "2");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment1, pFragment)
                        .addToBackStack(null)
                        .commit();

                b1.setBackgroundColor(getColor(R.color.purple));
                b2.setBackgroundColor(getColor(R.color.blue));
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalendarFragment calendarFragment = CalendarFragment.newInstance("1", "2");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment1, calendarFragment)
                        .addToBackStack(null)
                        .commit();
                b2.setBackgroundColor(getColor(R.color.purple));
                b1.setBackgroundColor(getColor(R.color.blue));
            }
        });

        logo = findViewById(R.id.Logo2);
        logo.setImageResource(R.drawable.logo);

        // Pokreće servis za praćenje sesija
        startSessionNotificationService();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Otkačinje se od servisa ali NE zaustavlja servis
        if (isServiceBound) {
            unbindService(this);
            isServiceBound = false;
            Log.d(TAG, "Service unbound in onDestroy - but service continues running as foreground service");
        }
        // NE pozivam stopService() jer želimo da servis nastavi da radi
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Activity stopped, foreground service continues running");
    }

    //Pokreće servis kao foreground service + bind za komunikaciju

    private void startSessionNotificationService() {
        Log.d(TAG, "Starting session notification service");

        Intent serviceIntent = new Intent(StudentViewActivity.this, MyService.class);

        // KLJUČNO: PRVO pozivamo startService da pokrene foreground service
        // Ovo osigurava da servis ostane živ kada se aktivnost zatvori
        startService(serviceIntent);
        Log.d(TAG, "Service started with startService() - this will call onStartCommand()");

        // ZATIM se povezujemo sa bindService za komunikaciju
        boolean bindResult = bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);

        if (bindResult) {
            Log.d(TAG, "Service bind initiated successfully");
        } else {
            Log.e(TAG, "Failed to bind to service");
            Toast.makeText(this, "Failed to start notification service", Toast.LENGTH_SHORT).show();
        }
    }

    //Poziva se kada se aktivnost uspešno poveže sa servisom

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "Service connected successfully");

        serviceBinder = (MyBinder) iBinder;
        isServiceBound = true;

        Toast.makeText(this, "Notification service started as foreground service", Toast.LENGTH_SHORT).show();

        if (serviceBinder.isServiceRunning()) {
            Log.d(TAG, "Service is running and monitoring sessions");
        }
    }

    //Poziva se kada se veza sa servisom prekine

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "Service disconnected - but foreground service continues running");
        serviceBinder = null;
        isServiceBound = false;
    }

    //Javna metoda za dobijanje trenutnog statusa servisa

    public boolean isNotificationServiceRunning() {
        if (serviceBinder != null) {
            return serviceBinder.isServiceRunning();
        }
        return false;
    }

    //Javna metoda za dobijanje broja proverenih sesija

    public int getCheckedSessionsCount() {
        if (serviceBinder != null) {
            return serviceBinder.getCheckedSessionsCount();
        }
        return 0;
    }
}