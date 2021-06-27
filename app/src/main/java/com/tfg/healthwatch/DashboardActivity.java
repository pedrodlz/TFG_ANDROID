package com.tfg.healthwatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
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
import androidx.appcompat.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
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
    private static NavController navController;
    public static final int RecordAudioRequestCode = 1;
    public static final int CallRequestCode = 2;
    public static final String emergencyIntent = "com.tfg.healthwatch.EMERGENCY_CALL";
    public static final String fallIntent = "com.tfg.healthwatch.FALL_DETECTED";
    private Boolean firstAlert = true;

    @Override
    public void onStart() {
        super.onStart();
        // Bind to LocalService
        startService(new Intent(this,BLEService.class));
        startService(new Intent(this,FallingService.class));
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if((action.equals(emergencyIntent) || action.equals(fallIntent)) && firstAlert){
                if (ActivityCompat.checkSelfPermission(DashboardActivity.this,
                        Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    firstAlert = false;
                    String title = "Emergency";
                    if(action.equals(emergencyIntent)) title = "High heart rate";
                    else if(action.equals(fallIntent)) title = "Fall detected";

                    new AlertDialog.Builder(DashboardActivity.this ,R.style.AlertDialogCustom)
                            .setTitle(title)
                            .setMessage(getString(R.string.question_call_emergency))
                            .setPositiveButton( getString(R.string.yes) , new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                            String phoneNumber = intent.getStringExtra("emergencyNumber");
                                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                                            callIntent.setData(Uri.parse("tel:"+phoneNumber));
                                            startActivity(callIntent);
                                        }
                                    })
                            .setNegativeButton( "No" , new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                            firstAlert = true;
                                        }
                                    })
                            .show() ;
                }
            }
        }
    };

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

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CallRequestCode);
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
        IntentFilter params = new IntentFilter();
        params.addAction(emergencyIntent);
        params.addAction(fallIntent);
        registerReceiver(receiver,params);

        startService(new Intent(this,BLEService.class));
        startService(new Intent(this,FallingService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        stopService(new Intent(this,BLEService.class));
        stopService(new Intent(this,FallingService.class));
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(new Intent(this,BLEService.class));
        stopService(new Intent(this,FallingService.class));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        /*if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
        if (requestCode == CallRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }*/
    }
}