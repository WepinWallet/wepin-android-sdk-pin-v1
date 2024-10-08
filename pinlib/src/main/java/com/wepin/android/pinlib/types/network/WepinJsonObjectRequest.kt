package com.wepin.android.pinlib.types.network

import com.android.volley.Response
import org.json.JSONObject

internal class WepinJsonObjectRequest<T>(
    method: Int,
    url: String,
    jsonRequest: JSONObject?,
    private val clazz: Class<T>,
    private val addHeader: Map<String, String>?,
    listener: Response.Listener<T>,
    errorListener: Response.ErrorListener,
    )  :
    NetworkJsonObjectRequest<T>(method, url, jsonRequest, clazz, listener, errorListener) {

    companion object{
        private var _appDomain:String? = null
        private var _appKey:String? = null
        private var _version:String? = null
        internal fun setHeaderInfo (appDomain:String, appKey:String, version:String){
            this._appDomain = appDomain
            this._appKey = appKey
            this._version = version
        }

    }
    override fun getHeaders(): Map<String, String> {
        val headers = HashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["X-API-KEY"] = _appKey!!
        headers["X-API-DOMAIN"] = _appDomain!!
        headers["X-SDK-TYPE"] = "android-login"
        headers["X-SDK-VERSION"] = _version!!
        if(addHeader != null){
            headers += addHeader
        }
        return headers
    }

    override fun getBodyContentType(): String {
        return "application/json; charset=utf-8"
    }
}