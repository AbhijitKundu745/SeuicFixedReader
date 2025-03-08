package com.psl.seuicfixedreader.MQTT

import android.util.Log
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.common.MqttException

class MqttSubscriber {
    fun subscribeMessage(mqttConn : MQTTConnection, topic : String, callback: MqttCallback){
        val mqttClient = mqttConn.getClient()

        if (mqttClient == null || !mqttConn.isConnected()) {
            Log.e("MQTT", "Cannot publish, MQTT client is not connected!")
            return
        }
        try{
            mqttClient.setCallback(callback)
            mqttClient.subscribe(topic, 1).waitForCompletion()
        }
        catch (e : MqttException){
            Log.e("MQTT", "Error subscribing: ${e.message}")
        }
    }
}