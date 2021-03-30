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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.tfg.healthwatch.DashboardActivity;
import com.tfg.healthwatch.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothFragment extends Fragment{

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<BluetoothObject> scannedDevices = new ArrayList<BluetoothObject>();
    private ArrayList<BluetoothObject> connectedDevices = new ArrayList<BluetoothObject>();
    private View root;
    private String TAG = "BluetoothFragment";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final String SCANNED_INTENT = "com.tfg.healthwatch.SCANNED_DEVICES";
    private static final String GET_CONNECTED_INTENT = "com.tfg.healthwatch.GET_CONNECTED";
    private static String CONNECTED_LIST_INTENT = "com.tfg.healthwatch.CONNECTED_LIST";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        IntentFilter params = new IntentFilter();
        params.addAction(SCANNED_INTENT);
        params.addAction(CONNECTED_LIST_INTENT);
        getActivity().registerReceiver(receiver,params);

        if(checkBluetooth()){
            Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);

            if(mBluetoothAdapter.isEnabled()){
                bluetooth_switch.setChecked(true);
                getContext().sendBroadcast(new Intent(GET_CONNECTED_INTENT));

            }else{
                bluetooth_switch.setChecked(false);
            }

            startListeners();
        }

        return root;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);
        if(resultCode == Activity.RESULT_OK){
            // bluetooth enabled
            bluetooth_switch.setChecked(true);
            Toast.makeText(getContext(),"Bluetooth ON",Toast.LENGTH_SHORT);
        }else{
            bluetooth_switch.setChecked(false);
            Toast.makeText(getContext(),"Bluetooth OFF",Toast.LENGTH_SHORT);
        }
    }

    public void requestBluetooth(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public void setScannedDevices(ArrayList<BluetoothObject> scanned){
        scannedDevices = scanned;

        BluetoothListAdapter bAdapter = new BluetoothListAdapter(
                getContext(),R.layout.bluetooth_list_view,scannedDevices);

        ListView searchDevice = root.findViewById(R.id.search_devices_list);
        searchDevice.setAdapter(bAdapter);
    }

    public void setConnectedList(ArrayList<BluetoothObject> connected){
        connectedDevices = connected;

        BluetoothListAdapter bAdapter = new BluetoothListAdapter(
                getContext(),R.layout.bluetooth_list_view,connectedDevices);

        ListView connectedDevices = root.findViewById(R.id.connected_devices_list);
        connectedDevices.setAdapter(bAdapter);
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(SCANNED_INTENT)){
                setScannedDevices((ArrayList<BluetoothObject>) intent.getExtras().getSerializable("scannedDevices"));
            }
            else if (action.equals(CONNECTED_LIST_INTENT)){
                Toast.makeText(getContext(), "Connected List send", Toast.LENGTH_SHORT).show();
                setConnectedList((ArrayList<BluetoothObject>) intent.getExtras().getSerializable("connectedDevices"));
            }
        }
    };

    public void startListeners(){

        String subTag = ": startListeners";
        Log.d(TAG+subTag,"Starting listeners");

        Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);
        Button searchButton = (Button) root.findViewById(R.id.search_button);

        bluetooth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Toast.makeText(getContext(),"Attempting to activate Bluetooth...",Toast.LENGTH_SHORT);
                    requestBluetooth();
                } else {
                    Toast.makeText(getContext(),"Disconnecting Bluetooth",Toast.LENGTH_SHORT);
                    mBluetoothAdapter.disable();
                }
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().sendBroadcast(new Intent().setAction("com.tfg.healthwatch.SCAN"));
                getContext().sendBroadcast(new Intent(GET_CONNECTED_INTENT));
            }
        });
    }
}