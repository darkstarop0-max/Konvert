package com.curosoft.konvert;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "KonvertPrefs";
    private static final String KEY_SEEN_WELCOME = "seen_welcome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button getStartedButton = findViewById(R.id.btn_get_started);
        getStartedButton.setOnClickListener(v -> {
            // Mark welcome as seen
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_SEEN_WELCOME, true).apply();

            // Navigate to main activity
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
