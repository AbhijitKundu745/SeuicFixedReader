package com.psl.seuicfixedreader.MQTT

interface MqttConnectionCallBack {
    fun onSuccess()
    fun onFailure(errorMessage: String)
}