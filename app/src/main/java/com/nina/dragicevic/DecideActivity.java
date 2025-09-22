package com.nina.dragicevic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DecideActivity extends AppCompatActivity {

    ImageView im;
    TextView t1, t2, t3, t4;
    Button btn1, btn2, btn3;

    Button selected = null;
    DecideItDbHelper dbHelper;
    String sessionName;
    String sessionDate;
    Session currentSession;
    ExecutorService executor;
    Handler mainHandler;

    private static final String TAG = "DECIDE_DEBUG";
    private boolean isVoting = false; // da izbegnemo duplo glasanje

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

        dbHelper = new DecideItDbHelper(this, "decideit_v2.db", null, 1);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // iz intenta ime i datum
        sessionName = getIntent().getStringExtra("sessionName");
        sessionDate = getIntent().getStringExtra("sessionDate");

        Log.d(TAG, "DecideActivity started with sessionName: " + sessionName + ", sessionDate: " + sessionDate);

        // celu sesiju iz db
        if (sessionDate != null) {
            currentSession = dbHelper.getSessionByDate(sessionDate);
            if (currentSession != null) {
                Log.d(TAG, "Session loaded from DB: " + currentSession.getNaziv() + " Status: " + currentSession.getAtribut());

                // setujem ime i datum sesije
                t1.setText(currentSession.getNaziv());
                t2.setText("Session description");
                t3.setText(currentSession.getDatum());
                t4.setText("Status: " + currentSession.getAtribut());
            } else {
                Log.e(TAG, "Session not found in database for date: " + sessionDate);
                Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        btn1.setEnabled(true);
        btn2.setEnabled(true);
        btn3.setEnabled(true);

        btn1.setBackgroundColor(getColor(R.color.blue));
        btn2.setBackgroundColor(getColor(R.color.blue));
        btn3.setBackgroundColor(getColor(R.color.blue));

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "YES button clicked");
                if (isVoting) return; // da nema duplog glasanja

                if (selected == null) {
                    btn1.setBackgroundColor(getColor(R.color.red));
                    btn2.setBackgroundColor(getColor(R.color.blue));
                    btn3.setBackgroundColor(getColor(R.color.blue));
                    selected = btn1;
                } else if (selected == btn1) {
                    btn1.setBackgroundColor(getColor(R.color.blue));
                    selected = null;
                } else {
                    btn1.setBackgroundColor(getColor(R.color.red));
                    btn2.setBackgroundColor(getColor(R.color.blue));
                    btn3.setBackgroundColor(getColor(R.color.blue));
                    selected = btn1;
                }
            }
        });

        btn1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (selected == btn1 && !isVoting) {
                    Log.d(TAG, "Submitting YES vote for session: " + sessionName + ", date: " + sessionDate);
                    submitVote(1, "YES");
                }
                return true;
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "NO button clicked");
                if (isVoting) return;

                if (selected == null) {
                    btn2.setBackgroundColor(getColor(R.color.red));
                    btn1.setBackgroundColor(getColor(R.color.blue));
                    btn3.setBackgroundColor(getColor(R.color.blue));
                    selected = btn2;
                } else if (selected == btn2) {
                    btn2.setBackgroundColor(getColor(R.color.blue));
                    selected = null;
                } else {
                    btn2.setBackgroundColor(getColor(R.color.red));
                    btn1.setBackgroundColor(getColor(R.color.blue));
                    btn3.setBackgroundColor(getColor(R.color.blue));
                    selected = btn2;
                }
            }
        });

        btn2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (selected == btn2 && !isVoting) {
                    Log.d(TAG, "Submitting NO vote for session: " + sessionName + ", date: " + sessionDate);
                    submitVote(2, "NO");
                }
                return true;
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "ABSTAIN button clicked");
                if (isVoting) return;

                if (selected == null) {
                    btn3.setBackgroundColor(getColor(R.color.red));
                    btn1.setBackgroundColor(getColor(R.color.blue));
                    btn2.setBackgroundColor(getColor(R.color.blue));
                    selected = btn3;
                } else if (selected == btn3) {
                    btn3.setBackgroundColor(getColor(R.color.blue));
                    selected = null;
                } else {
                    btn3.setBackgroundColor(getColor(R.color.red));
                    btn1.setBackgroundColor(getColor(R.color.blue));
                    btn2.setBackgroundColor(getColor(R.color.blue));
                    selected = btn3;
                }
            }
        });

        btn3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (selected == btn3 && !isVoting) {
                    Log.d(TAG, "Submitting ABSTAIN vote for session: " + sessionName + ", date: " + sessionDate);
                    submitVote(3, "ABSTAIN");
                }
                return true;
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Submits vote to server and updates UI
     */
    private void submitVote(int voteType, String voteLabel) {
        if (isVoting) {
            Log.d(TAG, "Already voting, ignoring request");
            return;
        }

        isVoting = true;

        // da prikazem promenu stanja
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                btn1.setEnabled(false);
                btn2.setEnabled(false);
                btn3.setEnabled(false);

                if (selected != null) {
                    selected.setText("Voting...");
                }

                Toast.makeText(DecideActivity.this, "Submitting " + voteLabel + " vote...", Toast.LENGTH_SHORT).show();
            }
        });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Background thread: submitting vote to server");

                //submitujem glas ali sad http serveru
                boolean success = dbHelper.insertOrUpdateVote(sessionName, sessionDate, voteType);

                // updateujem UI na main niti
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        isVoting = false;

                        // resetujem duggmad
                        btn1.setEnabled(true);
                        btn2.setEnabled(true);
                        btn3.setEnabled(true);

                        btn1.setText(getString(R.string.yes));
                        btn2.setText(getString(R.string.no));
                        btn3.setText(getString(R.string.abstain));

                        if (success) {
                            Log.d(TAG, voteLabel + " vote submitted successfully");
                            Toast.makeText(DecideActivity.this, voteLabel + " vote submitted successfully!", Toast.LENGTH_SHORT).show();

                            // resetujem selekciju
                            selected = null;
                            btn1.setBackgroundColor(getColor(R.color.blue));
                            btn2.setBackgroundColor(getColor(R.color.blue));
                            btn3.setBackgroundColor(getColor(R.color.blue));

                            //zatvorim nakon glasanja
                            finish();

                        } else {
                            Log.e(TAG, "Failed to submit " + voteLabel + " vote");
                            Toast.makeText(DecideActivity.this, "Failed to submit vote. Check network connection.", Toast.LENGTH_LONG).show();

                            // resetujem dugmad ali zadrzim izabrano
                            if (selected == btn1) {
                                btn1.setBackgroundColor(getColor(R.color.red));
                                btn2.setBackgroundColor(getColor(R.color.blue));
                                btn3.setBackgroundColor(getColor(R.color.blue));
                            } else if (selected == btn2) {
                                btn2.setBackgroundColor(getColor(R.color.red));
                                btn1.setBackgroundColor(getColor(R.color.blue));
                                btn3.setBackgroundColor(getColor(R.color.blue));
                            } else if (selected == btn3) {
                                btn3.setBackgroundColor(getColor(R.color.red));
                                btn1.setBackgroundColor(getColor(R.color.blue));
                                btn2.setBackgroundColor(getColor(R.color.blue));
                            }
                        }
                    }
                });
            }
        });
    }
}