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

public class StudentViewActivity extends AppCompatActivity {


    Button b1,b2;

    ImageView logo;

    String username1, name, surname;

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

        ProfileFragment profileFragment = ProfileFragment.newInstance(username1,"2");

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment1, profileFragment)
                .commit();


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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


}