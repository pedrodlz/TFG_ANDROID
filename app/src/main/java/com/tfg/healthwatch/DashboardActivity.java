package com.tfg.healthwatch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardActivity extends AppCompatActivity {

    private static BLEService mService;
    private Database DBService;
    boolean mBound = false;
    static final String HEART_RATE_INTENT = "com.tfg.healthwatch.HEART_RATE";
    static final String SCAN_INTENT = "com.tfg.healthwatch.SCAN";

    @Override
    public void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BLEService.class);
        if(bindService(intent, connection, Context.BIND_AUTO_CREATE)){
            //Toast.makeText(getApplicationContext(), "Service binded", Toast.LENGTH_SHORT).show();
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        startService(new Intent(this,BLEService.class));
        IntentFilter params = new IntentFilter();
        params.addAction(HEART_RATE_INTENT);
        params.addAction(SCAN_INTENT);
        params.addAction("com.tfg.healthwatch.CUSTOM_INTENT");
        this.registerReceiver(receiver,params);
        //bleService = new BLEService();

        /*FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("test");

        myRef.setValue("MAMON DE MIERDA");

        Log.d("PESAO","AAAAAAAAAAAAA");*/

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_bluetooth, R.id.navigation_home, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(new Intent(this,BLEService.class));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if(action.equals(HEART_RATE_INTENT)){
                Toast.makeText(getApplicationContext(), "Heart Rate Received", Toast.LENGTH_SHORT).show();
            }
            else if(action.equals(SCAN_INTENT)){
                Toast.makeText(getApplicationContext(), "Scan Received", Toast.LENGTH_SHORT).show();
                mService.scanDevices();
            }
            else{
                Toast.makeText(getApplicationContext(), "Other Intent", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public static BLEService getBleService(){
        return mService;
    }
}