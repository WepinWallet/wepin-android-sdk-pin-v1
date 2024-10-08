package com.wepin.android.pinlib.types

sealed class StorageDataType {
    data class FirebaseWepin(val provider: String, val idToken: String, val refreshToken: String): StorageDataType()
    data class OauthProviderPending(val provider: Providers): StorageDataType()
    data class StringValue(val value: String): StorageDataType()
    data class WepinToken(val accessToken: String, val refreshToken: String): StorageDataType()
    data class UserInfo(val status: String, val userInfo: UserInfoDetails?, val walletId: String?=null): StorageDataType()
    //data class AppLanguage(val locale: Locale, val currency: String?): StorageDataType()
    data class AppLanguage(val locale: String, val currency: String?): StorageDataType()
    data class UserStatus(val loginStatus: String, var pinRequired: Boolean?): StorageDataType()
    data class WepinProviderSelectedAddress(val addresses: List<SelectedAddress>): StorageDataType()
}

data class UserInfoDetails(
    val userId: String,
    val email: String,
    val provider: String,
    val use2FA: Boolean
)

enum class Locale {
    Ko, En
}

data class SelectedAddress(
    val userId: String,
    val address: String,
    val network: String
)