package com.wepin.android.pinlib.utils

import android.content.pm.PackageManager
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wepin.android.pinlib.BuildConfig
import com.wepin.android.pinlib.storage.StorageDataTypeAdapter
import com.wepin.android.pinlib.types.StorageDataType
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.math.BigInteger

//inline fun createCodeVerifier() : String{
//    val secureRandom = SecureRandom()
//    val bytes = ByteArray(64)
//    secureRandom.nextBytes(bytes)
//
//
//    val encoding = Base64.URL_SAFE or Base64.NO_PADDING  or Base64.NO_WRAP
//    return Base64.encodeToString(bytes, encoding)
//}
//
//inline fun createCodeChallenge(codeVerifier:String) : String {
//    val encoding = Base64.URL_SAFE or Base64.NO_PADDING  or Base64.NO_WRAP
//    val digest = MessageDigest.getInstance("SHA-256")
//    val hash = digest.digest(codeVerifier.toByteArray())
//    return Base64.encodeToString(hash, encoding)
//}

// BigInteger를 바이트 배열로 변환하고 앞의 00을 제거하는 함수
fun bigIntegerToByteArrayTrimmed(value: BigInteger): ByteArray {
    val hexChars = "0123456789ABCDEF"
    var byteArray = value.toByteArray()
    if (byteArray.isNotEmpty() && byteArray[0] == 0.toByte() && byteArray[1].toInt() < 0) {
        byteArray = byteArray.copyOfRange(1, byteArray.size)
    }
    return byteArray
}


fun decodeBase64(base64String: String): String {
    val decodeBytes = base64String.let { Base64.decode(it, 0) }
    return String(decodeBytes)
}

// LocalStorageData 인스턴스를 JSON 문자열로 변환
fun convertLocalStorageDataToJson(data: StorageDataType): String {
    val gson = Gson()
    return gson.toJson(data)
}

// JSON 문자열을 LocalStorageData 인스턴스로 변환
fun convertJsonToLocalStorageData(json: String): StorageDataType {
//    val gson = Gson()
    // LocalStorageData의 정확한 타입을 지정하기 위해 TypeToken을 사용

    val gson = GsonBuilder()
        .registerTypeAdapter(StorageDataType::class.java, StorageDataTypeAdapter())
        .create()

    return gson.fromJson(json, StorageDataType::class.java)
}

fun hashPassword(password: String): String {
    val BCRYPT_SALT = "\$2a\$10\$QCJoWqnN.acrjPIgKYCthu"
    return BCrypt.hashpw(password, BCRYPT_SALT)
}

inline fun <reified T> convertToJsonRequest(data: T): JSONObject {
    val gson = Gson()
    val jsonString = gson.toJson(data)
    return JSONObject(jsonString)
}

// JSONObject를 HashMap<String, String>으로 변환하는 함수
fun convertJSONObjectToHashMap(jsonObject: JSONObject): HashMap<String, String> {
    val map = HashMap<String, String>()
    val keys = jsonObject.keys()

    while (keys.hasNext()) {
        val key = keys.next()
        val value = jsonObject.getString(key)
        map[key] = value
    }

    return map
}

fun getVersionMetaDataValue(): String {
    try {
        return BuildConfig.LIBRARY_VERSION
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return ""
}

