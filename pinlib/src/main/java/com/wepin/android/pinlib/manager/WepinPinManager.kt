package com.wepin.android.pinlib.manager

import WepinPinWebviewDialog
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.wepin.android.pinlib.error.WepinError
import com.wepin.android.pinlib.network.WepinNetworkManager
import com.wepin.android.pinlib.types.KeyType
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.utils.Log
import com.wepin.android.pinlib.utils.getVersionMetaDataValue
import com.wepin.android.pinlib.webview.JSProcessor
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.CompletableFuture

class WepinPinManager() {
    private val TAG = this.javaClass.name
    private var _appContext: Context?  = null
    private var _webView: WebView? = null
    private var _webViewUrl: String = ""
    private var _appKey: String? = null
    private var _appId: String? = null
    private var _packageName: String? = null
    private var _version: String? = null
    var wepinNewtorkManager: WepinNetworkManager? = null
    private var _attributes: WepinPinAttributes? = WepinPinAttributes()
    private var _currentWepinRequest: Map<String, Any?>? = null
    private var _wepinWebviewDialog: WepinPinWebviewDialog? = null
    var _responseDeferred: CompletableDeferred<String>? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var _instance: WepinPinManager? = null
        fun getInstance(): WepinPinManager {
            if (null == _instance) {
                _instance = WepinPinManager()
            }
            return _instance as WepinPinManager
        }
    }

    fun init(context:Context, appKey: String, appId: String, attributes: WepinPinAttributes?) {
        Log.i(TAG, "init")
        try {
            _appContext = context
            _version = getVersionMetaDataValue()
            _packageName = (context as Activity).packageName
            _appKey = appKey
            _appId = appId
            if( attributes != null ){
                _attributes = attributes
            }
            wepinNewtorkManager = WepinNetworkManager(_appContext as Activity, _appKey!!, _packageName!!, _version!!)
            initWebView()
        }catch (e: Exception){
            throw WepinError.generalUnKnownEx(e.message)
        }
    }

    private fun initWebView() {
        Log.i(TAG, "initWebView")

        try {
            _webView = WebView(_appContext!!)
            _webView?.apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true

                webViewClient = object : WebViewClient() {

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(TAG,"onReceivedError : $error")
                        super.onReceivedError(view, request, error)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG,"onPageFinished url : $url")
                    }

                    override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                        // SSL 오류 무시
                        Log.e(TAG,"onReceivedSslError : $error")
                        handler?.proceed()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        Log.d(TAG,"onCloseWindow")
                    }
                }

                // JavaScript 인터페이스 추가
                addJavascriptInterface(JSInterface(this), "root")

                // PIN 입력 웹 페이지 로드
                setWebViewUrl() // PIN 입력을 위한 웹 페이지 URL 로드
                _wepinWebviewDialog = WepinPinWebviewDialog(_appContext!!, _webView!!)
            }
        }catch (e: Exception){
            throw WepinError.generalUnKnownEx(e.message)
        }

    }

    private fun setWebViewUrl() {
        Log.i(TAG, "setWebViewUrl")
        _webViewUrl = when (KeyType.fromAppKey(_appKey!!)) {
            KeyType.DEV -> {
                "https://dev-v1-widget.wepin.io/"
                //"https://192.168.0.89:8989/"
            }

            KeyType.STAGE -> {
                "http://stage-v1-widget.wepin.io/"
            }

            KeyType.PROD -> {
                "https://v1-widget.wepin.io/"
            }

            else -> {
                throw WepinError.INVALID_APP_KEY
            }
        }
        Log.d(TAG,"webview loadUrl : $_webViewUrl")
    }

    fun getVersion() : String?{
        Log.i(TAG, "getVersion")
        return _version
    }

    fun getAppKey() : String?{
        Log.i(TAG, "getAppKey")
        return this._appKey
    }

    fun getAppId() : String?{
        Log.i(TAG, "getAppId")
        return this._appId
    }

    fun getPackageName(): String? {
        Log.i(TAG, "getPackageName")
        return _packageName!!
    }

    fun getWepinAttributes(): WepinPinAttributes {
        Log.i(TAG, "getWepinAttributes")
        return _attributes!!
    }

    fun getWebView(): WebView {
        Log.i(TAG, "getWebView")
        return _webView!!
    }

    fun finalizeWebivew(){
        Log.i(TAG, "finalizeWebivew")
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            _webView!!.destroy()
            _wepinWebviewDialog!!.dismiss()
            _webView = null
            _wepinWebviewDialog = null
        }
    }

    fun openAndRequestWepinWidgetAsync(command: String, parameter: Any?): CompletableFuture<Any> {
        Log.i(TAG, "openAndRequestWepinWidgetAsync [command] : $command [param] : ${parameter.toString()}")
        val completableFuture = CompletableFuture<Any>()
        val id = System.currentTimeMillis()
        val finalParameter = parameter ?: emptyMap<String, Any?>()
        _currentWepinRequest = mapOf(
            "header" to mapOf(
                "request_from" to "native",
                "request_to" to "wepin_widget",
                "id" to id
            ),
            "body" to mapOf(
                "command" to command,
                "parameter" to finalParameter
            )
        )

        Log.d(TAG, "currentWepinRequest : ${_currentWepinRequest.toString()}")
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            initWebView()
            _webView!!.loadUrl(_webViewUrl)
            _wepinWebviewDialog!!.show()
        }

        _responseDeferred = CompletableDeferred<String>()

        _responseDeferred!!.invokeOnCompletion { throwable ->
            if (throwable != null) {
                Log.e(TAG,"Error occurred: ${throwable.message}")
                completableFuture.completeExceptionally(throwable)
            } else {
                try {
                    val result = _responseDeferred!!.getCompleted()
                    Log.d(TAG, "_responseDeferred result : $result")
                    completableFuture.complete(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Error occurred: ${e.message}")
                    completableFuture.completeExceptionally(e)
                }
            }
            _currentWepinRequest = null
        }
        return completableFuture
    }

    fun getCurrentWepinRequest(): Map<String, Any?>? {
        return _currentWepinRequest
    }

    fun getCurrentDeffered(): CompletableDeferred<String>? {
        return _responseDeferred
    }


    class JSInterface(private val webView: WebView) {
        private val TAG = this.javaClass.name
        private val _jsProcessor: JSProcessor = JSProcessor()

        @JavascriptInterface
        fun onLoad() {
            Log.d(TAG,"onLoad")
        }

        @JavascriptInterface
        fun showToastMsg(msg: String, isDebug: Boolean) {
            Log.d(TAG,"showToastMsg >> $msg")
        }

        @JavascriptInterface
        fun onNativeEventResponse(response: String, msg: String) {
            Log.d(TAG,"onNativeEventResponse >> $response")
            Log.d(TAG,"onNativeEventResponse msg>> $msg")
        }

        @JavascriptInterface
        fun getErrorType(): String? {
            Log.d(TAG, "getErrorType")
            return null
        }

        @JavascriptInterface
        fun onError(error: String) {
            Log.e(TAG, "onError : $error")
        }

        @JavascriptInterface
        fun post(request: String) {
            Log.d(TAG, "post :  $request")
            _jsProcessor.processRequest(request, this)
        }

        fun onResponse(response: String) {
            Log.d(TAG,"onResponse :  $response")
            callJavaScript("onResponse", response)
        }

        fun sendNativeEvent(requestCode: Int, event: String?, param: String?) {
            Log.d(TAG, "sendNativeEvent >>  requestCode: $requestCode")
            callJavaScript(
                "onNativeEvent",
                event!!,
                param!!
            )
        }
        internal fun callJavaScript(methodName: String, vararg params: Any) {
            Log.d(TAG,"callJavaScript")
            Log.d(TAG,"methodName: $methodName")

            val stringBuilder = StringBuilder().apply {
                append("javascript:try{")
                append(methodName)
                append("(")
                params.forEachIndexed { index, param ->
                    if (param is String) {
                        append("'")
                        append(param.replace("'", "\\'"))
                        append("'")
                    }
                    if (index < params.size - 1) {
                        append(",")
                    }
                }
                append(")}catch(error){root.onError(error.message);}")
            }

            Log.d(TAG,stringBuilder.toString())
            webView.post { webView.loadUrl(stringBuilder.toString()) }
        }
    }


}

