    package com.psl.seuicfixedreader.helper

    import android.content.Context
    import android.net.ConnectivityManager
    import android.net.Network
    import android.net.NetworkCapabilities
    import android.net.NetworkRequest


    class ConnectionManager(private val context: Context, private val listener: ConnectionListener) {

        private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        private val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isConnectedToWiFi()) {
                    listener.onNetworkChanged(true) // Notify when WiFi is connected
                }
            }

            override fun onLost(network: Network) {
                listener.onNetworkChanged(false) // Notify when WiFi is disconnected
            }
        }

        fun registerNetworkCallback() {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        fun unregisterNetworkCallback() {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }

        fun isConnectedToWiFi(): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }

        interface ConnectionListener {
            fun onNetworkChanged(isConnected: Boolean)
        }
    }