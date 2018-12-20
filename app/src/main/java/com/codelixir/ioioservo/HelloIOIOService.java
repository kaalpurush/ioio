package com.codelixir.ioioservo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

/**
 * An example IOIO service. While this service is alive, it will attempt to
 * connect to a IOIO and blink the LED. A notification will appear on the
 * notification bar, enabling the user to stop the service.
 */
public class HelloIOIOService extends IOIOService implements
        SensorEventListener {

    NotificationManager mNotificationManager;
    public static final int NOTIFICATION_ID = 1;

    float tilt;
    boolean mLed;
    int lastRoll = 0;

    SensorManager m_sensorManager;

    private final IBinder myBinder = new IOIOBinder();

    protected IHelloIOIOService listener;

    public interface IHelloIOIOService {
        void onConnect();
        void onRollChanged(int roll);
        void onDisconnect();
    }

    private void registerListeners() {
        m_sensorManager.registerListener(this,
                m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
    }

    private void unregisterListeners() {
        m_sensorManager.unregisterListener(this);
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            // private AnalogInput input_;
            private PwmOutput pwmOutput_;
            private DigitalOutput led_;

            @Override
            public void disconnected() {
                super.disconnected();
                listener.onDisconnect();
            }

            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
                //input_ = ioio_.openAnalogInput(40);
                pwmOutput_ = ioio_.openPwmOutput(12, 50);
            }

            @Override
            public void loop() throws ConnectionLostException,
                    InterruptedException {
                //final float reading = input_.read();
                // setText(Float.toString(reading));

                int roll = (int) (500 + ((2000 / 20) * (tilt + 10)));

                Log.d("roll", String.valueOf(roll));

                if (Math.abs(lastRoll - roll) > 10 && roll > 500
                        && roll < 2500) {
                    pwmOutput_.setPulseWidth(roll);
                    listener.onRollChanged(roll);
                    lastRoll = roll;
                }

                led_.write(!mLed);

                Thread.sleep(10);
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showNotification();
        m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerListeners();

        Log.d("HelloIOIOService", "onCreate");
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(NOTIFICATION_ID);
        unregisterListeners();

        Log.d("HelloIOIOService", "onDestroy");

        super.onDestroy();
    }

    private void showNotification() {
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "general";
        String channelName = "General";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this, channelId)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getText(R.string.app_name))
                .setSound(
                        RingtoneManager
                                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(contentIntent).setOngoing(true);

        Intent buttonIntent = new Intent(this, NotificationActionBroadcastReceiver.class);
        buttonIntent.setAction("stopService");
        PendingIntent buttonPendingIntent = PendingIntent.getBroadcast(this, 0,
                buttonIntent, 0);

        mBuilder.addAction(R.drawable.stop, "Stop", buttonPendingIntent);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void toggleLed() {
        mLed = !mLed;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            tilt = event.values[0];
            Log.d("tilt", String.valueOf(tilt));
        }
    }

    public void attachListener(IHelloIOIOService thelistener) {
        listener = thelistener;
    }

    public void detachListener() {
        listener = null;
    }

    public class IOIOBinder extends Binder {
        public HelloIOIOService getService() {
            return HelloIOIOService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return myBinder;
    }

}
