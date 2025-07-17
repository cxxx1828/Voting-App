package com.nina.dragicevic;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DecideActivity extends AppCompatActivity {

    ImageView im;
    TextView t1, t2, t3, t4;
    Button btn1, btn2, btn3;

    Button selected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_decide);

        im = findViewById(R.id.Logo3);
        t1 = findViewById(R.id.tView);
        t2 = findViewById(R.id.tView1);
        t3 = findViewById(R.id.tView2);
        t4 = findViewById(R.id.tView3);

        btn1 = findViewById(R.id.b1);
        btn2 = findViewById(R.id.b2);
        btn3 = findViewById(R.id.b3);

        btn1.setEnabled(true);
        btn2.setEnabled(true);
        btn3.setEnabled(true);

        btn1.setBackgroundColor(getColor(R.color.blue));
        btn2.setBackgroundColor(getColor(R.color.blue));
        btn3.setBackgroundColor(getColor(R.color.blue));

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected == null) {
                    btn1.setBackgroundColor(getColor(R.color.red));
                    selected = btn1;
                } else if (selected == btn1) {
                    btn1.setBackgroundColor(getColor(R.color.blue));
                    selected = null;
                }

            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected == null) {
                    btn2.setBackgroundColor(getColor(R.color.red));
                    selected = btn2;
                } else if (selected == btn2) {
                    btn2.setBackgroundColor(getColor(R.color.blue));
                    selected = null;
                }
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected == null) {
                    btn3.setBackgroundColor(getColor(R.color.red));
                    selected = btn3;
                } else if (selected == btn3) {
                    btn3.setBackgroundColor(getColor(R.color.blue));
                    selected = null;
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
