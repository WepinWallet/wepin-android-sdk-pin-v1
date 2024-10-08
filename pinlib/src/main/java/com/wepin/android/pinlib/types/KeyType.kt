package com.wepin.android.pinlib.types

enum class KeyType(val value: String) {
    DEV("ak_dev"),
    STAGE("ak_test"),
    PROD("ak_live");

    companion object {
        fun fromAppKey(appKey: String): KeyType? {
            return if(appKey.startsWith(DEV.value, false)) DEV
            else if(appKey.startsWith(STAGE.value, false)) STAGE
            else if(appKey.startsWith(PROD.value, false)) PROD
            else null
        }
    }
}