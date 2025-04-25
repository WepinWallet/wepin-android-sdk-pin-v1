package com.wepin.sample

import ChangePinRequest
import GetAccountListRequest
import Network
import OtpCode
import RegisterRequest
import SignRequest
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.loginlib.types.WepinLoginStatus
import com.wepin.android.loginlib.types.WepinUser
import com.wepin.android.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.android.pinlib.WepinPin
import com.wepin.android.pinlib.types.AuthOTP
import com.wepin.android.pinlib.types.AuthPinBlock
import com.wepin.android.pinlib.types.ChangePinBlock
import com.wepin.android.pinlib.types.RegistrationPinBlock
import com.wepin.android.pinlib.types.WepinPinParams
import com.wepin.sample.ui.theme.WepinAndroidSDKTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WepinAndroidSDKTheme {
                WepinLoginTestScreen()
            }
        }
    }
}

class WepinPinViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WepinPinModel"
    private val context: Context
        get() = getApplication<Application>().applicationContext

    private var _status = mutableStateOf("Not Initialized")
    val status: State<String> = _status
    private var _lastAction = mutableStateOf("No action yet")
    val lastAction: State<String> = _lastAction

    private var wepinUser: WepinUser? = null

    private var wepinLogin: WepinLogin? = null
    private var wepinPin: WepinPin? = null
    private var network: Network? = null

    private var registerPinBlock: MutableState<RegistrationPinBlock?> = mutableStateOf(null)
    private var authPinBlock: MutableState<AuthPinBlock?> = mutableStateOf(null)
    private var changePinBlock: MutableState<ChangePinBlock?> = mutableStateOf(null)
    private var authOTPBlock: MutableState<AuthOTP?> = mutableStateOf(null)

    private var _oauthResult = mutableStateOf<LoginOauthResult?>(null)
    val oauthResult: State<LoginOauthResult?> = _oauthResult

    private var _idTokenResult = mutableStateOf<LoginResult?>(null)
    val idTokenResult: State<LoginResult?> = _idTokenResult

    private var _wepinUserResult = mutableStateOf<WepinUser?>(null)
    val wepinUserResult: State<WepinUser?> = _wepinUserResult

    private var _oauthError = mutableStateOf<String?>(null)
    val oauthError: State<String?> = _oauthError

    private var _idTokenError = mutableStateOf<String?>(null)
    val idTokenError: State<String?> = _idTokenError

    private var _wepinLoginError = mutableStateOf<String?>(null)
    val wepinLoginError: State<String?> = _wepinLoginError

    fun initializeWepinLogin(context: Context, appId: String, appKey: String) {
//        wepinLogin = WepinLogin(WepinLoginOptions(context, appId, appKey))
        wepinPin = WepinPin(
            WepinPinParams(
                context = context,
                appId = appId,
                appKey = appKey
            )
        )
        network = Network(appKey, context)
    }

    fun initialize() {
        val result = mutableStateOf<String>("")
        val spacer = mutableStateOf<String>("")
        val initTasks = mutableListOf<CompletableFuture<Void>>()
        if (wepinLogin?.isInitialized() != true) {
            val loginInit = wepinLogin?.init()?.thenApply {
                spacer.value = "\n"
                result.value += if (it) "WepinLogin initialized" else "WepinLogin init() fail"
            }
            loginInit?.let { initTasks.add(it.thenAccept {}) }
        }
        if (wepinPin?.isInitialized() != true) {
            val pinInit = wepinPin?.initialize()?.thenApply {
                wepinLogin = wepinPin?.login
                result.value += spacer.value
                result.value += if (it) "WepinPin initialized" else "WepinPin initialize fail"
            }?.exceptionally {
                Log.e(TAG, "$it")
                result.value += spacer.value + "WepinPin exception: $it"
                null
            }

            pinInit?.let { initTasks.add(it.thenAccept {}) }
        }

        // 모든 비동기 작업 완료 후 updateStatus 실행
        CompletableFuture.allOf(*initTasks.toTypedArray()).thenRun {
            updateStatus("initialize", result.value)
        }
    }

    fun checkInitializationStatus() {
        var result = ""
        if (wepinLogin?.isInitialized() == true) {
            result = "WepinLogin is initialized"
        } else {
            result = "WepinLogin is not initialized"
        }
        result += "\n"
        if (wepinPin?.isInitialized() == true)
            result += "WepinPin is Initialized"
        else
            result += "WepinPin is not Initialized"

        updateStatus("checkInitializationStatus", result)
    }


