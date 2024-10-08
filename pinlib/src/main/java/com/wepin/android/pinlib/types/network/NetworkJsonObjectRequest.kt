package com.wepin.android.pinlib.types.network

import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import org.json.JSONObject

internal open class NetworkJsonObjectRequest<T>(
    method: Int,
    url: String,
    jsonRequest: JSONObject?,
    private val clazz: Class<T>,
    listener: Response.Listener<T>,
    errorListener: Response.ErrorListener):
    JsonObjectRequest(method, url, jsonRequest, Response.Listener{ response ->
        try {
            if(clazz == JSONObject::class.java) {
                listener.onResponse(response as T)
            }else {
                val gson = Gson()
                val parsedResponse = gson.fromJson(response.toString(), clazz)
                listener.onResponse(parsedResponse)
            }
        }catch (error: Exception){
            errorListener.onErrorResponse(VolleyError("gson error"))
        }

    }, errorListener) {

    override fun getHeaders(): Map<String, String> {
        val headers = HashMap<String, String>()
        headers["Content-Type"] = "application/json"
        return headers
    }

    override fun getBodyContentType(): String {
        return "application/json; charset=utf-8"
    }
}