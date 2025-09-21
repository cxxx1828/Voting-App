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

public class LoginActivity extends AppCompatActivity {


    ImageView logo;
    TextView tw1,tw2;
    EditText username, password;

    Button btn1,btn2;
    Bundle bundle;

    DecideItDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        logo = findViewById(R.id.Logo);
        logo.setImageResource(R.drawable.logo);

        tw1 = findViewById(R.id.textView1);
        tw2 = findViewById(R.id.textView2);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        btn1 = findViewById(R.id.button1);
        btn2 = findViewById(R.id.button2);

        btn2.setBackgroundColor(getColor(R.color.blue));
        btn1.setBackgroundColor(getColor(R.color.blue));

        bundle = new Bundle();
        dbHelper = new DecideItDbHelper(this, "decideit_v2.db", null, 1);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LOGIN_DEBUG", "Login button clicked");

                String usernameText = username.getText().toString();
                String passwordText = password.getText().toString();

                Log.d("LOGIN_DEBUG", "Entered -> username: " + usernameText + ", password: " + passwordText);

                bundle.putString("username", usernameText);

                if(usernameText.isEmpty() || passwordText.isEmpty()) {
                    Log.d("LOGIN_DEBUG", "Empty username or password");
                    Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // preko baze podataka
                String[] userInfo = dbHelper.authenticateUser(usernameText, passwordText);
                Log.d("LOGIN_DEBUG", "authenticateUser returned: " + (userInfo != null ? "SUCCESS" : "FAILED"));

                if (userInfo != null) {
                    String name = userInfo[0];
                    String surname = userInfo[1];
                    String role = userInfo[2];

                    Log.d("LOGIN_DEBUG", "User authenticated -> name: " + name + ", surname: " + surname + ", role: " + role);

                    bundle.putString("username", usernameText);
                    bundle.putString("name", name);
                    bundle.putString("surname", surname);

                    if (role.equals("student")) {
                        Log.d("LOGIN_DEBUG", "Redirecting to StudentViewActivity");
                        Intent studentView = new Intent(LoginActivity.this, StudentViewActivity.class);
                        studentView.putExtras(bundle);
                        startActivity(studentView);
                        finish();
                    } else if (role.equals("admin") || role.equals("administrator")) {
                        Log.d("LOGIN_DEBUG", "Redirecting to AdminActivity");
                        Intent adminActivity = new Intent(LoginActivity.this, AdminActivity.class);
                        adminActivity.putExtras(bundle);
                        startActivity(adminActivity);
                        finish();
                    }
                } else {
                    Log.d("LOGIN_DEBUG", "Invalid credentials entered");
                    Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });


        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LOGIN_DEBUG", "Register button clicked");
                Intent registerActivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(registerActivity);
            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}