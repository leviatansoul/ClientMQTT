package leviatansoul.mqttapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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

    // Sensor Managment elements
    private SensorManager sensorManager;
    Sensor accelerometer;
    String acc_val = "0";

    //MQTT Options
    MqttAndroidClient client;
    boolean isClientConnect = false; //To indicate if the connection is established
    Thread introThread;



    //Default configuration
    String ip, port, frec = "";
    String AZURE_IP = "51.140.222.237";
    String LOCAL_IP = "192.168.2.244";
    String MQTT_PORT = "1883";
    String TOPIC_ACCELEROMETER = "/Sensor/Accelerometer/x";
    String topic = "/Sensor/Accelerometer/x";
    int delay = 50;

    //UI elements
    TextView ip_view, port_view, frec_view;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Init");

        //Configuration
        ip = LOCAL_IP;
        port = MQTT_PORT;
        frec = Integer.toString(delay);


        //UI Stuff
        Button connect = (Button) findViewById(R.id.connect);
        ip_view = (TextView) findViewById(R.id.ip);
        ip_view.setText(ip);
        port_view = (TextView) findViewById(R.id.port);
        port_view.setText(port);
        frec_view = (TextView) findViewById(R.id.frec);
        frec_view.setText(frec);



        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isClientConnect){
                    Toast.makeText(getApplicationContext(), "Disconecting",   Toast.LENGTH_SHORT).show();
                    try {
                        client.disconnect();
                    } catch (MqttException e){
                        e.printStackTrace();
                    }
                } else {

                    try {
                        client = mqttClientConfiguration();
                        MqttConnectOptions options = new MqttConnectOptions();
                        options.setCleanSession(true); //the client and server will not maintain state across restarts of the client, the server or the connection.
                        options.setAutomaticReconnect(true);
                        IMqttToken token = client.connect(options);
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // We are connected
                                isClientConnect = true;
                                Log.d(TAG, "onSuccess");
                                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                                introThread = new Thread(runnableTest);
                                introThread.start();


                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                // Something went wrong e.g. connection timeout or firewall problems
                                Log.d(TAG, "onFailure");
                                isClientConnect = false;

                            }
                        });
                    } catch (MqttException e) {
                        Log.d(TAG, "Client disconnected");

                        e.printStackTrace();
                    }


                    Toast.makeText(getApplicationContext(), "Client is disconnected",   Toast.LENGTH_SHORT).show();
                }

            }
        });

        //Sensor Configuration
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);





    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event){
   // Log.d(TAG, "onSensorChanged: X "+ event.values[0]);
    acc_val = ""+event.values[0];
    }

    public MqttAndroidClient mqttClientConfiguration(){
        //MQTT Configuration

        final String clientId = "SensorApp"+ System.currentTimeMillis();
        port = port_view.getText().toString();
        ip = ip_view.getText().toString();
        MqttAndroidClient client_test = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+ip+":"+port, clientId);


        client_test.setCallback(new MqttCallbackExtended() {
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
                isClientConnect = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        return  client_test;

    }

    Runnable runnableTest =  new Runnable() {
        @Override
        public void run() {
            boolean exit = false;
            String name = client.getClientId();
            delay =  Integer.parseInt(frec_view.getText().toString());
                while (!exit){
                    try {
                        Thread.sleep(delay);

                        try {
                            if (client.isConnected() && client.getClientId().equals(name)){ //Make sure the client is connect and the id is the same

                                String payload = acc_val+" "+System.currentTimeMillis();
                                MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
                                message.setQos(0);
                                client.publish(topic, message);

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
