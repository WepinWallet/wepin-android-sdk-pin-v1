package com.wepin.android.pinlib.error

import org.json.JSONObject

class WepinError : Exception {
    var errorReason: JSONObject? = null
    var errorMessage: String? = null
    var code: Int = 0

    constructor(code: Int, errorDescription: String) : super(errorDescription) {
        this.code = code
        this.errorMessage = errorDescription
    }

    fun setErrorCode(errCode: Int) {
        this.code = errCode
    }
    fun getErrorCode(): Int = code

    companion object {
        enum class ErrorCode {
            INVALID_APP_KEY,
            INVALID_PARAMETER,
            INVALID_TOKEN,
            INVALID_LOGIN_SESSION,
            NOT_INITIALIZED_ERROR,
            ALREADY_INITIALIZED_ERROR,
            NOT_ACTIVITY,
            USER_CANCELLED,
            UNKNOWN_ERROR,
            NOT_CONNECTED_INTERNET,
            FAILED_LOGIN,
            ALREADY_LOGOUT,
            NOT_INITIALIZED_NETWORK
        }

        val USER_CANCELED = generalEx(ErrorCode.USER_CANCELLED, "User cancelled")
        val INVALID_APP_KEY = generalEx(ErrorCode.INVALID_APP_KEY, "Invalid app key")
        val INVALID_PARAMETER = generalEx(ErrorCode.INVALID_PARAMETER, "Invalid parameter")
        val INVALID_TOKEN = generalEx(ErrorCode.INVALID_TOKEN, "Token does not exist")
        val INVALID_LOGIN_SESSION = generalEx(ErrorCode.INVALID_LOGIN_SESSION, "Invalid Login Session")
        val NOT_INITIALIZED_ERROR = generalEx(ErrorCode.NOT_INITIALIZED_ERROR, "Not initialized Error")
        val ALREADY_INITIALIZED_ERROR = generalEx(ErrorCode.ALREADY_INITIALIZED_ERROR, "Already initialized")
        val NOT_ACTIVITY = generalEx(ErrorCode.NOT_ACTIVITY, "Context is not activity")
        val NOT_CONNECTED_INTERNET = generalEx(ErrorCode.NOT_CONNECTED_INTERNET, "No internet connection")
        val FAILED_LOGIN = generalEx(ErrorCode.FAILED_LOGIN, "Failed Oauth log in")
        val NOT_INITIALIZED_NETWORK = generalEx(ErrorCode.NOT_INITIALIZED_NETWORK, "Network Manager not initialized.")
        val ALREADY_LOGOUT = generalEx(ErrorCode.ALREADY_LOGOUT, "Already logged out")
        val UNKNOWN_ERROR = generalEx(ErrorCode.UNKNOWN_ERROR, "UnKnown Error")

        fun generalEx(errorCode: ErrorCode, message: String): WepinError {
            return WepinError(errorCode.ordinal, message)
        }

        fun generalUnKnownEx(message: String?): WepinError {
            return WepinError(ErrorCode.UNKNOWN_ERROR.ordinal, message ?: "UnKnown error")
        }

        // Noti : Webview의 Error Message를 WepinError로 변환
        fun mapWebviewErrorToWepinError(errorMessage: String): WepinError {
            return when (errorMessage) {
                "User Cancel" -> USER_CANCELED
                "Invalid App Key" -> INVALID_APP_KEY
                "Invalid Parameter" -> INVALID_PARAMETER
                "Invalid Login Session" -> INVALID_LOGIN_SESSION
                "Not Initialized" -> NOT_INITIALIZED_ERROR
                "Already Initialized" -> ALREADY_INITIALIZED_ERROR
                "Network Error" -> NOT_CONNECTED_INTERNET
                "Failed Login" -> FAILED_LOGIN
                else -> generalUnKnownEx(errorMessage)
            }
        }
    }
}
