package com.tfg.healthwatch.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.MainActivity;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.dummy.DummyContent;


public class AlertsFragment extends Fragment {

    private String TAG = "AlertsFragment";
    private FirebaseUser currentUser;
    private DatabaseReference userAlerts;
    private ConstraintLayout mSOSButton;
    private CheckBox mFallCheckbox,weightCheckbox,heartRateCheckbox,batteryCheckbox, phoneCheckbox;
    private EditText weightEdit,heartRateEdit,batteryEdit, phoneEdit;
    private String phoneNumber = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState){
        View root = inflater.inflate(R.layout.fragment_alerts, container, false);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userAlerts = FirebaseDatabase.getInstance().getReference().child("Alerts").child(currentUser.getUid());

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CALL_PHONE}, 1);
        }

        mSOSButton = root.findViewById(R.id.sos_layout);
        mFallCheckbox = root.findViewById(R.id.falls_checkbox);
        weightCheckbox = root.findViewById(R.id.weight_check_box);
        heartRateCheckbox = root.findViewById(R.id.heart_rate_checkbox);
        batteryCheckbox = root.findViewById(R.id.battery_checkbox);
        phoneCheckbox = root.findViewById(R.id.phone_checkbox);
        weightEdit = root.findViewById(R.id.weight_edit_text);
        heartRateEdit = root.findViewById(R.id.heart_rate_edit_text);
        batteryEdit = root.findViewById(R.id.battery_edit_text);
        phoneEdit = root.findViewById(R.id.phone_edit_text);

        userAlerts.child("fallSensor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String checked = snapshot.child("checked").toString();

                if(!checked.isEmpty()) mFallCheckbox.setChecked((Boolean) snapshot.child("checked").getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Read weight failed: \n" + error);
            }
        });

        userAlerts.child("weight").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String limit = snapshot.child("limit").getValue().toString();
                String checked = snapshot.child("checked").getValue().toString();

                if(!limit.isEmpty()){
                    weightEdit.setText(limit);
                    if(!checked.isEmpty()) weightCheckbox.setChecked((Boolean) snapshot.child("checked").getValue());
                }
                else{
                    weightCheckbox.setChecked(false);
                    userAlerts.child("weight").child("checked").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Read weight failed: \n" + error);
            }
        });

        userAlerts.child("heartRate").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String limit = snapshot.child("limit").getValue().toString();
                String checked = snapshot.child("checked").getValue().toString();

                if(!limit.isEmpty()){
                    heartRateEdit.setText(limit);
                    if(!checked.isEmpty()) heartRateCheckbox.setChecked((Boolean) snapshot.child("checked").getValue());
                }
                else{
                    heartRateCheckbox.setChecked(false);
                    userAlerts.child("heartRate").child("checked").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Read heartRate failed: \n" + error);
            }
        });

        userAlerts.child("battery").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String limit = snapshot.child("limit").getValue().toString();
                String checked = snapshot.child("checked").getValue().toString();

                if(!limit.isEmpty()){
                    batteryEdit.setText(limit);
                    if(!checked.isEmpty()) batteryCheckbox.setChecked((Boolean) snapshot.child("checked").getValue());
                }
                else{
                    batteryCheckbox.setChecked(false);
                    userAlerts.child("battery").child("checked").setValue(false);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Read weight failed: \n" + error);
            }
        });

        userAlerts.child("emergencyNumber").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String limit = snapshot.child("limit").getValue().toString();
                String checked = snapshot.child("checked").getValue().toString();

                if(!limit.isEmpty()){
                    phoneEdit.setText(limit);
                    phoneNumber = limit;
                    if(!checked.isEmpty()) phoneCheckbox.setChecked((Boolean) snapshot.child("checked").getValue());
                }
                else{
                    phoneCheckbox.setChecked(false);
                    userAlerts.child("emergencyNumber").child("checked").setValue(false);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Read weight failed: \n" + error);
            }
        });


        mSOSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!phoneNumber.isEmpty()){
                    Log.d(TAG,"Phone NUmber: " + phoneNumber);
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:"+phoneNumber));

                    if (ActivityCompat.checkSelfPermission(getContext(),
                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CALL_PHONE}, 1);
                    }else startActivity(callIntent);
                }
            }
        });

        mFallCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userAlerts.child("fallSensor").child("checked").setValue(isChecked);
            }
        });

        weightCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userAlerts.child("weight").child("checked").setValue(isChecked);
            }
        });

        heartRateCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userAlerts.child("heartRate").child("checked").setValue(isChecked);
            }
        });

        batteryCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userAlerts.child("battery").child("checked").setValue(isChecked);
            }
        });

        phoneCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userAlerts.child("emergencyNumber").child("checked").setValue(isChecked);
            }
        });


        weightEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    String value = weightEdit.getText().toString();
                    if(!value.isEmpty()){
                        userAlerts.child("weight").child("limit").setValue(value);
                    }
                }
            }
        });

        heartRateEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    String value = heartRateEdit.getText().toString();
                    if(!value.isEmpty()){
                        userAlerts.child("heartRate").child("limit").setValue(value);
                    }
                }
            }
        });

        batteryEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    String value = batteryEdit.getText().toString();
                    if(!value.isEmpty()){
                        userAlerts.child("battery").child("limit").setValue(value);
                    }
                }
            }
        });

        phoneEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    String value = phoneEdit.getText().toString();
                    if(!value.isEmpty()){
                        userAlerts.child("emergencyNumber").child("limit").setValue(value);
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}