package leviatansoul.mqttapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.security.Timestamp;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";

    private SensorManager sensorManager;
    Sensor accelerometer;
    MqttAndroidClient client;
    Thread introThread;
    String acc_val = "0";
    String AZURE_IP = "51.140.222.237";
    String LOCAL_IP = "192.168.2.244";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        String clientId = MqttClient.generateClientId();
        //client = new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.2.244:1883", clientId);
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+AZURE_IP+":1883", clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                    introThread = new Thread(runnableTest);
                    introThread.start();


                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event){
    Log.d(TAG, "onSensorChanged: X "+ event.values[0]);
    acc_val = ""+event.values[0];
    }

    Runnable runnableTest =  new Runnable() {
        @Override
        public void run() {
                while (true){
                    try {
                        Thread.sleep(60);
                        //Log.d("TIME", "50 mili-seconds");

                        String topic = "/Sensor/Accelerometer/x";
                        String payload = acc_val+" "+System.currentTimeMillis();
                        byte[] encodedPayload = new byte[0];
                        try {
                            encodedPayload = payload.getBytes("UTF-8");
                            MqttMessage message = new MqttMessage(encodedPayload);
                            client.publish(topic, message);
                        } catch (UnsupportedEncodingException | MqttException e) {
                            e.printStackTrace();
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        }
    };

}
