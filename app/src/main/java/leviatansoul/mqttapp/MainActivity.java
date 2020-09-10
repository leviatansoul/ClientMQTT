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
    String accelerometer_value = "0";
    String gyroscope_value = "0";

    //MQTT Options
    MqttAndroidClient client;
    boolean isClientConnect = false; //To indicate if the connection is established
    MqttClientManager mqttClientManager = new MqttClientManager();


    //Default configuration
    String user, ip, port, frec = "";
    String AZURE_IP = "51.140.222.237";
    String LOCAL_IP = "192.168.2.244";
    String MQTT_PORT = "1883";
    String topic = "/Sensor/user1/Accelerometer/x";
    int frecuency = 50;

    //UI elements
    TextView user_view, ip_view, port_view, frec_view;
    Spinner sensorType;
    boolean exit = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Configuration
        ip = LOCAL_IP;
        port = MQTT_PORT;
        frec = Integer.toString(frecuency);
        user = "user1";


        //UI initial configuration
        final Button connect = (Button) findViewById(R.id.connect);
        user_view = (TextView) findViewById(R.id.client);
        user_view.setText(user);
        ip_view = (TextView) findViewById(R.id.ip);
        ip_view.setText(ip);
        port_view = (TextView) findViewById(R.id.port);
        port_view.setText(port);
        frec_view = (TextView) findViewById(R.id.frec);
        frec_view.setText(frec);

        //Sensor Configuration
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        /**
         * Spinner is configured to select which type of sensor to use
         */
        sensorType = findViewById(R.id.sensorList);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sensors, R.layout.support_simple_spinner_dropdown_item);
        sensorType.setAdapter(adapter);


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mqttClientManager.isClientConnected()) {
                    Toast.makeText(getApplicationContext(), "Disconecting", Toast.LENGTH_SHORT).show();
                    try {

                        //MQTT client is disconnected and thread is stopped
                        exit = true;
                        mqttClientManager.getClient().disconnect();
                        mqttClientManager.setStatus(false);
                        isClientConnect = false;

                        //Enabling UI elements
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

                        /*
                        A new MQTT client is generated and connected to the broker
                         */
                        user = user_view.getText().toString();
                        port = port_view.getText().toString();
                        ip = ip_view.getText().toString();

                        mqttClientManager.setClient(getApplicationContext(), ip, port, user);
                        mqttClientManager.getOptions();


                        IMqttToken token = mqttClientManager.getClient().connect(mqttClientManager.getOptions());
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // We are connected
                                mqttClientManager.setStatus(true);
                                Log.d(TAG, "onSuccess");
                                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();

                                //UI elements are disabled
                                frec_view.setEnabled(false);
                                ip_view.setEnabled(false);
                                port_view.setEnabled(false);
                                user_view.setEnabled(false);
                                sensorType.setEnabled(false);
                                connect.setText("Disconnect");

                                new Thread(sendMQTTMessages).start();


                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                // Something went wrong e.g. connection timeout or firewall problems
                                Log.d(TAG, "onFailure");

                                isClientConnect = false;
                                mqttClientManager.setStatus(false);

                            }
                        });
                    } catch (MqttException e) {
                        Log.d(TAG, "Client disconnected");
                        e.printStackTrace();
                    }

                }

            }
        });



        sensorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On selecting a spinner item
                String item = parent.getItemAtPosition(position).toString();
                switch (item) {
                    case "Accelerometer":
                        sensorManager.unregisterListener(MainActivity.this);
                        sensorManager.registerListener(MainActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
                        sensorSelected = Sensor.TYPE_ACCELEROMETER;
                        break;

                    case "Gyroscope":
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

    /**
     * Once sensor are being tracked, everytime there are changes in the sensor values it will be saved
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

    /**
     * This method generate a new MQTT client based in the configuration introduced in the UI
     * @return MQTT Client object
     */
    public MqttAndroidClient mqttClientConfiguration() {
        //MQTT Configuration

        final String clientId = user_view.getText().toString();
        port = port_view.getText().toString();
        ip = ip_view.getText().toString();
        MqttAndroidClient client_test = new MqttAndroidClient(this.getApplicationContext(), "tcp://" + ip + ":" + port, clientId);

        client_test.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) { //Called when the connection to the server is completed successfully.

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
                mqttClientManager.setStatus(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception { //This method is called when a message arrives from the server.
                Log.d(TAG, "Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { //For QoS 0 messages it is called once the message has been handed to the network for delivery

            }
        });

        return client_test;

    }

    /**
     * Send concurrently the sensor values to the MQTT broker until the user stops the connection
     */
    public Runnable sendMQTTMessages = new Runnable() {
        @Override
        public void run() {

            exit = false;
            String name = mqttClientManager.getClient().getClientId();
            Log.d(TAG, "usuario "+ name);

            frecuency = Integer.parseInt(frec_view.getText().toString());

            while (!exit) {
                try {
                    try {
                        if (mqttClientManager.isClientConnected()) { //Make sure the client is connect and the id is the same

                            String payload = "";
                            MqttMessage message = new MqttMessage();
                            switch (sensorSelected) {
                                case Sensor.TYPE_ACCELEROMETER:
                                    message = mqttClientManager.prepareMQTTMessage(accelerometer_value, 0);
                                    topic = "/Sensor/"+name+"/Accelerometer/x";
                                    break;
                                case Sensor.TYPE_GYROSCOPE:
                                    message = mqttClientManager.prepareMQTTMessage(gyroscope_value, 0);
                                    topic = "/Sensor/"+name+"/Gyroscope/x";
                                    break;
                                default:
                                    break;
                            }

                            mqttClientManager.getClient().publish(topic, message);

                            Thread.sleep(frecuency);

                        } else {
                            exit = true;
                        }
                    } catch ( MqttException e) {
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
