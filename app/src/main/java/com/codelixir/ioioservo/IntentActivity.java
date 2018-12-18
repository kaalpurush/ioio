package com.codelixir.ioioservo;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class IntentActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();

		if (intent.getAction() != null) {
			if (intent.getAction().equals("stopService")) {
				stopService(new Intent(this, HelloIOIOService.class));
				NotificationManager mNotificationManager = (NotificationManager) this
						.getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.cancelAll();
				finish();
				System.exit(0);
				// android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
	}
}
