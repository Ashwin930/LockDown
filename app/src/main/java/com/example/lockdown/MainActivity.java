package com.example.lockdown;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView countdownText;
    private TextView headerText;

    private TextView colonText;

    private Button startLockDownBtn;
    private EditText hourInput;
    private EditText minuteInput;
    private AdView mAdView;
    private TextView instructionText;

    Switch themeToggle;

    private boolean isInLockDown = false;
    private int volumeUpPressCount = 0;
    private long lastPressTime = 0;
    private static final int PRESS_THRESHOLD = 10;
    private static final int TIME_WINDOW_MS = 30000; // 30 seconds max between presses

    private CountDownTimer lockdownCounter;

    private int originalRingerMode = AudioManager.RINGER_MODE_NORMAL;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_IS_DARK_MODE = "is_dark_mode";




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MobileAds.initialize(this, initializationStatus -> {});




        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //Initialize the Ad

        MobileAds.initialize(this, initializationStatus -> {});

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        headerText = findViewById(R.id.headerText);
        instructionText = findViewById(R.id.instructionText);
        countdownText = findViewById(R.id.countdownText);
        colonText = findViewById(R.id.colonText);

        startLockDownBtn = findViewById(R.id.startLD);

        hourInput = findViewById(R.id.hourInput);

        minuteInput = findViewById(R.id.minuteInput);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_IS_DARK_MODE, false);

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        themeToggle = findViewById(R.id.themeToggle);
        themeToggle.setChecked(isDarkMode);

        themeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_IS_DARK_MODE, isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });



// Add the validation watcher
        hourInput.addTextChangedListener(new TimeInputWatcher(hourInput, 12));
        minuteInput.addTextChangedListener(new TimeInputWatcher(minuteInput, 59));

        instructionText.setVisibility(GONE);



        //listing click on button
        startLockDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //getting inputs
                String hourStr = hourInput.getText().toString();
                String minuteStr = minuteInput.getText().toString();
                int hours = hourStr.isEmpty()?0:Integer.parseInt(hourStr);
                int minutes = minuteStr.isEmpty()?0:Integer.parseInt(minuteStr);
                String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);

                if(hours==0 && minutes ==0) {
                    Toast.makeText(MainActivity.this,"Enter Valid time",Toast.LENGTH_SHORT).show();
                }else{
                    //calling the startLockdown method
                    startLockdown(hours,minutes);
                }


                //Toast.makeText(MainActivity.this,"App is locked for " +selectedTime,Toast.LENGTH_SHORT).show();

            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }



    //method to Lock the screen
    private void startLockdown(int hours,int minutes) {

        //hiding Texts

        headerText.setVisibility(GONE);
        colonText.setVisibility(GONE);

        hourInput.setVisibility(GONE);
        minuteInput.setVisibility(GONE);

        startLockDownBtn.setVisibility(GONE);
        themeToggle.setVisibility(GONE);

        // making text visible so if someone restarts
        countdownText.setVisibility(View.VISIBLE);
        instructionText.setVisibility(VISIBLE);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            originalRingerMode = audioManager.getRingerMode();  // Save current mode
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);  // Set to silent
        }


        startLockTask(); // Lock the app in kiosk mode
        //Toast.makeText(this, "Lockdown started for 1 minute", Toast.LENGTH_SHORT).show();

        isInLockDown = true;

        long timeDuration  = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000);


        lockdownCounter = new CountDownTimer(timeDuration, 1000) {
            public void onTick(long millisUntilFinished) {
                // Optional: update UI with remaining time
                long seconds = millisUntilFinished / 1000;
                long h = seconds / 3600;
                long m = (seconds % 3600) / 60;
                long s = seconds % 60;

                String timeFormatted = String.format("%02d:%02d:%02d", h, m, s);
                countdownText.setText(timeFormatted);

            }

            public void onFinish() {

                stopLockDown();
            }
        }.start();
    }


    //method to release the lock
    public void stopLockDown(){

        if(lockdownCounter !=null){
            lockdownCounter.cancel();
            lockdownCounter = null;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setRingerMode(originalRingerMode);  // Restore saved mode
        }


        stopLockTask(); // End lockdown
        Toast.makeText(MainActivity.this, "Lockdown ended", Toast.LENGTH_SHORT).show();


        countdownText.setVisibility(GONE);
        headerText.setVisibility(VISIBLE);
        colonText.setVisibility(VISIBLE);

        hourInput.setVisibility(VISIBLE);
        minuteInput.setVisibility(VISIBLE);
        startLockDownBtn.setVisibility(VISIBLE);
        themeToggle.setVisibility(VISIBLE);
        instructionText.setVisibility(GONE);

        hourInput.setText("");
        minuteInput.setText("");

        isInLockDown = false;
    }


    //blocking back key
    @Override
    public void onBackPressed() {
        if (isInLockDown) {
            Toast.makeText(this, "Back is disabled during Lockdown", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }


    //listening volume up key
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && isInLockDown) {
            triggerVibration();
            long currentTime = System.currentTimeMillis();

            // Reset if time between presses is too long
            if (currentTime - lastPressTime > TIME_WINDOW_MS) {
                volumeUpPressCount = 0;
            }

            volumeUpPressCount++;
            lastPressTime = currentTime;

            Log.d("LockdownApp", "Volume up pressed: " + volumeUpPressCount);

            if (volumeUpPressCount >= PRESS_THRESHOLD) {
                volumeUpPressCount = 0;
                stopLockDown(); // Call method
            }

            return true; // Consume the volume event
        }

        return super.onKeyDown(keyCode, event);
    }

    //vibration trigger
    private void triggerVibration() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

}