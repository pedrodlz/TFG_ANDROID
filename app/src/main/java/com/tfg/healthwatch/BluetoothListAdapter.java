package com.tfg.healthwatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tfg.healthwatch.ui.bluetooth.BluetoothObject;

import java.util.ArrayList;

public class BluetoothListAdapter extends ArrayAdapter<BluetoothObject> {
    ArrayList<BluetoothObject> devicesList = new ArrayList<>();

    public BluetoothListAdapter(Context context, int textViewResourceId,
                                ArrayList<BluetoothObject> objects) {
        super(context, textViewResourceId, objects);
        devicesList = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.bluetooth_list_view, null);

        TextView deviceName = (TextView) v.findViewById(R.id.device_name);
        TextView deviceAddress = (TextView) v.findViewById(R.id.device_address);

        deviceName.setText(devicesList.get(position).getName());
        deviceAddress.setText(devicesList.get(position).getAddress());

        return v;
    }
}
