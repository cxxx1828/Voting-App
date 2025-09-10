package com.nina.dragicevic;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminActivity extends AppCompatActivity {


    ImageView logo;
    Button b1,b2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_acitivty);

        logo = findViewById(R.id.logo3);
        logo.setImageResource(R.drawable.logo);

        b1 = findViewById(R.id.d1);
        b2 = findViewById(R.id.d2);

        b1.setBackgroundColor(getColor(R.color.purple));
        b2.setBackgroundColor(getColor(R.color.blue));

        StudentListFragment studentListFragment = StudentListFragment.newInstance("1","2");

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment3, studentListFragment)
                .commit();

        b1.setEnabled(true);
        b2.setEnabled(true);
        b1.setBackgroundColor(getColor(R.color.purple));
        b2.setBackgroundColor(getColor(R.color.purple));

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionListFragment sessionListFragment = SessionListFragment.newInstance("1", "2");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment3, sessionListFragment)
                        .addToBackStack(null)
                        .commit();

                b1.setBackgroundColor(getColor(R.color.purple));
                b2.setBackgroundColor(getColor(R.color.blue));
            }
        });

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StudentListFragment studentListFragment1 = StudentListFragment.newInstance("1", "2");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment3, studentListFragment1)
                        .addToBackStack(null)
                        .commit();
                b2.setBackgroundColor(getColor(R.color.purple));
                b1.setBackgroundColor(getColor(R.color.blue));
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}