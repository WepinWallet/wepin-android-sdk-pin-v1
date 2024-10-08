package com.wepin.android.pinlib.types

object WepinPinError {

    fun getError(errorCode: ErrorCode, message: String?=null): String {
        return when (errorCode) {
            ErrorCode.INVALID_APP_KEY -> {
                "Invalid app key"
            }
            ErrorCode.INVALID_PARAMETER -> {
                "Invalid parameter"
            }
            ErrorCode.INVALID_TOKEN -> {
                "Token does not exist"
            }
            ErrorCode.INVALID_LOGIN_PROVIDER -> {
                "Invalid login provider"
            }
            ErrorCode.NOT_INITIALIZED_ERROR -> {
                "Not initialized error"
            }
            ErrorCode.ALREADY_INITIALIZED_ERROR -> {
                "Already Initialized"
            }
            ErrorCode.NOT_ACTIVITY -> {
                "Context is not activity"
            }
            ErrorCode.USER_CANCELLED -> {
                "User Cancelled"
            }
            ErrorCode.UNKNOWN_ERROR -> {
                "unknown error: $message"
            }
            ErrorCode.NOT_CONNECTED_INTERNET -> {
                "No internet connection"
            }
            ErrorCode.FAILED_LOGIN -> {
                "Failed Oauth log in"
            }
            ErrorCode.ALREADY_LOGOUT -> {
                "already logout"
            }
            ErrorCode.INVALID_EMAIL_DOMAIN -> {
                "Invalid email domain"
            }
            ErrorCode.INVALID_LOGIN_SESSION -> {
                "Invalid Login Session"
            }
            ErrorCode.FAILED_SEND_EMAIL -> {
                "Failed to send email"
            }
            ErrorCode.REQUIRED_EMAIL_VERIFIED -> {
                "Email verification required"
            }
            ErrorCode.INCORRECT_EMAIL_FORM -> {
                "Incorrect email format"
            }
            ErrorCode.INCORRECT_PASSWORD_FORM -> {
                "Incorrect password format"
            }
            ErrorCode.NOT_INITIALIZED_NETWORK -> {
                "Network Manager not initialized."
            }
            ErrorCode.REQUIRED_SIGNUP_EMAIL -> {
                "Email sign-up required"
            }
            ErrorCode.FAILED_EMAIL_VERIFIED -> {
                "Failed to verify email."
            }
            ErrorCode.FAILED_PASSWORD_SETTING -> {
                "Failed to set password"
            }
            ErrorCode.EXISTED_EMAIL -> {
                "Email already exists"
            }
//            ErrorCode.NOUSERFOUND -> {
//                "No user found, please login again!"
//            }
//            ErrorCode.ENCODING_ERROR -> {
//                "Encoding Error"
//            }
//            ErrorCode.DECODING_ERROR -> {
//                "Decoding Error"
//            }
//            ErrorCode.SOMETHING_WENT_WRONG -> {
//                "Something went wrong!"
//            }
//            ErrorCode.RUNTIME_ERROR -> {
//                "Runtime Error"
//            }
//
//            ErrorCode.INVALID_LOGIN -> {
//                "Invalid Login"
//            }
        }
    }
}
enum class ErrorCode {
    INVALID_APP_KEY,
    INVALID_PARAMETER,
    INVALID_LOGIN_PROVIDER,
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
    INVALID_EMAIL_DOMAIN,
    FAILED_SEND_EMAIL,
    REQUIRED_EMAIL_VERIFIED,
    INCORRECT_EMAIL_FORM,
    INCORRECT_PASSWORD_FORM,
    NOT_INITIALIZED_NETWORK,
    REQUIRED_SIGNUP_EMAIL,
    FAILED_EMAIL_VERIFIED,
    FAILED_PASSWORD_SETTING,
    EXISTED_EMAIL,
//    ENCODING_ERROR,
//    DECODING_ERROR,
//    RUNTIME_ERROR,
//    APP_CANCELLED,
//    SOMETHING_WENT_WRONG,
//    INVALID_LOGIN,
}