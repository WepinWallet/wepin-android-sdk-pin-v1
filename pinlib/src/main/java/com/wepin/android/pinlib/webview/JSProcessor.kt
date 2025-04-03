package com.wepin.android.pinlib.webview

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.JSResponse
import com.wepin.android.pinlib.manager.WepinPinManager
import com.wepin.android.storage.WepinStorageManager
import com.wepin.android.storage.utils.convertJsonToLocalStorageData
import org.json.JSONObject

interface Command {
    companion object {
        /**
         * Commands for JS processor
         */
        const val CMD_READY_TO_WIDGET: String = "ready_to_widget"
        const val CMD_GET_SDK_REQUEST: String = "get_sdk_request"
        const val CMD_CLOSE_WEPIN_WIDGET: String = "close_wepin_widget"
        const val CMD_SET_LOCAL_STORAGE: String = "set_local_storage"

        /**
         * Commands for PINPAD
         */
        const val CMD_SUB_PIN_REGISTER: String = "pin_register"  // only for creating wallet
        const val CMD_SUB_PIN_AUTH: String = "pin_auth" //
        const val CMD_SUB_PIN_CHANGE: String = "pin_change"
        const val CMD_SUB_PIN_OTP: String = "pin_otp"
    }
}

object JSProcessor {
    private val TAG = this.javaClass.name

    fun processRequest(request: String, callback: (response: String) -> Any) {
        Log.d(TAG, "processRequest : $request")
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
                    Log.d(TAG, "CMD_READY_TO_WIDGET")
                    val appKey = WepinPinManager.getInstance().appKey
                    val appId = WepinPinManager.getInstance().appId
                    val domain = WepinPinManager.getInstance().packageName
                    val platform = 2  // android sdk platform number
                    val type = WepinPinManager.getInstance().sdkType
                    val version = WepinPinManager.getInstance().version
                    val attributes = WepinPinManager.getInstance().wepinAttributes
                    var storageData = WepinStorageManager.getAllStorage()
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
                            version = version,
                            localData = storageData ?: {},
                            attributes = attributes!!
                        ).build()
                }

                Command.CMD_GET_SDK_REQUEST -> {
                    Log.d(TAG, "CMD_GET_SDK_REQUEST")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        headerObject.getString("request_from"),
                        command
                    )
                        .build()
                    jsResponse.body.data =
                        WepinPinManager.getInstance().getCurrentWepinRequest() ?: "No request"

                }

                Command.CMD_SET_LOCAL_STORAGE -> {
                    Log.d(TAG, "CMD_SET_LOCAL_STORAGE")
                    try {
                        val data = bodyObject.getJSONObject("parameter").getJSONObject("data")

                        val storageDataMap = mutableMapOf<String, Any>()

                        data.keys().forEach { key ->
                            val storageValue = when (val value = data.get(key)) {
                                is JSONObject -> {
                                    val jsonString = value.toString()
                                    convertJsonToLocalStorageData(jsonString)
                                }
                                //is String -> StorageDataType.StringValue(value)
                                is String -> value
                                is Boolean -> value
                                else -> value //throw IllegalArgumentException("Unsupported data type for key: $key")
                            }
                            storageDataMap[key] = storageValue
                        }

                        WepinStorageManager.setAllStorage(storageDataMap)

                        if (storageDataMap["user_info"] != null && WepinPinManager.getInstance()
                                .getResponseWepinUserDeferred() != null
                        ) {
                            WepinPinManager.getInstance().getResponseWepinUserDeferred()
                                ?.complete(true)
                        }
                        jsResponse = JSResponse.Builder(
                            headerObject.getString("id"),
                            headerObject.getString("request_from"),
                            command
                        ).build()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing JSON data: ${e.message}")
                        throw WepinError.generalUnKnownEx(e.message)
                    }
                }

                Command.CMD_SUB_PIN_REGISTER,
                Command.CMD_SUB_PIN_AUTH,
                Command.CMD_SUB_PIN_CHANGE,
                Command.CMD_SUB_PIN_OTP -> {
                    Log.d(TAG, "CMD_SUB_PIN_REGISTER")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        "wepin_widget",
                        command
                    ).build()
                    WepinPinManager.getInstance().getCurrentWepinRequest()
                    WepinPinManager.getInstance().getResponseDeferred()!!.complete(request)
                }

                Command.CMD_CLOSE_WEPIN_WIDGET -> {
                    Log.d(TAG, "CMD_CLOSE_WEPIN_WIDGET")
                    jsResponse = null
                    WepinPinManager.getInstance().closeWebview()
                }
            }
            if (jsResponse == null) {
                Log.d(TAG, "JSProcessor Response is null")
                return
            }

            val response = objectMapper.writeValueAsString(jsResponse)
            Log.d(TAG, "JSProcessor Response : $response")

            // JSInterface의 onResponse 메서드를 통해 JavaScript로 응답 전송
            callback(response)

        } catch (e: Exception) {
            e.printStackTrace()
            throw WepinError.generalUnKnownEx(e.message)
        }
    }
}