package com.nina.dragicevic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usernameText = username.getText().toString();
                String passwordText = password.getText().toString();

                bundle.putString("username", usernameText);

                if(usernameText.isEmpty() || passwordText.isEmpty()) {

                    return;
                }

                if((usernameText.equals("student") && passwordText.equals("student"))){
                    Intent studentView = new Intent(LoginActivity.this, StudentViewActivity.class);
                    studentView.putExtras(bundle);
                    startActivity(studentView);
                }


                if((usernameText.equals("admin") && passwordText.equals("admin"))){
                    Intent adminActivity = new Intent(LoginActivity.this, AdminAcitivty.class);
                    startActivity(adminActivity);
                }
            }
        });


        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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