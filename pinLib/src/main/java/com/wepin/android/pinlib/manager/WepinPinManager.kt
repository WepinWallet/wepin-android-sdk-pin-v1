package com.wepin.android.pinlib.manager

import android.content.Context
import com.wepin.android.commonlib.WepinCommon
import com.wepin.android.core.utils.getVersionMetaDataValue
import com.wepin.android.core.WepinCoreManager
import com.wepin.android.core.network.WepinNetwork
import com.wepin.android.core.session.WepinSessionManager
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.types.WepinPinParams
import com.wepin.android.pinlib.webview.WepinWebViewManager
import java.util.concurrent.CompletableFuture

internal class WepinPinManager {
    private val TAG = this.javaClass.name
    private var _appContext: Context? = null
    var appId: String? = null
    var appKey: String? = null
    var packageName: String? = null
    val version: String = getVersionMetaDataValue()
    var sdkType: String = ""
    var wepinAttributes: WepinPinAttributes? = null
    var wepinSessionManager: WepinSessionManager? = null
    var wepinNetwork: WepinNetwork? = null
    var wepinWebViewManager: WepinWebViewManager? = null

    companion object {
        @Volatile
        private var instance: WepinPinManager? = null

        fun getInstance(): WepinPinManager {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = WepinPinManager()
                    }
                }
            }
            return instance!!
        }

        fun clearInstance() {
            instance = null
        }
    }

    fun getResponseDeferred() = wepinWebViewManager?.getResponseDeferred()
    fun getResponseWepinUserDeferred() = wepinWebViewManager?.getResponseWepinUserDeferred()
    fun getCurrentWepinRequest() = wepinWebViewManager?.getCurrentWepinRequest()

    fun initialize(
        wepinPinParams: WepinPinParams,
        attributes: WepinPinAttributes?,
        platform: String = "android"
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        _appContext = wepinPinParams.context
        appId = wepinPinParams.appId
        appKey = wepinPinParams.appKey
        packageName = wepinPinParams.context.packageName
        sdkType = "$platform-pin"
        wepinAttributes = WepinPinAttributes(
            defaultLanguage = attributes?.defaultLanguage,
            defaultCurrency = attributes?.defaultCurrency
        )
        val urlInfo = WepinCommon.getWepinSdkUrl(appKey!!)

        WepinCoreManager.initialize(
            context = _appContext!!,
            appId = appId!!,
            appKey = appKey!!,
            platformType = platform,
            sdkType = sdkType
        )
            .thenApply {
                wepinNetwork = WepinCoreManager.getNetwork()
                wepinSessionManager = WepinCoreManager.getSession()

                wepinWebViewManager =
                    WepinWebViewManager("$platform-$sdkType", urlInfo["wepinWebview"] ?: "")

                future.complete(true)
            }.exceptionally { throwable ->
                future.completeExceptionally(throwable)
                null
            }
        return future
    }


    fun clear() {
        WepinCoreManager.clear()
        _appContext = null
        appId = null
        appKey = null
        packageName = null
        sdkType = ""
        wepinAttributes = null
        wepinNetwork = null
        wepinSessionManager = null
        wepinWebViewManager?.closeWidget()
        wepinWebViewManager = null
    }

    fun closeWebview() {
        wepinWebViewManager?.closeWidget()
    }
}