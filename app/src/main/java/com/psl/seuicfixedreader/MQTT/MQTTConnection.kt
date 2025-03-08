package com.psl.seuicfixedreader.MQTT

import android.util.Log
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import java.net.URI

class MQTTConnection {
    private var mqttClient: MqttAsyncClient? = null
    private var isConnected = false

    fun connect(url: String, clientID: String, callback: MqttConnectionCallBack?) {
        val uri = URI(url)
        val host = uri.host
        val tcpBrokerUrl = "tcp://$host:1883"
        Log.e("MQTT", "Connecting to $tcpBrokerUrl")

        try {
            val connOpts = MqttConnectionOptions()
            connOpts.connectionTimeout = 30  // Time to establish connection
            connOpts.keepAliveInterval = 60  // Keep connection alive
            connOpts.sessionExpiryInterval = 0L  // Keep session alive after disconnect
            connOpts.isCleanStart = false  // Keep previous session state
            connOpts.isAutomaticReconnect = true // Auto-reconnect when network is available
            //connOpts.userName = "mqttuser"
            //connOpts.password = "psladmin!23".toByteArray()

            if(mqttClient==null){
                mqttClient = MqttAsyncClient(tcpBrokerUrl, clientID, MemoryPersistence())
            }
            if (mqttClient!!.isConnected) {
                Log.e("MQTT", "Already connected to MQTT broker.")
                isConnected = true
                callback?.onSuccess()
                return
            }
            mqttClient!!.connect(connOpts, null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.e("MQTT", "Connected to MQTT broker: $tcpBrokerUrl")
                    isConnected = true
                    callback?.onSuccess()
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Connection failed: ${exception.message}")
                    isConnected = false
                    callback?.onFailure("Error connecting to MQTT broker: ${exception.message}")
                }
            })
        }
        catch (e: MqttException){
            Log.e("MQTT", "MQTTException in connection: ${e.message}")
            e.printStackTrace()
            isConnected = false
            callback?.onFailure("MQTTException in MQTT connection: ${e.message}")
        }
        catch (e : Exception){
            Log.e("MQTT", "Exception in connection: ${e.message}")
            e.printStackTrace()
            isConnected = false
            callback?.onFailure("Exception in MQTT connection: ${e.message}")
        }
    }
    fun getClient(): MqttAsyncClient? {
        return mqttClient
    }
    fun isConnected(): Boolean {
        return isConnected
    }
    fun disconnect() {
        try {
            mqttClient?.disconnect()?.waitForCompletion()
            mqttClient = null
            isConnected = false
            Log.e("MQTT", "Disconnected from MQTT broker")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error while disconnecting: ${e.message}")
            e.printStackTrace()
        }
    }
}