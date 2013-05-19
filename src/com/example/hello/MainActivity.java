package com.example.hello;

import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private Button mButton = null;
	private TextView mText = null;
	private ScrollView mScroll = null;
	
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
	
	

}
