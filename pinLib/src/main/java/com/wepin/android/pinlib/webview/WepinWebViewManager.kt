package com.wepin.android.pinlib.webview

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.wepin.android.commonlib.error.WepinError.Companion.mapWebviewErrorToWepinError
import com.wepin.android.core.utils.Log
import com.wepin.android.modal.WepinModal
import com.wepin.android.pinlib.webview.JSProcessor.processRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

internal class WepinWebViewManager(platformType: String, widgetUrl: String) {
    private val TAG = this.javaClass.name
    private var _wepinModal: WepinModal = WepinModal(platformType)
    private var _widgetUrl: String = widgetUrl
    private var _currentWepinRequest: Map<String, Any?>? = null
    var _responseDeferred: CompletableDeferred<String>? = null
    var _responseWepinUserDeferred: CompletableDeferred<Boolean>? = null


    fun getResponseDeferred(): CompletableDeferred<String>? {
        return _responseDeferred
    }

    fun getResponseWepinUserDeferred(): CompletableDeferred<Boolean>? {
        return _responseWepinUserDeferred
    }

    fun resetResponseWepinUserDeferred() {
        if (_responseWepinUserDeferred != null)
            _responseWepinUserDeferred?.cancel("Resetting deferred instance")
        _responseWepinUserDeferred = CompletableDeferred<Boolean>()
    }

    fun getCurrentWepinRequest(): Map<String, Any?>? {
        return _currentWepinRequest
    }

    fun openWidgetWithCommand(
        context: Context,
        command: String,
        parameter: Any?
    ): CompletableFuture<Any> {
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

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            _wepinModal.openModal(context, _widgetUrl, ::processRequest)
        }

        _responseDeferred = CompletableDeferred<String>()

        _responseDeferred!!.invokeOnCompletion { throwable ->
            if (throwable != null) {
                Log.e(TAG, "Error occurred: ${throwable.message}")
                completableFuture.completeExceptionally(throwable)
            } else {
                try {
                    val result = _responseDeferred!!.getCompleted()
                    val jsonResult = JSONObject(result)
                    val state = jsonResult.getJSONObject("body").getString("state")

                    if (state.equals("ERROR", true)) {
                        val errorMessage =
                            jsonResult.getJSONObject("body").optString("data", "UnKnown error")
                        val mappedError = mapWebviewErrorToWepinError(errorMessage)
                        completableFuture.completeExceptionally(mappedError)
                    } else {
                        completableFuture.complete(result)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception occurred: ${e.message}")
                    completableFuture.completeExceptionally(e)
                }
            }
            _currentWepinRequest = null
        }
        return completableFuture

    }

    fun openWidget(context: Context) {
        _wepinModal.openModal(context, _widgetUrl, ::processRequest)
    }

    fun closeWidget() {
        _currentWepinRequest = null
        _wepinModal.closeModal()
    }
}