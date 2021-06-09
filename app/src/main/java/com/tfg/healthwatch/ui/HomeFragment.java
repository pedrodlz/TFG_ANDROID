package com.tfg.healthwatch.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.BLEService;
import com.tfg.healthwatch.DashboardActivity;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final String BATTERY = "battery";
    private static final String TAG = "HomeFragment";
    private FirebaseUser currentUser;
    private DatabaseReference heartRateTable;
    private TextView heartDisplay;
    private TextView batteryLevelDisplay;
    private Button mAddButton;
    static final String HEART_RATE_INTENT = "com.tfg.healthwatch.HEART_RATE";
    private static final String BATTERY_INTENT = "com.tfg.healthwatch.BATTERY_LEVEL";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        heartDisplay = root.findViewById(R.id.heart_rate_display);
        batteryLevelDisplay = root.findViewById(R.id.battery_level_display);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

        heartRateTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid()).child(stringDate);

        heartRateTable.child("Average Heart Rate").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String value = snapshot.getValue().toString();
                    Log.d(TAG, "Average heart rate today: " + value);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return root;
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(HEART_RATE_INTENT)){
                String heartRate = intent.getStringExtra("heartRate");
                heartDisplay.setText(heartRate);
            }
            else if(action.equals(BATTERY_INTENT)){
                String batteryLevel = intent.getStringExtra("batteryLevel");
                batteryLevelDisplay.setText(batteryLevel+"%");
                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                actionBar.setTitle("Pulsera conectada " + batteryLevel + "%");
                Log.d("Battery level",batteryLevel + "%");
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            batteryLevelDisplay.setText(savedInstanceState.getString(BATTERY));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BATTERY, (String) batteryLevelDisplay.getText());
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter params = new IntentFilter();
        params.addAction(HEART_RATE_INTENT);
        params.addAction(BATTERY_INTENT);
        getActivity().registerReceiver(receiver,params);
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(receiver);
    }
}