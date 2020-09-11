package leviatansoul.mqttapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.io.UnsupportedEncodingException;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";

    // Sensor Managment elements
    private SensorManager sensorManager;
    private int sensorSelected = Sensor.TYPE_ACCELEROMETER;
    public static String accelerometer_value = "0";
    public static String gyroscope_value = "0";

    //MQTT Options
    MqttAndroidClient client;
    boolean isClientConnect = false; //To indicate if the connection is established
    private MqttClientManager mqttClientManager;


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


    private MainActivityModel mainActivityModel;


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

        mainActivityModel = new ViewModelProvider(this).get(MainActivityModel.class);

        mainActivityModel.brokerConnection().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean hasConnected) {

                Log.d(TAG, "onSuccess " + hasConnected);
                if (hasConnected) {

                    //UI elements are disabled
                    frec_view.setEnabled(false);
                    ip_view.setEnabled(false);
                    port_view.setEnabled(false);
                    user_view.setEnabled(false);
                    sensorType.setEnabled(false);
                    connect.setText("Disconnect");
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();

                } else {

                    //Enabling UI elements
                    frec_view.setEnabled(true);
                    ip_view.setEnabled(true);
                    port_view.setEnabled(true);
                    user_view.setEnabled(true);
                    sensorType.setEnabled(true);
                    connect.setText("Connect");
                    Toast.makeText(getApplicationContext(), "Disconected", Toast.LENGTH_SHORT).show();

                }
            }
        });


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mqttClientManager != null && mqttClientManager.isClientConnected()) {
                    Toast.makeText(getApplicationContext(), "Disconecting", Toast.LENGTH_SHORT).show();

                    //MQTT client is disconnected and thread is stopped
                    mqttClientManager.setStatus(false);
                    mainActivityModel.disconnectToBroker(mqttClientManager);
                    mqttClientManager = null;


                } else {

                    /*
                    A new MQTT client is generated and connected to the broker
                    */
                    user = user_view.getText().toString();
                    port = port_view.getText().toString();
                    ip = ip_view.getText().toString();
                    frec = frec_view.getText().toString();
                    Log.d(TAG, "data are " + user + port + ip);
                    mqttClientManager = new MqttClientManager(getApplicationContext(), ip, port, user);
                    mainActivityModel.connectToBroker(mqttClientManager, getApplicationContext(), ip, port, user, frec, sensorSelected);


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
     *
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


}
