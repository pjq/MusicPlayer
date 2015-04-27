package me.pjq.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

@SuppressLint("NewApi")
public class StepCountEventManager implements SensorEventListener {

    private SensorManager sManager;
    Sensor countSensor;


    private StepCountListener listener;

    public StepCountEventManager() {
    }

    public void setListener(StepCountListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NewApi")
    public void init(Context ctx) {
        sManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        countSensor = sManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        register();
    }

    @SuppressLint("NewApi")
    public void register() {
        if (countSensor != null) {
            sManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            listener.onNotSupportStepSensor();
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void onSensorChanged(SensorEvent sensorEvent) {
        int count = (int) sensorEvent.values[0];

        listener.onStepCount(count);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @SuppressLint("NewApi")
    public void deregister() {
        if (null != countSensor) {
            sManager.unregisterListener(this, countSensor);
        }
    }

    public static interface StepCountListener {
        public void onStepCount(int count);

        public void onNotSupportStepSensor();
    }
}
