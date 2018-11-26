package com.example.junaidfahad.arffrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;



/**
 * Created by junaidfahad on 27/4/18.
 */

public class ARFFRecorderService extends Service implements SensorEventListener {
    private final String TAG = ARFFRecorderService.class.getSimpleName();

    private final int NOTIFICATION_ID = 101;

    private IBinder mBinder = new MyBinder();
    private boolean mServiceStart;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private final float NOISE = (float) 2.0;
    private float mLastX, mLastY, mLastZ;
    private float deltaX, deltaY, deltaZ;
    private boolean mInitialized;


    @Override
    public void onCreate() {
        super.onCreate();
        mInitialized = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServiceStart = false;
        mSensorManager.unregisterListener(this);
        if (!mServiceStart) {
            clearNotification();
        }

    }


    class MyBinder extends Binder {
        public ARFFRecorderService getService() {
            return ARFFRecorderService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStart = true;
        if (mServiceStart) {
            showNotification();
        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, getString(R.string.text_no_accelerometer_sensor), Toast.LENGTH_SHORT).show();
        }
        return Service.START_STICKY;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;

            mInitialized = true;
        } else {
            deltaX = Math.abs(mLastX - x);
            deltaY = Math.abs(mLastY - y);
            deltaZ = Math.abs(mLastZ - z);
            if (deltaX < NOISE) deltaX = (float) 0.0;
            if (deltaY < NOISE) deltaY = (float) 0.0;
            if (deltaZ < NOISE) deltaZ = (float) 0.0;
            mLastX = x;
            mLastY = y;
            mLastZ = z;
        }


        Log.e(TAG, "_log : onSensorChanged : " + deltaX + " " + deltaY + " " + deltaZ);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }


    private void showNotification() {
        NotificationManager mgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder note = new NotificationCompat.Builder(this);
        note.setContentTitle(getString(R.string.text_recorder_on));
        note.setTicker(getString(R.string.text_new_message_alert));
        note.setAutoCancel(false);
        //note.setDefaults(Notification.DEFAULT_ALL);
        note.setSmallIcon(R.drawable.ic_launcher_background);

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        note.setContentIntent(pi);
        mgr.notify(NOTIFICATION_ID, note.build());

    }


    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public float getDeltaX() {
        return deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }

    public float getDeltaZ() {
        return deltaZ;
    }




}
