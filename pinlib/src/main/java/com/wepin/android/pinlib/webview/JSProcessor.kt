package com.wepin.android.pinlib.webview

import com.fasterxml.jackson.databind.ObjectMapper
import com.wepin.android.pinlib.error.WepinError
import com.wepin.android.pinlib.manager.WepinPinManager
import com.wepin.android.pinlib.storage.StorageManager
import com.wepin.android.pinlib.types.Command
import com.wepin.android.pinlib.utils.Log
import com.wepin.android.pinlib.utils.convertJsonToLocalStorageData
import org.json.JSONObject

class JSProcessor {
    private val TAG = this.javaClass.name

    fun processRequest(request: String, jsInterface: WepinPinManager.JSInterface) {
        Log.d(TAG,"processRequest : $request")
        try {
            val objectMapper = ObjectMapper()
            // 메시지를 JSONObject로 변환
            val jsonObject = JSONObject(request)
            val headerObject = jsonObject.getJSONObject("header")
            // "body" 객체를 가져옴
            val bodyObject = jsonObject.getJSONObject("body")

            // "command" 값을 가져옴

            val command = bodyObject.getString("command")
            var jsResponse: JSResponse? = null

            when (command) {
                Command.CMD_READY_TO_WIDGET -> {
                    Log.d(TAG,"CMD_READY_TO_WIDGET")
                    val appKey = WepinPinManager.getInstance().getAppKey()
                    val appId = WepinPinManager.getInstance().getAppId()
                    val domain = WepinPinManager.getInstance().getPackageName()
                    val platform = 2  // android sdk platform number
                    val type = "android-pin"
                    val version = WepinPinManager.getInstance().getVersion()
                    val attributes = WepinPinManager.getInstance().getWepinAttributes()
                    var storageData = StorageManager.getAllStorage()
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        headerObject.getString("request_from"),
                        command
                    )
                        .setReadyToWidgetData(
                            appKey = appKey!!,
                            appId = appId!!,
                            domain = domain!!,
                            platform = platform,
                            type = type,
                            version = version!!,
                            localData = storageData,
                            attributes = attributes
                        ).build()
                }

                Command.CMD_GET_SDK_REQUEST -> {
                    Log.d(TAG,"CMD_GET_SDK_REQUEST")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        headerObject.getString("request_from"),
                        command
                    )
                        .build()
                    jsResponse.body.data =
                        WepinPinManager.getInstance().getCurrentWepinRequest()?: "No request"

                }

                Command.CMD_SET_LOCAL_STORAGE -> {
                    Log.d(TAG,"CMD_SET_LOCAL_STORAGE")
                    try {
                        val data = bodyObject.getJSONObject("parameter").getJSONObject("data")

                        val storageDataMap = mutableMapOf<String, Any>()

                        data.keys().forEach { key ->
                            val value = data.get(key)
                            val storageValue  = when (value) {
                                is JSONObject -> {
                                    val jsonString = value.toString()
                                    convertJsonToLocalStorageData(jsonString)
                                }
                                //is String -> StorageDataType.StringValue(value)
                                is String -> value
                                else -> throw IllegalArgumentException("Unsupported data type for key: $key")
                            }
                            storageDataMap[key] = storageValue
                        }

                        StorageManager.setAllStorage(storageDataMap)
                        jsResponse = JSResponse.Builder(headerObject.getString("id"),
                            headerObject.getString("request_from"),
                            command).build()
                    } catch (e: Exception) {
                        Log.e(TAG,"Error processing JSON data: ${e.message}")
                        throw WepinError.generalUnKnownEx(e.message)
                    }
                }

                Command.CMD_CLOSE_WEPIN_WIDGET -> {
                    Log.d(TAG,"CMD_CLOSE_WEPIN_WIDGET")
                    jsResponse = null
//                    jsResponse = JSResponse.Builder(
//                        headerObject.getString("id"),
//                        headerObject.getString("request_from"),
//                        command
//                    ).build()
                    WepinPinManager.getInstance().finalizeWebivew()

                }
                // CMD_GET_SDK_REQUEST 에 요청했던 command에 대한 웹뷰 응답처리
                Command.CMD_SUB_PIN_REGISTER -> {
                    Log.d(TAG,"CMD_SUB_PIN_REGISTER")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        //headerObject.getString("request_from"),
                        "wepin_widget",
                        command
                    ).build()
                    WepinPinManager.getInstance()?.getCurrentDeffered()!!.complete(request)
                }
                Command.CMD_SUB_PIN_AUTH -> {
                    Log.d(TAG,"CMD_SUB_PIN_AUTH")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        //headerObject.getString("request_from"),
                        "wepin_widget",
                        command
                    ).build()
                    WepinPinManager.getInstance()?.getCurrentDeffered()!!.complete(request)
                }
                Command.CMD_SUB_PIN_CHANGE -> {
                    Log.d(TAG,"CMD_SUB_PIN_CHANGE")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        "wepin_widget",
                        command
                    ).build()
                    WepinPinManager.getInstance()?.getCurrentDeffered()!!.complete(request)
                }
                Command.CMD_SUB_PIN_OTP -> {
                    Log.d(TAG,"CMD_SUB_PIN_OTP")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        "wepin_widget",
                        command
                    ).build()
                    WepinPinManager.getInstance()?.getCurrentDeffered()!!.complete(request)
                }
            }
            if (jsResponse == null) {
                Log.d(TAG, "JSProcessor Response is null")
                return
            }

            val response = objectMapper.writeValueAsString(jsResponse)
            Log.d(TAG,"JSProcessor Response : $response")

            // JSInterface의 onResponse 메서드를 통해 JavaScript로 응답 전송
            jsInterface.onResponse(response)

        } catch (e: Exception) {
            e.printStackTrace()
            throw WepinError.generalUnKnownEx(e.message)
        }
    }
}
