package com.psl.seuicfixedreader.MQTT

import android.util.Log
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import java.net.URI
import java.nio.charset.StandardCharsets

class MQTTSub {
    private var mqttClient: MqttAsyncClient? = null


    fun subscribe(url: String, topic: String, clientID : String, callbackHandler: MqttCallback?) {
        val uri = URI(url)
        val host = uri.host
        val tcpBrokerUrl = "tcp://"+host+":1883"
        Log.e("tcp", tcpBrokerUrl)
        val subQos = 1

        try {
            val options = MqttConnectionOptions()
//            options.userName = "mqttuser"
//            options.password = "psladmin!23".toByteArray(StandardCharsets.UTF_8)
            options.isCleanStart = true
            options.sessionExpiryInterval = 0L

            mqttClient = MqttAsyncClient(tcpBrokerUrl, clientID, MemoryPersistence())
            mqttClient!!.setCallback(callbackHandler)
            mqttClient!!.connect(options).waitForCompletion()
            println("Connected to MQTT broker")
            mqttClient!!.subscribe(topic, subQos).waitForCompletion()
            println("Subscribed to topic: $topic")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient!!.unsubscribe(topic).waitForCompletion()
            println("Unsubscribed from topic: $topic")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient!!.disconnect().waitForCompletion()
            println("Disconnected from MQTT broker")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}