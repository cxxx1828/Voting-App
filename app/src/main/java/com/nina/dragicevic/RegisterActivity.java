package com.nina.dragicevic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {


    ImageView logo1;
    TextView tew1,tew2,tew3,tew4,tew5, tew7, tew8;
    EditText et1, et2, et3, et4, et5, et6;

    Button btn1;
    DecideItDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        logo1 = findViewById(R.id.Logo1);
        logo1.setImageResource(R.drawable.logo);
        tew1 = findViewById(R.id.textView3);
        tew2 = findViewById(R.id.textView4);
        tew3 = findViewById(R.id.textView5);
        tew4 = findViewById(R.id.textView6);
        tew5 = findViewById(R.id.textView7);
        tew7 = findViewById(R.id.textView8);
        tew8 = findViewById(R.id.textView9);

        et1 = findViewById(R.id.username1);
        et2 = findViewById(R.id.index);
        et3 = findViewById(R.id.name);
        et4 = findViewById(R.id.passw);
        et5 = findViewById(R.id.surname);
        et6 = findViewById(R.id.role);

        btn1 = findViewById(R.id.button3);
        btn1.setBackgroundColor(getColor(R.color.blue));

        dbHelper = new DecideItDbHelper(this, "decideit_v2.db", null, 1);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = et1.getText().toString().trim();
                String index = et2.getText().toString().trim();
                String name = et3.getText().toString().trim();
                String password = et4.getText().toString().trim();
                String surname = et5.getText().toString().trim();
                String role = et6.getText().toString().trim();

                Log.d("REGISTER_DEBUG", "Entered values -> username: " + username +
                        ", index: " + index + ", name: " + name +
                        ", surname: " + surname + ", role: " + role);


                if (username.isEmpty() || index.isEmpty() || name.isEmpty() ||
                        password.isEmpty() || surname.isEmpty() || role.isEmpty()) {
                    Log.d("REGISTER_DEBUG", "Some fields are empty");
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.isUsernameExists(username)) {
                    Log.d("REGISTER_DEBUG", "Username already exists: " + username);
                    Toast.makeText(RegisterActivity.this, "Username already exists. Please choose another.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean success = dbHelper.insertUser(name, surname, username, index, password, role);
                Log.d("REGISTER_DEBUG", "Insert user result -> " + success);

                if (success) {
                    Log.d("REGISTER_DEBUG", "Registration successful, redirecting to LoginActivity");
                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent loginActivity = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(loginActivity);
                    finish();
                } else {
                    Log.d("REGISTER_DEBUG", "Registration failed");
                    Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}