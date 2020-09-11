package leviatansoul.mqttapp;


import android.content.Context;
import android.hardware.Sensor;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static android.content.ContentValues.TAG;

public class MainActivityModel extends ViewModel {


    private MutableLiveData<Boolean> isConnected = new MutableLiveData<>();

    public MainActivityModel() {

    }

    public LiveData<Boolean> brokerConnection() {
        return isConnected;
    }

    public void disconnectToBroker(final MqttClientManager mqttClientManager) {


        mqttClientManager.setStatus(false);
        isConnected.postValue(false);


        mqttClientManager.disconnetClient();

        //mqttClientManager.getClient().close();
    }

    public void connectToBroker(final MqttClientManager mqttClientManager, final Context context, String ip, String port, String user, final String frec, final int sensorSelected) {

        if (mqttClientManager.isClientConnected()) {
            Toast.makeText(context, "Disconecting", Toast.LENGTH_SHORT).show();
            try {

                //MQTT client is disconnected and thread is stopped


                mqttClientManager.setStatus(false);
                isConnected.postValue(false);
                mqttClientManager.getClient().disconnect();

                Log.d(TAG, "Enter in disconnect");
            } catch (MqttException e) {
                e.printStackTrace();
                Log.d(TAG, "error Disconnectings");
            }
        } else {

            try {

                        /*
                        A new MQTT client is generated and connected to the broker
                         */


                //mqttClientManager.setClient(context, ip, port, user);

                IMqttToken token = mqttClientManager.getClient().connect(mqttClientManager.getOptions());

                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        mqttClientManager.setStatus(true);
                        Log.d(TAG, "onSuccess");

                        isConnected.postValue(true);


                        /**
                         * Send concurrently the sensor values to the MQTT broker until the user stops the connection
                         */
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        String name = mqttClientManager.getClient().getClientId();
                                        Log.d(TAG, "usuario " + name);
                                        int frecuency = Integer.parseInt(frec);
                                        String topic = "";

                                        while (mqttClientManager.isClientConnected()) {
                                            try {

                                                Log.d(TAG, "value " + mqttClientManager.getClient().isConnected());
                                                String payload = "";
                                                MqttMessage message = new MqttMessage();
                                                switch (sensorSelected) {
                                                    case Sensor.TYPE_ACCELEROMETER:
                                                        message = mqttClientManager.prepareMQTTMessage(MainActivity.accelerometer_value, 0);
                                                        topic = "/Sensor/" + name + "/Accelerometer/x";
                                                        break;
                                                    case Sensor.TYPE_GYROSCOPE:
                                                        message = mqttClientManager.prepareMQTTMessage(MainActivity.gyroscope_value, 0);
                                                        topic = "/Sensor/" + name + "/Gyroscope/x";
                                                        break;
                                                    default:
                                                        break;
                                                }

                                                mqttClientManager.getClient().publish(topic, message);
                                                Thread.sleep(frecuency);


                                            } catch (MqttException | InterruptedException e) {
                                                e.printStackTrace(); //When thread breaks
                                                mqttClientManager.setStatus(false);
                                                Log.d(TAG, "Thread rota");
                                                isConnected.postValue(false);
                                            }
                                        }
                                        Log.d(TAG, "NO MOREEEEE");
                                    }
                                }
                        ).start();


                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.d(TAG, "onFailure");


                        mqttClientManager.setStatus(false);
                        isConnected.postValue(false);

                    }
                });
            } catch (MqttException e) {
                Log.d(TAG, "Client disconnected");
                e.printStackTrace();
            }

        }


    }


}
