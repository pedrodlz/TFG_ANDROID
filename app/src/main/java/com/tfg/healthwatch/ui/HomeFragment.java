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
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.tfg.healthwatch.BLEService;
import com.tfg.healthwatch.DashboardActivity;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;

public class HomeFragment extends Fragment {

    private static final String BATTERY = "battery";
    private static final String TAG = "HomeFragment";
    private FirebaseUser currentUser;
    private DatabaseReference heartRateTable;
    private TextView heartDisplay;
    private Button mAddButton;
    static final String HEART_RATE_INTENT = "com.tfg.healthwatch.HEART_RATE";
    private static final String BATTERY_INTENT = "com.tfg.healthwatch.BATTERY_LEVEL";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        heartDisplay = root.findViewById(R.id.heart_rate_display);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

        heartRateTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid());

        heartRateTable.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                class element {
                       String value;
                       String date;

                       element(String v, String d){
                           value = v;
                           date = d;
                       }
                }

                GraphView graph = (GraphView) root.findViewById(R.id.heart_rate_graph);
                ArrayList<element> intPoints = new ArrayList<>();

                for(DataSnapshot day : snapshot.getChildren()){
                    if(day.child("Average Heart Rate").exists()){
                        intPoints.add(new element(day.child("Average Heart Rate").getValue().toString(),day.child("date").getValue().toString()));
                    }
                }

                DataPoint[] dataPoints = new DataPoint[intPoints.size()]; // declare an array of DataPoint objects with the same size as your list
                for (int i = 0; i < intPoints.size(); i++) {
                    // add new DataPoint object to the array for each of your list entries
                    String stringDate = intPoints.get(i).date;
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").parse(stringDate);
                        dataPoints[i] = new DataPoint(date, Double.parseDouble(intPoints.get(i).value)); // not sure but I think the second argument should be of type double

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints);
                graph.addSeries(series);

                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            Format formatter = new SimpleDateFormat("dd/MM");
                            return formatter.format(value);
                        }
                        return super.formatLabel(value, isValueX);
                    }
                });

                graph.getViewport().setScrollable(true); // enables horizontal scrolling
                graph.getViewport().setScrollableY(true); // enables vertical scrolling
                graph.setTitle("Pulsaciones medias");
                graph.setTitleColor(R.color.custom_dark_grey);
                graph.getGridLabelRenderer().setGridColor(R.color.custom_dark_grey);
                graph.getGridLabelRenderer().setHorizontalLabelsColor(R.color.custom_dark_grey);
                graph.getGridLabelRenderer().setVerticalLabelsColor(R.color.custom_dark_grey);
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
                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                actionBar.setTitle("Pulsera conectada " + batteryLevel + "%");
                Log.d("Battery level",batteryLevel + "%");
            }
        }
    };


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