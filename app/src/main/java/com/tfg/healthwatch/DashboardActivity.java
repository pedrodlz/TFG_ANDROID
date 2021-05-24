package com.tfg.healthwatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static BLEService mServiceBLE;
    private static FallingService mServiceFalling;
    private Fragment mMyFragment;
    boolean mBoundBLE = false;
    boolean mBoundFalling = false;

    @Override
    public void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        intent = new Intent(this, FallingService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_goals, R.id.navigation_alerts, R.id.navigation_home, R.id.navigation_profile, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
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
    }


    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if(requestCode == )

    }*/



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