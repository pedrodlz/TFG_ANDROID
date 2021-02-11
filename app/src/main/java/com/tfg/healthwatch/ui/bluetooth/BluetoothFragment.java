package com.tfg.healthwatch.ui.bluetooth;

import android.app.Activity;
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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tfg.healthwatch.BLEService;
import com.tfg.healthwatch.BluetoothListAdapter;
import com.tfg.healthwatch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothFragment extends Fragment{

    private BluetoothViewModel bluetoothViewModel;
    private BLEService bleService;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private View root;
    private String TAG = "BluetoothFragment";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        bluetoothViewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        root = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        bleService = new BLEService("DD:80:0D:1E:D0:53",getActivity(),root);

        if(bleService.checkBluetooth()){
            Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);

            if(mBluetoothAdapter.isEnabled()){
                bluetooth_switch.setChecked(true);
                bleService.getConnectedDevices();
            }else{
                bluetooth_switch.setChecked(false);
            }

            startListeners();
        }

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);
        if(resultCode == Activity.RESULT_OK){
            // bluetooth enabled
            bluetooth_switch.setChecked(true);
            bleService.createToast(getContext(),"Bluetooth On");
        }else{
            bluetooth_switch.setChecked(false);
            bleService.createToast(getContext(),"Bluetooth Off");
        }
    }

    public void startListeners(){

        String subTag = ": startListeners";
        Log.d(TAG+subTag,"Starting listeners");

        Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);
        Button searchButton = (Button) root.findViewById(R.id.search_button);

        bluetooth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Log.d(TAG+subTag,"Attempting to activate Bluetooth...");
                    bleService.requestBluetooth();
                } else {
                    Log.d(TAG+subTag,"Disconnecting Bluetooth");
                    mBluetoothAdapter.disable();
                    if(!mBluetoothAdapter.isEnabled()) {
                        bluetooth_switch.setChecked(false);
                        bleService.createToast(getContext(),"Bluetooth Off");
                    }
                }
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleService.scanDevices(v);
                bleService.getConnectedDevices();
            }
        });
    }
}