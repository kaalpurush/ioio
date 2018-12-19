package com.codelixir.ioioservo;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationActionBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("stopService")) {
            context.stopService(new Intent(context, HelloIOIOService.class));
            context.startActivity(new Intent(context, MainActivity.class).setAction("stopActivity"));
        }
    }
}

