package com.wepin.android.pinlib.types

enum class Providers(val value: String) {
    GOOGLE("google"),
    APPLE("apple"),
    NAVER("naver"),
    DISCORD("discord"),
    EMAIL("email"),
    EXTERNAL_TOKEN("external_token");

    companion object {
        fun fromValue(value: String): Providers? {
            return entries.find { it.value == value }
        }
    }
}