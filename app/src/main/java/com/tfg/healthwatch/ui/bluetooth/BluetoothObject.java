package com.tfg.healthwatch.ui.bluetooth;

public class BluetoothObject {

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
