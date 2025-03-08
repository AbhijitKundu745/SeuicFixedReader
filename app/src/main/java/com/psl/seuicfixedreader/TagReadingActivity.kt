package com.psl.seuicfixedreader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.psl.seuicfixedreader.APIHelpers.APIResponse
import com.psl.seuicfixedreader.APIHelpers.APIService
import com.psl.seuicfixedreader.APIHelpers.AuthRequest
import com.psl.seuicfixedreader.APIHelpers.dataModels
import com.psl.seuicfixedreader.MQTT.MQTTConnection
import com.psl.seuicfixedreader.MQTT.MQTTPub
import com.psl.seuicfixedreader.MQTT.MqttConnectionCallBack
import com.psl.seuicfixedreader.MQTT.MqttPublisher
import com.psl.seuicfixedreader.MQTT.MqttResponseCallback
import com.psl.seuicfixedreader.adapter.TagInfoAdapter
import com.psl.seuicfixedreader.bean.TagBean
import com.psl.seuicfixedreader.databinding.ActivityTagReadingBinding
import com.psl.seuicfixedreader.handler.UHFActivity
import com.psl.seuicfixedreader.helper.ConnectionManager
import com.psl.seuicfixedreader.helper.SharedPreferencesUtils
import com.seuic.uhfandroid.ext.totalCounts
import com.seuic.uhfandroid.util.ByteUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TagReadingActivity : UHFActivity() {
    private val context: Context = this
    lateinit var binding: ActivityTagReadingBinding
    private lateinit var sharedPreferencesUtils : SharedPreferencesUtils
    private var deviceId : String= ""
    private lateinit var adapter : TagInfoAdapter
    private lateinit var antennaCheckHandler: Handler
    private lateinit var antennaCheckRunnable: Runnable
    val connectedAnts: MutableList<Int> = ArrayList()
    private var isRfidReadingIsInProgress : Boolean = false
    private lateinit var cd : ConnectionManager
    private var authResData: dataModels.AuthRes? = null
    private val tagDetails: MutableList<TagBean> = mutableListOf()
    private lateinit var mqttPub: MqttPublisher
    private lateinit var dataPostingHandler: Handler
    private lateinit var dataPostingRunnable: Runnable
    private var mqttConnection: MQTTConnection = MQTTConnection()
    @SuppressLint("HardwareIds", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tag_reading)
        cd = ConnectionManager.getInstance(application)
        sharedPreferencesUtils = SharedPreferencesUtils(context)
        deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        deviceId = deviceId.uppercase()
        Log.e("DeviceID", deviceId)
        binding.deviceID.text = "DeviceID : "+ deviceId
        sharedPreferencesUtils.setDeviceID(deviceId)

        adapter = TagInfoAdapter(R.layout.layout_tag)
        adapter.notifyDataSetChanged()

        tagData.observe(this) {bean ->
            bean?.let { setListData(bean) }
        }

        binding.btnStart.setOnClickListener{
            tagData.postValue(null)
            adapter.notifyDataSetChanged()
            isRfidReadingIsInProgress = true
            Handler(Looper.getMainLooper()).postDelayed({
                Log.e("START INV", "HERE111")
                startInventory()
            }, 1000)
        }
        binding.btnStop.setOnClickListener{
            stopInventory()
            tagData.postValue(null)
            isRfidReadingIsInProgress = false
            totalCounts.set(0)
            tagDetails.clear()
            adapter.setList(emptyList())
            adapter.notifyDataSetChanged()
        }
        binding.rlvEpc.adapter = adapter
        binding.rlvEpc.layoutManager = LinearLayoutManager(context)

        if(!isRfidReadingIsInProgress){
            startAntennaHandler()
            connectedAntennas.observe(this) { antennaArray ->
                Log.e("HereAnts", antennaArray.contentToString())
                updateCheckboxes(antennaArray)
                if(authResData?.Topic?.isNotEmpty() == true){
                    val configTopic = authResData!!.Topic.filter { it.Title == "ReaderConfig" }
                    if(configTopic.isNotEmpty()){
                        for (topic in configTopic) {
                            val topicName = topic.TopicName+sharedPreferencesUtils.getDeviceID()
                            sendConfigData(topicName)
                        }
                    }
                }
                else{
                    Toast.makeText(context, "No Topic to send", Toast.LENGTH_SHORT).show()
                }
            }
        }

        startdataPostingHandler()


        binding.URLButton.setOnClickListener{
            if (binding.URL.text.isNotEmpty()) {
                sharedPreferencesUtils.setURL(binding.URL.text.toString())
                binding.showURL.text = sharedPreferencesUtils.getURL().toString()
                Toast.makeText(this, "URL Set", Toast.LENGTH_SHORT).show()
                sharedPreferencesUtils.getDeviceID()?.let { getAuthorizeData(it) }
            } else {
                Toast.makeText(this, "No URL", Toast.LENGTH_SHORT).show()
            }
        }
        binding.showURL.text = sharedPreferencesUtils.getURL().toString()

        cd.observe(this) { isConnected ->
            if(isConnected){
                if(binding.showURL.text.isNotEmpty()){
                    sharedPreferencesUtils.getDeviceID()?.let { getAuthorizeData(it) }
                    mqttConnect()
                }
            }
        }

    }
    private fun startAntennaHandler() {
        antennaCheckHandler = Handler(Looper.getMainLooper())
        antennaCheckRunnable = object : Runnable {
            override fun run() {
                getInvantAnt()
                antennaCheckHandler.postDelayed(this, 10000)
            }
        }
        antennaCheckHandler.postDelayed(antennaCheckRunnable, 0)
    }
    private fun startdataPostingHandler() {
        dataPostingHandler = Handler(Looper.getMainLooper())
        dataPostingRunnable = object : Runnable {
            override fun run() {
                cd.observe(this@TagReadingActivity) { isConnected ->
                    if(isConnected){
                        if(tagDetails.isNotEmpty()){
                            if(authResData?.Topic?.isNotEmpty() == true){
                                val transactionTopic = authResData!!.Topic.filter { it.Title == "ReaderTransaction" }
                                Log.e("Topic", transactionTopic.toString())
                                if(transactionTopic.isNotEmpty()){
                                    for (topic in transactionTopic) {
                                        val topicName = topic.TopicName+sharedPreferencesUtils.getDeviceID()
                                        Log.e("DATA", "Found Title: ${topic.Title}, TopicName: ${topic.TopicName}")
                                        Log.e("TopicURL", topicName)
                                        sendTagData(tagDetails, topicName)
                                    }
                                }
                            }
                        }
                    }
                }
                dataPostingHandler.postDelayed(this, 2000)
            }
        }
        dataPostingHandler.postDelayed(dataPostingRunnable, 0)
    }
    private fun stopHandler(){
        antennaCheckHandler.removeCallbacks(antennaCheckRunnable)
        dataPostingHandler.removeCallbacks(dataPostingRunnable)
    }
    fun setListData(bean1: TagBean?){
        bean1?.let { myBean ->
                val epcId = myBean.epcId
                var rssiValue = myBean.rssi
                val antennaId = myBean.antenna
                val companyID = myBean.epcId.substring(0,2)
                val hexCompanyID = ByteUtil.hexToString(companyID)

//                if (rssiValue < 0) {
//                    rssiValue *= -1
//                }
//                myBean.rssi = rssiValue
                adapter.notifyDataSetChanged()
                Log.e("STOPRFID:tagid", epcId)
                Log.e("STOPRFID:ANTENA", antennaId)
                Log.e("RSSI", rssiValue.toString())
                Log.e("CompanyID", companyID)
                Log.e("HexCompanyID", hexCompanyID)
            //To show only one data at a time
            if(hexCompanyID=="20"){
                val isDuplicate = tagDetails.any { it.epcId == epcId && it.antenna == antennaId }
                if (!isDuplicate) {
                    tagDetails.add(myBean) // Add only if it's not a duplicate
                    adapter.setList(tagDetails)
                    adapter.notifyDataSetChanged()

                    Log.e("RFID: New Tag Added", "EPC: $epcId | Antenna: $antennaId")
                } else {
                    Log.e("RFID: Duplicate Tag Ignored", "EPC: $epcId | Antenna: $antennaId")
                }
            }
        }
    }
    private fun updateCheckboxes(currentAntennaArray: IntArray) {
        var powerList: List<Int?>

        //Reset ChekcBox
        arrayOf(
            binding.rbAnt1, binding.rbAnt2, binding.rbAnt3, binding.rbAnt4,
            binding.rbAnt5, binding.rbAnt6, binding.rbAnt7, binding.rbAnt8
        ).forEachIndexed { index, rb ->
            rb.isChecked = false
            rb.isEnabled = true
        }
        connectedAnts.clear()
        for (bean in currentAntennaArray) {
            when (bean) {
                1 -> {
                    binding.rbAnt1.isChecked = true
                    binding.rbAnt1.isEnabled = true
                    connectedAnts.add(1)
                }

                2 -> {
                    binding.rbAnt2.isChecked = true
                    binding.rbAnt2.isEnabled = true
                    connectedAnts.add(2)
                }

                3 -> {
                    binding.rbAnt3.isChecked = true
                    binding.rbAnt3.isEnabled = true
                    connectedAnts.add(3)
                }

                4 -> {
                    binding.rbAnt4.isChecked = true
                    binding.rbAnt4.isEnabled = true
                    connectedAnts.add(4)
                }

                5 -> {
                    binding.rbAnt5.isChecked = true
                    binding.rbAnt5.isEnabled = true
                    connectedAnts.add(5)
                }

                6 -> {
                    binding.rbAnt6.isChecked = true
                    binding.rbAnt6.isEnabled = true
                    connectedAnts.add(6)
                }

                7 -> {
                    binding.rbAnt7.isChecked = true
                    binding.rbAnt7.isEnabled = true
                    connectedAnts.add(7)
                }

                8 -> {
                    binding.rbAnt8.isChecked = true
                    binding.rbAnt8.isEnabled = true
                    connectedAnts.add(8)
                }
            }

        }
        // Fetch power asynchronously so it doesn't block RFID reading
        CoroutineScope(Dispatchers.IO).launch {
            getAntsPower(connectedAnts)
        }
        power.observe(this) { integerIntegerMap ->
            powerList = integerIntegerMap.values.toList()
            binding.power.text = powerList.joinToString(", ")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHandler()
        stopInventory()
        tagDetails.clear()
    }

    override fun onPause() {
        super.onPause()
        stopHandler()
        stopInventory()
    }
    override fun onBackPressed() {
        super.onBackPressed()
        stopHandler()
        stopInventory()
        tagDetails.clear()
    }
    fun isRFIDActive(): Boolean {
        return isRfidReadingIsInProgress
    }
    private fun getAuthorizeData(deviceID : String){
        val requestBody = AuthRequest(ClientDeviceID = deviceID)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit : Retrofit = Retrofit.Builder()
            .baseUrl(sharedPreferencesUtils.getURL().toString())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        val baseUrl = sharedPreferencesUtils.getURL().toString()
        Log.e("API_DEBUG", "Base URL: $baseUrl")
        val apiService : APIService = retrofit.create(APIService::class.java)
        apiService.getAuthorization(requestBody).enqueue(object : Callback<APIResponse<dataModels.AuthRes>> {
            @SuppressLint("SuspiciousIndentation")
            override fun onResponse(
                call: Call<APIResponse<dataModels.AuthRes>>,
                response: Response<APIResponse<dataModels.AuthRes>>
            ) {

                if (response.isSuccessful){

                    val result = response.body()
                    Log.e("API_RESPONSE", "Success Body: ${Gson().toJson(result)}")
                    if (result != null) {
                        if(result.status){
                            val pairedDeviceID = result.data?.PairedDeviceID
                            sharedPreferencesUtils.setPairedDeviceID(pairedDeviceID.toString())

                            authResData = result.data ?: return
                                Log.e("DATA", "PairedDeviceID: ${authResData!!.PairedDeviceID}")
                            authResData!!.Topic.forEach { topic ->
                                    Log.e("DATA", "Title: ${topic.Title}, TopicName: ${topic.TopicName}")
                                }
                        }
                        else{
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else{
                    Log.e("HTTP_ERROR", "HTTP Error Code: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Unknown HTTP Error:" + response.errorBody()?.string(), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<APIResponse<dataModels.AuthRes>>, t: Throwable) {
                when (t) {
                    is SocketTimeoutException -> {
                        Toast.makeText(context, "NETWORK_ERROR: Request Timeout! Server took too long to respond.", Toast.LENGTH_SHORT).show()
                    }
                    is UnknownHostException -> {
                        Toast.makeText(context, "NETWORK_ERROR: No internet connection.", Toast.LENGTH_SHORT).show()
                    }
                    is ConnectException -> {
                        Toast.makeText(context, "NETWORK_ERROR: Failed to connect to the server.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.e("NETWORK_ERROR", "Network Failure: ${t.localizedMessage}")
                        Toast.makeText(context, "Network Failure: ${t.localizedMessage}.", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        })
    }
    private fun sendTagData(tagData : MutableList<TagBean>, topicName : String){
        mqttPub = MqttPublisher()
        val jsonObject = JsonObject()
        jsonObject.addProperty("PubDeviceID", sharedPreferencesUtils.getDeviceID())
        jsonObject.addProperty("SubDeviceID", authResData?.PairedDeviceID ?: "")
        val jsonArray = JsonArray()
        if (tagData.isNotEmpty()) {
            tagDetails.forEach{ data ->
                val tags = JsonObject().apply {
                    addProperty("TagID", data.epcId)
                    addProperty("RSSI", data.rssi)
                    addProperty("AntennaID", data.antenna)
                }
                jsonArray.add(tags)
            }
            jsonObject.add("TagDetails", jsonArray)
        }
        Log.e("JSON", jsonObject.toString())
        Log.e("Topic1", topicName)
        if(mqttConnection.isConnected()){
            mqttPub.publishMessage(mqttConnection, topicName,jsonObject.toString(),object : MqttResponseCallback {
                override fun onPublishSuccess() {
                    Log.e("Success", "Published SuccesFully")
                    tagDetails.clear()
                    totalCounts.set(0)
                }

                override fun onPublishFailure(error: String) {
                    Log.e("Failure", error)
                }

            })
        } else{
            mqttConnect()
            mqttPub.publishMessage(mqttConnection, topicName,jsonObject.toString(),object : MqttResponseCallback {
                override fun onPublishSuccess() {
                    Log.e("Success", "Published SuccesFully")
                    tagDetails.clear()
                    totalCounts.set(0)
                }

                override fun onPublishFailure(error: String) {
                    Log.e("Failure", error)
                }

            })
        }
//        mqttPub.connectAndPublish("http://192.168.0.172/WMS31/",topicName, jsonObject.toString(), "Reader", object : MqttResponseCallback {
//            override fun onPublishSuccess() {
//                Log.e("Success", "Published SuccesFully")
//                tagDetails.clear()
//                totalCounts.set(0)
//            }
//
//            override fun onPublishFailure(error: String) {
//                Log.e("Failure", error)
//            }
//
//        })
    }
    private fun sendConfigData(topicName : String){
        mqttPub = MqttPublisher()
        var antennas = connectedAnts.joinToString(",")
        val jsonObject = JsonObject()
        jsonObject.addProperty("PubDeviceID", sharedPreferencesUtils.getDeviceID())
        jsonObject.addProperty("SubDeviceID", authResData?.PairedDeviceID ?: "")
        jsonObject.addProperty("Power", binding.power.text.toString())
        jsonObject.addProperty("ReaderStatus", true)
        jsonObject.addProperty("AntennaID", antennas)

        Log.e("JSON", jsonObject.toString())
        Log.e("Topic1", topicName)
        if(mqttConnection.isConnected()){
            mqttPub.publishMessage(mqttConnection,topicName, jsonObject.toString(), object : MqttResponseCallback{
                override fun onPublishSuccess() {
                    Log.e("Success", "Published SuccesFully")
                    antennas = ""
                }

                override fun onPublishFailure(error: String) {
                    Log.e("Failure", error)
                }
            })
        }
        else{
            mqttConnect()
            mqttPub.publishMessage(mqttConnection,topicName, jsonObject.toString(), object : MqttResponseCallback{
                override fun onPublishSuccess() {
                    Log.e("Success", "Published SuccesFully")
                    antennas = ""
                }

                override fun onPublishFailure(error: String) {
                    Log.e("Failure", error)
                }
            })
        }
//        mqttPub.connectAndPublish("http://192.168.0.172/WMS31/",topicName, jsonObject.toString(), "Reader",object : MqttResponseCallback{
//                override fun onPublishSuccess() {
//                    Log.e("Success", "Published SuccesFully")
//                    antennas = ""
//                }
//
//                override fun onPublishFailure(error: String) {
//                    Log.e("Failure", error)
//                }
//            })
    }
    private fun mqttConnect(){
        mqttConnection.connect("http://192.168.0.172/WMS31/", "Reader", object : MqttConnectionCallBack {
            override fun onSuccess() {
                Log.e("MQTTConn", "MQTT connected Succesfully")
                Toast.makeText(context, "MQTT connected Succesfully", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(errorMessage: String) {
                Log.e("MQTTConn", errorMessage)

            }
        })
    }
}