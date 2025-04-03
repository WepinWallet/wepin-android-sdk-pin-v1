package com.wepin.android.pinlib.utils

import com.wepin.android.commonlib.error.WepinError
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

internal fun <T> handleJsonResult(
    result: String,
    subCommand: String,
    completableFuture: CompletableFuture<T>
): Boolean {
    val jsonResult = JSONObject(result)
    val command = jsonResult.getJSONObject("body").getString("command")
    val state = jsonResult.getJSONObject("body").getString("state")

    return if (command == subCommand) {
        if (state.equals("SUCCESS", true)) {
            true
        } else {
            val data = jsonResult.getJSONObject("body").getString("data")
            completableFuture.completeExceptionally(WepinError.generalUnKnownEx(data))
            false
        }
    } else {
        val errMsg = "Unexpected command: command=$command, expected=$subCommand"
        completableFuture.completeExceptionally(WepinError.generalUnKnownEx(errMsg))
        false
    }
}
