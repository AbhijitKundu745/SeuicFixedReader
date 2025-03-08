package com.psl.seuicfixedreader.MQTT

import android.util.Log
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import java.net.URI

class MQTTPub {
    private var mqttClient: MqttAsyncClient? = null

    fun connectAndPublish(url : String, topic: String, message: String, clientId: String?, callback: MqttResponseCallback) {
        val uri = URI(url)
        val host = uri.host
        val tcpBrokerUrl = "tcp://"+host+":1883"
        Log.e("tcp", tcpBrokerUrl)
        try {
            val connOpts = MqttConnectionOptions()
            connOpts.connectionTimeout = 30
            connOpts.keepAliveInterval = 60
            //connOpts.userName = "mqttuser"
            //connOpts.password = "psladmin!23".toByteArray()

            // connOpts.setCleanStart(false);  // MQTT 5 clean start equivalent
            // connOpts.setSessionExpiryInterval(0L);  // Retain session indefinitely
            mqttClient = MqttAsyncClient(tcpBrokerUrl, clientId, MemoryPersistence())
            mqttClient!!.connect(connOpts, null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    println("Connected to MQTT broker: $tcpBrokerUrl")
                    publishMessage(topic, message, 1, callback)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    println("Error connecting to MQTT broker: " + exception.message)
                    exception.printStackTrace()
                    println("Error details: " + asyncActionToken.exception.message)
                    callback.onPublishFailure("Error connecting to MQTT broker: " + exception.message)
                }
            })
        } catch (e: MqttException) {
            println("Error connecting to MQTT broker: " + e.message)
            e.printStackTrace()
        }
    }

    private fun publishMessage(topic: String, message: String, qos: Int, callback: MqttResponseCallback) {
        try {
            val messagePayload = message.toByteArray()
            if (mqttClient == null) {
                println("MQTT Client is null. Cannot publish message.")
                return
            }

            mqttClient!!.publish(topic, messagePayload, qos, false, null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    println("Message published successfully!")
                    callback.onPublishSuccess()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    println("Error publishing message: ${exception?.message}")
                    exception?.printStackTrace()
                    callback.onPublishFailure("Error publishing message: ${exception?.message}")
                }
            })

        } catch (e: MqttException) {
            println("Error publishing message: " + e.message)
            e.printStackTrace()
            callback.onPublishFailure("Error publishing message: " + e.message)
        }
    }
}