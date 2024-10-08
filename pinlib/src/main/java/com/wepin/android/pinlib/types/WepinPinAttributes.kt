package com.wepin.android.pinlib.types

data class WepinPinAttributes(
    var defaultLanguage: String? = "en"
) {
    init {
        if (defaultLanguage !in listOf("ko", "ja", "en")) {
            defaultLanguage = "en" // "ko", "ja", "en" 외의 값은 기본 "en"으로
        }
    }
}

