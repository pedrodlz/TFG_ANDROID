package com.tfg.healthwatch;

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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;

import java.io.Serializable;
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
    private String mBluetoothDeviceAddress;
    private ArrayList<String> scannedStringArray = new ArrayList<String>();
    private ArrayList<BluetoothObject> scannedDevices = new ArrayList<BluetoothObject>();
    private BluetoothGatt mBluetoothGatt;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private boolean notificationOn = false;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private String TAG = "BLEService";
    private static final String GET_CONNECTED_INTENT = "com.tfg.healthwatch.GET_CONNECTED";
    private static String CONNECTED_LIST_INTENT = "com.tfg.healthwatch.CONNECTED_LIST";
    static final String SCAN_INTENT = "com.tfg.healthwatch.SCAN";
    private static final String SCANNED_INTENT = "com.tfg.healthwatch.SCANNED_DEVICES";


    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    public final static UUID HEART_RATE_SERVICE =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public final static UUID HEART_RATE_CPOINT_CHAR =
            UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    public final static UUID CLIENT_CHARACTERISTIC_CONFIGURATION =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID Battery_Service_UUID =
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Level_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");


    public BLEService(){};

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BLEService getService() {
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

        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(receiver);
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
                mBluetoothDeviceAddress = null;
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.d(TAG,"GATT SUCCESS");
                if(!notificationOn){

                    BluetoothGattCharacteristic characteristic = gatt.getService(HEART_RATE_SERVICE).getCharacteristic(UUID_HEART_RATE_MEASUREMENT);
                    gatt.setCharacteristicNotification(characteristic,true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    gatt.writeDescriptor(descriptor);

                    notificationOn = true;
                }
            }
            else{
                //Log.d(TAG,"GATT UNSUCCESS");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(HEART_RATE_SERVICE)
                            .getCharacteristic(HEART_RATE_CPOINT_CHAR);
            characteristic.setValue(new byte[]{1, 1});
            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            Log.d(TAG,"Characteristic Changed");
            readCharacteristic(characteristic);
        }
    };

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

        Set pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d("Paired devices",pairedDevices.toString());
        /*BluetoothGattService batteryService = mBluetoothGatt.getService(Battery_Service_UUID);

        List<BluetoothGattService> servicesList;
        servicesList = getSupportedGattServices();
        Iterator<BluetoothGattService> iter = servicesList.iterator();
        while (iter.hasNext()) {
            BluetoothGattService bService = (BluetoothGattService) iter.next();
            if (bService.getUuid().toString().equals(Battery_Level_UUID)){
                batteryService = bService;
            }
        }
        if(batteryService == null) {
            Log.d(TAG, "Battery service not found!");
            return;
        }


        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(Battery_Level_UUID);
        if(batteryLevel == null) {
            Log.d(TAG, "Battery level not found!");
            return;
        }
        mBluetoothGatt.readCharacteristic(batteryLevel);
        Log.v(TAG, "batteryLevel = " + mBluetoothGatt.readCharacteristic(batteryLevel));*/
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
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

    public void readCharacteristic(BluetoothGattCharacteristic characteristic){

        if(mBluetoothGatt != null){
            int flag = characteristic.getProperties();
            int format = -1;
            if((flag & 0x01) != 0){
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            }else{
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }

            Integer heartRate = characteristic.getIntValue(format,1);

            FirebaseUser user = mAuth.getCurrentUser();
            String userId = user.getUid();

            LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
            String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Heart Rates").child(userId).child(stringDate);

            ref.push().setValue(heartRate);

            sendBroadcast(new Intent("com.tfg.healthwatch.HEART_RATE").putExtra("heartRate",heartRate.toString()));

            Log.d(TAG,"HEART RATE: "+heartRate);
        }
    }
}