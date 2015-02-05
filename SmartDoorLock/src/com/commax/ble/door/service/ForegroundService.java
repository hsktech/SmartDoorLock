package com.commax.ble.door.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.commax.forgroundservice.R;

public class ForegroundService extends Service {

	private static final String LOG_TAG = ForegroundService.class.getSimpleName();

	AsyncTask<Integer, Integer, Void> mBackgroundTask = null;
	
	private BluetoothLeService mBluetoothLeService;
	//private String mDeviceAddress = "78:A5:04:55:E9:76";
	//private String mDeviceAddress = "78:A5:04:55:E1:FD";
	//private String mDeviceAddress = "78:A5:04:55:CF:7D";
	//private String mDeviceAddress = "78:A5:04:55:D8:E5";
	
	public static BluetoothGatt door_gatt;
	
	
	private boolean backgourndTaskFlag = false;
	
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			
			Log.d(LOG_TAG, "Serviceconnected");
			
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(LOG_TAG, "Unable to initialize Bluetooth");
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(PunchBleGattAttributes.mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
			
			Log.d(LOG_TAG, "ServiceDisconnected");
			
		}
	};

	public void NotifyFun(BluetoothGatt gatt) {
		// TODO Auto-generated method stub
		if(null != mBluetoothLeService){
			BluetoothGattService cmxDoorService = gatt
					.getService(PunchBleGattAttributes.CMX_BLE_SERVICE);

			if (null != cmxDoorService) {
				
				
				BluetoothGattCharacteristic door_notify_characteristic = cmxDoorService
						.getCharacteristic(PunchBleGattAttributes.CMX_BLE_CHAR_NOTIFY);
				
				if (null != door_notify_characteristic) {
					
					Log.d(LOG_TAG, "Notify Enabled");
					mBluetoothLeService.setCharacteristicNotification(
							door_notify_characteristic, true);
				}

			}
		}
	}

	
	public void WriteFun(BluetoothGatt gatt) {

		if(null != mBluetoothLeService){
			BluetoothGattService cmxDoorService = gatt
					.getService(PunchBleGattAttributes.CMX_BLE_SERVICE);

			if (null != cmxDoorService) {

				BluetoothGattCharacteristic door_write_characteristic = cmxDoorService
						.getCharacteristic(PunchBleGattAttributes.CMX_BLE_CHAR_WRITE);
			
				if (null != door_write_characteristic) {
					
					Log.d(LOG_TAG, "Door Open");
					mBluetoothLeService.writeCharacteristic(door_write_characteristic);
					
					SystemClock.sleep(500);
					
					Log.d(LOG_TAG, "Door Open");
					mBluetoothLeService.writeCharacteristic(door_write_characteristic);
					
					
					BluetoothLeService.connectionFlag = 2;
					
				}
			}
		}

	}
	
	
	public void PrintNotify(BluetoothGatt gatt) {
		
	}
	
	private static ForegroundService instance;

	public static ForegroundService getInstance() {
		return instance;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		//instance = this;
		
	}

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		instance = this;
		
		if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
			Log.i(LOG_TAG, "Received Start Foreground Intent ");
			Intent notificationIntent = new Intent(this, MainActivity.class);
			notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);

			Bitmap icon = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);

			Notification notification = new NotificationCompat.Builder(this)
					.setContentTitle("Cmx Smart Door Lock")
//					.setTicker("hello")
					.setContentText("Service Running")
					.setSmallIcon(R.drawable.ic_launcher)
					.setLargeIcon(
							Bitmap.createScaledBitmap(icon, 128, 128, false))
					.setContentIntent(pendingIntent).setOngoing(true).build();
			startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
					notification);

			
			BluetoothLeService.connectionFlag = 0;
			
			bindBleService();
			
			if(null == mBackgroundTask){
				mBackgroundTask = new BackgroundTask().execute(10);
				backgourndTaskFlag = true;
			}
			
	
		} else if (intent.getAction().equals(
				Constants.ACTION.STOPFOREGROUND_ACTION)) {
			Log.i(LOG_TAG, "Received Stop Foreground Intent");
			
			stopSmartDoorLockService();
			
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(LOG_TAG, "In onDestroy");
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Used only in case of bound services.
		return null;
	}
	
	public void bindBleService() {
		// TODO Auto-generated method stub
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		Log.d(LOG_TAG, "서비스 연결 시도");
		
		//WriteFun(door_gatt);
	}
	
	
	public void unbindBleService() {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "서비스 끊기 시도");
	

		try {
			unbindService(mServiceConnection);

			if(null != mBluetoothLeService){
				mBluetoothLeService = null;			
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void stopSmartDoorLockService(){
		stopForeground(true);
		stopSelf();
		
		
		if(null != mBackgroundTask){
			mBackgroundTask = null;
			backgourndTaskFlag = false;
		}

		unbindBleService();
	}
	
	
	public void connectBleDevice(){
		try {
			mBluetoothLeService.connect(PunchBleGattAttributes.mDeviceAddress);
			
			Log.d(LOG_TAG, "connectBleDevice");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void disconnectBleDevice(){
		
		try {
			mBluetoothLeService.disconnect();
			
			Log.d(LOG_TAG, "disconnectBleDevice");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class BackgroundTask extends AsyncTask<Integer, Integer, Void>{

		@Override
		protected Void doInBackground(Integer... arg0) {
			// TODO Auto-generated method stub
			
			while (backgourndTaskFlag) {

				//bindBleService();
				
				if(BluetoothLeService.connectionFlag == 0){
					connectBleDevice();
				}
				
				if(BluetoothLeService.connectionFlag == 2){
					disconnectBleDevice();
					BluetoothLeService.connectionFlag = 0;
				}
				
				SystemClock.sleep(2000);
	
				//unbindBleService();
			}
			
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			
			if(values[0] == 0){
				
			}
		}
	
		
	}

}
