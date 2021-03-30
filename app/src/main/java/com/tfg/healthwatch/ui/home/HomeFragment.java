package com.tfg.healthwatch.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tfg.healthwatch.BLEService;
import com.tfg.healthwatch.DashboardActivity;
import com.tfg.healthwatch.R;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private FirebaseUser currentUser;
    private BLEService bleService;
    private TextView heartDisplay;
    private BroadcastReceiver _refreshReceiver = new MyReceiver();

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.makeText(context, "Broadcast received", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        heartDisplay = root.findViewById(R.id.heart_rate_display);
        TextView welcomeText = root.findViewById(R.id.welcome_name);

        bleService = DashboardActivity.getBleService();
        IntentFilter filter = new IntentFilter("HeartRate");
        requireActivity().registerReceiver(_refreshReceiver,filter);

        //heartDisplay.setText(getActivity().getIntent().getExtras().getString("heartRate"));

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeText.setText("Welcome " + currentUser.getDisplayName());
            // User is signed in
        } else {
            // No user is signed in
        }
        return root;
    }

    /*private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String heartRate = intent.getExtras().get("heartRate").toString();

            heartDisplay.setText(heartRate);

            //or
            //exercises = ParseJSON.ChallengeParseJSON(intent.getStringExtra(MY_KEY));

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter("heartRate"));
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(receiver);
    }*/
}