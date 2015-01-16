package com.commax.ble.door.service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.commax.forgroundservice.R;

public class MainActivity extends Activity implements OnClickListener {

	private static final String LOG_TAG = "MainActivity";

	private BluetoothAdapter mBluetoothAdapter;
	
	private static final int REQUEST_ENABLE_BT = 1;
	
	private static MainActivity instance;

	public static MainActivity getInstance() {
		return instance;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = this;
		
		
		// Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
		
		setContentView(R.layout.activity_main);
		Button startButton = (Button) findViewById(R.id.button1);
		Button stopButton = (Button) findViewById(R.id.button2);

		startButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);

	}

	@Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
      
    }
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1:
			Intent startIntent = new Intent(MainActivity.this,
					ForegroundService.class);
			startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
			startService(startIntent);


			break;
		case R.id.button2:
			Intent stopIntent = new Intent(MainActivity.this,
					ForegroundService.class);
			stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
			startService(stopIntent);

			break;

		default:
			break;
		}

	}
	
	
	public void finishApp(){
		finish();
	}

}
