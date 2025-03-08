package com.psl.seuicfixedreader.MQTT
import android.util.Log
import android.widget.Toast
import com.psl.seuicfixedreader.MQTT.MQTTConnection
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.common.MqttException

class MqttPublisher {
    fun publishMessage(mqttConn : MQTTConnection, topic : String, message : String, callback: MqttResponseCallback){
        val mqttClient = mqttConn.getClient()
        Log.e("MqqCl", mqttClient.toString())
        if (mqttClient == null || !mqttConn.isConnected()) {
            Log.e("MQTT", "Cannot publish, MQTT client is not connected!")

        } else{
            mqttConn.connect("http://192.168.0.172/WMS31/", "Reader", object : MqttConnectionCallBack {
                override fun onSuccess() {
                    Log.e("MQTTConn", "MQTT connected Succesfully")
                }

                override fun onFailure(errorMessage: String) {
                    Log.e("MQTTConn", errorMessage)
                }
            })
        }
        try{
            val messagePayload = message.toByteArray()
            mqttClient?.publish(topic, messagePayload, 1, false, null, object :
                MqttActionListener {
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
        }
        catch (e : MqttException){
            println("Error publishing message: " + e.message)
            e.printStackTrace()
            callback.onPublishFailure("Error publishing message: " + e.message)
        }
    }
}