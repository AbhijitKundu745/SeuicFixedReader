package com.psl.seuicfixedreader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.google.gson.Gson
import com.psl.seuicfixedreader.APIHelpers.APIResponse
import com.psl.seuicfixedreader.APIHelpers.APIService
import com.psl.seuicfixedreader.APIHelpers.AuthRequest
import com.psl.seuicfixedreader.APIHelpers.dataModels
import com.psl.seuicfixedreader.databinding.ActivityUrlconfigBinding
import com.psl.seuicfixedreader.helper.ConnectionManager
import com.psl.seuicfixedreader.helper.SharedPreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.util.concurrent.TimeUnit

class URLConfigActivity : AppCompatActivity(), ConnectionManager.ConnectionListener {
    private var context : Context = this
    private lateinit var binding : ActivityUrlconfigBinding
    private lateinit var sharedPreferencesUtils : SharedPreferencesUtils
    private var deviceId : String= ""
    private lateinit var cd : ConnectionManager
    private var host_config : Boolean = false
    private lateinit var HOST_URL : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = DataBindingUtil.setContentView(this, R.layout.activity_urlconfig)
        cd = ConnectionManager(this, this)
        cd.registerNetworkCallback()
        sharedPreferencesUtils = SharedPreferencesUtils(context)
        deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        deviceId = deviceId.uppercase()
        Log.e("DeviceID", deviceId)
        binding.deviceID.text = "DeviceID : "+ deviceId
        sharedPreferencesUtils.setDeviceID(deviceId)

        HOST_URL = sharedPreferencesUtils.getURL().toString()
        host_config = sharedPreferencesUtils.getIsHostConfig()

        if (host_config) {
            binding.URL.setText(HOST_URL)
            binding.showURL.text = HOST_URL
        } else {
            binding.URL.setText(sharedPreferencesUtils.getURL())
            binding.showURL.text = sharedPreferencesUtils.getURL()
        }
        binding.ClearButton.setOnClickListener{
            binding.URL.setText("")
        }

        binding.URLButton.setOnClickListener{
            if (binding.URL.text.isNotEmpty() || binding.URL.text.toString().length > 8) {
                if(cd.isConnectedToWiFi()){
                    val url = binding.URL.text.toString().trim()
                    try{
                        GetAccessToken(url)
                    }
                    catch (ex : Exception){
                        Log.e("ConfigException", ex.toString())
                    }
                }
                else {
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Enter valid URL", Toast.LENGTH_SHORT).show()
            }
        }
        binding.NextButton.setOnClickListener{
            if(sharedPreferencesUtils.getIsHostConfig()){
                if(cd.isConnectedToWiFi()){
                    try{
                        getAuthorizeData(deviceId)
                    }
                    catch (ex : Exception){
                        Log.e("AuthException", ex.toString())
                    }
                }
                else {
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(this, "Please set and config URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun GetAccessToken(url : String){
        val requestBody = AuthRequest(ClientDeviceID = "")

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit : Retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        Log.e("API_DEBUG", "Base URL: $url")

        val apiService : APIService = retrofit.create(APIService::class.java)
        apiService.getAuthorization(requestBody).enqueue(object :
            Callback<APIResponse<dataModels.AuthRes>> {
            @SuppressLint("SuspiciousIndentation")
            override fun onResponse(
                call: Call<APIResponse<dataModels.AuthRes>>,
                response: Response<APIResponse<dataModels.AuthRes>>
            ) {

                if (response.isSuccessful){

                    val result = response.body()
                    Log.e("API_RESPONSE", "Success Body: ${Gson().toJson(result)}")
                    if (result != null) {
                        sharedPreferencesUtils.setIsHostConfig(true)
                        sharedPreferencesUtils.setURL(url)
                        HOST_URL = url
                        Toast.makeText(context, "Server URL configured successfully", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        sharedPreferencesUtils.setIsHostConfig(false)
                        HOST_URL = ""
                        Toast.makeText(context, "Network Error: Failed to connect to the server", Toast.LENGTH_SHORT).show()
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
    private fun getAuthorizeData(deviceID : String){
        val requestBody = AuthRequest(ClientDeviceID = deviceID)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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

                            val topicList1 = HashMap<String, String>()

                            result.data?.Topic?.forEach{ topic ->
                                topicList1[topic.Title] = topic.TopicName
                            }
                            Log.e("DATA", "Title: ${topicList1}")

                            val WOIntent = Intent(context, TagReadingActivity::class.java).apply {
                                putExtra("TOPIC_LIST", topicList1)
                            }
                            startActivity(WOIntent)
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
    override fun onNetworkChanged(isConnected: Boolean) {
        Log.e("S", isConnected.toString())
    }

    override fun onDestroy() {
        cd.unregisterNetworkCallback()
        super.onDestroy()
    }

    override fun onBackPressed() {
        cd.unregisterNetworkCallback()
        super.onBackPressed()
    }
}