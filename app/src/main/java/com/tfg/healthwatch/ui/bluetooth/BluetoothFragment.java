package com.tfg.healthwatch.ui.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.tfg.healthwatch.BluetoothListAdapter;
import com.tfg.healthwatch.MainActivity;
import com.tfg.healthwatch.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BluetoothFragment extends Fragment{

    private BluetoothViewModel bluetoothViewModel;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_ENABLE_BT = 0;
    private View root;
    private String tag = "BluetoothFragment";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        bluetoothViewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        root = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);
        Button searchButton = (Button) root.findViewById(R.id.search_button);

        if (mBluetoothAdapter == null) {
            Log.d(tag, "Cannot start bluetooth adapter");
        } else {
            bluetooth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        bluetooth_switch.setChecked(true);
                    } else {
                        mBluetoothAdapter.disable();
                        bluetooth_switch.setChecked(false);
                    }
                }
            });

            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanDevices(v);
                }
            });

            if(mBluetoothAdapter.isEnabled()){
                getConnectedDevices();
            }
            bluetooth_switch.setChecked(mBluetoothAdapter.isEnabled());
        }

        return root;
    }

    private void getConnectedDevices(){
        Set<BluetoothDevice> devices_set = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothObject> devicesArray = null;

        if(devices_set.size() > 0){
            devicesArray = new ArrayList<BluetoothObject>();
            ListView connectedDevicesList = (ListView) root.findViewById(R.id.connected_devices_list);

            for(BluetoothDevice device : devices_set){
                devicesArray.add(new BluetoothObject(device.getName(),device.getAddress()));
            }

            BluetoothListAdapter bAdapter = new BluetoothListAdapter(
                    getContext(),R.layout.bluetooth_list_view,devicesArray);
            connectedDevicesList.setAdapter(bAdapter);
        }
    }


    public void scanDevices(View v){
        ArrayList<String> scannedStringArray = new ArrayList<String>();
        ArrayList<BluetoothObject> scannedDevices = new ArrayList<BluetoothObject>();

        BluetoothListAdapter bAdapter = new BluetoothListAdapter(
                getContext(),R.layout.bluetooth_list_view,scannedDevices);

        ListView scannedDevicesList = (ListView) root.findViewById(R.id.search_devices_list);
        scannedDevicesList.setAdapter(bAdapter);

        ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d("onScanResult",result.getDevice().toString());

                if(result.getDevice().getName() != null){
                    if(!scannedStringArray.contains(result.getDevice().getAddress())){
                        scannedStringArray.add(result.getDevice().getAddress());
                        scannedDevices.add(new BluetoothObject(result.getDevice().getName(),result.getDevice().getAddress()));
                        bAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode){
                Log.d("onScanFailed",""+errorCode);
            }
        };

        try{
            Handler mHandler = new Handler(Looper.getMainLooper());

            mHandler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                }
            },8000);
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);
            Log.d("startScan","Scanning...");
        } catch (Exception e){
            Log.d("startScan ERROR",e.toString());
        }
    }
}