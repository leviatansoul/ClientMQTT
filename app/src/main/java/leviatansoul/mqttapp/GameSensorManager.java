package leviatansoul.mqttapp;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Class to manage the sensor used for the game.
 */
public class GameSensorManager implements SensorEventListener {

    // Sensor Management elements
    private SensorManager sensorManager;
    private int sensorSelected;
    private String accelerometer_value = "0";
    private String gyroscope_value = "0";

    public GameSensorManager(Context c) {
        sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(GameSensorManager.this);
        sensorManager.registerListener(GameSensorManager.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        sensorSelected = Sensor.TYPE_ACCELEROMETER;
    }

    /**
     * Register in the Sensor Manager a listerner for the introduced sensor
     * @param sensorType
     */
    public void registerSensor(String sensorType) {

        switch (sensorType) {
            case "Accelerometer":
                sensorManager.unregisterListener(GameSensorManager.this);
                sensorManager.registerListener(GameSensorManager.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
                sensorSelected = Sensor.TYPE_ACCELEROMETER;
                break;

            case "Gyroscope":
                sensorManager.unregisterListener(GameSensorManager.this);
                sensorManager.registerListener(GameSensorManager.this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                sensorSelected = Sensor.TYPE_GYROSCOPE;
                break;

            default:
                break;
        }
    }

    public int getSensorSelected() {
        return sensorSelected;
    }

    public void setSensorSelected(int sensorSelected) {
        this.sensorSelected = sensorSelected;
    }

    /**
     * Get the current value of the selected sensor
     * @return
     */
    public String getSensorValue() {
        String sensorValue = "0";
        switch (sensorSelected) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorValue = accelerometer_value;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorValue = gyroscope_value;
                break;
            default:
                break;
        }
        return sensorValue;
    }

    /**
     * Get the topic from the selected sensor
     * @param id
     * @return
     */
    public String getSensorTopic(String id) {
        String topic = "";
        switch (sensorSelected) {
            case Sensor.TYPE_ACCELEROMETER:
                topic = "/Sensor/" + id + "/Accelerometer/x";
                break;
            case Sensor.TYPE_GYROSCOPE:
                topic = "/Sensor/" + id + "/Gyroscope/x";
                break;
            default:
                break;
        }
        return topic;

    }

    /**
     * Every time the sensor value changes it is updated.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometer_value = "" + event.values[0];
                //Log.d(TAG, "Accelerometer value: X " + event.values[0]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscope_value = "" + event.values[0];
                // Log.d(TAG, "Gyroscope value: X " + event.values[0]);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