//    fun loginWepin(type: String) {
//        when (type) {
//            "email" -> {
//                loginWepinWithEmail()
//            }
//
//            "oauth" -> {
//                loginWepinWithOauth("google")
//            }
//        }
//    }

    fun loginWepinWithEmail(email: String, password: String) {
        val params = LoginWithEmailParams(
            email,
            password
        )
        wepinLogin?.loginWithEmailAndPassword(params)?.thenApply { loginResult ->
            wepinLogin?.loginWepin(loginResult)?.thenApply { wepinUser ->
                updateStatus("loginWepinWithEmail", "$wepinUser")
            }?.exceptionally {
                updateStatus("loginWepinWithEmail", "$it")
            }
        }?.exceptionally {
            updateStatus("loginWepinWithEmail", "$it")
            null
        }
    }

    fun loginWepinWithOauth(provider: String) {
        _oauthError.value = null
        _oauthResult.value = null
        val params = LoginOauth2Params(
            provider = provider,
            clientId = when (provider) {
                "google" -> context.getString(R.string.default_google_web_client_id)
                "apple" -> context.getString(R.string.default_apple_client_id)
                "discord" -> context.getString(R.string.default_discord_client_id)
                "naver" -> context.getString(R.string.default_naver_client_id)
                "facebook" -> context.getString(R.string.default_facebook_client_id)
                "line" -> context.getString(R.string.default_line_client_id)
                else -> ""
            }
        )
        wepinLogin?.loginWithOauthProvider(params)?.thenApply { result ->
            _oauthResult.value = result
            updateStatus("loginWithOauthProvider", "$result")
        }?.exceptionally {
            _oauthError.value = it.toString()
            updateStatus("loginWithOauthProvider", "$it")
            null
        }
    }

    fun loginWithToken() {
        Log.d(TAG, "loginWithToken type: ${_oauthResult.value?.type}")
        _idTokenResult.value = null
        when (_oauthResult.value?.type) {
            OauthTokenType.ID_TOKEN -> loginWithIdToken()
            OauthTokenType.ACCESS_TOKEN -> loginWithAccessToken()
            null -> updateStatus("loginWithToken", "type is null")
        }
    }

    private fun loginWithAccessToken() {
        _idTokenError.value = null
        val accesstokenParams = LoginOauthAccessTokenRequest(
            provider = _oauthResult.value?.provider ?: "",
            accessToken = _oauthResult.value?.token ?: ""
        )
        wepinLogin?.loginWithAccessToken(accesstokenParams)?.thenApply { result ->
            _idTokenResult.value = result
            updateStatus("loginWithAccessToken", "$result")
        }?.exceptionally {
            _idTokenError.value = it.toString()
            updateStatus("loginWithAccessToken", "$it")
            null
        }
    }

    fun loginWithIdToken() {
        _idTokenError.value = null
        val idTokenParams = LoginOauthIdTokenRequest(
            idToken = _oauthResult.value?.token ?: ""
        )
        wepinLogin?.loginWithIdToken(idTokenParams)?.thenApply { result ->
            _idTokenResult.value = result
            updateStatus("loginWithIdToken", "$result")
        }?.exceptionally {
            _idTokenError.value = it.toString()
            updateStatus("loginWithIdToken", "$it")
            null
        }
    }

    fun loginWepin() {
        _wepinLoginError.value = null
        _wepinUserResult.value = null
        _idTokenResult.value?.let { loginResult ->
            wepinLogin?.loginWepin(loginResult)?.thenApply { wepinUser ->
                _wepinUserResult.value = wepinUser
                updateStatus("loginWepin", "$wepinUser")
            }?.exceptionally {
                _wepinLoginError.value = it.toString()
                updateStatus("loginWepin", "$it")
            }
        }
    }

    //
    fun getWepinUser() {
        Log.d(TAG, "getWepinUser")
        wepinLogin?.getCurrentWepinUser()?.thenApply {
            Log.d(TAG, "getWepinUser: $it")
            wepinUser = it
            updateStatus("wepinUser", "$it")
        }?.exceptionally {
            Log.e(TAG, "getWepinUserError: $it")
            updateStatus("wepinUser", "$it")
        }
        Log.d(TAG, "getWepinUser end")
    }

    fun logoutWepin() {
        wepinLogin?.logoutWepin()?.thenApply {
            _status.value = "$it"
        }?.exceptionally {
            _status.value = "$it"
        }
    }

    private fun <T, U> execFun(
        method: String,
        params: T? = null,
        generateFunc: (T?) -> CompletableFuture<U>,
        onSuccess: (U) -> Unit
    ) {
        generateFunc(params).thenApply { result ->
            updateStatus(method, "$result")
            onSuccess(result)
        }.exceptionally {
            updateStatus(method, "$it")
        }
    }

    private fun checkUserAvailable(
        requiredLoginStatus: WepinLoginStatus? = null
    ): Boolean {
        if (network == null) {
            updateStatus("sendRequest", "Network is null")
            return false
        }
        if (wepinUser == null) {
            updateStatus("sendRequest", "wepinUser is null")
            return false
        }
//        if (requiredLoginStatus != null && wepinUser!!.userStatus?.loginStatus != requiredLoginStatus) {
//            updateStatus("sendRequest", "LoginStatus is not $requiredLoginStatus")
//            return false
//        }
        return true
    }

    private fun setToken() {
        if (wepinUser == null) {
            updateStatus("setToken", "wepinUser is null")
            return
        }
        network!!.setAuthToken(wepinUser!!.token!!.accessToken, wepinUser!!.token!!.refreshToken)
    }

    private fun sendRegisterRequest() {
        if (!checkUserAvailable(WepinLoginStatus.REGISTER_REQUIRED)) return
        if (registerPinBlock.value == null) {
            updateStatus("sendRequest", "register pin block is null")
        }

        try {
            setToken()
            CoroutineScope(Dispatchers.Main).launch {
                val registerRequest = RegisterRequest(
                    userId = wepinUser!!.userInfo!!.userId,
                    walletId = wepinUser!!.walletId,
                    loginStatus = wepinUser!!.userStatus!!.loginStatus.value,
                    uvd = registerPinBlock.value!!.uvd,
                    hint = registerPinBlock.value!!.hint
                )
                val registerRes = withContext(Dispatchers.IO) {
                    network!!.register(registerRequest)
                }
                updateStatus("sendRegisterRequest", "$registerRes")
            }
        } catch (error: Exception) {
            updateStatus("sendRegisterRequest", "$error")
        }
    }

    private fun sendAuthRequest() {
        if (authPinBlock.value == null) {
            updateStatus("sendAuthRequest", "authPinBlock is null")
            return
        }
        checkUserAvailable(WepinLoginStatus.REGISTER_REQUIRED)
        setToken()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val getAccountListRequest = GetAccountListRequest(
                    walletId = wepinUser!!.walletId!!,
                    userId = wepinUser!!.userInfo!!.userId,
                    localeId = "1"
                )
                val accountRes = withContext(Dispatchers.IO) {
                    network!!.getAppAccountList(getAccountListRequest)
                }

                val firstUvd = authPinBlock.value!!.uvdList.firstOrNull()
                if (firstUvd == null) {
                    updateStatus("sendAuthRequest", "firstUVD is null")
                    return@launch
                } else {
                    val otpCode = if (authPinBlock.value!!.otp != null) {
                        OtpCode(
                            code = authPinBlock.value!!.otp ?: "",
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
                        txData = mapOf("data" to "test123456"),
                        otpCode = otpCode
                    )

                    val authRes = withContext(Dispatchers.IO) {
                        network!!.sign(signRequest)
                    }
                    updateStatus("send Auth Request", "$authRes")
                }
            } catch (error: Exception) {
                updateStatus("send Auth Request Error", "$error")
            }
        }
    }

    private fun sendChangePinRequest() {
        checkUserAvailable(WepinLoginStatus.REGISTER_REQUIRED)
        setToken()
        CoroutineScope(Dispatchers.Main).launch {
            try {

                val otpCode = if (changePinBlock.value!!.otp != null) {
                    OtpCode(
                        code = changePinBlock.value!!.otp!!,
                        recovery = false
                    )
                } else {
                    null
                }

                val changePinRequest = ChangePinRequest(
                    userId = wepinUser!!.userInfo!!.userId,
                    walletId = wepinUser!!.walletId!!,
                    uvd = changePinBlock.value!!.uvd,
                    newUVD = changePinBlock.value!!.newUVD,
                    hint = changePinBlock.value!!.hint,
                    otpCode = otpCode
                )

                val changePisRes = withContext(Dispatchers.IO) {
                    network!!.changePin(changePinRequest)
                }
                updateStatus("send Change PIN Request", "$changePisRes")

            } catch (error: Exception) {
                updateStatus("send Change PIN Request Error", "$error")
            }
        }
    }

    fun sendRequest(type: String) {
        when (type) {
            "register" -> sendRegisterRequest()
            "auth" -> sendAuthRequest()
            "change" -> sendChangePinRequest()
            else -> _status.value = "invalid request type"
        }
    }

    fun generatePinBlock(type: String) {
        if (wepinPin == null) {
            updateStatus("generatePinBlock", "wepinPin is null")
        }
        when (type) {
            "register" -> execFun<Void, RegistrationPinBlock?>(
                method = "Register Pin Block Generate",
                generateFunc = { wepinPin!!.generateRegistrationPINBlock() },
                onSuccess = { registerPinBlock.value = it }
            )

            "auth" -> execFun(
                method = "Auth Pin Block Generate",
                params = 1, generateFunc = wepinPin!!::generateAuthPINBlock,
                onSuccess = { authPinBlock.value = it })

            "change" -> execFun<Void, ChangePinBlock>(
                method = "Change Pin Block Generate",
                generateFunc = { wepinPin!!.generateChangePINBlock() },
                onSuccess = { changePinBlock.value = it })

            "otp" -> execFun<Void, AuthOTP>(
                method = "Auth OTP Generate",
                generateFunc = { wepinPin!!.generateAuthOTPCode() },
                onSuccess = { authOTPBlock.value = it })
        }
    }

    fun finalize(sdk: String? = "pin") {
//        if (sdk == "login")
//            wepinLogin?.finalize()
//        else
            wepinPin?.finalize()
        wepinLogin = null
        updateStatus("finalize", "")
    }

    private fun updateStatus(action: String, message: String) {
        _lastAction.value = action
        _status.value = message
    }
}

