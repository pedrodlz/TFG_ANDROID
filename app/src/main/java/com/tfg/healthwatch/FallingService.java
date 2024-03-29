package com.tfg.healthwatch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class FallingService extends Service implements SensorEventListener {

    private static final String TAG = "FallingService";
    private final IBinder binder = new LocalBinder();
    private FirebaseUser currentUser;
    private DatabaseReference userAlerts;
    private SensorManager mSensorManager;
    private Sensor mGravity;
    private Boolean sensorActivated = false;
    private String phoneNumber;

    public FallingService() {
    }

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userAlerts = FirebaseDatabase.getInstance().getReference().child("Alerts").child(currentUser.getUid());

        userAlerts.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.child("fallSensor").exists()){
                    if(snapshot.child("fallSensor").child("checked").getValue() != null){
                        String checked = snapshot.child("fallSensor").child("checked").getValue().toString();

                        if(!checked.isEmpty()) sensorActivated = (Boolean) snapshot.child("fallSensor").child("checked").getValue();
                        else sensorActivated=false;
                    }
                    else sensorActivated=false;

                    if(sensorActivated){
                        startGravitySensor();
                    }
                    else{
                        if(mSensorManager != null) {
                            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && mGravity != null) {
                                mSensorManager.unregisterListener(FallingService.this, mGravity);
                            }
                        }
                    }
                }

                if(snapshot.child("emergencyNumber").exists()){
                    phoneNumber = snapshot.child("emergencyNumber").child("limit").getValue().toString();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"Falling started");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        FallingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return FallingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            double loX = event.values[0];
            double loY = event.values[1];
            double loZ = event.values[2];

            double loAccelerationReader = Math.sqrt(Math.pow(loX, 2)
                    + Math.pow(loY, 2)
                    + Math.pow(loZ, 2));

            DecimalFormat precision = new DecimalFormat("0,00");
            double ldAccRound = 0;
            try{
                String formatted = precision.format(loAccelerationReader);
                ldAccRound = Double.parseDouble(formatted);
            }
            catch (NumberFormatException e){
                ldAccRound = loAccelerationReader/10;
            }

            if (ldAccRound > 0.3d && ldAccRound < 0.5d) {
                //Do your stuff
                Log.e(TAG,"FALL_DETECTED");

                sendBroadcast(new Intent("com.tfg.healthwatch.FALL_DETECTED").putExtra("emergencyNumber",phoneNumber));
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void startGravitySensor(){

        if(mSensorManager != null){
            if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if(mGravity != null){
                    Log.d(TAG,"Gravity not null");
                    if(mSensorManager.registerListener(this,mGravity, SensorManager.SENSOR_DELAY_NORMAL)){
                        Log.d(TAG,"Supported sensor");
                    }else{
                        Log.d(TAG,"NOT supported");
                    }
                }
                else{
                    Log.d(TAG,"Gravity NULL");
                }

            }
            else{
                Log.e(TAG,"Gravity sensor is not present");
            }
        }
        else{
            Log.e(TAG,"Unable to get system sensor service");
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.unregisterListener(this,mGravity);
        }
    }
}