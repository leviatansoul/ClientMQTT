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


    /*
    The attribute isConnected represent the status of the client connection
     */
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

    }

    public void connectToBroker(final MqttClientManager mqttClientManager, final GameSensorManager gameSensorManager, String ip, String port, final String user, final String frec) {

        if (mqttClientManager.isClientConnected()) {


            //MQTT client is disconnected and thread is stopped
            mqttClientManager.setStatus(false);
            isConnected.postValue(false);
            mqttClientManager.disconnetClient();

            Log.d(TAG, "Enter in disconnect");

        } else {

            try {
                /*
                 A new MQTT client is generated and connected to the broker
                 */
                IMqttToken token = mqttClientManager.getClient().connect(mqttClientManager.getOptions());
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        mqttClientManager.setStatus(true);
                        isConnected.postValue(true);
                        Log.d(TAG, "Client connected Succsefully");


                        /**
                         * Send concurrently the sensor values to the MQTT broker until the user stops the connection
                         */
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        int frecuency = Integer.parseInt(frec);

                                        while (mqttClientManager.isClientConnected()) {
                                            try {

                                                String topic = gameSensorManager.getSensorTopic(user);
                                                MqttMessage message = mqttClientManager.prepareMQTTMessage(gameSensorManager.getSensorValue(), 0);
                                                mqttClientManager.publishMessage(topic, message);

                                                Thread.sleep(frecuency);

                                            } catch (  InterruptedException e) {
                                                e.printStackTrace();

                                                mqttClientManager.setStatus(false);
                                                isConnected.postValue(false);
                                                Log.d(TAG, "Thread interrupted");
                                            }
                                        }
                                        Log.d(TAG, "Connection finished" );

                                    }
                                }
                        ).start();


                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.d(TAG, "onFailure - connection timeout or firewall problems");

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
