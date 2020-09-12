package leviatansoul.mqttapp;

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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Sensor Manager
    private GameSensorManager gameSensorManager;

    //MQTT Manager
    private MqttClientManager mqttClientManager;

    //Model of the MainActivity
    private MainActivityModel mainActivityModel;

    //Default configuration
    private String user, ip, port, frec = "";
    private static final String DEFAULT_IP = "192.168.8.110";
    private static final String DEFAULT_USER = "user1";
    private static final String MQTT_DEFAULT_PORT = "1883";
    private static final int DEFAULT_FRECUENCY = 50;

    //UI elements
    TextView user_view, ip_view, port_view, frec_view;
    Spinner sensorSelector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Defaut configuration
        ip = DEFAULT_IP;
        port = MQTT_DEFAULT_PORT;
        frec = Integer.toString(DEFAULT_FRECUENCY);
        user = DEFAULT_USER;


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

        //Sensor manager initialization
        gameSensorManager = new GameSensorManager(getApplicationContext());

        /**
         * Spinner is configured to select which type of sensor to use
         */
        sensorSelector = findViewById(R.id.sensorList);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sensors, R.layout.support_simple_spinner_dropdown_item);
        sensorSelector.setAdapter(adapter);

        //MainActivity model initialization
        mainActivityModel = new ViewModelProvider(this).get(MainActivityModel.class);

        mainActivityModel.brokerConnection().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean hasConnected) {

                if (hasConnected) {

                    //UI elements are disabled
                    frec_view.setEnabled(false);
                    ip_view.setEnabled(false);
                    port_view.setEnabled(false);
                    user_view.setEnabled(false);
                    sensorSelector.setEnabled(false);
                    connect.setText("Disconnect");
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();

                } else {

                    //Enabling UI elements
                    frec_view.setEnabled(true);
                    ip_view.setEnabled(true);
                    port_view.setEnabled(true);
                    user_view.setEnabled(true);
                    sensorSelector.setEnabled(true);
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
                    A new MQTT manager and client is generated and connected to the broker
                    */
                    user = user_view.getText().toString();
                    port = port_view.getText().toString();
                    ip = ip_view.getText().toString();
                    frec = frec_view.getText().toString();

                    mqttClientManager = new MqttClientManager(getApplicationContext(), ip, port, user);
                    mainActivityModel.connectToBroker(mqttClientManager, gameSensorManager, ip, port, user, frec );


                }

            }
        });


        sensorSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On selecting a spinner item
                String item = parent.getItemAtPosition(position).toString();
                gameSensorManager.registerSensor(item);

                // Showing selected spinner item
                Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }


}
