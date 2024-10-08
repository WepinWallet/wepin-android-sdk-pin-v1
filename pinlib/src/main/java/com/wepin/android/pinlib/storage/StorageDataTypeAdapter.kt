package com.wepin.android.pinlib.storage

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.wepin.android.pinlib.types.StorageDataType
import com.wepin.android.pinlib.utils.Log
import java.lang.reflect.Type

class StorageDataTypeAdapter : JsonDeserializer<StorageDataType> {
    private val TAG = this.javaClass.name

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): StorageDataType {
        Log.i(TAG,"deserialize")
        val jsonObject = json.asJsonObject

        return when {
            jsonObject.has("idToken") && jsonObject.has("refreshToken") && jsonObject.has("provider") -> {
                context.deserialize(jsonObject, StorageDataType.FirebaseWepin::class.java)
            }
            jsonObject.has("provider") && !jsonObject.has("idToken") -> {
                context.deserialize(jsonObject, StorageDataType.OauthProviderPending::class.java)
            }
            jsonObject.has("value") -> {
                context.deserialize(jsonObject, StorageDataType.StringValue::class.java)
            }
            jsonObject.has("accessToken") && jsonObject.has("refreshToken") -> {
                context.deserialize(jsonObject, StorageDataType.WepinToken::class.java)
            }
            jsonObject.has("status") && jsonObject.has("userInfo") -> {
                context.deserialize(jsonObject, StorageDataType.UserInfo::class.java)
            }
            jsonObject.has("locale") && jsonObject.has("currency") -> {
                context.deserialize(jsonObject, StorageDataType.AppLanguage::class.java)
            }
            jsonObject.has("loginStatus") && jsonObject.has("pinRequired") -> {
                context.deserialize(jsonObject, StorageDataType.UserStatus::class.java)
            }
            jsonObject.has("addresses") -> {
                context.deserialize(jsonObject, StorageDataType.WepinProviderSelectedAddress::class.java)
            }
            else -> throw JsonParseException("Unknown type")
        }
    }
}