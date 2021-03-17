package com.tfg.healthwatch;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;
import com.tfg.healthwatch.ui.home.HomeFragment;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private String mBluetoothDeviceAddress;
    private BluetoothDevice tempDevice;
    private ArrayList<String> scannedStringArray = new ArrayList<String>();
    private ArrayList<BluetoothObject> scannedDevices = new ArrayList<BluetoothObject>();
    private BluetoothListAdapter bAdapter;
    private BluetoothGatt mBluetoothGatt;
    private boolean notificationOn = false;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private static final int REQUEST_ENABLE_BT = 0;
    private String TAG = "BluetoothFragment";
    private ListView scannedDevicesListView;
    private TextView heartRateTextView;

    public final static UUID HEART_RATE_SERVICE =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public final static UUID HEART_RATE_CPOINT_CHAR =
            UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    public final static UUID CLIENT_CHARACTERISTIC_CONFIGURATION =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public void onCreate() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        intent.getStringExtra("");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                mBluetoothDeviceAddress = tempDevice.getAddress();

                // Attempts to discover services after successful connection.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                mBluetoothDeviceAddress = null;
                tempDevice = null;
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

            bAdapter = new BluetoothListAdapter(
                    getApplicationContext(),R.layout.bluetooth_list_view,scannedDevices);

            scannedDevicesListView.setAdapter(bAdapter);

            if(!scannedStringArray.contains(result.getDevice().getAddress())){
                scannedStringArray.add(result.getDevice().getAddress());
                scannedDevices.add(new BluetoothObject(result.getDevice().getName()==null? "No name":result.getDevice().getName(),result.getDevice().getAddress()));
                bAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode){
            Log.d(TAG,": onScanFailed"+errorCode);
        }
    };

    public ArrayList<BluetoothObject> getConnectedDevices(){

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

        return devicesArray;
    }

    public boolean checkBluetooth(){
        String subTag = ": checkBluetooth";
        if (mBluetoothAdapter == null) {
            Log.d(TAG+subTag, "Cannot start bluetooth adapter");
            return false;
        } else {
            Log.d(TAG+subTag, "Started bluetooth adapter");
            return true;
        }
    }

    public void createToast(Context context, String text){
        Toast toast = Toast.makeText(context,text,Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    public BluetoothListAdapter scanDevices(ListView scannedDevicesList){
        String subTag = "scanDevices";
        scannedDevicesListView = scannedDevicesList;

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

        return bAdapter;
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
            tempDevice = device;
        }

        return true;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic){
        String subTag = ": readCharacteristic";

        if(mBluetoothGatt != null){
            int flag = characteristic.getProperties();
            int format = -1;
            if((flag & 0x01) != 0){
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            }else{
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }

            Integer heartRate = characteristic.getIntValue(format,1);

            Intent intent = new Intent("heartRate"); //FILTER is a string to identify this intent
            intent.putExtra("heartRate", heartRate.toString());
            sendBroadcast(intent);

            Log.d(TAG,"HEART RATE: "+heartRate);
        }
    }
}