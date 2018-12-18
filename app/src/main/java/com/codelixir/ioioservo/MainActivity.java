package com.codelixir.ioioservo;

import com.codelixir.ioioservo.HelloIOIOService.IHelloIOIOService;
import com.codelixir.ioioservo.HelloIOIOService.IOIOBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity implements IHelloIOIOService {

	private ToggleButton toggleButton_;

	private HelloIOIOService mHelloIOIOService;
	boolean isBound = false;

	private ServiceConnection myConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			IOIOBinder binder = (IOIOBinder) service;
			mHelloIOIOService = binder.getService();
			mHelloIOIOService.attachListener(MainActivity.this);
			isBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mHelloIOIOService.detachListener();
			isBound = false;

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, HelloIOIOService.class);
		bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
		startService(intent);
		setContentView(R.layout.main);

		toggleButton_ = findViewById(R.id.ToggleButton);

		toggleButton_.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isBound)
					mHelloIOIOService.toggleLed();
			}
		});

	}

	@Override
	protected void onDestroy() {
		if (isBound)
			unbindService(myConnection);
		super.onDestroy();
	}

	@Override
	public void onConnect() {

	}

	@Override
	public void onDisConnect() {
	    runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "IOIO Disconnected!", Toast.LENGTH_LONG);
            }
        });

	}
}