package com.tfg.healthwatch.ui.bluetooth;

import android.os.Parcelable;

import java.io.Serializable;

public class BluetoothObject implements Serializable {

    private String name;
    private String address;
    private String state;
    private String type;
    private String uuids;

    public BluetoothObject(String Name, String Address){
        name = Name;
        address = Address;
    }

    public String getName(){
        return name;
    }

    public String getAddress() {
        return address;
    }
}
