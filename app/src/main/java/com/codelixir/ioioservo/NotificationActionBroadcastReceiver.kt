package com.codelixir.ioioservo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codelixir.ioioservo.HelloIOIOService

class NotificationActionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.action == "stopService") {
            context.stopService(Intent(context, HelloIOIOService::class.java))
            context.startActivity(
                Intent(
                    context,
                    MainActivity::class.java
                ).setAction("stopActivity")
            )
        }
    }
}