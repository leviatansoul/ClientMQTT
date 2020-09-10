package leviatansoul.mqttapp;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MqttClientManager {



    private MqttAndroidClient client;
    private boolean isClientConnected;

    public MqttClientManager (){
        this.isClientConnected = false;
    }


    public MqttAndroidClient getClient() {
        return client;
    }

    public void setClient(Context c, String ip, String port, String user) {

        this.client = new MqttAndroidClient(c, "tcp://" + ip + ":" + port, user);

    }

    public boolean isClientConnected() {
        return isClientConnected;
    }

    public void setStatus(boolean clientConnected) {
        isClientConnected = clientConnected;
    }

    public MqttConnectOptions getOptions(){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); //the client and server will not maintain state across restarts of the client, the server or the connection.
        options.setAutomaticReconnect(true);
        return options;
    }

    public MqttMessage prepareMQTTMessage(String data, int quality){
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


}
