package com.wepin.android.pinlib.utils

import android.content.pm.PackageManager
import com.wepin.android.pinlib.BuildConfig

fun getVersionMetaDataValue(): String {
    try {
        return BuildConfig.LIBRARY_VERSION
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return ""
}