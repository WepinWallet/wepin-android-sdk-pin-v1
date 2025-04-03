package com.wepin.android.pinlib.manager

import android.content.Context
import com.wepin.android.commonlib.WepinCommon
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.networklib.WepinNetwork
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.types.WepinPinParams
import com.wepin.android.pinlib.utils.getVersionMetaDataValue
import com.wepin.android.pinlib.webview.WepinWebViewManager
import com.wepin.android.sessionlib.WepinSessionManager
import java.util.concurrent.CompletableFuture

internal class WepinPinManager {
    private val TAG = this.javaClass.name
    private var _appContext: Context? = null
    var appId: String? = null
    var appKey: String? = null
    var packageName: String? = null
    val version: String = getVersionMetaDataValue()
    lateinit var platformType: String
    lateinit var sdkType: String
    var wepinAttributes: WepinPinAttributes? = null
    var wepinSessionManager: WepinSessionManager? = null
    var wepinNetwork: WepinNetwork? = null
    var wepinWebViewManager: WepinWebViewManager? = null
    var loginLib: WepinLogin? = null

    companion object {
        private var instance: WepinPinManager? = null
        fun getInstance(): WepinPinManager {
            if (instance == null) {
                instance = WepinPinManager()
            }
            return instance!!
        }
    }

    fun getResponseDeferred() = wepinWebViewManager?.getResponseDeferred()
    fun getResponseWepinUserDeferred() = wepinWebViewManager?.getResponseWepinUserDeferred()
    fun getCurrentWepinRequest() = wepinWebViewManager?.getCurrentWepinRequest()

    fun initialize(
        wepinPinParams: WepinPinParams,
        attributes: WepinPinAttributes?,
        platform: String? = "android"
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        _appContext = wepinPinParams.context
        appId = wepinPinParams.appId
        appKey = wepinPinParams.appKey
        packageName = wepinPinParams.context.packageName
        platformType = platform ?: "android"
        sdkType = "${platform}-pin"
        wepinAttributes = WepinPinAttributes(
            defaultLanguage = attributes?.defaultLanguage,
            defaultCurrency = attributes?.defaultCurrency
        )
        val urlInfo = WepinCommon.getWepinSdkUrl(appKey!!)

        WepinNetwork.initialize(_appContext!!, appKey!!, packageName!!, sdkType, version)
            .thenApply { network ->
                wepinNetwork = network

                WepinSessionManager.initialize()
                wepinSessionManager = WepinSessionManager.getInstance()

                wepinWebViewManager =
                    WepinWebViewManager(platformType, urlInfo["wepinWebview"] ?: "")
                val wepinLoginOptions = WepinLoginOptions(
                    context = _appContext!!,
                    appId = wepinPinParams.appId,
                    appKey = wepinPinParams.appKey,
                )
                loginLib = WepinLogin(wepinLoginOptions)
                future.complete(true)
            }.exceptionally { throwable ->
                future.completeExceptionally(throwable)
                null
            }
        return future
    }

    fun finalize() {
        wepinNetwork?.finalize()
        wepinNetwork = null
        wepinSessionManager?.finalize()
        wepinSessionManager = null
        wepinWebViewManager?.closeWidget()
        wepinWebViewManager = null
    }

    fun closeWebview() {
        wepinWebViewManager?.closeWidget()
    }
}