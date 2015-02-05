package com.commax.ble.door.service;

import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BluetoothLeService extends Service {

	private final static String TAG = BluetoothLeService.class.getSimpleName();
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;

	private int mConnectionState = STATE_DISCONNECTED;
	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public static final String ACTION_DATA_READ = "android.ble.common.ACTION_DATA_READ";
	public static final String ACTION_DATA_WRITE = "android.ble.common.ACTION_DATA_WRITE";

	public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
	public final static String EXTRA_UUID = "android.ble.common.EXTRA_UUID";
	public final static String EXTRA_STATUS = "android.ble.common.EXTRA_STATUS";

	public final static UUID UUID_PUNCH_BLE_SERVICE = UUID.fromString(PunchBleGattAttributes.PUNCH_BLE_SERVICE);
	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	
	public static int connectionFlag = 0;
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				
				connectionFlag = 1;
				
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				
				ForegroundService.getInstance().NotifyFun(gatt);
				
				// Attempts to discover services after successful connection.
				Log.i(TAG, "Attempting to start service discovery:"
						+ mBluetoothGatt.discoverServices());
				
				
				

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				Log.i(TAG, "Disconnected from GATT server.");

				broadcastUpdate(intentAction);
				
				connectionFlag = 0;
				
				
			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				Log.i(TAG, "STATE_CONNECTING");
				
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.i(TAG, "STATE_DISCONNECTING");
			} else{
				Log.i(TAG, "STATE_OTHERS");
			}
			
			
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

				//ForegroundService.getInstance().NotifyFun(gatt);
				
				ForegroundService.getInstance().WriteFun(gatt);
				ForegroundService.door_gatt = gatt;
				
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
				Log.d(TAG, "onCharacteristicRead : " + status);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			
			Log.d(TAG, "값 변화 감지");
			
			if(PunchBleGattAttributes.CMX_BLE_CHAR_NOTIFY == characteristic.getUuid()){
				Log.d(TAG, "CMX_BLE_CHAR_NOTIFY 값 변화 감지");
			}
			
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			// TODO Auto-generated method stub
			// super.onCharacteristicWrite(gatt, characteristic, status);

			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
				Log.d(TAG, "onCharacteristicWrite ( characteristic :"
						+ characteristic + " ,status : " + status + ")");
			}

		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			// TODO Auto-generated method stub
			super.onReadRemoteRssi(gatt, rssi, status);

			Log.d(TAG, "Rssi : " + rssi);

		}

	};

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);

		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(data.length);
			for (byte byteChar : data)
				stringBuilder.append(String.format("%02X ", byteChar));
			/*intent.putExtra(EXTRA_DATA,
					new String(data) + "\n" + stringBuilder.toString());*/
			intent.putExtra(EXTRA_DATA,
			new String(data));
		}
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic, final int status) {

		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
		intent.putExtra(EXTRA_DATA, characteristic.getValue());
		intent.putExtra(EXTRA_STATUS, status);
		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}
		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG,
					"BluetoothAdapter not initialized or unspecified address.");
			return false;
		}
		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			Log.d(TAG,
					"Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}
		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	public void writeCharacteristic(
			final BluetoothGattCharacteristic characteristic) {
		// TODO Auto-generated method stub
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.d(TAG, "BluetoothAdapter not initialized");
			return;
		}

		characteristic.setValue(PunchBleGattAttributes.getmOpenKey());

		boolean status = mBluetoothGatt.writeCharacteristic(characteristic);
		Log.d(TAG, "writeCharacteristic : " + status);

	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);

	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		
		if (PunchBleGattAttributes.CMX_BLE_CHAR_NOTIFY.equals(characteristic.getUuid())) {
			
			Log.d(TAG, "PUNCH_BLE_Notify_Characteristic");
			
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID
							.fromString(PunchBleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}

	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;
		return mBluetoothGatt.getServices();
	}

}
