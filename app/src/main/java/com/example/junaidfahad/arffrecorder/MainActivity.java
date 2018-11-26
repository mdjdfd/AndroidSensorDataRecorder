package com.example.junaidfahad.arffrecorder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();


    public static final long NOTIFY_INTERVAL = 1 * 50;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;

    private TextView mTvAxisX, mTvAxisY, mTvAxisZ;
    private EditText mEtFilename;
    private Button mBtnRecorderOn, mBtnRecorderOff;


    private ServiceConnection mServiceConnection;
    private boolean mIsServiceBind = false;
    private Intent mIntent;
    private ARFFRecorderService mService;
    private ArrayList<Attribute> atts = new ArrayList<>();
    private Instances dataRaw;
    private long timeOffsetMs;
    private FileWriter writer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFileCreation();
        initView();
    }

    private void initFileCreation() {
        try {

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File directory = new File(dir, "accrecording.arff");

            writer = new FileWriter(directory);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initView() {


        mTvAxisX = findViewById(R.id.text_view_axis_x);
        mTvAxisY = findViewById(R.id.text_view_axis_y);
        mTvAxisZ = findViewById(R.id.text_view_axis_z);
        mEtFilename = findViewById(R.id.edit_text_file_name);
        mBtnRecorderOn = findViewById(R.id.button_recorder_on);
        mBtnRecorderOn.setOnClickListener(this);
        mBtnRecorderOff = findViewById(R.id.button_recorder_off);
        mBtnRecorderOff.setOnClickListener(this);

        atts.add(new Attribute(getString(R.string.text_attr_timestamp)));
        atts.add(new Attribute(getString(R.string.text_attr_accelerationx)));
        atts.add(new Attribute(getString(R.string.text_attr_accelerationy)));
        atts.add(new Attribute(getString(R.string.text_attr_accelerationz)));

        dataRaw = new Instances(getString(R.string.text_attr_class_name), atts, 0);
    }


    private void mBindService() {
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    ARFFRecorderService.MyBinder myBinder = (ARFFRecorderService.MyBinder) iBinder;
                    mService = myBinder.getService();
                    mIsServiceBind = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    mIsServiceBind = false;
                }
            };
        }
        bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void mUnBindService() {
        if (mIsServiceBind) {
            unbindService(mServiceConnection);
            mIsServiceBind = false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_recorder_on:
//                startSavingSensorData();
                if (mServiceConnection == null) {
                    startSavingSensorData();
                } else {
                    Toast.makeText(this, getString(R.string.text_service_already_running), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button_recorder_off:
                if (mServiceConnection != null) {
                    stopSavingSensorData();
                } else {
                    Toast.makeText(this, getString(R.string.text_no_service_running), Toast.LENGTH_SHORT).show();
                }
//                stopSavingSensorData();
                break;
        }
    }


    private void startSavingSensorData() {
        mIntent = new Intent(this, ARFFRecorderService.class);
        startService(mIntent);
        mBindService();
        startTimer();

    }


    private void stopSavingSensorData() {
        mUnBindService();
        Intent intent = new Intent(this, ARFFRecorderService.class);
        stopService(intent);
        stopTimer();
    }


    class TimeDisplayTimerTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (mIsServiceBind) {
                        mTvAxisX.setText(String.format("%.1f", mService.getDeltaX()));
                        mTvAxisY.setText(String.format("%.1f", mService.getDeltaY()));
                        mTvAxisZ.setText(String.format("%.1f", mService.getDeltaZ()));

                        timeOffsetMs = System.currentTimeMillis();

                        recordDataToWeka(timeOffsetMs, mService.getDeltaX(), mService.getDeltaY(), mService.getDeltaZ());
                    }
                }

            });
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();

            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void startTimer() {

        if (mTimer != null) {
            mTimer.cancel();
        }
        else {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
        }


    }


    private void recordDataToWeka(long timeMs, float deltaX, float deltaY, float deltaZ) {

        double[] instanceValue1 = new double[dataRaw.numAttributes()];

        instanceValue1[0] = timeMs;
        instanceValue1[1] = deltaX;
        instanceValue1[2] = deltaY;
        instanceValue1[3] = deltaZ;
        dataRaw.add(new DenseInstance(1.0, instanceValue1));

        try {
            writer.append(dataRaw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "_log : recordDataToWeka : " + dataRaw);

//        ArffSaver arffSaverInstance = new ArffSaver();
//        arffSaverInstance.setInstances(dataRaw);
//
//        try {
//            arffSaverInstance.setFile(new File("/Users/junaidfahad/Downloads/sysdev.arff"));
//            arffSaverInstance.writeBatch();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }


}
