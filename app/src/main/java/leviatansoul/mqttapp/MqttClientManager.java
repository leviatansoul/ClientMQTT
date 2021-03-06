package leviatansoul.mqttapp;

import android.content.Context;
import android.util.Log;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.io.UnsupportedEncodingException;

import static android.content.ContentValues.TAG;

public class MqttClientManager {


    private MqttAndroidClient client;
    private String user;
    private  String ip;
    private  String port;
    private  boolean isClientConnected;

    public MqttClientManager(Context context, String ip, String port, String user) {
        this.user = user;
        this.ip = ip;
        this.port = port;
        this.isClientConnected = false;

        client = new MqttAndroidClient(context, "tcp://" + this.ip + ":" + this.port, this.user);
        client.setCallback(new MqttCallbackExtended() {
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
                isClientConnected = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception { //This method is called when a message arrives from the server.
                Log.d(TAG, "Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { //For QoS 0 messages it is called once the message has been handed to the network for delivery

            }
        });
    }


    public boolean isClientConnected() {
        return isClientConnected;
    }

    public void disconnectClient() {
        //client.disconnect();
        client.close();

    }

    public void publishMessage(String topic, MqttMessage mqttMessage){
        try {
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public IMqttToken connectClient( MqttConnectOptions options){
        try {
            return client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.d(TAG, "Client disconnected");
        }
        return null;
    }

    public void setStatus(boolean clientConnected) {
        isClientConnected = clientConnected;
    }

    public MqttConnectOptions getOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); //the client and server will not maintain state across restarts of the client, the server or the connection.
        options.setAutomaticReconnect(true);
        return options;
    }

    public MqttMessage prepareMQTTMessage(String data, int quality) {
        String payload = data + " " + System.currentTimeMillis();
        MqttMessage message = null;
        try {
            message = new MqttMessage(payload.getBytes("UTF-8"));
            message.setQos(0);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return message;
    }

    public  String getUser() {
        return user;
    }

    public  String getIp() {
        return ip;
    }

    public  String getPort() {
        return port;
    }


}
