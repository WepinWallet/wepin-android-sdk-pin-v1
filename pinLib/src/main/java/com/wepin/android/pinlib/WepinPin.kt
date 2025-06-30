package com.wepin.android.pinlib

import android.content.Context
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.WepinAttribute
import com.wepin.android.commonlib.types.WepinLifeCycle
import com.wepin.android.core.utils.Log
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.pinlib.manager.WepinPinManager
import com.wepin.android.pinlib.types.AuthOTP
import com.wepin.android.pinlib.types.AuthPinBlock
import com.wepin.android.pinlib.types.ChangePinBlock
import com.wepin.android.pinlib.types.EncPinHint
import com.wepin.android.pinlib.types.EncUVD
import com.wepin.android.pinlib.types.RegistrationPinBlock
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.types.WepinPinParams
import com.wepin.android.pinlib.utils.handleJsonResult
import com.wepin.android.pinlib.webview.Command
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class WepinPin(wepinPinParams: WepinPinParams, private var platformType: String? = "android") {
    private val TAG = this.javaClass.name
    private var _appContext: Context? = wepinPinParams.context
    private var _appId: String = wepinPinParams.appId
    private var _appKey: String = wepinPinParams.appKey
    private var _isInitialized: Boolean = false
    private var _attributes: WepinAttribute? = null
    private lateinit var _wepinPinManager: WepinPinManager

    var login: WepinLogin? = null
        private set // 외부에서 변경 불가능하게 설정

    init {
        val wepinLoginOptions = WepinLoginOptions(
            context = _appContext!!,
            appId = wepinPinParams.appId,
            appKey = wepinPinParams.appKey,
        )
        login = WepinLogin(wepinLoginOptions, platformType)
    }

    fun initialize(attributes: WepinPinAttributes? = null): CompletableFuture<Boolean> {
        Log.i(TAG, "initialize")
        val wepinCompletableFuture = CompletableFuture<Boolean>()

        if (_isInitialized) {
            wepinCompletableFuture.completeExceptionally(WepinError.ALREADY_INITIALIZED_ERROR)
            return wepinCompletableFuture
        }
        _attributes = attributes

        _wepinPinManager = WepinPinManager.getInstance()
        _wepinPinManager.initialize(
            wepinPinParams = WepinPinParams(
                _appContext!!,
                _appId,
                _appKey
            ),
            attributes = attributes
        ).thenCompose {
            _wepinPinManager.wepinNetwork?.let { network ->
                login?.init()?.thenCompose {
                    _wepinPinManager.wepinSessionManager?.checkLoginStatusAndGetLifeCycle()
                        ?.thenApply {
                            _isInitialized = true
                            wepinCompletableFuture.complete(true)
                            true
                        } ?: run {
                        _isInitialized = true

                        wepinCompletableFuture.complete(true)
                        wepinCompletableFuture
                    }
                }?.exceptionally { error ->
                    _wepinPinManager.clear()
                    _isInitialized = false
                    wepinCompletableFuture.completeExceptionally(error)
                    null
                }
            }
        }?.exceptionally { error ->
            _isInitialized = false
            wepinCompletableFuture.completeExceptionally(error)
        }
        return wepinCompletableFuture
    }

    fun isInitialized(): Boolean {
        Log.i(TAG, "isInitialized")
        return _isInitialized
    }

    fun changeLanguage(language: String): CompletableFuture<Boolean> {
        Log.i(TAG, "changeLanguage")
        val completableFuture: CompletableFuture<Boolean> = CompletableFuture()

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

        _wepinPinManager.wepinSessionManager?.checkLoginStatusAndGetLifeCycle()
            ?.thenCompose { lifeCycle ->
                if (lifeCycle == WepinLifeCycle.LOGIN || lifeCycle == WepinLifeCycle.LOGIN_BEFORE_REGISTER) {

                    _wepinPinManager.wepinWebViewManager?.openWidgetWithCommand(
                        _appContext!!,
                        subCommand,
                        null
                    )
                } else {
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }?.thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data = JSONObject(result).getJSONObject("body").getJSONObject("data")
                    val uvd = EncUVD.fromJson(data.getJSONObject("UVD").toMap())
                    val hint = EncPinHint.fromJson(data.getJSONObject("hint").toMap())
                    completableFuture.complete(RegistrationPinBlock(uvd = uvd, hint = hint))
                }
            }?.exceptionally { error ->
                val actualError = if (error.cause is WepinError) {
                    error.cause
                } else {
                    WepinError.generalUnKnownEx(error.cause?.message ?: error.message)
                }
                completableFuture.completeExceptionally(actualError)
            }
        return completableFuture
    }

    fun generateAuthPINBlock(count: Int? = null): CompletableFuture<AuthPinBlock?> {
        val completableFuture = CompletableFuture<AuthPinBlock?>()
        val subCommand: String = Command.CMD_SUB_PIN_AUTH

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        _wepinPinManager.wepinSessionManager?.checkLoginStatusAndGetLifeCycle()
            ?.thenCompose { lifeCycle ->
                if (lifeCycle == WepinLifeCycle.LOGIN || lifeCycle == WepinLifeCycle.LOGIN_BEFORE_REGISTER) {
                    var param = mapOf("count" to 1)
                    if (count != null && count > 0) {
                        param = mapOf("count" to count)
                    }
                    _wepinPinManager.wepinWebViewManager?.openWidgetWithCommand(
                        _appContext!!,
                        subCommand,
                        param
                    )
                } else {
                    completableFuture.completeExceptionally(WepinError.INVALID_LOGIN_SESSION)
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }?.thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data =
                        JSONObject(result).getJSONObject("body").getJSONObject("data").toMap()
                    val authPinBlock = AuthPinBlock.fromJson(data)
                    completableFuture.complete(authPinBlock)
                }
            }?.exceptionally { error ->
                val actualError = if (error.cause is WepinError) {
                    error.cause
                } else {
                    WepinError.generalUnKnownEx(error.cause?.message ?: error.message)
                }
                completableFuture.completeExceptionally(actualError)
            }
        return completableFuture
    }

    fun generateChangePINBlock(): CompletableFuture<ChangePinBlock> {
        val completableFuture = CompletableFuture<ChangePinBlock>()
        val subCommand = Command.CMD_SUB_PIN_CHANGE

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        _wepinPinManager.wepinSessionManager?.checkLoginStatusAndGetLifeCycle()
            ?.thenCompose { lifeCycle ->
                if (lifeCycle == WepinLifeCycle.LOGIN || lifeCycle == WepinLifeCycle.LOGIN_BEFORE_REGISTER) {

                    _wepinPinManager.wepinWebViewManager?.openWidgetWithCommand(
                        _appContext!!,
                        subCommand,
                        null
                    )
                } else {
                    completableFuture.completeExceptionally(WepinError.INVALID_LOGIN_SESSION)
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }?.thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data =
                        JSONObject(result).getJSONObject("body").getJSONObject("data").toMap()
                    val changePinBlock = ChangePinBlock.fromJson(data)
                    completableFuture.complete(changePinBlock)
                }
            }?.exceptionally { error ->
                val actualError = if (error.cause is WepinError) {
                    error.cause
                } else {
                    WepinError.generalUnKnownEx(error.cause?.message ?: error.message)
                }
                completableFuture.completeExceptionally(actualError)
            }
        return completableFuture
    }

    fun generateAuthOTPCode(): CompletableFuture<AuthOTP> {
        val completableFuture = CompletableFuture<AuthOTP>()
        val subCommand: String = Command.CMD_SUB_PIN_OTP

        if (!_isInitialized) {
            completableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
            return completableFuture
        }

        _wepinPinManager.wepinSessionManager?.checkLoginStatusAndGetLifeCycle()
            ?.thenCompose { lifeCycle ->
                if (lifeCycle == WepinLifeCycle.LOGIN || lifeCycle == WepinLifeCycle.LOGIN_BEFORE_REGISTER) {

                    _wepinPinManager.wepinWebViewManager?.openWidgetWithCommand(
                        _appContext!!,
                        subCommand,
                        null
                    )
                } else {
                    completableFuture.completeExceptionally(WepinError.INVALID_LOGIN_SESSION)
                    throw WepinError.INVALID_LOGIN_SESSION
                }
            }?.thenApply { result ->
                if (result is String && handleJsonResult(result, subCommand, completableFuture)) {
                    val data =
                        JSONObject(result).getJSONObject("body").getJSONObject("data")

                    val otpCode = AuthOTP(data.getString("code"))
                    completableFuture.complete(otpCode)
                }
            }?.exceptionally { error ->
                val actualError = if (error.cause is WepinError) {
                    error.cause
                } else {
                    WepinError.generalUnKnownEx(error.cause?.message ?: error.message)
                }
                completableFuture.completeExceptionally(actualError)
            }
        return completableFuture
    }

    fun finalize(): Boolean {
        _wepinPinManager.clear()
        WepinPinManager.clearInstance()
        login?.finalize()
        _isInitialized = false
        return true
    }

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