package com.tfg.healthwatch.ui.home;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tfg.healthwatch.BLEService;
import com.tfg.healthwatch.DashboardActivity;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FirebaseUser currentUser;
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
        TextView welcomeText = root.findViewById(R.id.welcome_name);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeText.setText("Welcome " + currentUser.getDisplayName());
            // User is signed in
        } else {
            // No user is signed in
        }
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
                Log.d("Battery level",batteryLevel + "%");
            }
        }
    };

    /*private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String heartRate = intent.getExtras().get("heartRate").toString();

            heartDisplay.setText(heartRate);

            //or
            //exercises = ParseJSON.ChallengeParseJSON(intent.getStringExtra(MY_KEY));

        }
    };*/

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