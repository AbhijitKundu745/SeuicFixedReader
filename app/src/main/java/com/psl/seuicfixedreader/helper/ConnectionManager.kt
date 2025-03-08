    package com.psl.seuicfixedreader.helper

    import android.app.Application
    import android.content.Context
    import android.net.ConnectivityManager
    import android.net.Network
    import android.net.NetworkCapabilities
    import android.net.NetworkRequest
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import androidx.lifecycle.LiveData
    import androidx.lifecycle.MutableLiveData

    class ConnectionManager(application: Application) : MutableLiveData<Boolean>() {

        private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        private val handler = Handler(Looper.getMainLooper()) // UI Thread Handler
        private var isConnected = false

        private val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected = true
                postValue(true) // Notify observers that internet is available
                Log.e("Network", "Internet Available")
            }

            override fun onLost(network: Network) {
                isConnected = false
                postValue(false) // Notify observers that internet is lost
                Log.e("Network", "Internet Lost")
                retryInternetConnection() // Start retrying until internet is back
            }
        }

        init {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }

        private fun retryInternetConnection() {
            handler.postDelayed({
                if (!isConnected) {
                    Log.e("Network", "Retrying Internet Connection...")
                    val activeNetwork = connectivityManager.activeNetwork
                    if (activeNetwork != null) {
                        isConnected = true
                        postValue(true) // Notify observers that internet is restored
                        Log.e("Network", "Internet Reconnected")
                    } else {
                        retryInternetConnection() // Keep retrying if still no internet
                    }
                }
            }, 2000) // Retry every 5 seconds
        }

        companion object {
            @Volatile
            private var instance: ConnectionManager? = null

            fun getInstance(application: Application) =
                instance ?: synchronized(this) {
                    instance ?: ConnectionManager(application).also { instance = it }
                }
        }
    }
