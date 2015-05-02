/*
 * Copyright (C) 2014 Francesco Azzola
 *  Surviving with Android (http://www.survivingwithandroid.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.pjq.musicplayer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ShakeEventManager implements SensorEventListener {
    private static final String TAG = ShakeEventManager.class.getSimpleName();

    private SensorManager sManager;
    private Sensor s;


    private ShakeListener listener;
    FileWriter fileWriter;

    public ShakeEventManager() {
    }

    public void setListener(ShakeListener listener) {
        this.listener = listener;
    }

    public void init(Context ctx) {
        sManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        s = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        register();

        try {
            fileWriter = new FileWriter(new File("/sdcard/sensor.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void register() {
        sManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
//        Log.i(TAG, "onSensorChanged,x=" + x + ",y=" + y + ",z=" + z);
        shakeHandler(sensorEvent);
//        rotateHandler(sensorEvent);
//        flipBackHandler(sensorEvent);
//        onLeftHandShake(sensorEvent);
        writeFile(sensorEvent);
    }

    private void writeFile(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        String value = System.currentTimeMillis() + ", onSensorChanged,x=" + x + ",y=" + y + ",z=" + z;
        try {
            fileWriter.append(value + '\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    RotateSession rotateSession = new RotateSession(new OnTriggerListener() {
        @Override
        public void onTrigger() {
            listener.onShake();
            try {
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    private void rotateHandler(SensorEvent e) {
//        Log.i("Sensor", "onSensorChanged,x=" + x + ",y=" + y + ",z=" + z);

        rotateSession.addEvent(e);
        rotateSession.process();
    }


    ShakeSession shakeSession = new ShakeSession(new OnTriggerListener() {
        @Override
        public void onTrigger() {
            listener.onShake();
            try {
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    private void shakeHandler(SensorEvent sensorEvent) {
        shakeSession.addEvent(sensorEvent);
        shakeSession.process();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void deregister() {
        sManager.unregisterListener(this);
    }


    private void flipBackHandler(SensorEvent e) {
        float x = e.values[0];
        float y = e.values[1];
        float z = e.values[2];

        // log("onSensorChanged,x=" + x + ",y=" + y + ",z=" + z);

        boolean isStop = false;
        int zMinValue = -8;

        if (x < 1 && x > -1 && y < 1 && y > -1) {
            if (z <= zMinValue) {
                isStop = true;
            }
        } else {
            if (z <= zMinValue) {
                isStop = true;
            }
        }

        if (isStop) {
            listener.onShake();
        }
    }

    private void onLeftHandShake(SensorEvent e) {
        float x = e.values[0];
        float y = e.values[1];
        float z = e.values[2];

        // log("onSensorChanged,x=" + x + ",y=" + y + ",z=" + z);

        boolean isStop = false;
        int zMinValue = -8;

        if (x < 1 && x > -1 && y < 1 && y > -1) {
            if (z <= zMinValue) {
                isStop = true;
            }
        } else {
            if (z <= zMinValue) {
                isStop = true;
            }
        }

        if (isStop) {
            listener.onShake();
        }
    }


    public static interface ShakeListener {
        public void onShake();

        public void onFlipBack();
    }

    public static interface OnTriggerListener {
        public void onTrigger();
    }

    public static abstract class BaseSession {
        protected int counter = 0;
        protected long firstMovTime;
        protected OnTriggerListener listener;

        public BaseSession(OnTriggerListener listener) {
            this.listener = listener;
        }

        protected void reset() {
            counter = 0;
            firstMovTime = System.currentTimeMillis();
        }

        public abstract void addEvent(SensorEvent e);

        protected float SHAKE_WINDOW_TIME_INTERVAL() {
            return 400;
        }

        protected int MOV_COUNTS_TRIGGER() {
            return 4;
        }

        public void process() {
            if (isTriggered()) {
                if (counter == 0) {
                    counter++;
                    firstMovTime = System.currentTimeMillis();
                    Log.d(TAG, "First mov..");
                } else {
                    long now = System.currentTimeMillis();
                    if ((now - firstMovTime) < SHAKE_WINDOW_TIME_INTERVAL())
                        counter++;
                    else {
                        reset();
                        counter++;
                        return;
                    }
                    Log.d(TAG, "Mov counter [" + counter + "]" + MOV_COUNTS_TRIGGER());

                    if (counter >= MOV_COUNTS_TRIGGER()) {
                        if (listener != null)
                            listener.onTrigger();
                        Log.d(TAG, "onTrigger");

//                        reset();
                    }
                }
            }
        }

        protected abstract boolean isTriggered();
    }


    private static class ShakeSession extends BaseSession {
        private static final int MOV_THRESHOLD = 17;
        private static final float ALPHA = 0.8F;
        float maxAcc;
        // Gravity force on x,y,z axis
        private float gravity[] = new float[3];

        public ShakeSession(OnTriggerListener listener) {
            super(listener);
        }

        @Override
        public void addEvent(SensorEvent e) {
            maxAcc = calcMaxAcceleration(e);
        }

        @Override
        protected float SHAKE_WINDOW_TIME_INTERVAL() {
            return 400;
        }

        @Override
        protected boolean isTriggered() {
            return maxAcc >= MOV_THRESHOLD;
        }

        @Override
        protected void reset() {
            super.reset();
//            maxAcc = 0;
        }

        private float calcMaxAcceleration(SensorEvent event) {
            gravity[0] = calcGravityForce(event.values[0], 0);
            gravity[1] = calcGravityForce(event.values[1], 1);
            gravity[2] = calcGravityForce(event.values[2], 2);

            float accX = event.values[0] - gravity[0];
            float accY = event.values[1] - gravity[1];
            float accZ = event.values[2] - gravity[2];

            float max1 = Math.max(accX, accY);
            return Math.max(max1, accZ);
        }

        // Low pass filter
        private float calcGravityForce(float currentVal, int index) {
            return ALPHA * gravity[index] + (1 - ALPHA) * currentVal;
        }

    }

    private static class RotateSession extends BaseSession {
        private boolean value7_8 = false;
        private boolean value4_5 = false;
        private boolean value0_1 = false;
        private boolean value_1_0 = false;
        private boolean value_5_4 = false;
        private boolean value_8_7 = false;

        public RotateSession(OnTriggerListener listener) {
            super(listener);
        }

        public void addEvent(SensorEvent e) {
            float x = e.values[0];
            float y = e.values[1];
            float z = e.values[2];
            float val = z;

            if (2 <= val && val <= 7) {
                value7_8 = true;
            }

            if (value7_8) {
                if (1 <= val && val <= 5) {
                    value4_5 = true;
                }
            }

            if (0 <= val && val <= 5) {
                value0_1 = true;
            }

            if (value7_8) {
                if (-5 <= val && val <= 0) {
                    value_1_0 = true;
                }
            }

            if (-7 <= val && val <= -2) {
                value_5_4 = true;
            }

            if (value_1_0) {
                if (-10 <= val && val <= -5.5) {
                    value_8_7 = true;
                }
            }
        }

        @Override
        protected float SHAKE_WINDOW_TIME_INTERVAL() {
            return 3000;
        }

        protected int MOV_COUNTS_TRIGGER() {
            return 2;
        }

        @Override
        protected boolean isTriggered() {
            return isSessionFinished();
        }

        public boolean isSessionFinished() {
            if (value7_8 && value4_5 && value0_1 && value_1_0 && value_5_4 && value_8_7) {
                Log.i(TAG, "isSessionFinished = " + true);
                resetData();
                return true;
            } else {
                return false;
            }
        }

        public void resetData() {

            value7_8 = false;
            value0_1 = false;
            value_1_0 = false;
            value_5_4 = false;
            value_8_7 = false;
        }

    }

}
