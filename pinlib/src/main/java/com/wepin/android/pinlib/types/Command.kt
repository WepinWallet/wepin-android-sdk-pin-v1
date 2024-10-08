package com.wepin.android.pinlib.types

interface Command {
    companion object {
        /**
         * Commands for JS processor
         */
        const val CMD_READY_TO_WIDGET: String = "ready_to_widget"
        const val CMD_GET_SDK_REQUEST: String = "get_sdk_request"
        const val CMD_CLOSE_WEPIN_WIDGET: String = "close_wepin_widget"
        const val CMD_SET_LOCAL_STORAGE: String = "set_local_storage"

        /**
         * Commands for get_sdk_request
         */
        const val CMD_SUB_PIN_REGISTER: String = "pin_register"  // only for creating wallet
        const val CMD_SUB_PIN_AUTH: String = "pin_auth" //
        const val CMD_SUB_PIN_CHANGE: String = "pin_change"
        const val CMD_SUB_PIN_OTP: String = "pin_otp"

    }
}