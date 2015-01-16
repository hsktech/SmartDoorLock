package com.commax.ble.door.service;

import java.util.HashMap;
import java.util.UUID;

public class PunchBleGattAttributes {

	private static HashMap<String, String> attributes = new HashMap();
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String PUNCH_BLE_SERVICE = "0000ff00-0000-1000-8000-00805f9b34fb";
    
	public static final UUID CMX_BLE_SERVICE = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
	public static final UUID CMX_BLE_CHAR_WRITE = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    
    static {
        // Sample Services.
        //attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        //attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
    	attributes.put("0000ff00-0000-1000-8000-00805f9b34fb", "USER Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        //attributes.put("0000ff01-0000-1000-8000-00805f9b34fb", "Write Characteristic");
        attributes.put("0000ff01-0000-1000-8000-00805f9b34fb", "Door Open");
        attributes.put("0000ff02-0000-1000-8000-00805f9b34fb", "Read Characteristic");
        attributes.put("0000ff03-0000-1000-8000-00805f9b34fb", "Notify Characteristic");
    }
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
