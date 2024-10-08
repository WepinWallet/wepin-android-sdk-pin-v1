package com.wepin.android.pinlib

import WepinPinWebviewDialog
import android.app.Activity
import android.content.Context
import com.wepin.android.pinlib.error.WepinError
import com.wepin.android.pinlib.manager.WepinPinManager
import com.wepin.android.pinlib.network.WepinNetworkManager
import com.wepin.android.pinlib.storage.StorageManager
import com.wepin.android.pinlib.types.AuthOTP
import com.wepin.android.pinlib.types.AuthPinBlock
import com.wepin.android.pinlib.types.ChangePinBlock
import com.wepin.android.pinlib.types.Command
import com.wepin.android.pinlib.types.EncPinHint
import com.wepin.android.pinlib.types.EncUVD
import com.wepin.android.pinlib.types.RegistrationPinBlock
import com.wepin.android.pinlib.types.StorageDataType
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.types.WepinPinParams
import com.wepin.android.pinlib.utils.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class WepinPin(wepinPinParams: WepinPinParams) {

    private val TAG = this.javaClass.name
    private var _appContext: Context? = wepinPinParams.context
    private var _isInitialized: Boolean = false
    private var _appId: String? = wepinPinParams.appId
    private var _appKey: String? = wepinPinParams.appKey
    private var _wepinPinManager: WepinPinManager = WepinPinManager.getInstance()
    private var _attributes: WepinPinAttributes? = null
    private var _wepinWebviewDialog: WepinPinWebviewDialog? = null
    private var _wepinNetworkManager: WepinNetworkManager? = null

    fun initialize(attributes: WepinPinAttributes? = null): CompletableFuture<Boolean> {
        Log.i(TAG, "init")
        val wepinCompletableFuture = CompletableFuture<Boolean>()

        if (_isInitialized) {
            wepinCompletableFuture.completeExceptionally(WepinError.ALREADY_INITIALIZED_ERROR)
            return wepinCompletableFuture
        }

        try {
            _attributes = attributes
            StorageManager.init(_appContext as Activity, _appId!!)

            // Wepin PinManager 초기화
            _wepinPinManager.init(_appContext!!, _appKey!!, _appId!!, _attributes)
            _wepinNetworkManager = _wepinPinManager.wepinNewtorkManager

            _wepinNetworkManager?.getAppInfo()
                ?.thenApply { infoResponse ->
                    Log.d(TAG, "infoResponse $infoResponse")

                    if (!infoResponse) {
                        _isInitialized = false
                        wepinCompletableFuture.complete(false)
                        return@thenApply false
                    }

                    checkExistWepinLoginSession().thenApply {}

                    val activity = _appContext as? Activity ?: run {
                        _isInitialized = false
                        wepinCompletableFuture.completeExceptionally(WepinError.NOT_ACTIVITY)
                        return@thenApply false
                    }

                    _wepinWebviewDialog = WepinPinWebviewDialog(activity, _wepinPinManager.getWebView())

                    _isInitialized = true
                    wepinCompletableFuture.complete(true)
                    true
                }?.exceptionally { throwable ->
                    _isInitialized = false
                    wepinCompletableFuture.completeExceptionally(throwable)
                    null
                }
        } catch (e: Exception) {
            _isInitialized = false
            e.printStackTrace()
            wepinCompletableFuture.completeExceptionally(e)
        }

        return wepinCompletableFuture
    }

    fun isInitialized(): Boolean {
        Log.i(TAG, "isInitialized")
        return _isInitialized
    }

    fun changeLanguage(language: String): CompletableFuture<Boolean> {
        Log.i(TAG, "changeLanguage")
        val completableFuture = CompletableFuture<Boolean>()
        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }
        _attributes?.defaultLanguage = language
        completableFuture.complete(true)
        return completableFuture
    }

    fun generateRegistrationPINBlock(): CompletableFuture<RegistrationPinBlock?> {
        Log.i(TAG, "generateRegistrationPINBlock")
        val completableFuture = CompletableFuture<RegistrationPinBlock?>()
        val subCommand: String = Command.CMD_SUB_PIN_REGISTER

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        checkExistWepinLoginSession()
            .thenCompose { sessionExists ->
                if (sessionExists) {
                    _wepinPinManager.openAndRequestWepinWidgetAsync(subCommand, null)
                } else {
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }
            .thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data = JSONObject(result).getJSONObject("body").getJSONObject("data")
                    val uvd = EncUVD.fromJson(data.getJSONObject("UVD").toMap())
                    val hint = EncPinHint.fromJson(data.getJSONObject("hint").toMap())
                    completableFuture.complete(RegistrationPinBlock(uvd = uvd, hint = hint))
                }
            }
            .exceptionally { e ->
                completableFuture.completeExceptionally(WepinError.generalUnKnownEx(e.message))
                null
            }

        return completableFuture
    }

    fun generateAuthPINBlock(): CompletableFuture<AuthPinBlock?> {
        Log.i(TAG, "generateAuthPINBlock")
        val completableFuture = CompletableFuture<AuthPinBlock?>()
        val subCommand: String = Command.CMD_SUB_PIN_AUTH

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        checkExistWepinLoginSession()
            .thenCompose { sessionExists ->
                if (sessionExists) {
                    val param = mapOf("count" to 1)
                    _wepinPinManager.openAndRequestWepinWidgetAsync(subCommand, param)
                } else {
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }
            .thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data = JSONObject(result).getJSONObject("body").getJSONObject("data").toMap()
                    val authPinBlock = AuthPinBlock.fromJson(data)
                    completableFuture.complete(authPinBlock)
                }
            }
            .exceptionally { e ->
                completableFuture.completeExceptionally(WepinError.generalUnKnownEx(e.message))
                null
            }

        return completableFuture
    }

    fun generateChangePINBlock(): CompletableFuture<ChangePinBlock> {
        Log.i(TAG, "generateChangePINBlock")
        val completableFuture = CompletableFuture<ChangePinBlock>()
        val subCommand: String = Command.CMD_SUB_PIN_CHANGE

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        checkExistWepinLoginSession()
            .thenCompose { sessionExists ->
                if (sessionExists) {
                    _wepinPinManager.openAndRequestWepinWidgetAsync(subCommand, null)
                } else {
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }
            .thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data = JSONObject(result).getJSONObject("body").getJSONObject("data").toMap()
                    val changePinBlock = ChangePinBlock.fromJson(data)
                    completableFuture.complete(changePinBlock)
                }
            }
            .exceptionally { e ->
                completableFuture.completeExceptionally(WepinError.generalUnKnownEx(e.message))
                null
            }

        return completableFuture
    }

    fun generateAuthOTPCode(): CompletableFuture<AuthOTP> {
        Log.i(TAG, "generateAuthOTPCode")
        val completableFuture = CompletableFuture<AuthOTP>()
        val subCommand: String = Command.CMD_SUB_PIN_OTP

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        checkExistWepinLoginSession()
            .thenCompose { sessionExists ->
                if (sessionExists) {
                    _wepinPinManager.openAndRequestWepinWidgetAsync(subCommand, null)
                } else {
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }
            .thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data = JSONObject(result).getJSONObject("body").getJSONObject("data")
                    val otpCode = data.getString("code")
                    completableFuture.complete(AuthOTP(otpCode))
                }
            }
            .exceptionally { e ->
                completableFuture.completeExceptionally(WepinError.generalUnKnownEx(e.message))
                null
            }

        return completableFuture
    }

    fun finalize() {
        Log.i(TAG, "finalize")
        _isInitialized = false
    }


    private fun <T> handleJsonResult(result: String, subCommand: String, completableFuture: CompletableFuture<T>): Boolean {
        Log.i(TAG, "handleJsonResult")
        val jsonResult = JSONObject(result)
        val command = jsonResult.getJSONObject("body").getString("command")
        val state = jsonResult.getJSONObject("body").getString("state")

        return if (command == subCommand) {
            if (state == "SUCCESS") {
                true
            } else {
                val data = jsonResult.getJSONObject("body").getString("data")
                completableFuture.completeExceptionally(WepinError.generalUnKnownEx(data))
                false
            }
        } else {
            val errMsg = "Unexpected command: command=$command, expected=$subCommand"
            completableFuture.completeExceptionally(WepinError.generalUnKnownEx(errMsg))
            false
        }
    }

    private fun checkExistWepinLoginSession(): CompletableFuture<Boolean> {
        Log.i(TAG, "checkExistWepinLoginSession")
        val wepinCompletableFuture = CompletableFuture<Boolean>()
        val token = StorageManager.getStorage("wepin:connectUser")
        val userId = StorageManager.getStorage("user_id")

        if (token != null && userId != null) {
            val wepinToken = token as StorageDataType.WepinToken
            _wepinNetworkManager?.setAuthToken(wepinToken.accessToken, wepinToken.refreshToken)
            _wepinNetworkManager?.getAccessToken(userId as String)
                ?.thenApply { response ->
                    StorageManager.setStorage(
                        "wepin:connectUser",
                        StorageDataType.WepinToken(
                            accessToken = response,
                            refreshToken = wepinToken.refreshToken
                        )
                    )
                    _wepinNetworkManager?.setAuthToken(response, wepinToken.refreshToken)
                    wepinCompletableFuture.complete(true)
                }?.exceptionally {
                    _wepinNetworkManager?.clearAuthToken()
                    wepinCompletableFuture.complete(false)
                }
        } else {
            _wepinNetworkManager?.clearAuthToken()
            wepinCompletableFuture.complete(false)
        }

        return wepinCompletableFuture
    }

    // Helper functions to convert JSONObject and JSONArray to Map and List respectively
    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = this.get(key)
            map[key] = when (value) {
                is JSONArray -> value.toList()
                is JSONObject -> value.toMap()
                else -> value
            }
        }
        return map
    }

    private fun JSONArray.toList(): List<Any?> {
        val list = mutableListOf<Any?>()
        for (i in 0 until this.length()) {
            val value = this.get(i)
            list.add(
                when (value) {
                    is JSONArray -> value.toList()
                    is JSONObject -> value.toMap()
                    else -> value
                }
            )
        }
        return list
    }
}
