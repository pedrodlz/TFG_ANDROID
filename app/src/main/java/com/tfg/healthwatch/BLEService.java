package com.tfg.healthwatch;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BLEService extends Service {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private static final String CHANNEL_ID = "mainChannel";
    private String mBluetoothDeviceAddress;
    private ArrayList<String> scannedStringArray = new ArrayList<String>();
    private ArrayList<BluetoothObject> scannedDevices = new ArrayList<BluetoothObject>();
    private BluetoothGatt mBluetoothGatt;
    private FirebaseUser currentUser;
    private boolean heartRateNotificationOn = false;
    private boolean batteryNotificationOn = false;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private DatabaseReference activityTable;
    private DatabaseReference alertsTable;
    private String stepsLimit, heartRateLimit,batteryLimit,emergencyNumber;
    private Boolean stepsChecked, heartRatesChecked, batteryChecked, emergencyChecked;
    private String TAG = "BLEService";
    private static final String GET_CONNECTED_INTENT = "com.tfg.healthwatch.GET_CONNECTED";
    private static String CONNECTED_LIST_INTENT = "com.tfg.healthwatch.CONNECTED_LIST";
    static final String SCAN_INTENT = "com.tfg.healthwatch.SCAN";
    private static final String SCANNED_INTENT = "com.tfg.healthwatch.SCANNED_DEVICES";
    private static final String BATTERY_INTENT = "com.tfg.healthwatch.BATTERY_LEVEL";

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb"; //this is common for all BTLE devices. see http://stackoverflow.com/questions/18699251/finding-out-android-bluetooth-le-gatt-profiles

    public final static UUID HEART_RATE_SERVICE =
            UUID.fromString(String.format(BASE_UUID, "180d"));
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(String.format(BASE_UUID, "2a37"));
    public final static UUID HEART_RATE_CPOINT_CHAR =
            UUID.fromString(String.format(BASE_UUID, "2a39"));
    public final static UUID CLIENT_CHARACTERISTIC_CONFIGURATION =
            UUID.fromString(String.format(BASE_UUID, "2902"));

    private static final UUID BATTERY_SERVICE_UUID =
            UUID.fromString(String.format(BASE_UUID, "180F"));
    private static final UUID BATTERY_LEVEL_UUID =
            UUID.fromString(String.format(BASE_UUID, "2a19"));

    private static final UUID UUID_SERVICE_MIBAND_SERVICE =
            UUID.fromString(String.format(BASE_UUID, "FEE0"));
    public static final UUID UUID_CHARACTERISTIC_REALTIME_STEPS =
            UUID.fromString("00000007-0000-3512-2118-0009af100700");


    public BLEService(){};

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public BLEService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BLEService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        IntentFilter params = new IntentFilter();
        params.addAction(SCAN_INTENT);
        params.addAction(GET_CONNECTED_INTENT);
        this.registerReceiver(receiver,params);
        initialize();
        getConnectedDevices();
        getbattery();
        return super.onStartCommand(intent, flags, startId);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(GET_CONNECTED_INTENT)){
                getConnectedDevices();
            }
            else if(action.equals(SCAN_INTENT)){
                scanDevices();
            }
        }
    };

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if(!mBluetoothAdapter.isEnabled()){
            requestBluetooth();
        }

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        activityTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid()).child(stringDate);
        alertsTable = FirebaseDatabase.getInstance().getReference().child("Alerts").child(currentUser.getUid());

        alertsTable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.child("steps").exists()){
                    String limit = snapshot.child("steps").child("limit").getValue().toString();
                    String checked = snapshot.child("steps").child("checked").getValue().toString();

                    if(!limit.isEmpty()){
                        if(!checked.isEmpty()) stepsChecked = (Boolean) snapshot.child("steps").child("checked").getValue();
                        if(stepsLimit != limit) getSteps();
                        stepsLimit = limit;
                    }
                    else{
                        stepsChecked =false;
                    }
                }

                if(snapshot.child("heartRate").exists()){
                    String limit = snapshot.child("heartRate").child("limit").getValue().toString();
                    String checked = snapshot.child("heartRate").child("checked").getValue().toString();

                    if(!limit.isEmpty()){
                        heartRateLimit = limit;
                        if(!checked.isEmpty()) heartRatesChecked = (Boolean) snapshot.child("heartRate").child("checked").getValue();
                    }
                    else{
                        heartRatesChecked = false;
                    }
                }

                if(snapshot.child("battery").exists()){
                    String limit = snapshot.child("battery").child("limit").getValue().toString();
                    String checked = snapshot.child("battery").child("checked").getValue().toString();

                    if(!limit.isEmpty()){
                        batteryLimit = limit;
                        if(!checked.isEmpty()) batteryChecked=(Boolean) snapshot.child("battery").child("checked").getValue();
                        getbattery();
                    }
                    else{
                        batteryChecked = false;
                    }
                }

                if(snapshot.child("emergencyNumber").exists()){
                    String limit = snapshot.child("emergencyNumber").child("limit").getValue().toString();
                    String checked = snapshot.child("emergencyNumber").child("checked").getValue().toString();

                    if(!limit.isEmpty()){
                        emergencyNumber = limit;
                        if(!checked.isEmpty()) emergencyChecked = (Boolean) snapshot.child("emergencyNumber").child("checked").getValue();
                    }
                    else{
                        emergencyChecked = false;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        getAvgHeartRate();

        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(receiver);
        stopNotifications();
        super.onDestroy();
    }

    public boolean checkBluetooh(){
        return mBluetoothAdapter.isEnabled();
    }

    public void requestBluetooth(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(enableBtIntent);
    }

    public void disableBluetooth(){
        mBluetoothAdapter.disable();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                mBluetoothDeviceAddress = gatt.getDevice().getAddress();
                getConnectedDevices();
                // Attempts to discover services after successful connection.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                gatt.close();
                mBluetoothDeviceAddress = null;
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.d(TAG,"GATT SUCCESS");
                if(!heartRateNotificationOn){

                    BluetoothGattCharacteristic characteristic = gatt.getService(HEART_RATE_SERVICE).getCharacteristic(UUID_HEART_RATE_MEASUREMENT);
                    gatt.setCharacteristicNotification(characteristic,true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    gatt.writeDescriptor(descriptor);

                    heartRateNotificationOn = true;
                }

                /*if(!batteryNotificationOn){

                    BluetoothGattCharacteristic characteristic = gatt.getService(BATTERY_SERVICE_UUID).getCharacteristic(BATTERY_LEVEL_UUID);

                    gatt.setCharacteristicNotification(characteristic,true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    gatt.writeDescriptor(descriptor);

                    batteryNotificationOn = true;
                }*/
            }
            else{
                //Log.d(TAG,"GATT UNSUCCESS");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                UUID charUUID = descriptor.getCharacteristic().getUuid();
                if(charUUID.equals(UUID_HEART_RATE_MEASUREMENT)){
                    BluetoothGattCharacteristic characteristic =
                        gatt.getService(HEART_RATE_SERVICE)
                                .getCharacteristic(HEART_RATE_CPOINT_CHAR);
                    characteristic.setValue(new byte[]{1, 1});
                    gatt.writeCharacteristic(characteristic);
                }
                else if(charUUID.equals(BATTERY_LEVEL_UUID)){
                    getbattery();
                }

            }
            else{
                Log.d(TAG,"Error writing descriptor");
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            UUID charUUID = characteristic.getUuid();
            Log.d(TAG,"Characteristic Write");
            if(charUUID.equals(HEART_RATE_CPOINT_CHAR)){
                BluetoothGattCharacteristic batteryLevel = gatt.getService(BATTERY_SERVICE_UUID).getCharacteristic(BATTERY_LEVEL_UUID);
                gatt.setCharacteristicNotification(batteryLevel,true);

                BluetoothGattDescriptor descriptor = batteryLevel.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                gatt.writeDescriptor(descriptor);
                batteryNotificationOn = true;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            Log.d(TAG,"Characteristic Changed");
            UUID charUUID = characteristic.getUuid();
            if(charUUID.equals(UUID_HEART_RATE_MEASUREMENT)){
                readHeartRate(characteristic);
            }
            else if(charUUID.equals(BATTERY_LEVEL_UUID)){
                readBatteryLevel(characteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG,"onCharacteristicRead");
            if(status == BluetoothGatt.GATT_SUCCESS) {
                UUID charUUID = characteristic.getUuid();
                if(charUUID.equals(BATTERY_LEVEL_UUID)){
                    readBatteryLevel(characteristic);
                }
                else if(charUUID.equals(UUID_CHARACTERISTIC_REALTIME_STEPS)){
                    readSteps(characteristic);
                }
            }
            else{
                Log.d(TAG,"Cannot read characteristic");
            }
        }
    };

    public void readSteps(BluetoothGattCharacteristic characteristic){

        byte[] data = characteristic.getValue();

        if (data.length >= 13) {
            int steps, distance, calories;

            steps =    ((((data[1] & 255) | ((data[2] & 255) << 8))) );
            distance = ((((data[5] & 255) | ((data[6] & 255) << 8)) | (data[7] & 16711680)) | ((data[8] & 255) << 24));
            calories = ((((data[9] & 255) | ((data[10] & 255) << 8)) | (data[11] & 16711680)) | ((data[12] & 255) << 24));

            Log.d("Steps:", steps+"");
            Log.d("distance:", distance+"");
            Log.d("calories:", calories+"");

            activityTable.child("Steps").setValue(steps);
            activityTable.child("Distance").setValue(distance);
            activityTable.child("Calories").setValue(calories);

            if(stepsLimit != null && stepsChecked){
                if(steps >= Integer.parseInt(stepsLimit)){
                    createNotification(R.drawable.ic_footprint_svgrepo_com,getString(R.string.steps_alert),getString(R.string.steps_alert_message)+stepsLimit+" "+getString(R.string.steps).toLowerCase()+"!",2);
                }
            }
        }
    }

    public void stopNotifications(){
        if(heartRateNotificationOn){
            BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(HEART_RATE_SERVICE).getCharacteristic(UUID_HEART_RATE_MEASUREMENT);
            mBluetoothGatt.setCharacteristicNotification(characteristic,false);
            heartRateNotificationOn = false;
        }

        if(batteryNotificationOn){
            BluetoothGattCharacteristic batteryLevel = mBluetoothGatt.getService(BATTERY_SERVICE_UUID).getCharacteristic(BATTERY_LEVEL_UUID);
            mBluetoothGatt.setCharacteristicNotification(batteryLevel,false);
            batteryNotificationOn = false;
        }

    }

    public final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG,": onScanResult" + result.getDevice().toString());

            if(!scannedStringArray.contains(result.getDevice().getAddress())){
                scannedStringArray.add(result.getDevice().getAddress());
                scannedDevices.add(new BluetoothObject(result.getDevice().getName()==null? "No name":result.getDevice().getName(),result.getDevice().getAddress()));

                Bundle bundle = new Bundle();
                bundle.putSerializable("scannedDevices",(Serializable)scannedDevices);
                sendBroadcast(new Intent(SCANNED_INTENT).putExtras(bundle));
            }
        }

        @Override
        public void onScanFailed(int errorCode){
            Log.d(TAG,": onScanFailed"+errorCode);
        }
    };

    public void getbattery() {

        if(mBluetoothGatt != null){
            BluetoothGattService batteryService = mBluetoothGatt.getService(BATTERY_SERVICE_UUID);
            if(batteryService == null) {
                Log.d(TAG, "Battery service not found!");
                return;
            }

            BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(BATTERY_LEVEL_UUID);
            if(batteryLevel == null) {
                Log.d(TAG, "Battery level not found!");
                return;
            }

            mBluetoothGatt.readCharacteristic(batteryLevel);
        }
    }

    public void getAvgHeartRate(){

        activityTable.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("Heart Rates").exists()){
                    int totalHeartRates = (int) snapshot.child("Heart Rates").getChildrenCount();
                    Integer avgHeartRate = 0;

                    for (DataSnapshot child: snapshot.child("Heart Rates").getChildren()) {
                        Integer rate = child.getValue(Integer.class);
                        avgHeartRate += rate;
                    }
                    avgHeartRate = avgHeartRate / totalHeartRates;

                    if(avgHeartRate != 0){
                        activityTable.child("Average Heart Rate").setValue(avgHeartRate);
                    }

                    if(!snapshot.child("date").exists()){
                        SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'");

                        String now = ISO_8601_FORMAT.format(new Date());
                        activityTable.child("date").setValue(now);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        getSteps();
    }

    public void getSteps(){
        Log.d(TAG,UUID_SERVICE_MIBAND_SERVICE.toString());
        if(mBluetoothGatt != null){
            BluetoothGattService miBandService = mBluetoothGatt.getService(UUID_SERVICE_MIBAND_SERVICE);
            if(miBandService == null) {
                Log.d(TAG, "miBandService service not found!");
                return;
            }

            BluetoothGattCharacteristic stepsCharacteristic = miBandService.getCharacteristic(UUID_CHARACTERISTIC_REALTIME_STEPS);
            if(stepsCharacteristic == null) {
                Log.d(TAG, "stepsCharacteristic not found!");
                return;
            }
            mBluetoothGatt.readCharacteristic(stepsCharacteristic);
        }
    }

    public void getConnectedDevices(){

        List<BluetoothDevice> connectedArray = null;
        ArrayList<BluetoothObject> devicesArray = null;

        if(mBluetoothAdapter.isEnabled()){
            devicesArray = new ArrayList<BluetoothObject>();
            connectedArray = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

            if(connectedArray.size() > 0){

                if(mBluetoothDeviceAddress != connectedArray.get(0).getAddress()){
                    connect(connectedArray.get(0).getAddress());
                }

                for(BluetoothDevice device : connectedArray){
                    devicesArray.add(new BluetoothObject(device.getName(),device.getAddress()));
                }
            }
        }

        Bundle bundle = new Bundle();
        bundle.putSerializable("connectedDevices",(Serializable)devicesArray);
        sendBroadcast(new Intent(CONNECTED_LIST_INTENT).putExtras(bundle));
    }

    public void scanDevices(){
        String subTag = "scanDevices";

        try{
            Handler mHandler = new Handler(Looper.getMainLooper());

            mHandler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                }
            },8000);

            Log.d(TAG+subTag,"Scanning...");
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);
        } catch (Exception e){
            Log.d(TAG+subTag,e.toString());
        }
    }

    public boolean connect(String address) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if(mBluetoothGatt == null){
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
            Log.d(TAG, "Trying to create a new connection.");
            mConnectionState = BluetoothProfile.STATE_CONNECTING;
        }

        return true;
    }

    public void readHeartRate(BluetoothGattCharacteristic characteristic){

        if(mBluetoothGatt != null){
            int flag = characteristic.getProperties();
            int format = -1;
            if((flag & 0x01) != 0){
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            }else{
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }

            Integer heartRate = characteristic.getIntValue(format,1);
            activityTable.child("Heart Rates").push().setValue(heartRate);

            if(heartRateLimit != null && heartRatesChecked) {

                if (heartRate >= Integer.parseInt(heartRateLimit)) {
                    if(emergencyNumber != null && emergencyChecked){
                        sendBroadcast(new Intent("com.tfg.healthwatch.EMERGENCY_CALL").putExtra("emergencyNumber",emergencyNumber));
                    }
                    else createNotification(R.drawable.ic_heartbeat_svgrepo_com,getString(R.string.heart_rate_alert),getString(R.string.heart_rate_alert_message)+heartRateLimit,3);
                }
            }

            sendBroadcast(new Intent("com.tfg.healthwatch.HEART_RATE").putExtra("heartRate",heartRate.toString()));

            Log.d(TAG,"HEART RATE: "+heartRate);
            getAvgHeartRate();
        }
    }

    public void readBatteryLevel(BluetoothGattCharacteristic characteristic){
        String batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString();

        if(batteryLimit != null && batteryChecked) {
            if (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) <= Integer.parseInt(batteryLimit)) {
                createNotification(R.drawable.ic_low_battery_svgrepo_com,getString(R.string.battery_alert),getString(R.string.battery_alert_message)+batteryLimit+"%",1);
            }
        }

        sendBroadcast(new Intent(BATTERY_INTENT).putExtra("batteryLevel",batteryLevel));
    }

    private void createNotification(int icon, String title, String text, int id){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // notificationId is a unique int for each notification that you must define
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(id, builder.build());
    }
}