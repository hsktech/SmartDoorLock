package com.commax.ble.door.service;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.commax.forgroundservice.R;

public class MainActivity extends Activity implements OnClickListener {

	private static final String LOG_TAG = "MainActivity";

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	private static MainActivity instance;

	Button enrollButton, encryptionButton, not_encryptionButton, startButton,
			stopButton;
	Button encryptionSha1Btn, decryptionSha1Btn, encryptionMd5Btn, decryptionMd5Btn;
	
	SecretKeySpec sks = null; // 선언
	SecureRandom sr = null;
	public IvParameterSpec iv;

	public static MainActivity getInstance() {
		return instance;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = this;

		//mHandler = new Handler();

		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}
		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		setContentView(R.layout.activity_main);
		//enrollButton = (Button) findViewById(R.id.enrollment_btn);
		encryptionButton = (Button) findViewById(R.id.encryp_btn);
		not_encryptionButton = (Button) findViewById(R.id.not_encryp_btn);

		encryptionSha1Btn = (Button) findViewById(R.id.encryp_sha1_btn);
		decryptionSha1Btn = (Button) findViewById(R.id.decryp_sha1_btn);
		
		encryptionMd5Btn = (Button) findViewById(R.id.encryption_md5_btn);
		decryptionMd5Btn = (Button) findViewById(R.id.decryption_md5_btn);
		
		startButton = (Button) findViewById(R.id.button1);
		stopButton = (Button) findViewById(R.id.button2);

		//enrollButton.setOnClickListener(this);
		encryptionButton.setOnClickListener(this);
		not_encryptionButton.setOnClickListener(this);
		
		encryptionSha1Btn.setOnClickListener(this);
		decryptionSha1Btn.setOnClickListener(this);
		
		encryptionMd5Btn.setOnClickListener(this);
		decryptionMd5Btn.setOnClickListener(this);

		startButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);

		iv = new IvParameterSpec(new byte[16]);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(
					R.layout.actionbar_indeterminate_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			
			if(null != ForegroundService.getInstance()){
				ForegroundService.getInstance().stopSmartDoorLockService();
			}
			
			mHandler = new Handler();
			
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			
			mHandler = null;
			
			showToast(getResources().getString(R.string.registration_scan_stop));
			
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
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
			
		case R.id.encryp_btn:

			encryptEkey(1);
			
			break;

		case R.id.not_encryp_btn:

			decryptEkey(1);
			
			break;
			
		case R.id.encryp_sha1_btn:

			encryptEkey(0);
			
			break;

		case R.id.decryp_sha1_btn:

			decryptEkey(0);
			
			break;
		
		case R.id.encryption_md5_btn:

			encryptEkey(2);
			
			break;

		case R.id.decryption_md5_btn:

			decryptEkey(2);
			
			break;

		default:
			break;
		}

	}

	private void decryptEkey(int hash_type) {
		// TODO Auto-generated method stub
		byte[] key = null;
		switch (hash_type) {
		case 0:
			key = generateHashSha1();
			break;
		case 1:
			key = generateHashSha1_PRNG();
			break;
		case 2:
			key = generateHashMd5();
			break;
		default:
			break;
		}
		
		//Toast.makeText(this, "eKey : " + PunchBleGattAttributes.getmOpenKey(), Toast.LENGTH_SHORT).show();
		
		byte[] deviceRegStatusByte = Base64.decode(PunchBleGattAttributes.getmOpenKey(), Base64.DEFAULT);
		//byte[] deviceRegStatusByte = PunchBleGattAttributes.getmOpenKey().getBytes();
		
		byte[] encodedBytes = deviceRegStatusByte;
		byte[] decodedBytes = null;
		// Decode the encoded data with AES
		try {

			Cipher c = Cipher.getInstance("AES");
			
			c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
			decodedBytes = c.doFinal(encodedBytes);
			
			PunchBleGattAttributes.setmOpenKey(new String(decodedBytes));
			Toast.makeText(this, "복호화된 eKey : " + PunchBleGattAttributes.getmOpenKey(), Toast.LENGTH_SHORT).show();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	private void encryptEkey(int hash_type) {
		// TODO Auto-generated method stub
		byte[] key = null;
		//String tempkey;
		switch (hash_type) {
		case 0:
			//key = generateHashSha1();
			break;
		case 1:
			//key = generateHashSha1_PRNG();
			break;
		case 2:
			key = generateHashMd5();
			//tempkey = generateHashMd5str();
			break;

		default:
			break;
		}
			
		
		// 암호화를 처리할 곳
		//String data = "HELLOCOMMAXDKFEOFELF";
		//String data = "HELLOCOMMAXCOMPA";
		//String data = "COMMAX";
		//String data = "434F4D4D415800000000000000000000";
		String data = "434F4D4D4158";
		// 내가 저장한 정보는.. 정보1:16자리난수 정보2:맥어드레스 정보3:서버에서 인증성공시 받아온값 등이다.
		//byte[] iv = ByteUtils.toBytes("7649ABAC8119B246CEE98E9B12E9197D", 16);
		//IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		byte[] encodedBytes = null;

		try {
			//Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
			//Cipher c = Cipher.getInstance("AES/ECB/NOPADDING");
			//Cipher c = Cipher.getInstance("AES/OFB128/ISO10126PADDING");
			//Cipher c = Cipher.getInstance("AES/CFB/NOPADDING");
			//Cipher c = Cipher.getInstance("AES/CBC/ISO10126PADDING");
			
			//c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ByteUtils.toBytes("c95d208f0ab057813a78e67baba0c6b4", 16), "AES"));
			/*try {
				c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ByteUtils.toBytes("2b7e151628aed2a6abf7158809cf4f3c", 16), "AES"), ivParameterSpec);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			
			
			//byte[] temp = data.getBytes();
			byte[] temp = ByteUtils.toBytes(data, 16);
			
			encodedBytes = c.doFinal(temp);
			
			
			//PunchBleGattAttributes.setmOpenKey(Base64.encodeToString(encodedBytes, Base64.DEFAULT));
			//PunchBleGattAttributes.setmOpenKey(byteArrayToHex(encodedBytes));
			PunchBleGattAttributes.setmOpenKey(ByteUtils.toHexString(encodedBytes));
			
			
			Toast.makeText(this, "암호화된 eKey : " + PunchBleGattAttributes.getmOpenKey(), Toast.LENGTH_SHORT).show();
			Toast.makeText(this, "암호화된 eKey HEX 값 : " + hexToByteArray(PunchBleGattAttributes.getmOpenKey()), Toast.LENGTH_SHORT).show();

		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public byte[] generateHashSha1_PRNG(){
		sks = null; // 선언
		sr = null; // 초기화
		
		KeyGenerator kg = null;
		
		try {
			// sr = SecureRandom.getInstance("SHA1PRNG");
			// 젤리빈(17) 이상에서는 오류가 발생함을 확인하였다.. SHA1PRNG 이 아니라 ("SHA1PRNG",
			// "Crypto") 을 해주어야한다!
			if (android.os.Build.VERSION.SDK_INT >= 17) {
				try {
					sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				}
			} else {
				sr = SecureRandom.getInstance("SHA1PRNG");
			}
			// sr.setSeed("any data used as random seed".getBytes());
			sr.setSeed("commax".getBytes()); // 암호화키값 : 나는 단말기의 macAddress를
	
			kg = KeyGenerator.getInstance("AES");
			kg.init(128, sr);
			
			//sks = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");
			
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return (kg.generateKey()).getEncoded();
		
	}
	
	@SuppressLint("DefaultLocale") public byte[] generateHashSha1(){
		String strData = "commax";
		String strENCDate = "";
		byte[] digest = null;
		byte[] bytData = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256"); // "MD5" or "SHA1" or "SHA-256"
			bytData = strData.getBytes();
			md.update(bytData);
			digest = md.digest();
			for (int i = 0; i < digest.length; i++) {
				strENCDate = strENCDate	+ Integer.toHexString(digest[i] & 0xFF).toUpperCase();
			}
		} catch (NoSuchAlgorithmException e) {
		};

		Toast.makeText(this, "암호화 키 값 : " + strENCDate,Toast.LENGTH_LONG).show();
		//strENCDate.getBytes()
		
		return digest;
	}
	
	public byte[] generateHashMd5(){
		String strData = "commax";
		String strENCDate = "";
		byte[] digest = null;
		byte[] bytData = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5"); // "MD5" or "SHA1" or "SHA-256"
			bytData = strData.getBytes();
			md.update(bytData);
			digest = md.digest();
			
			StringBuffer sb = new StringBuffer(); 
			for (int i = 0; i < digest.length; i++) {
				sb.append(Integer.toString((digest[i]&0xff) + 0x100, 16).substring(1));
			}
			strENCDate = sb.toString();
			
		} catch (NoSuchAlgorithmException e) {
		};

		Toast.makeText(this, "암호화 키 값 : " + strENCDate,Toast.LENGTH_LONG).show();
		//strENCDate.getBytes()
		//byte[] test = hexToByteArray(strENCDate);
		
		return digest;
	}

	public String generateHashMd5str(){
		String strData = "commax";
		String strENCDate = "";
		byte[] digest = null;
		byte[] bytData = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5"); // "MD5" or "SHA1" or "SHA-256"
			bytData = strData.getBytes();
			md.update(bytData);
			digest = md.digest();
			
			StringBuffer sb = new StringBuffer(); 
			for (int i = 0; i < digest.length; i++) {
				sb.append(Integer.toString((digest[i]&0xff) + 0x100, 16).substring(1));
			}
			strENCDate = sb.toString();
			
		} catch (NoSuchAlgorithmException e) {
		};

		Toast.makeText(this, "암호화 키 값 : " + strENCDate,Toast.LENGTH_LONG).show();
		//strENCDate.getBytes()
		//byte[] test = hexToByteArray(strENCDate);
		
		return strENCDate;
	}
	
	// hex to byte[]
	public static byte[] hexToByteArray(String hex) {
	    if (hex == null || hex.length() == 0) {
	        return null;
	    }
	 
	    byte[] ba = new byte[hex.length() / 2];
	    for (int i = 0; i < ba.length; i++) {
	        ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
	    }
	    return ba;
	}
	
	public static String byteArrayToHex(byte[] ba) {
	    if (ba == null || ba.length == 0) {
	        return null;
	    }
	 
	    StringBuffer sb = new StringBuffer(ba.length * 2);
	    String hexNumber;
	    for (int x = 0; x < ba.length; x++) {
	        hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
	 
	        sb.append(hexNumber.substring(hexNumber.length() - 2));
	    }
	    return sb.toString();
	} 
	
	
	private static String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
 
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02X", b[i]));
            if ((i + 1) % 16 == 0 && ((i + 1) != b.length)) {
                sb.append(" ");
            }
        }
 
        return sb.toString();
    }
	
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();

					mHandler = null;
					showToast(getResources().getString(R.string.registration_scan_stop));
				}
			}, SCAN_PERIOD);
			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();

	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					try {
						if (device.getName().equals("COMMAX BLE")) {
							PunchBleGattAttributes.mDeviceAddress = device
									.getAddress();

							showToast(getResources().getString(R.string.registrated_ekey));
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					scanLeDevice(false);
		

				}

			});
		}
	};

	public void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void finishApp() {
		finish();
	}

}
