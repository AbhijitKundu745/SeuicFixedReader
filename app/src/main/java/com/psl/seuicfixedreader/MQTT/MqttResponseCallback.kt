package com.psl.seuicfixedreader.MQTT

interface MqttResponseCallback {
    fun onPublishSuccess()
    fun onPublishFailure(error: String)
}