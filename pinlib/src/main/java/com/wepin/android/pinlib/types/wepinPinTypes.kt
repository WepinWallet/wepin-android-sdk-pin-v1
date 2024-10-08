package com.wepin.android.pinlib.types

data class EncUVD(
    val seqNum: Int?,
    val b64SKey: String,
    val b64Data: String
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): EncUVD {
            return EncUVD(
                seqNum = (json["seqNum"] as? Number)?.toInt(),  // Number로 받아 Int로 변환
                b64SKey = json["b64SKey"] as? String ?: throw IllegalArgumentException("b64SKey is required"),
                b64Data = json["b64Data"] as? String ?: throw IllegalArgumentException("b64Data is required")
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "seqNum" to seqNum,
            "b64SKey" to b64SKey,
            "b64Data" to b64Data
        )
    }

    override fun toString(): String {
        return "EncUVD(seqNum: $seqNum, b64SKey: $b64SKey, b64Data: $b64Data)"
    }
}

data class EncPinHint(
    val version: Int,
    val length: String,
    val data: String
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): EncPinHint {
            return EncPinHint(
                version = (json["version"] as? Number)?.toInt() ?: throw IllegalArgumentException("version is required"),
                length = json["length"] as? String ?: throw IllegalArgumentException("length is required"),
                data = json["data"] as? String ?: throw IllegalArgumentException("data is required")
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "version" to version,
            "length" to length,
            "data" to data
        )
    }

    override fun toString(): String {
        return "EncPinHint(version: $version, length: $length, data: $data)"
    }
}

data class ChangePinBlock(
    val uvd: EncUVD,
    val newUVD: EncUVD,
    val hint: EncPinHint,
    val otp: String?
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): ChangePinBlock {
            return ChangePinBlock(
                uvd = EncUVD.fromJson(json["UVD"] as Map<String, Any?>),
                newUVD = EncUVD.fromJson(json["newUVD"] as Map<String, Any?>),
                hint = EncPinHint.fromJson(json["hint"] as Map<String, Any?>),
                otp = json["otp"] as? String
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "UVD" to uvd.toJson(),
            "newUVD" to newUVD.toJson(),
            "hint" to hint.toJson(),
            "otp" to otp
        )
    }

    override fun toString(): String {
        return "ChangePinBlock(uvd: $uvd, newUVD: $newUVD, hint: $hint, otp: $otp)"
    }
}

data class RegistrationPinBlock(
    val uvd: EncUVD,
    val hint: EncPinHint
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): RegistrationPinBlock {
            return RegistrationPinBlock(
                uvd = EncUVD.fromJson(json["UVD"] as Map<String, Any?>),
                hint = EncPinHint.fromJson(json["hint"] as Map<String, Any?>)
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "UVD" to uvd.toJson(),
            "hint" to hint.toJson()
        )
    }

    override fun toString(): String {
        return "RegistrationPinBlock(uvd: $uvd, hint: $hint)"
    }
}

data class AuthOTP(
    val code: String
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): AuthOTP {
            return AuthOTP(
                code = json["code"] as? String ?: throw IllegalArgumentException("code is required")
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "code" to code
        )
    }

    override fun toString(): String {
        return "AuthOTP(code: $code)"
    }
}

data class AuthPinBlock(
    val uvdList: List<EncUVD>,
    val otp: String?
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): AuthPinBlock {
            return AuthPinBlock(
                uvdList = (json["UVDs"] as? List<Map<String, Any?>>)
                    ?.map { EncUVD.fromJson(it) }
                    ?: throw IllegalArgumentException("UVDs are required"),
                otp = json["otp"] as? String
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "UVDs" to uvdList.map { it.toJson() },
            "otp" to otp
        )
    }

    override fun toString(): String {
        return "AuthPinBlock(uvdList: $uvdList, otp: $otp)"
    }
}
