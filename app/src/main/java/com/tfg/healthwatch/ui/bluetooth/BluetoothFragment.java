package com.tfg.healthwatch.ui.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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

    private static final int REQUEST_FINE_LOCATION = 1;
    private ArrayList<BluetoothObject> scannedDevices = new ArrayList<BluetoothObject>();
    private ArrayList<BluetoothObject> connectedDevices = new ArrayList<BluetoothObject>();
    private View root;
    private String TAG = "BluetoothFragment";
    private static final String SCANNED_INTENT = "com.tfg.healthwatch.SCANNED_DEVICES";
    static final String SCAN_INTENT = "com.tfg.healthwatch.SCAN";
    private static final String GET_CONNECTED_INTENT = "com.tfg.healthwatch.GET_CONNECTED";
    private static String CONNECTED_LIST_INTENT = "com.tfg.healthwatch.CONNECTED_LIST";
    private BLEService mService;
    private BluetoothListAdapter connectedAdapter;
    private BluetoothListAdapter scannedAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        mService = DashboardActivity.getBleService();

        startListeners();

        getContext().sendBroadcast(new Intent(GET_CONNECTED_INTENT));

        return root;
    }

    private void checkPermission(String permission){
        Integer returnValue = getActivity().checkSelfPermission(permission);
        if (returnValue == PackageManager.PERMISSION_DENIED){
            requestPermissions(new String[]{permission},REQUEST_FINE_LOCATION);
        }
        else{
            checkLocationStatus();
        }
    }

    public void checkLocationStatus(){
        LocationManager lm = (LocationManager)
                getActivity().getSystemService(Context. LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(getContext() )
                    .setMessage( "GPS Enable" )
                    .setPositiveButton( "Settings" , new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                }
                            })
                    .setNegativeButton( "Cancel" , null )
                    .show() ;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationStatus();

                } else {

                    new AlertDialog.Builder(getContext())
                        .setMessage("Accept location permissions in order to search for devices")
                        .setTitle("Location permissions")
                        .setPositiveButton("Ok", null)
                        .show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void setScannedDevices(){

        scannedAdapter = new BluetoothListAdapter(
                getContext(),R.layout.bluetooth_list_view,scannedDevices);

        ListView searchDevice = root.findViewById(R.id.search_devices_list);
        searchDevice.setAdapter(scannedAdapter);
    }

    public void setConnectedList(){
        connectedAdapter = new BluetoothListAdapter(
                getContext(),R.layout.bluetooth_list_view,connectedDevices);

        ListView connectedDevices = root.findViewById(R.id.connected_devices_list);
        connectedDevices.setAdapter(connectedAdapter);
    }

    private void updateScanned(ArrayList<BluetoothObject> scanned){
        scannedDevices = scanned;
        scannedAdapter.clear();
        scannedAdapter.addAll(scannedDevices);
    }

    private void updateConnected(ArrayList<BluetoothObject> connected){
        connectedDevices = connected;
        connectedAdapter.clear();
        connectedAdapter.addAll(connectedDevices);
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(SCANNED_INTENT)){
                updateScanned((ArrayList<BluetoothObject>) intent.getExtras().getSerializable("scannedDevices"));
            }
            else if (action.equals(CONNECTED_LIST_INTENT)){
                updateConnected((ArrayList<BluetoothObject>) intent.getExtras().getSerializable("connectedDevices"));
            }
        }
    };

    public void startListeners(){

        String subTag = ": startListeners";
        Log.d(TAG+subTag,"Starting listeners");

        setScannedDevices();
        setConnectedList();

        Switch bluetooth_switch = (Switch) root.findViewById(R.id.bluetooth_switch);
        Button searchButton = (Button) root.findViewById(R.id.search_button);
        ListView scannedDevices = root.findViewById(R.id.search_devices_list);

        if(mService.checkBluetooh()){
            bluetooth_switch.setChecked(true);
        }

        scannedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id)
            {
                String selected = ((TextView) view.findViewById(R.id.device_address)).getText().toString();
                mService.connect(selected);
            }
        });

        bluetooth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mService.requestBluetooth();
                    if(!mService.checkBluetooh()) bluetooth_switch.setChecked(false);
                } else {
                    mService.disableBluetooth();
                }
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                getContext().sendBroadcast(new Intent(SCAN_INTENT));
                //getContext().sendBroadcast(new Intent(GET_CONNECTED_INTENT));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter params = new IntentFilter();
        params.addAction(SCANNED_INTENT);
        params.addAction(CONNECTED_LIST_INTENT);
        getActivity().registerReceiver(receiver,params);
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(receiver);
    }
}