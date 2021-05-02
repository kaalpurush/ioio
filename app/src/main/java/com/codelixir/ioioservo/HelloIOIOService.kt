package com.codelixir.ioioservo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codelixir.ioioservo.HelloIOIOService
import ioio.lib.api.DigitalOutput
import ioio.lib.api.IOIO
import ioio.lib.api.PwmOutput
import ioio.lib.api.exception.ConnectionLostException
import ioio.lib.util.BaseIOIOLooper
import ioio.lib.util.IOIOLooper
import ioio.lib.util.android.IOIOService

/**
 * An example IOIO service. While this service is alive, it will attempt to
 * connect to a IOIO and blink the LED. A notification will appear on the
 * notification bar, enabling the user to stop the service.
 */
class HelloIOIOService : IOIOService(), SensorEventListener {
    var mNotificationManager: NotificationManager? = null
    var tilt = 0f
    var mLed = false
    var lastRoll = 0
    var m_sensorManager: SensorManager? = null
    private val myBinder: IBinder = IOIOBinder()
    protected var listener: IHelloIOIOService? = null

    interface IHelloIOIOService {
        fun onConnect()
        fun onRollChanged(roll: Int)
        fun onDisconnect()
    }

    private fun registerListeners() {
        m_sensorManager!!.registerListener(
            this,
            m_sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun unregisterListeners() {
        m_sensorManager!!.unregisterListener(this)
    }

    override fun createIOIOLooper(): IOIOLooper {
        return object : BaseIOIOLooper() {
            // private AnalogInput input_;
            private var pwmOutput_: PwmOutput? = null
            private var led_: DigitalOutput? = null
            override fun disconnected() {
                super.disconnected()
                listener!!.onDisconnect()
            }

            @Throws(ConnectionLostException::class, InterruptedException::class)
            override fun setup() {
                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true)
                //input_ = ioio_.openAnalogInput(40);
                pwmOutput_ = ioio_.openPwmOutput(12, 50)
            }

            @Throws(ConnectionLostException::class, InterruptedException::class)
            override fun loop() {
                //final float reading = input_.read();
                // setText(Float.toString(reading));
                val roll = (500 + 2000 / 20 * (tilt + 10)).toInt()
                Log.d("roll", roll.toString())
                if (Math.abs(lastRoll - roll) > 10 && roll > 500 && roll < 2500) {
                    pwmOutput_!!.setPulseWidth(roll)
                    listener!!.onRollChanged(roll)
                    lastRoll = roll
                }
                led_!!.write(!mLed)
                Thread.sleep(10)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
        m_sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        registerListeners()
        Log.d("HelloIOIOService", "onCreate")
    }

    override fun onDestroy() {
        mNotificationManager!!.cancel(NOTIFICATION_ID)
        unregisterListeners()
        Log.d("HelloIOIOService", "onDestroy")
        super.onDestroy()
    }

    private fun showNotification() {
        mNotificationManager = this
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "general"
        val channelName = "General"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), 0
        )
        val mBuilder = NotificationCompat.Builder(
            this, channelId
        )
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getText(R.string.app_name))
            .setSound(
                RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            .setContentIntent(contentIntent).setOngoing(true)
        val buttonIntent = Intent(this, NotificationActionBroadcastReceiver::class.java)
        buttonIntent.action = "stopService"
        val buttonPendingIntent = PendingIntent.getBroadcast(
            this, 0,
            buttonIntent, 0
        )
        mBuilder.addAction(R.drawable.stop, "Stop", buttonPendingIntent)
        mNotificationManager!!.notify(NOTIFICATION_ID, mBuilder.build())
    }

    fun toggleLed() {
        mLed = !mLed
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            tilt = event.values[0]
            Log.d("tilt", tilt.toString())
        }
    }

    fun attachListener(thelistener: IHelloIOIOService?) {
        listener = thelistener
    }

    fun detachListener() {
        listener = null
    }

    inner class IOIOBinder : Binder() {
        val service: HelloIOIOService
            get() = this@HelloIOIOService
    }

    override fun onBind(arg0: Intent): IBinder? {
        return myBinder
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}