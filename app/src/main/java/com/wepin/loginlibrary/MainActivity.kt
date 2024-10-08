package com.wepin.loginlibrary

import ChangePinRequest
import GetAccountListRequest
import Network
import OtpCode
import RegisterRequest
import SignRequest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.loginlib.types.WepinLoginStatus
import com.wepin.android.loginlib.types.WepinUser
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.android.pinlib.WepinPin
import com.wepin.android.pinlib.error.WepinError
import com.wepin.android.pinlib.types.AuthOTP
import com.wepin.android.pinlib.types.AuthPinBlock
import com.wepin.android.pinlib.types.ChangePinBlock
import com.wepin.android.pinlib.types.RegistrationPinBlock
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.types.WepinPinParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var wepinLogin: WepinLogin
    private lateinit var wepinPin: WepinPin
    private lateinit var network : Network
    private var wepinUser: WepinUser? = null
    private var registerPin: RegistrationPinBlock? = null
    private var authPin: AuthPinBlock? = null
    private var changePin: ChangePinBlock? = null
    private var authOTPCode: AuthOTP? = null

    private var itemListView: ListView? = null
    private var tvResult: TextView? = null
    private lateinit var testItem: Array<String>

    private var loginResult: LoginResult? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example_main)

        initView()
        val wepinLoginOptions =  WepinLoginOptions(
            context = this,
            appId=resources.getString(R.string.wepin_app_id),
            appKey = resources.getString(R.string.wepin_app_key)
        )
        // Wepin Login Library
        wepinLogin = WepinLogin(wepinLoginOptions)
        // Wepin Pin Library
        val wepinPinParams =  WepinPinParams(
            context = this,
            appId=resources.getString(R.string.wepin_app_id),
            appKey = resources.getString(R.string.wepin_app_key),
        )
        wepinPin = WepinPin(wepinPinParams)
        network = Network(resources.getString(R.string.wepin_app_key), this)
    }

    override fun onNewIntent(intent: Intent?) {
        println("onNewIntent")
        super.onNewIntent(intent)

    }
    override fun onResume() {
        println("onResume")
        super.onResume()

    }

    private fun initView() {
        try {
            itemListView = findViewById(R.id.lv_menu)
            tvResult = findViewById(R.id.tv_result)
            testItem = arrayOf(
                resources.getString(R.string.item_login_with_loginLib_google),
                resources.getString(R.string.item_init),
                resources.getString(R.string.item_isInitialized),
                resources.getString(R.string.item_change_language),
                resources.getString(R.string.item_gen_registration_pin_block_for_register),
                resources.getString(R.string.item_request_register_with_registration_pin_block),
                resources.getString(R.string.item_gen_auth_pin_block_for_register),
                resources.getString(R.string.item_request_register_with_auth_pin_block),
                resources.getString(R.string.item_gen_auth_pin_block_for_sign),
                resources.getString(R.string.item_request_sign_with_auth_pin_block),
                resources.getString(R.string.item_gen_change_pin_block),
                resources.getString(R.string.item_request_change_pin_with_change_pin_block),
                resources.getString(R.string.item_gen_auth_otp_code),
                resources.getString(R.string.item_finalize),
            )
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                testItem
            )
            itemListView?.adapter = adapter
            itemListView?.itemsCanFocus = true
            itemListView?.choiceMode = ListView.CHOICE_MODE_SINGLE
            itemListView?.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
                val operationItem = arg0.adapter.getItem(arg2).toString()
                when (operationItem) {
                    // Test With LoginLibrary
                    // Users must log in to Wepin before using the PIN pad library.
                    resources.getString(R.string.item_login_with_loginLib_google) -> {
                        Log.d("MainAcivity", "clicked - $operationItem")
                        loginWithLoginLibGoogle(operationItem)
                    }
                    // End Test With pinLibrary

                    // Test With pinLibrary
                    resources.getString(R.string.item_init) -> {
                        Log.d("MainAcivity", "clicked - $operationItem")
                        initWepinPin(operationItem)
                    }

                    resources.getString(R.string.item_isInitialized) -> {
                        val result = wepinPin.isInitialized()
                        tvResult?.text = String.format(
                            " Item : %s\n Result : %s",
                            operationItem,
                            result
                        )
                    }
                    resources.getString(R.string.item_change_language) -> {
                        wepinPin.changeLanguage("ko").whenComplete{ res, err ->
                            if (err == null) {
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Error : %s",
                                    operationItem,
                                    err
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_gen_registration_pin_block_for_register) -> {
                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }

                        if( wepinUser!!.userStatus!!.loginStatus != WepinLoginStatus.PIN_REQUIRED ){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "LoginStatus is not PIN_REQUIRED"
                            )
                            return@OnItemClickListener
                        }
                        wepinPin.generateRegistrationPINBlock().whenComplete { res, err ->
                            if (err == null) {
                                registerPin = RegistrationPinBlock(uvd = res!!.uvd, hint = res!!.hint)
                                println("registerPin.uvd : ${registerPin!!.uvd}")
                                println("registerPin.hint : ${registerPin!!.hint}")
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Error : %s",
                                    operationItem,
                                    err
                                )
                            }
                        }

                    }

                    resources.getString(R.string.item_request_register_with_registration_pin_block) -> {

                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }

                        if( wepinUser!!.userStatus!!.loginStatus != WepinLoginStatus.PIN_REQUIRED ){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "LoginStatus is not PIN_REQUIRED"
                            )
                            return@OnItemClickListener
                        }

                        if( registerPin == null ){
                            tvResult?.text = String.format(
                                " Item : %s\n Result : %s",
                                operationItem,
                                "registerPin null"
                            )
                            return@OnItemClickListener
                        }

                        // Wepin RESTful API 요청
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val registerRequest = RegisterRequest(
                                    userId = wepinUser!!.userInfo!!.userId,
                                    loginStatus = wepinUser!!.userStatus!!.loginStatus.value,
                                    uvd = registerPin!!.uvd,
                                    hint = registerPin!!.hint
                                )
                                network.setAuthToken(wepinUser!!.token!!.accessToken, wepinUser!!.token!!.refreshToken)

                                val registerRes = withContext(Dispatchers.IO) {
                                    network.register(registerRequest)
                                }

                                // 성공적으로 응답을 받은 후의 처리
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    registerRes
                                )

                            } catch (e: Exception) {
                                // 예외 처리
                                e.printStackTrace()
                                println("failed: ${e.message}")
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    e.message
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_gen_auth_pin_block_for_register) -> {
                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }

                        if( wepinUser!!.userStatus!!.loginStatus != WepinLoginStatus.REGISTER_REQUIRED ){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "LoginStatus is not REGISTER_REQUIRED"
                            )
                            return@OnItemClickListener
                        }
                        wepinPin.generateAuthPINBlock().whenComplete { res, err ->
                            if (err == null) {
                                authPin = AuthPinBlock(uvdList = res!!.uvdList, otp = res!!.otp)
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Error : %s",
                                    operationItem,
                                    err
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_request_register_with_auth_pin_block) -> {

                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }

                        if( wepinUser!!.userStatus!!.loginStatus != WepinLoginStatus.REGISTER_REQUIRED ){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "LoginStatus is not REGISTER_REQUIRED"
                            )
                            return@OnItemClickListener
                        }

                        if( authPin == null ){
                            tvResult?.text = String.format(
                                " Item : %s\n Result : %s",
                                operationItem,
                                "aauthPin null"
                            )
                            return@OnItemClickListener
                        }

                        // Wepin RESTful API 요청
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val registerRequest = RegisterRequest(
                                    userId = wepinUser!!.userInfo!!.userId,
                                    walletId = wepinUser!!.walletId,
                                    loginStatus = wepinUser!!.userStatus!!.loginStatus.value,
                                    uvd = authPin!!.uvdList.first(),
                                )
                                network.setAuthToken(wepinUser!!.token!!.accessToken, wepinUser!!.token!!.refreshToken)

                                val registerRes = withContext(Dispatchers.IO) {
                                    network.register(registerRequest)
                                }

                                // 성공적으로 응답을 받은 후의 처리
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    registerRes
                                )

                            } catch (e: Exception) {
                                // 예외 처리
                                e.printStackTrace()
                                println("failed: ${e.message}")
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    e.message
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_gen_auth_pin_block_for_sign) -> {
                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }
                        wepinPin.generateAuthPINBlock().whenComplete { res, err ->
                            if (err == null) {
                                authPin = AuthPinBlock(uvdList = res!!.uvdList, otp = res!!.otp)
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Error : %s",
                                    operationItem,
                                    err
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_request_sign_with_auth_pin_block) -> {
                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }
                        if( authPin == null ){
                            tvResult?.text = String.format(
                                " Item : %s\n Result : %s",
                                operationItem,
                                "authPin null"
                            )
                            return@OnItemClickListener
                        }

                        network.setAuthToken(wepinUser!!.token!!.accessToken, wepinUser!!.token!!.refreshToken)
                        // Wepin RESTful API 요청
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val getAccountListRequest = GetAccountListRequest(
                                    walletId = wepinUser!!.walletId!!,
                                    userId = wepinUser!!.userInfo!!.userId,
                                    localeId = "1"
                                )
                                val accountRes = withContext(Dispatchers.IO) {
                                    network.getAppAccountList(getAccountListRequest)
                                }

                                val firstUvd = authPin!!.uvdList.firstOrNull()
                                if (firstUvd == null) {
                                    tvResult?.text = String.format(
                                        " Item : %s\n Result : %s",
                                        operationItem,
                                        "firstUvd is null"
                                    )
                                    return@launch

                                } else {
                                    val otpCode = if (authPin!!.otp != null) {
                                        OtpCode(
                                            code = authPin!!.otp!!,
                                            recovery = false
                                        )
                                    } else {
                                        null
                                    }

                                    val signRequest = SignRequest(
                                        userId = wepinUser!!.userInfo!!.userId,
                                        type = "msg_sign",
                                        accountId = accountRes.accounts.first().accountId,
                                        walletId = wepinUser!!.walletId!!,
                                        pin = firstUvd,
                                        txData = mapOf("data" to "test123456" ),
                                        otpCode = otpCode
                                    )

                                    val registerRes = withContext(Dispatchers.IO) {
                                        network.sign(signRequest)
                                    }

                                    tvResult?.text = String.format(
                                        " Item : %s\n Result : %s",
                                        operationItem,
                                        registerRes
                                    )
                                }

                            } catch (e: Exception) {
                                // 예외 처리
                                e.printStackTrace()
                                println("failed: ${e.message}")
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    e.message
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_gen_change_pin_block) -> {
                        wepinPin.generateChangePINBlock().whenComplete { res, err ->
                            if (err == null) {
                                changePin = ChangePinBlock(uvd = res!!.uvd, newUVD = res.newUVD, hint = res.hint, otp = res.otp)
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Error : %s",
                                    operationItem,
                                    err
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_request_change_pin_with_change_pin_block) -> {
                        if (wepinUser == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Error : %s",
                                operationItem,
                                "Login required"
                            )
                            return@OnItemClickListener
                        }
                        if( changePin == null ){
                            tvResult?.text = String.format(
                                " Item : %s\n Result : %s",
                                operationItem,
                                "changePin null"
                            )
                            return@OnItemClickListener
                        }

                        network.setAuthToken(wepinUser!!.token!!.accessToken, wepinUser!!.token!!.refreshToken)
                        // Wepin RESTful API 요청
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val otpCode = if (changePin!!.otp != null) {
                                    OtpCode(
                                        code = changePin!!.otp!!,
                                        recovery = false
                                    )
                                } else {
                                    null
                                }

                                val changePinRequest = ChangePinRequest(
                                    userId = wepinUser!!.userInfo!!.userId,
                                    walletId = wepinUser!!.walletId!!,
                                    uvd = changePin!!.uvd,
                                    newUVD = changePin!!.newUVD,
                                    hint = changePin!!.hint,
                                    otpCode = otpCode
                                )

                                val changePinRes = withContext(Dispatchers.IO) {
                                    network.changePin(changePinRequest)
                                }

                                // 성공적으로 응답을 받은 후의 처리
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    changePinRes
                                )
                            } catch (e: Exception) {
                                // 예외 처리
                                e.printStackTrace()
                                println("failed: ${e.message}")
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    e.message
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_gen_auth_otp_code) -> {
                        wepinPin.generateAuthOTPCode().whenComplete { res, err ->
                            if (err == null) {
                                authOTPCode = AuthOTP(res!!.code)
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Error : %s",
                                    operationItem,
                                    err
                                )
                            }
                        }
                    }

                    resources.getString(R.string.item_finalize) -> {
                        wepinPin.finalize()
                        tvResult?.text = String.format(
                            " Item : %s\n Result : %s",
                            operationItem,
                            "Success"
                        )
                    }
                    // End Test With pinLibrary
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // Test With LoginLibrary
    private fun loginWithLoginLibGoogle(operationItem: String) {
        try {
            val res = wepinLogin.init()

            res?.whenComplete { infResponse, error ->
                if (error == null || (error is WepinError && error == WepinError.ALREADY_INITIALIZED_ERROR)) {
                    // 구글 Oauth idtoken 로그인 후 위핀 로그인 수행

                    val loginOption = LoginOauth2Params(
                        provider = "google",
                        clientId = getString(R.string.default_google_web_client_id),
                    )

                    wepinLogin.loginWithOauthProvider(loginOption).whenComplete { loginResponse, loginError ->
                        if (loginError == null) {
                            // 로그인 성공 시 처리

                            val sign = wepinLogin.getSignForLogin(resources.getString(R.string.wepin_app_private_key), loginResponse.token)
                            val idTokenRequest = LoginOauthIdTokenRequest(loginResponse.token, sign)
                            wepinLogin.loginWithIdToken(idTokenRequest).whenComplete { idTokenResponse, idTokenError ->
                                if (idTokenError == null) {
                                    // ID 토큰 로그인 성공 시 처리
                                    println(idTokenResponse)
                                    loginResult = idTokenResponse
                                    // 위핀 로그인 호출
                                    wepinLogin.loginWepin(loginResult!!).whenComplete { wepinResponse, wepinError ->
                                        loginResult = null

                                        if (wepinError == null) {
                                            tvResult?.text = String.format(
                                                " Item : %s\n Result : %s",
                                                operationItem,
                                                wepinResponse
                                            )
                                            wepinUser = wepinResponse
                                        } else {
                                            tvResult?.text = String.format(
                                                " Item : %s\n Result : %s",
                                                operationItem,
                                                "fail: ${wepinError.message}"
                                            )
                                        }
                                    }
                                } else {
                                    println("ID Token login error - ${idTokenError.message}")
                                    // 에러 UI 처리
                                }
                            }
                        } else {
                            println("Oauth Provider login error - ${loginError.message}")
                            // 에러 UI 처리
                        }
                    }
                } else {
                    // 초기화 오류 처리
                    tvResult?.text = String.format(
                        " Item : %s\n Result : %s",
                        operationItem,
                        "fail : ${error.message}"
                    )
                    println(error)
                }
            }
        } catch (e: Exception) {
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "fail"
            )
        }
    }

    private fun initWepinPin(operationItem: String) {
        if (wepinPin.isInitialized()) {
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "Already initialized"
            )
            return
        } else {
            try {
                var attributes = WepinPinAttributes("en")
                val res = wepinPin.initialize(attributes)
                res?.whenComplete { infResponse, error ->
                    if (error == null) {
                        // 작업이 성공적으로 완료되었을 때
                        println(infResponse)
                        tvResult?.text = String.format(
                            "Item : %s\nResult : %s",
                            operationItem,
                            infResponse
                        )
                    } else {
                        // 작업 중 오류가 발생했을 때
                        println(error)
                        tvResult?.text = String.format(
                            "Item : %s\nResult : %s",
                            operationItem,
                            "fail: ${error.message}"
                        )
                    }
                }
                res?.exceptionally { throwable ->
                    val cause = throwable.cause
                    if (cause is WepinError) {
                        // Handle WepinError directly
                        when (cause) {
                            WepinError.NOT_INITIALIZED_ERROR -> {
                                // Handle specific error
                            }
                            WepinError.INVALID_LOGIN_SESSION -> {
                                // Handle specific error
                            }
                            else -> {
                                // Handle general WepinError
                            }
                        }
                    } else {
                        // Handle other exceptions
                    }
                    null // or any fallback value
                }
                println("res - $res")
            } catch (e: Exception) {
                tvResult?.text = String.format(
                    "Item : %s\nResult : %s",
                    operationItem,
                    "fail"
                )
            }
        }
    }
}



