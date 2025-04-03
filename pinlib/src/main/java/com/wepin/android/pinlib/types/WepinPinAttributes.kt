package com.wepin.android.pinlib.types

import android.content.Context
import com.wepin.android.commonlib.types.WepinAttribute

//typealias WepinPinAttributes = WepinAttribute

class WepinPinAttributes(defaultLanguage: String? = "en", defaultCurrency: String? = "USD") :
    WepinAttribute(defaultLanguage, defaultCurrency)

class WepinPinAttributeWithProviders(
    defaultLanguage: String? = "en",
    defaultCurrency: String? = "USD",
    var loginProviders: List<String> = emptyList()
) : WepinAttribute(defaultLanguage, defaultCurrency)


data class WepinWidgetParams(val context: Context, val appId: String, val appKey: String)