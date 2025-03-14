package com.psl.seuicfixedreader.MQTT


import android.os.Handler
import android.os.Looper
import android.util.Log
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.net.URI
import java.util.Timer
import java.util.TimerTask

class MQTTConnection {
    private var mqttClient: MqttAsyncClient? = null
    private var isConnected = false
    private var brokerUrl: String? = null
    private var clientID: String? = null
    private var callback: MqttConnectionCallBack? = null
    private var reconnectTimer: Timer? = null

    fun connect(url: String, clientID: String, callback: MqttConnectionCallBack?) {
        this.brokerUrl = url
        this.clientID = clientID
        this.callback = callback

        val uri = URI(url)
        val host = uri.host
        val tcpBrokerUrl = "tcp://$host:1883"
        Log.e("MQTT", "Connecting to $tcpBrokerUrl")

        try {
            val connOpts = MqttConnectionOptions()
            connOpts.connectionTimeout = 30  // Time to establish connection
            connOpts.keepAliveInterval = 60  // Keep connection alive
            connOpts.sessionExpiryInterval = 0L  // Keep session alive after disconnect
            connOpts.isCleanStart = true  // Keep previous session state
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
                    stopReconnectTimer() // Stop reconnect attempts
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTT", "Connection failed: ${exception.message}")
                    isConnected = false
                    callback?.onFailure("Error connecting to MQTT broker: ${exception.message}")

                    scheduleReconnect()
                }
            })
        }
        catch (e: MqttException){
            Log.e("MQTT", "MQTTException in connection: ${e.message}")
            e.printStackTrace()
            isConnected = false
            callback?.onFailure("MQTTException in MQTT connection: ${e.message}")

            scheduleReconnect()
        }
        catch (e : Exception){
            Log.e("MQTT", "Exception in connection: ${e.message}")
            e.printStackTrace()
            isConnected = false
            callback?.onFailure("Exception in MQTT connection: ${e.message}")
            scheduleReconnect()
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
    private fun scheduleReconnect() {
        if (reconnectTimer == null) {
            reconnectTimer = Timer()
        }

        reconnectTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (!isConnected) {
                    Log.e("MQTT", "Attempting MQTT reconnection...")
                    brokerUrl?.let { url ->
                        clientID?.let { id ->
                            connect(url, id, callback)
                        }
                    }
                }
            }
        }, 5000, 10000) // Initial delay: 5 sec, Repeat every 10 sec
    }
    private fun stopReconnectTimer() {
        reconnectTimer?.cancel()
        reconnectTimer = null
    }
}