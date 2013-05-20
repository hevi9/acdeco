package com.example.hello;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/*

http://developer.android.com/guide/topics/connectivity/bluetooth.html

*/

public class MainActivity extends Activity implements SensorEventListener {

	private Button mButton = null;
	private TextView mText = null;
	private ScrollView mScroll = null;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private long lastUpdate;
	private boolean xReady = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//
		mText = (TextView)findViewById(R.id.textView1);
		mButton = (Button)findViewById(R.id.button1);
		mScroll = (ScrollView)findViewById(R.id.scrollView1);
		//
		mButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				log("Test");
			}
		});
		// Sensor
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// Bluetooth
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
		}
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    int REQUEST_ENABLE_BT = 123;
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT );
		}
		//
		log("Start");
	}
	
	@Override
	 protected void onResume() {
	    super.onResume();
	    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	  }

	  @Override
	  protected void onPause() {
	    super.onPause();
	    mSensorManager.unregisterListener(this);
	  }
	
	public void log(String msg) {
		Spanned html = Html.fromHtml(
				"<font color='green'>"+ msg +"</font><br/>", null, null);
		mText.append(html);
		mScroll.scrollTo(0, mText.getBottom());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
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
	 public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	

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
		if (x > 4.0 && xReady ) {
			log("Tilt left");
			xReady = false;
		}
		if (x < -4.0 && xReady) {
			log("Tilt right");
			xReady = false;
		}
		if (x < 1.0 && x > -1.0) {
			xReady = true;
		}
	}
	 
}
