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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";

    // Sensor Managment elements
    private SensorManager sensorManager;
    private int sensorSelected = Sensor.TYPE_ACCELEROMETER;
    String acc_val = "0";
    String gyr_val = "0";

    //MQTT Options
    MqttAndroidClient client;
    boolean isClientConnect = false; //To indicate if the connection is established
    Thread introThread;


    //Default configuration
    String user, ip, port, frec = "";
    String AZURE_IP = "51.140.222.237";
    String LOCAL_IP = "192.168.2.244";
    String MQTT_PORT = "1883";
    String TOPIC_ACCELEROMETER = "/Sensor/Accelerometer/x";
    String topic = "/Sensor/Accelerometer/x";
    int delay = 50;

    //UI elements
    TextView user_view, ip_view, port_view, frec_view;
    Spinner sensorType;
    boolean exit = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Init");

        //Configuration
        ip = LOCAL_IP;
        port = MQTT_PORT;
        frec = Integer.toString(delay);
        user = "user1";



        //UI Stuff
        final Button connect = (Button) findViewById(R.id.connect);
        user_view = (TextView) findViewById(R.id.client);
        user_view.setText(user);
        ip_view = (TextView) findViewById(R.id.ip);
        ip_view.setText(ip);
        port_view = (TextView) findViewById(R.id.port);
        port_view.setText(port);
        frec_view = (TextView) findViewById(R.id.frec);
        frec_view.setText(frec);


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isClientConnect) {
                    Toast.makeText(getApplicationContext(), "Disconecting", Toast.LENGTH_SHORT).show();
                    try {
                        exit = true;
                        client.disconnect();
                        isClientConnect = false;

                        frec_view.setEnabled(true);
                        ip_view.setEnabled(true);
                        port_view.setEnabled(true);
                        user_view.setEnabled(true);
                        sensorType.setEnabled(true);
                        connect.setText("Connect");

                    } catch (MqttException e) {
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

                                frec_view.setEnabled(false);
                                ip_view.setEnabled(false);
                                port_view.setEnabled(false);
                                user_view.setEnabled(false);
                                sensorType.setEnabled(false);
                                connect.setText("Disconnect");

                                introThread = new Thread(sendMQTTMessages);
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



                }

            }
        });

        //Sensor Configuration
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorType = findViewById(R.id.sensorList);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sensors, R.layout.support_simple_spinner_dropdown_item);
        sensorType.setAdapter(adapter);

        sensorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On selecting a spinner item
                String item = parent.getItemAtPosition(position).toString();
                switch (item) {
                    case "Accelerometer":
                        topic = "/Sensor/Accelerometer/x";
                        sensorManager.unregisterListener(MainActivity.this);
                        sensorManager.registerListener(MainActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
                        sensorSelected = Sensor.TYPE_ACCELEROMETER;
                        break;

                    case "Gyroscope":
                        topic = "/Sensor/Gyroscope/x";
                        sensorManager.unregisterListener(MainActivity.this);
                        sensorManager.registerListener(MainActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                        sensorSelected = Sensor.TYPE_GYROSCOPE;
                        break;

                    default:
                        break;
                }


                // Showing selected spinner item
                Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                acc_val = "" + event.values[0];
                //Log.d(TAG, "Accelerometer value: X " + event.values[0]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyr_val = "" + event.values[0];
               // Log.d(TAG, "Gyroscope value: X " + event.values[0]);
                break;
            default:
                break;
        }
    }

    public MqttAndroidClient mqttClientConfiguration() {
        //MQTT Configuration

        final String clientId = user_view.getText().toString();
        port = port_view.getText().toString();
        ip = ip_view.getText().toString();
        MqttAndroidClient client_test = new MqttAndroidClient(this.getApplicationContext(), "tcp://" + ip + ":" + port, clientId);


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

        return client_test;

    }

    /**
     * Send concurrently the sensor values to the MQTT broker until the user stops the connection
     */
    Runnable sendMQTTMessages = new Runnable() {
        @Override
        public void run() {

            exit = false;
            String name = client.getClientId();
            Log.d(TAG, "usuario "+ name);

            delay = Integer.parseInt(frec_view.getText().toString());
            while (!exit) {
                try {


                    try {
                        if (client.isConnected() && client.getClientId().equals(name)) { //Make sure the client is connect and the id is the same

                            String payload = "";
                            switch (sensorSelected) {
                                case Sensor.TYPE_ACCELEROMETER:
                                    payload = acc_val + " " + System.currentTimeMillis();
                                    topic = "/Sensor/"+name+"/Accelerometer/x";
                                    break;
                                case Sensor.TYPE_GYROSCOPE:
                                    payload = gyr_val + " " + System.currentTimeMillis();
                                    topic = "/Sensor/"+name+"/Gyroscope/x";
                                    break;
                                default:
                                    break;
                            }

                            MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
                            message.setQos(0);
                            client.publish(topic, message);

                            Thread.sleep(delay);

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
