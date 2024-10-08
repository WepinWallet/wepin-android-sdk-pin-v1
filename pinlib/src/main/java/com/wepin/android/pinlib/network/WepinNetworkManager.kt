package com.wepin.android.pinlib.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.wepin.android.pinlib.error.WepinError
import com.wepin.android.pinlib.types.KeyType
import com.wepin.android.pinlib.types.network.GetAccessTokenResponse
import com.wepin.android.pinlib.types.network.WepinJsonObjectRequest
import com.wepin.android.pinlib.types.network.WepinStringObjectRequest
import com.wepin.android.pinlib.utils.Log
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class WepinNetworkManager(context: Context, appKey:String, domain:String, version: String) {
    private val TAG = this.javaClass.name
    internal var wepinBaseUrl: String? = null
    private var appKey: String? = null
    private var domain: String? = null
    private var version: String? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var executorService: ExecutorService? = null
    private var _appContext: Context? = null
    private var requestQueue: RequestQueue
    
    init {
        wepinBaseUrl = getSdkUrl(appKey)
        this.appKey = appKey
        this.domain = domain
        this.version = version
        this._appContext = context
        WepinJsonObjectRequest.setHeaderInfo(domain, appKey, version)
        WepinStringObjectRequest.setHeaderInfo(domain, appKey, version)
        executorService = Executors.newSingleThreadExecutor();
        requestQueue = Volley.newRequestQueue(context)
    }
    companion object {
        @SuppressLint("ObsoleteSdkInt")
        fun isInternetAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        }
        internal fun getErrorMessage(volleyError: VolleyError) : String? {
            val networkResponse = volleyError.networkResponse
            if (networkResponse != null) {
                val statusCode = networkResponse.statusCode
                val errorMessage = String(networkResponse.data)
                Log.e("volleyError","Status Code: $statusCode, Error Message: $errorMessage")
                return "Status Code: $statusCode, Error Message: $errorMessage"
            }
            return null
        }
    }

    internal fun setAuthToken(accessToken:String, refreshToken:String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    internal fun clearAuthToken(){
        this.accessToken = null
        this.refreshToken = null
    }

    fun getAppInfo(): CompletableFuture<Boolean> {
        Log.i(TAG, "getAppInfo")
        val future: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        executorService?.submit {
            val url = wepinBaseUrl + "app/info"
            val jsonObjectRequest = WepinJsonObjectRequest<JSONObject>(
                Request.Method.GET, url, null,
                JSONObject::class.java,
                null,
                { response ->
                    Log.d(TAG,"GET getAppInfo: $response")
                    if (response != null) {
                        future.complete(true)
                    } else {
                        future.complete(false)
                    }
                },
                { error ->
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    fun getAccessToken(userId: String): CompletableFuture<String>{
        Log.i(TAG, "getAccessToken")
        val future: CompletableFuture<String> = CompletableFuture<String>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/access-token?userId=${userId}&refresh_token=${this.refreshToken}"
            val headers = HashMap<String, String>()
            headers["Authorization"] = "Bearer $accessToken"
            val jsonObjectRequest = WepinJsonObjectRequest<GetAccessTokenResponse>(
                Request.Method.GET, url, null,
                GetAccessTokenResponse::class.java,
                headers,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG,"GET getAccessToken: $response")
                    if(response != null) {
                        setAuthToken(response.token, this.refreshToken!!)
                        future.complete(response.token)
                    }else {
                        future.completeExceptionally(WepinError.INVALID_TOKEN)
                    }
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if(message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future;
    }

    private fun getSdkUrl(apiKey:String): String {
        Log.i(TAG, "getSdkUrl")
        return when (KeyType.fromAppKey(apiKey)) {
            KeyType.DEV -> {
                "https://dev-sdk.wepin.io/v1/"
            }

            KeyType.STAGE-> {
                "https://stage-sdk.wepin.io/v1/"
            }

            KeyType.PROD -> {
                "https://sdk.wepin.io/v1/"
            }

            else -> {
                throw WepinError.INVALID_APP_KEY
            }
        }
    }
}