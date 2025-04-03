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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wepin.android.commonlib.types.LoginOauthIdTokenRequest
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.WepinLoginStatus
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

class WepinLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WepinPinModel"
    private val context: Context
        get() = getApplication<Application>().applicationContext

    private var _status = mutableStateOf("Not Initialized")
    val status: State<String> = _status
    private var _lastAction = mutableStateOf("No action yet")
    val lastAction: State<String> = _lastAction

    private var wepinUser: WepinUser? = null

    private var wepinPin: WepinPin? = null
    private var network: Network? = null

    private var registerPinBlock: MutableState<RegistrationPinBlock?> = mutableStateOf(null)
    private var authPinBlock: MutableState<AuthPinBlock?> = mutableStateOf(null)
    private var changePinBlock: MutableState<ChangePinBlock?> = mutableStateOf(null)
    private var authOTPBlock: MutableState<AuthOTP?> = mutableStateOf(null)

    fun initializeWepinLogin(context: Context, appId: String, appKey: String) {
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
        wepinPin?.initialize()
            ?.thenApply {
                if (it) updateStatus(
                    "initialize",
                    "Initialized"
                ) else updateStatus("initialize", "Initialization Failed")
            }?.exceptionally {
                Log.e(TAG, "$it")
                updateStatus("initialize", "$it")
            }
    }

    fun checkInitializationStatus() {
        if (wepinPin?.isInitialized() == true)
            updateStatus("checkInitializationStatus", "Widget is Initialized")
        else
            updateStatus("checkInitializationStatus", "Not Initialized")
    }

    fun loginWepinWithOauth() {
        val params = LoginOauth2Params(
            "google",
            context.getString(R.string.default_google_web_client_id)
        )
        wepinPin?.login?.loginWithOauthProvider(params)?.thenApply { loginOauthResult ->
            wepinPin?.login?.loginWithIdToken(LoginOauthIdTokenRequest(idToken = loginOauthResult.token))
                ?.thenApply { loginResult ->
                    wepinPin?.login?.loginWepin(loginResult)?.thenApply {
                        wepinUser = it
                        updateStatus("loginWepinWithOauth", "$it")
                    }?.exceptionally {
                        updateStatus("loginWepinWithOauth", "$it")
                    }
                }?.exceptionally {
                    updateStatus("loginWepinWithOauth", "$it")
                    null
                }
        }?.exceptionally {
            updateStatus("loginWepinWithOauth", "$it")
            null
        }
    }

    fun loginWepin(type: String) {
        when (type) {
            "oauth" -> {
                loginWepinWithOauth()
            }

            "email" -> {
                loginWepinWithEmail()
            }

            else -> throw Exception("")
        }
    }

    fun loginWepinWithEmail() {
        val params = LoginWithEmailParams(
            "8856d4f3ff1f@drmail.in",
            "doublebk13!@"
        )
        wepinPin?.login?.loginWithEmailAndPassword(params)?.thenApply { loginResult ->
//                loginOauthResult.value = it
            wepinPin?.login?.loginWepin(loginResult)?.thenApply {
                wepinUser = it
                updateStatus("loginWepinWithEmail", "$it")
            }?.exceptionally {
                updateStatus("loginWepinWithEmail", "$it")
            }
        }?.exceptionally {
            updateStatus("loginWepinWithEmail", "$it")
            null
        }
    }

    //
    fun getWepinUser() {
        Log.d(TAG, "getWepinUser")
        wepinPin?.login?.getCurrentWepinUser()?.thenApply {
            Log.d(TAG, "getWepinUser: $it")
            wepinUser = it
            updateStatus("wepinUser", "$it")
        }?.exceptionally {
            Log.e(TAG, "getWepinUserError: $it")
            updateStatus("wepinUser", "$it")
        }
        Log.d(TAG, "getWepinUser end")
    }
//
//    fun logoutWepin() {
//        wepinLogin?.logoutWepin()?.thenApply {
//            _status.value = "$it"
//        }?.exceptionally {
//            _status.value = "$it"
//        }
//    }

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
        network!!.setAuthToken(wepinUser!!.token!!.accessToken, wepinUser!!.token!!.refreshToken)
    }

    private fun sendRegisterRequest() {
        if (!checkUserAvailable(com.wepin.android.commonlib.types.WepinLoginStatus.REGISTER_REQUIRED)) return
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
        checkUserAvailable(com.wepin.android.commonlib.types.WepinLoginStatus.REGISTER_REQUIRED)
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
        checkUserAvailable(com.wepin.android.commonlib.types.WepinLoginStatus.REGISTER_REQUIRED)
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

    fun finalize() {
        wepinPin?.finalize()
        updateStatus("finalize", "")
    }

    private fun updateStatus(action: String, message: String) {
        _lastAction.value = action
        _status.value = message
    }
}

@Composable
fun WepinLoginTestScreen(
    viewModel: WepinLoginViewModel = viewModel()
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
            onLogin = { type -> viewModel.loginWepin(type) },
            onGetUser = { viewModel.getWepinUser() },
            onGeneratePin = { type -> viewModel.generatePinBlock(type) },
            onSendRequest = { type -> viewModel.sendRequest(type) },
            onFinalize = { viewModel.finalize() }
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
    Text("Wepin Login Test", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun ButtonSection(
    modifier: Modifier = Modifier,
    onInitialize: () -> Unit,
    onCheckStatus: () -> Unit,
    onLogin: (type: String) -> Unit,
    onGetUser: () -> Unit,
    onGeneratePin: (type: String) -> Unit,
    onSendRequest: (type: String) -> Unit,
    onFinalize: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WepinButton(text = "Initialize", onClick = onInitialize)
        WepinButton(text = "Check Initialization Status", onClick = onCheckStatus)
        WepinButton(text = "Wepin Login With Email", onClick = { onLogin("email") })
        WepinButton(text = "Wepin Login With Oauth", onClick = { onLogin("oauth") })
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
        WepinButton(text = "WepinPin finalize", onClick = onFinalize)
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