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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.jjoe64.graphview.series.PointsGraphSeries;

import com.tfg.healthwatch.R;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FirebaseUser currentUser;
    private DatabaseReference activityTable;
    private TextView heartDisplay, stepsDisplay, moodDisplay;
    static final String HEART_RATE_INTENT = "com.tfg.healthwatch.HEART_RATE";
    private static final String BATTERY_INTENT = "com.tfg.healthwatch.BATTERY_LEVEL";

    static class element {
        String value;
        String date;

        element(String v, String d){
            value = v;
            date = d;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        requireActivity();
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        heartDisplay = root.findViewById(R.id.heart_rate_display);
        stepsDisplay = root.findViewById(R.id.stepsDisplay);
        moodDisplay = root.findViewById(R.id.mood_display);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

        activityTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid());

        DefaultLabelFormatter formatter = new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Format formatter = new SimpleDateFormat("dd/MM");
                    return formatter.format(value);
                }
                return super.formatLabel(value, isValueX);
            }
        };

        DefaultLabelFormatter moodFormatter = new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Format formatter = new SimpleDateFormat("dd/MM");
                    return formatter.format(value);
                }
                else{
                    String type;
                    switch((int) value){
                        case 5:
                            type= "Muy feliz";
                            break;
                        case 4:
                            type= "Contento";
                            break;
                        case 3:
                            type= "Neutro";
                            break;
                        case 2:
                            type= "Triste";
                            break;
                        case 1:
                            type= "Muy triste";
                            break;
                        default:
                            type= "Sin datos";
                            break;
                    }
                    return type;
                }
            }
        };

        activityTable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(isAdded()){
                    GraphView heartGraph = (GraphView) root.findViewById(R.id.heart_rate_graph);
                    GraphView stepsGraph = (GraphView) root.findViewById(R.id.steps_graph);
                    GraphView moodGraph = (GraphView) root.findViewById(R.id.mood_graph);

                    ArrayList<element> heartPoints = new ArrayList<>();
                    ArrayList<element> stepsPoints = new ArrayList<>();
                    ArrayList<element> moodPoints = new ArrayList<>();

                    for(DataSnapshot day : snapshot.getChildren()){
                        if(day.child("Average Heart Rate").exists() && day.child("date").exists()){
                            heartPoints.add(new element(day.child("Average Heart Rate").getValue().toString(), day.child("date").getValue().toString()));
                        }
                        if(day.child("Steps").exists() && day.child("date").exists()){
                            stepsPoints.add(new element(day.child("Steps").getValue().toString(), day.child("date").getValue().toString()));
                        }
                        if(day.child("generalFeeling").exists() && day.child("date").exists()){
                            moodPoints.add(new element(day.child("generalFeeling").getValue().toString(), day.child("date").getValue().toString()));
                        }
                    }

                    if(snapshot.child(stringDate).exists() && snapshot.child(stringDate).child("Steps").exists()){
                        stepsDisplay.setText(snapshot.child(stringDate).child("Steps").getValue().toString());
                    }

                    if(snapshot.child(stringDate).exists() && snapshot.child(stringDate).child("generalFeeling").exists()){
                        String type;
                        int value = snapshot.child(stringDate).child("generalFeeling").getValue(int.class);
                        switch(value){
                            case 5:
                                type= "Muy feliz";
                                break;
                            case 4:
                                type= "Contento";
                                break;
                            case 3:
                                type= "Neutro";
                                break;
                            case 2:
                                type= "Triste";
                                break;
                            case 1:
                                type= "Muy triste";
                                break;
                            default:
                                type= "Sin datos";
                                break;
                        }
                        moodDisplay.setText(type);
                    }

                    DataPoint[] heartDataPoints = new DataPoint[heartPoints.size()]; // declare an array of DataPoint objects with the same size as your list
                    DataPoint[] stepsDataPoints = new DataPoint[stepsPoints.size()];
                    DataPoint[] moodDataPoints = new DataPoint[moodPoints.size()];

                    for (int i = 0; i < heartPoints.size(); i++) {
                        // add new DataPoint object to the array for each of your list entries
                        String stringDate = heartPoints.get(i).date;
                        try {
                            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").parse(stringDate);
                            heartDataPoints[i] = new DataPoint(date, Double.parseDouble(heartPoints.get(i).value)); // not sure but I think the second argument should be of type double

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    for (int i = 0; i < stepsPoints.size(); i++) {
                        // add new DataPoint object to the array for each of your list entries
                        String stringDate = stepsPoints.get(i).date;
                        try {
                            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").parse(stringDate);
                            stepsDataPoints[i] = new DataPoint(date, Double.parseDouble(stepsPoints.get(i).value)); // not sure but I think the second argument should be of type double

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    for (int i = 0; i < moodPoints.size(); i++) {
                        // add new DataPoint object to the array for each of your list entries
                        String stringDate = moodPoints.get(i).date;
                        try {
                            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'").parse(stringDate);
                            moodDataPoints[i] = new DataPoint(date, Double.parseDouble(moodPoints.get(i).value)); // not sure but I think the second argument should be of type double

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    LineGraphSeries<DataPoint> heartSeries = new LineGraphSeries<DataPoint>(heartDataPoints);
                    heartSeries.setColor(ContextCompat.getColor(requireContext(),R.color.error));
                    heartGraph.addSeries(heartSeries);
                    heartGraph.getViewport().setScrollable(true); // enables horizontal scrolling
                    heartGraph.getViewport().setScalable(true);
                    heartGraph.setTitle("Pulsaciones medias");
                    heartGraph.setTitleColor(R.color.custom_dark_grey);
                    heartGraph.getGridLabelRenderer().setGridColor(R.color.custom_dark_grey);
                    heartGraph.getGridLabelRenderer().setHorizontalLabelsColor(R.color.custom_dark_grey);
                    heartGraph.getGridLabelRenderer().setVerticalLabelsColor(R.color.custom_dark_grey);
                    heartGraph.getGridLabelRenderer().setLabelFormatter(formatter);

                    BarGraphSeries<DataPoint> stepsSeries = new BarGraphSeries<>(stepsDataPoints);
                    stepsSeries.setColor(ContextCompat.getColor(requireContext(),R.color.third));
                    stepsGraph.addSeries(stepsSeries);
                    stepsGraph.getViewport().setScrollable(true); // enables horizontal scrolling
                    stepsGraph.getViewport().setScalable(true);
                    stepsGraph.setTitle("Pasos diarios");
                    stepsGraph.setTitleColor(R.color.custom_dark_grey);
                    stepsGraph.getGridLabelRenderer().setGridColor(R.color.custom_dark_grey);
                    stepsGraph.getGridLabelRenderer().setHorizontalLabelsColor(R.color.custom_dark_grey);
                    stepsGraph.getGridLabelRenderer().setVerticalLabelsColor(R.color.custom_dark_grey);
                    stepsGraph.getGridLabelRenderer().setLabelFormatter(formatter);

                    PointsGraphSeries<DataPoint> moodSeries = new PointsGraphSeries<>(moodDataPoints);
                    moodSeries.setColor(ContextCompat.getColor(requireContext(),R.color.correct));
                    moodGraph.addSeries(moodSeries);
                    moodGraph.getViewport().setYAxisBoundsManual(true);
                    moodGraph.getViewport().setMinY(1);
                    moodGraph.getViewport().setMaxY(5);
                    moodGraph.getViewport().setScrollable(true); // enables horizontal scrolling
                    moodGraph.getViewport().setScalable(true);
                    moodGraph.setTitle("Sentimientos diarios");
                    moodGraph.setTitleColor(R.color.custom_dark_grey);
                    moodGraph.getGridLabelRenderer().setGridColor(R.color.custom_dark_grey);
                    moodGraph.getGridLabelRenderer().setHorizontalLabelsColor(R.color.custom_dark_grey);
                    moodGraph.getGridLabelRenderer().setVerticalLabelsColor(R.color.custom_dark_grey);
                    moodGraph.getGridLabelRenderer().setLabelFormatter(moodFormatter);
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
                ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
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
        requireActivity().registerReceiver(receiver,params);
    }

    @Override
    public void onPause(){
        super.onPause();
        requireActivity().unregisterReceiver(receiver);
    }
}