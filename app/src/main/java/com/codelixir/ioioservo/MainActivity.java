package com.codelixir.ioioservo;

import com.codelixir.ioioservo.HelloIOIOService.IHelloIOIOService;
import com.codelixir.ioioservo.HelloIOIOService.IOIOBinder;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity implements IHelloIOIOService {

    private ToggleButton toggleButton_;

    private HelloIOIOService mHelloIOIOService;
    boolean isBound = false;

    Button btnStart;

    private final ServiceConnection myConnection = new ServiceConnection() {

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

        if (getIntent().getAction() != null && getIntent().getAction().equals("stopActivity")) {
            finish();
            return;
        }

        enableBluetooth();

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

        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter.isEnabled()) {
                    startIOIOService();
                }
            }
        });

        if (isServiceRunning()) {
            startIOIOService();
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (HelloIOIOService.class.getCanonicalName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startIOIOService() {
        Intent intent = new Intent(this, HelloIOIOService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        btnStart.setVisibility(View.GONE);
        toggleButton_.setVisibility(View.VISIBLE);
    }

    private void enableBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals("stopActivity"))
            finish();
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
    public void onRollChanged(final int roll) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setProgress(roll);
            }
        });

    }

    @Override
    public void onDisconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "IOIO Disconnected!", Toast.LENGTH_LONG).show();
            }
        });

    }
}