@Composable
fun WepinLoginTestScreen(
    viewModel: WepinPinViewModel = viewModel()
) {
    val context = LocalContext.current
    val appId = remember { context.getString(R.string.wepin_app_id) }
    val appKey = remember { context.getString(R.string.wepin_app_key) }

    LaunchedEffect(Unit) {
        viewModel.initializeWepinLogin(context, appId, appKey)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp)
    ) {
        HeaderSection()
        ButtonSection(
            modifier = Modifier.weight(2f),
            onInitialize = { viewModel.initialize() },
            onCheckStatus = { viewModel.checkInitializationStatus() },
            onGetUser = { viewModel.getWepinUser() },
            onGeneratePin = { type -> viewModel.generatePinBlock(type) },
            onSendRequest = { type -> viewModel.sendRequest(type) },
            onLogout = { viewModel.logoutWepin() },
            onFinalize = { sdk -> viewModel.finalize(sdk) }
        )
        ResultSection(
            modifier = Modifier.weight(1f),
            action = viewModel.lastAction.value,
            status = viewModel.status.value
        )
    }
}

@Composable
private fun HeaderSection() {
    Text("Wepin PIN Test", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun ButtonSection(
    modifier: Modifier = Modifier,
    onInitialize: () -> Unit,
    onCheckStatus: () -> Unit,
    onGetUser: () -> Unit,
    onGeneratePin: (type: String) -> Unit,
    onSendRequest: (type: String) -> Unit,
    onLogout: () -> Unit,
    onFinalize: (sdk: String) -> Unit
) {
    var showLoginScreen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WepinButton(text = "Initialize", onClick = onInitialize)
        WepinButton(text = "Check Initialization Status", onClick = onCheckStatus)
        WepinButton(text = "Login", onClick = { showLoginScreen = true })
        WepinButton(text = "Get Wepin User", onClick = onGetUser)
        WepinButton(text = "Generate Register Pin Block", onClick = { onGeneratePin("register") })
        WepinButton(
            text = "Send Request to Wepin (Register)",
            onClick = { onSendRequest("register") })
        WepinButton(text = "Generate Auth Pin Block", onClick = { onGeneratePin("auth") })
        WepinButton(text = "Send Request to Wepin (Auth PIN)", onClick = { onSendRequest("auth") })
        WepinButton(text = "Generate Change Pin Block", onClick = { onGeneratePin("change") })
        WepinButton(
            text = "Send Request to Wepin (Change PIN)",
            onClick = { onSendRequest("change") })
        WepinButton(text = "Generate OTP Pin Block", onClick = { onGeneratePin("otp") })
        WepinButton(text = "Logout Wepin", onClick = onLogout)
//        WepinButton(text = "WepinLogin finalize", onClick = { onFinalize("login") })
        WepinButton(text = "WepinPin finalize", onClick = { onFinalize("pin") })
    }

    if (showLoginScreen) {
        LoginScreen(
            onLogin = { type ->
                showLoginScreen = false
                // Don't call onLogin here since we've already completed the login process
            },
            onDismiss = { showLoginScreen = false },
            viewModel = viewModel()
        )
    }
}

@Composable
private fun LoginScreen(
    onLogin: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: WepinPinViewModel = viewModel()
) {
    var selectedProvider by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEmailLogin by remember { mutableStateOf(false) }
    var loginStep by remember { mutableStateOf(0) } // 0: provider selection, 1: email login, 2: oauth login
    var oauthStep by remember { mutableStateOf(0) } // 0: not started, 1: oauth provider, 2: id token

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.9f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loginStep == 0) {
                    Text("Select Login Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Login Button
                    WepinButton(
                        text = "Login with Email",
                        onClick = {
                            selectedProvider = "email"
                            showEmailLogin = true
                            loginStep = 1
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // OAuth Provider Buttons
                    WepinButton(
                        text = "Login with Google",
                        onClick = {
                            selectedProvider = "google"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Apple",
                        onClick = {
                            selectedProvider = "apple"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Discord",
                        onClick = {
                            selectedProvider = "discord"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Naver",
                        onClick = {
                            selectedProvider = "naver"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Facebook",
                        onClick = {
                            selectedProvider = "facebook"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Line",
                        onClick = {
                            selectedProvider = "line"
                            loginStep = 2
                        }
                    )
                } else if (loginStep == 1 && showEmailLogin) {
                    // Email Login Form
                    Text("Email Login", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    WepinButton(
                        text = "Login",
                        onClick = {
                            viewModel.loginWepinWithEmail(email, password)
                            onDismiss()
                        }
                    )
                } else if (loginStep == 2 && selectedProvider != null) {
                    when (oauthStep) {
                        0 -> {
                            Text(
                                "Login with ${selectedProvider?.replaceFirstChar { it.titlecase() }}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            WepinButton(
                                text = "Proceed",
                                onClick = {
                                    selectedProvider?.let { provider ->
                                        viewModel.loginWepinWithOauth(provider)
                                        oauthStep = 1
                                    }
                                }
                            )
                        }

                        1 -> {
                            Text(
                                "OAuth Provider Result",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(viewModel.oauthResult.value?.toString() ?: "No result yet")
                                    if (viewModel.oauthError.value != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Error: ${viewModel.oauthError.value}",
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            WepinButton(
                                text = "Next",
                                onClick = {
                                    viewModel.loginWithToken()
                                    oauthStep = 2
                                }
                            )
                        }

                        2 -> {
                            Text("ID Token Result", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        viewModel.idTokenResult.value?.toString() ?: "No result yet"
                                    )
                                    if (viewModel.idTokenError.value != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Error: ${viewModel.idTokenError.value}",
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            WepinButton(
                                text = "Complete",
                                onClick = {
                                    viewModel.loginWepin()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                WepinButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun WepinButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun ResultSection(
    modifier: Modifier = Modifier,
    action: String,
    status: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 10.dp)
            .background(Color.White)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = action, style = MaterialTheme.typography.bodyLarge)
                Text(text = status, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun loginProviderSelectionButton(
    onLoginResult: () -> Unit,
    onError: (String) -> Unit,
) {

}