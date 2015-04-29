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

public class ShakeEventManager implements SensorEventListener {

    private SensorManager sManager;
    private Sensor s;

    private static final int MOV_COUNTS_TRIGGER = 4;
    private static final int MOV_THRESHOLD = 17;
    private static final float ALPHA = 0.8F;
    private static final int SHAKE_WINDOW_TIME_INTERVAL = 400; // milliseconds

    // Gravity force on x,y,z axis
    private float gravity[] = new float[3];

    private int counter;
    private long firstMovTime;
    private ShakeListener listener;

    public ShakeEventManager() {
    }

    public void setListener(ShakeListener listener) {
        this.listener = listener;
    }

    public void init(Context ctx) {
        sManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        s = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        register();
    }

    public void register() {
        sManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        shakeHandler(sensorEvent);
        rotateHandler(sensorEvent);
//        flipBackHandler(sensorEvent);
//        onLeftHandShake(sensorEvent);
    }

    RotateSession session = new RotateSession();

    private void rotateHandler(SensorEvent e) {
        float x = e.values[0];
        float y = e.values[1];
        float z = e.values[2];

        Log.i("Sensor", "onSensorChanged,x=" + x + ",y=" + y + ",z=" + z);

        session = session.value(z);

        if (session.isSessionFinished()) {
            listener.onShake();
            session.reset();
        }
    }


    private void shakeHandler(SensorEvent sensorEvent) {
        float maxAcc = calcMaxAcceleration(sensorEvent);

        if (maxAcc >= MOV_THRESHOLD) {
            Log.d("SwA", "Max Acc [" + maxAcc + "]");
            if (counter == 0) {
                counter++;
                firstMovTime = System.currentTimeMillis();
                Log.d("SwA", "First mov..");
            } else {
                long now = System.currentTimeMillis();
                if ((now - firstMovTime) < SHAKE_WINDOW_TIME_INTERVAL)
                    counter++;
                else {
                    resetAllData();
                    counter++;
                    return;
                }
                Log.d("SwA", "Mov counter [" + counter + "]");

                if (counter >= MOV_COUNTS_TRIGGER)
                    if (listener != null)
                        listener.onShake();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void deregister() {
        sManager.unregisterListener(this);
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


    private void resetAllData() {
        Log.d("SwA", "Reset all data");
        counter = 0;
        firstMovTime = System.currentTimeMillis();
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

    private class RotateSession {
        private boolean value7_8 = false;
        private boolean value4_5 = false;
        private boolean value0_1 = false;
        private boolean value_1_0 = false;
        private boolean value_5_4 = false;
        private boolean value_8_7 = false;

        public RotateSession value(float val) {
            if (2 <= val && val <= 7) {
                value7_8 = true;
            }

            if (value7_8) {
                if (1 <= val && val <= 3) {
                    value4_5 = true;
                }
            }

            if (0 <= val && val <= 2) {
                value0_1 = true;
            }

            if (value7_8) {
                if (-2 <= val && val <= 0) {
                    value_1_0 = true;
                }
            }

            if (-3 <= val && val <= -2) {
                value_5_4 = true;
            }

            if (value_1_0) {
                if (-10 <= val && val <= -7.5) {
                    value_8_7 = true;
                }
            }

            return this;
        }

        public boolean isSessionFinished() {
            if (value7_8 && value4_5 && value0_1 && value_1_0 && value_5_4 && value_8_7) {
                return true;
            } else {
                return false;
            }
        }

        public RotateSession reset() {
            value7_8 = false;
            value0_1 = false;
            value_1_0 = false;
            value_5_4 = false;
            value_8_7 = false;

            return this;
        }

    }

}
