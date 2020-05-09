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
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
    String port = "1883";
    String topic = "/Sensor/Accelerometer/x";
    int delay = 2000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI Stuff

        //Sensor Configuration
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //MQTT Configuration
        String clientId = "SensorApp"+ System.currentTimeMillis();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+AZURE_IP+":"+port, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); //the client and server will not maintain state across restarts of the client, the server or the connection.
        options.setAutomaticReconnect(true);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Log.d(TAG, "Reconnected to:");

                    // Because Clean Session is true, we need to re-subscribe
                   // subscribeToTopic(); Not needed yet
                } else {
                    Log.d(TAG, "Connected to: ");

                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

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
            Log.d(TAG, "Client disconnected");

            e.printStackTrace();
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event){
   // Log.d(TAG, "onSensorChanged: X "+ event.values[0]);
    acc_val = ""+event.values[0];
    }

    Runnable runnableTest =  new Runnable() {
        @Override
        public void run() {
            boolean exit = false;
                while (!exit){
                    try {
                        Thread.sleep(delay);

                        try {

                            if (client.isConnected()){
                                String payload = acc_val+" "+System.currentTimeMillis();
                                MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
                                message.setQos(0);
                                client.publish(topic, message);
                                //message.setRetained(false); Default

                            } else {
                                exit = true;
                            }
                        } catch (UnsupportedEncodingException | MqttException e) {
                            e.printStackTrace();
                            Log.d(TAG, "MqttException in thread");
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        }
    };

}
