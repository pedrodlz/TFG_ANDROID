package com.tfg.healthwatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static BLEService mServiceBLE;
    private static FallingService mServiceFalling;
    private static NavController navController;
    private ImageView mMicButton;
    private EditText mResultText;
    private Fragment mMyFragment;
    boolean mBoundBLE = false;
    boolean mBoundFalling = false;
    public static final int RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    static final String HEART_RATE_INTENT = "com.tfg.healthwatch.HEART_RATE";

    @Override
    public void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        intent = new Intent(this, FallingService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        setBarColor();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_goals, R.id.navigation_alerts, R.id.navigation_home, R.id.navigation_diagnose, R.id.navigation_settings)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

    }

    private void setBarColor(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary,this.getTheme())));
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary,this.getTheme()));
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this,BLEService.class));
        startService(new Intent(this,FallingService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopService(new Intent(this,BLEService.class));
        stopService(new Intent(this,FallingService.class));
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(new Intent(this,BLEService.class));
        stopService(new Intent(this,FallingService.class));
        speechRecognizer.destroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            String name = className.getShortClassName();
            if(name.equals(".BLEService")){
                BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
                mServiceBLE = binder.getService();
                mBoundBLE = true;
            }
            else if(name.equals(".FallingService")){
                FallingService.LocalBinder binder = (FallingService.LocalBinder) service;
                mServiceFalling = binder.getService();
                mBoundFalling = true;
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBoundBLE = false;
        }
    };

    public static BLEService getBleService(){
        return mServiceBLE;
    }
}