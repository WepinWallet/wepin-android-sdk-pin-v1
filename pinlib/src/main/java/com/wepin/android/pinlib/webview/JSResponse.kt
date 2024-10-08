package com.wepin.android.pinlib.webview

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.wepin.android.pinlib.types.WepinPinAttributes
import com.wepin.android.pinlib.utils.Log

data class JSResponse(
    @JsonProperty("header")
    val header: JSResponseHeader,

    @JsonProperty("body")
    val body: JSResponseBody
) {

    data class JSResponseHeader(
        @JsonProperty("id")
        val id: String,

        @JsonProperty("response_from")
        val response_from: String,

        @JsonProperty("response_to")
        val response_to: String
    )

    data class JSResponseBody(
        @JsonProperty("command")
        val command: String,

        @JsonProperty("state")
        var state: String,

        @JsonProperty("data")
        var data: Any? = null
    )

    class Builder(private val id: String, private val request_from: String, private val command: String) {
        private val response: JSResponse = JSResponse(
            header = JSResponseHeader(id, "native", request_from),
            body = JSResponseBody(command, "SUCCESS")
        )

        fun setReadyToWidgetData(
            appKey: String,
            appId: String,
            domain: String,
            platform: Int,
            type: String,
            version: String,
            attributes: WepinPinAttributes,
            localData: Any
        ): Builder {
            val data = ReadyToWidgetBodyData(
                appKey = appKey,
                appId = appId,
                domain = domain,
                platform = platform,
                type = type,
                version = version,
                localDate = localData,
                attributes = attributes
            )
            response.body.data = data
            return this
        }

        // 새로운 setBodyData 메서드 정의
        fun setBodyData(command: String, parameter: Map<String, Any?>): Builder {
            val data = mapOf(
                "command" to command,
                "parameter" to parameter
            )
            response.body.data = data
            return this
        }

        fun build(): JSResponse {
            return response
        }
    }

    override fun toString(): String {
        return try {
            Log.d("JSResponse","##### [Response] ${this.body.data.toString()}")
            ObjectMapper().writeValueAsString(this)
        } catch (e: JsonProcessingException) {
            e.message ?: "Error converting to JSON"
        }
    }

    data class ReadyToWidgetBodyData(
        @JsonProperty("appKey")
        val appKey: String,

        @JsonProperty("appId")
        val appId: String,

        @JsonProperty("domain")
        val domain: String,

        @JsonProperty("platform")
        val platform: Int,

        @JsonProperty("attributes")
        val attributes: WepinPinAttributes,

        @JsonProperty("type")
        val type: String = "android-pin",

        @JsonProperty("version")
        val version: String,

        @JsonProperty("localDate")
        val localDate: Any
    )
}
