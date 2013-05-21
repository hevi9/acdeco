/*
 * Copyright (C) 2013 Petri Heinil�, License LGPL 2.1
 * 
 *  acdeco - Accelerated Desktop Control
 *  
 *  Android part. Read accelerator sensor and send
 *  signal messages to desktop over bluetooth.
 */

package com.example.hello;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {

	private Button mButton = null;
	private TextView mText = null;
	private ScrollView mScroll = null;
	private SensorManager mSensorManager = null;
	private Sensor mAccelerometer = null;
	private long lastUpdate = 0;
	private boolean xReady = true;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothDevice mDevice = null;
	public UUID SERVICE_UUID = UUID
			.fromString("350da82e-c15a-11e2-949e-001e4fbfb714");
	private Communicate mCommunicate = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//
		mText = (TextView) findViewById(R.id.textView1);
		mButton = (Button) findViewById(R.id.button1);
		mScroll = (ScrollView) findViewById(R.id.scrollView1);
		//
		mButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activate();
			}
		});
		// Sensor
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			int REQUEST_ENABLE_BT = 123;
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		log("Start");
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mCommunicate != null) {
			// mCommunicate.cancel();
			mCommunicate.close(); // SIGSEGV, but at least closes socket
		}
		Log.i("acdeco", "onDestroy");
	}

	public void log(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Spanned html = Html.fromHtml("<font color='green'>" + msg
						+ "</font><br/>", null, null);
				mText.append(html);
				mScroll.scrollTo(0, mText.getBottom());
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			getAccelerometer(event);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private void getAccelerometer(SensorEvent event) {
		float[] values = event.values;
		// Movement
		float x = values[0];
		float y = values[1];
		float z = values[2];
		// log(Float.toString(x));
		long actualTime = System.currentTimeMillis();
		if (actualTime - lastUpdate < 200) {
			return;
		}
		lastUpdate = actualTime;
		if (x > 4.0 && xReady) {
			log("Tilt left");
			xReady = false;
			if(mCommunicate != null) {
				mCommunicate.send_sig('L');
			}
		}
		if (x < -4.0 && xReady) {
			log("Tilt right");
			xReady = false;
			if(mCommunicate != null) {
				mCommunicate.send_sig('R');
			}
		}
		if (x < 1.0 && x > -1.0) {
			xReady = true;
		}
	}

	@SuppressLint("NewApi")
	public void activate() {
		log("Activating");
		selectDevice();
		mCommunicate = new Communicate(mDevice);
		mCommunicate.start();
	}

	public void inactivate() {
		log("Inactivating");
	}
	
	public void selectDevice() {
		// user dialog should be here
		final String THE_DEVICE = "mint-1";
		log("BT Paired Devices ..");
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				log("Device " + device.getName() + " " + device.getAddress());
				if (device.getName().equals(THE_DEVICE)) {
					mDevice = device;
				}
			}
		} else {
			log("No paired devices");
			return;
		}
		ParcelUuid[] pa = mDevice.getUuids();
		for (int i = 0; i < pa.length; i++) {
			log(pa[i].toString());
		}
		log("Selected " + mDevice.getName());
	}

	private class Communicate extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private InputStream mmInput = null;
		private OutputStream mmOutput = null;

		public Communicate(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			log("Socket for " + SERVICE_UUID.toString());
			try {
				tmp = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmSocket = tmp;
		}

		public void run() {
			mBluetoothAdapter.cancelDiscovery();
			try {
				log("Connecting ..");
				mmSocket.connect(); // BLOCK
			} catch (IOException ex) {
				log(ex.toString());
				try {
					mmSocket.close();
				} catch (IOException e) {
					log(e.toString());
				}
				return;
			}
			log("Connected");
			try {
				mmInput = mmSocket.getInputStream();
				mmOutput = mmSocket.getOutputStream();
				mmOutput.write(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while(true) {
				try {
					log("reading ..");
					int sig = mmInput.read(); // BLOCK ?
					log("DATA: " + sig);
					if (sig == -1) {
						inactivate();
						break;
					}
				} catch (IOException e) {
					try {
						mmSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					log(e.toString());
					break;
				}
			}
			log("Communication end");
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void send_sig(int sig) {
			try {
				log("Write " + sig);
				mmOutput.write(sig);
			} catch (IOException e) {
				log("write error");
			}
		}

		public void close() {
			try {
				mmSocket.close();
			} catch (IOException ex) {
				log(ex.toString());
			}
		}		
		
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException ex) {
				log(ex.toString());
			}
		}
	}
}
