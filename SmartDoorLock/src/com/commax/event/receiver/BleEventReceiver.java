package com.commax.event.receiver;

import com.commax.ble.door.service.BluetoothLeService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BleEventReceiver extends BroadcastReceiver {

	private final static String LOG_TAG = BleEventReceiver.class
			.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		final String action = intent.getAction();

		if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

			Log.d(LOG_TAG, "Connected Broadcast");
			/*
			 * BluetoothGattCharacteristic characteristic = null;
			 * 
			 * characteristic.getService().getCharacteristic(PunchBleGattAttributes
			 * .CMX_BLE_CHAR_WRITE);
			 * 
			 * 
			 * 
			 * mBluetoothLeService.writeCharacteristic(characteristic);
			 */

		} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

		}
	}

}
