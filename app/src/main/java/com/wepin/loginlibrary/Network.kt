import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wepin.android.pinlib.types.EncPinHint
import com.wepin.android.pinlib.types.EncUVD
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL

data class RegisterRequest(
    //@SerializedName("appId") val appId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("loginStatus") val loginStatus: String,
    @SerializedName("walletId") val walletId: String? = null,
    @SerializedName("UVD") val uvd: EncUVD? = null,
    @SerializedName("hint") val hint: EncPinHint? = null
)


@Serializable
data class RegisterResponse(
    val success: Boolean,
    val walletId: String
)

@Serializable
data class OtpCode(
    val code: String,
    val recovery: Boolean
)

data class ChangePinRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("walletId") val walletId: String,
    @SerializedName("UVD") val uvd: EncUVD,
    @SerializedName("newUVD") val newUVD: EncUVD,
    @SerializedName("hint") val hint: EncPinHint,
    @SerializedName("otpCode") val otpCode: OtpCode? = null
)

@Serializable
data class ChangePinResponse(
    val status: Boolean
)

data class SignRequest(
    @SerializedName("type") val type: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("walletId") val walletId: String,
    @SerializedName("accountId") val accountId: String,
    @SerializedName("contract") val contract: String? = null,
    @SerializedName("tokenId") val tokenId: String? = null,
    @SerializedName("isNft") val isNft: String? = null,
    @SerializedName("pin") val pin: EncUVD,
    @SerializedName("otpCode") val otpCode: OtpCode? = null,
    @SerializedName("txData") val txData: Map<String, Any>
)

@Serializable
data class SignResponse(
    val signatureResult: Any?,
    val transaction: Map<String, Any>,
    val broadcastData: String? = null,
    val txId: String? = null
)

@Serializable
data class GetAccountListRequest(
    val walletId: String,
    val userId: String,
    val localeId: String,
)

@Serializable
data class GetAccountListResponse(
    val walletId: String,
    val accounts: List<IAppAccount>,
    val aa_accounts: List<IAppAccount>?
)

data class IAppAccount(
    val accountId: String,
    val address: String,
    val eoaAddress: String?,
    val addressPath: String,
    val coinId: Int?,
    val contract: String?,
    val symbol: String,
    val label: String,
    val name: String,
    val network: String,
    val balance: String,
    val decimals: Int,
    val iconUrl: String,
    val ids: String?,
    val accountTokenId: String?,
    val cmkId: Int?,
    val isAA: Boolean?
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): IAppAccount {
            return IAppAccount(
                accountId = json["accountId"] as String,
                address = json["address"] as String,
                eoaAddress = json["eoaAddress"] as? String,
                addressPath = json["addressPath"] as String,
                coinId = (json["coinId"] as? Number)?.toInt(),
                contract = json["contract"] as? String,
                symbol = json["symbol"] as String,
                label = json["label"] as String,
                name = json["name"] as String,
                network = json["network"] as String,
                balance = json["balance"] as String,
                decimals = (json["decimals"] as Number).toInt(),
                iconUrl = json["iconUrl"] as String,
                ids = json["ids"] as? String,
                accountTokenId = json["accountTokenId"] as? String,
                cmkId = (json["cmkId"] as? Number)?.toInt(),
                isAA = json["isAA"] as? Boolean
            )
        }
    }
}

class Network(private val appKey: String, private val context: Context) {

    private val wepinBaseUrl: String = getUrl(appKey)
    private var accessToken: String? = null
    private var refreshToken: String? = null

    private fun getUrl(key: String): String {
        return when {
            key.startsWith("ak_dev") -> "https://dev-sdk.wepin.io/v1/"
            key.startsWith("ak_test") -> "https://stage-sdk.wepin.io/v1/"
            key.startsWith("ak_live") -> "https://sdk.wepin.io/v1/"
            else -> throw IllegalArgumentException("Invalid app key")
        }
    }

    private fun setHeaders(): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Content-Type"] = "application/json"
        headers["X-API-KEY"] = appKey
        headers["X-API-DOMAIN"] = (context as Activity).packageName
        headers["X-SDK-TYPE"] = "android-rest-api"
        headers["X-SDK-VERSION"] = "0.0.1" // Wepin Pin Pad Library Version
        accessToken?.let {
            headers["Authorization"] = "Bearer $it"
        }
        return headers
    }

    fun setAuthToken(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    // 공통 HTTP 요청 함수
    inline fun <reified T> httpRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null
    ): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

        if (method == "POST" || method == "PATCH") {
            connection.doOutput = true
            body?.let {
                connection.outputStream.write(it.toByteArray())
            }
        }

        val responseCode = connection.responseCode

        return if (responseCode in 200..299) {
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            gson.fromJson(responseBody, T::class.java) // JSON 문자열을 객체로 역직렬화
        } else {
            val errorBody = connection.errorStream.bufferedReader().use { it.readText() }
            throw WepinError(responseCode, errorBody)
        }
    }

    inline fun <reified T> getRequest(url: String, headers: Map<String, String>): T {
        return httpRequest(url, "GET", headers)
    }

    inline fun <reified T> patchRequest(url: String, body: String, headers: Map<String, String>): T {
        return httpRequest(url, "PATCH", headers, body)
    }

    inline fun <reified T> postRequest(url: String, body: String, headers: Map<String, String>): T {
        return httpRequest(url, "POST", headers, body)
    }

    suspend fun register(params: RegisterRequest): RegisterResponse {
        val url = "${wepinBaseUrl}app/register"
        val jsonRequestBody = Gson().toJson(params)
        return postRequest(url, jsonRequestBody, setHeaders())
    }

    suspend fun changePin(params: ChangePinRequest): ChangePinResponse {
        val url = "${wepinBaseUrl}wallet/pin/change"
        val jsonRequestBody = Gson().toJson(params)
        return patchRequest(url, jsonRequestBody, setHeaders())
    }

    suspend fun sign(params: SignRequest): SignResponse {
        val url = "${wepinBaseUrl}tx/sign"
        val jsonRequestBody = Gson().toJson(params)
        return postRequest(url, jsonRequestBody, setHeaders())
    }

    suspend fun getAppAccountList(params: GetAccountListRequest): GetAccountListResponse {
        val gson = Gson()
        val jsonRequestQuery = gson.toJson(params)
            .replace("{", "")
            .replace("}", "")
            .replace("\"", "")
            .replace(":", "=")
            .replace(",", "&")

        // URL 생성
        val url = "${wepinBaseUrl}account?$jsonRequestQuery"
        return getRequest(url, setHeaders())
    }
}


class WepinError(code: Int, message: String) : Exception("WepinError: $code, $message")
