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

public class RegisterActivity extends AppCompatActivity {


    ImageView logo1;
    TextView tew1,tew2,tew3,tew4,tew5;
    EditText et1, et2, et3, et4;

    Button btn1;

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

        et1 = findViewById(R.id.username1);
        et2 = findViewById(R.id.index);
        et3 = findViewById(R.id.name);
        et4 = findViewById(R.id.passw);

        btn1 = findViewById(R.id.button3);
        btn1.setBackgroundColor(getColor(R.color.blue));

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String s1 = et1.getText().toString();
                String s2 = et2.getText().toString();
                String s3 = et3.getText().toString();
                String s4 = et4.getText().toString();

                if(s1.isEmpty() || s2.isEmpty() || s3.isEmpty() || s4.isEmpty()){
                   return;
                }

                Intent loginActivity = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(loginActivity);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}