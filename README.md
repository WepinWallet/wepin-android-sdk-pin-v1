<br/>

<p align="center">
  <a href="https://www.wepin.io/">
      <picture>
        <source media="(prefers-color-scheme: dark)">
        <img alt="wepin logo" src="https://github.com/WepinWallet/wepin-web-sdk-v1/blob/main/assets/wepin_logo_color.png?raw=true" width="250" height="auto">
      </picture>
</a>
</p>

<br>

# Wepin Android SDK PIN Pad Library v1

[![platform - android](https://img.shields.io/badge/platform-Android-3ddc84.svg?logo=android&style=for-the-badge)](https://www.android.com/)
[![SDK Version](https://img.shields.io/jitpack/version/com.github.WepinWallet/wepin-android-sdk-pin-v1.svg?logo=jitpack&style=for-the-badge)](https://jitpack.io/v/com.github.WepinWallet/wepin-android-sdk-pin-v1)

Wepin Pin Pad library for Android. This package is exclusively available for use in Android
environments.

## ⏩ Get App ID and Key

After signing up for [Wepin Workspace](https://workspace.wepin.io/), navigate to the development
tools menu, and enter the required information for each app platform to receive your App ID and App
Key.

## ⏩ Requirements

- **Android**: API version **24** or newer is required.

## ⏩ Install

> ⚠️ Important Notice for v1.0.0 Update
>
> 🚨 Breaking Changes & Migration Guide 🚨
>
> This update includes major changes that may impact your app. Please read the following carefully
> before updating.
>
> 🔄 Storage Migration
> • In rare cases, stored data may become inaccessible due to key changes.
> • Starting from v1.0.0, if the key is invalid, stored data will be cleared, and a new key will be
> generated automatically.
> • Existing data will remain accessible unless a key issue is detected, in which case a reset will
> occur.
> • ⚠️ Downgrading to an older version after updating to v1.0.0 may prevent access to previously
> stored data.
> • Recommended: Backup your data before updating to avoid any potential issues.
>
> 🆕 What's New in v1.1.0
> ✅ WepinPin now includes WepinLogin by default. • Starting from v1.1.0, the WepinLogin module is
> bundled within WepinPin.
> • You no longer need to install or manage WepinLogin separately when using WepinPin.
> • This simplifies integration and reduces dependency management for login-related features. • For
> consistent behavior, please ensure all Wepin modules used are updated to v1.1.0 or higher.

> 🔧 How to Disable Backup (Android)
>
> Modify your AndroidManifest.xml file:
>
>    ```xml
>    <application
>        android:allowBackup="false"
>        android:fullBackupContent="false">
>    ```
> 🔹 If android:allowBackup is true, the migration process may not work correctly, leading to
> potential data loss or storage issues.
>

1. Add JitPack repository in your project-level build gradle file

- kts
  ```kotlin
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
           maven("https://jitpack.io") // <= Add JitPack Repository
       }
   }
  ```

2. Add implementation in your app-level build gradle file

- kts
  ```
  dependencies {
    // ...
    implementation("com.github.WepinWallet:wepin-android-sdk-pin-v1:vX.X.X") 
  }
  ```
  > **<span style="font-size: 35px;"> !!Caution!! </span>**  We recommend
  using [the latest released version of the SDK](https://github.com/WepinWallet/wepin-android-sdk-pin-v1/releases)

## ⏩ Import SDK

```kotlin
  import com.wepin.android.pinlib.WepinPin;
```

## ⏩ Initialize

```kotlin
    val wepinPinParams = WepinPinParams(
    context = this,
    appId = "Wepin-App-ID",
    appKey = "Wepin-App-Key",
)
var WepinPin wepinPin = WepinPin(wepinPinParams)
```

### init

```kotlin
    val res = wepinPin.initialize(attributes)
```

#### Parameters

- `attributes` \<WepinPinAttributes>
    - `defaultLanguage` \<String> - The language to be displayed on the widget (default: 'en').
      Currently, only 'ko', 'en', and 'ja' are supported.

#### Returns

- CompletableFuture\<Boolean>
    - Returns `true` if success

#### Example

```kotlin
      var attributes = WepinPinAttributes("en")
val res = wepinPin.initialize(attributes)
res?.whenComplete { infResponse, error ->
    if (error == null) {
        println(infResponse)
    } else {
        println(error)
    }
}
```

### isInitialized

```kotlin
wepinPin.isInitialized()
```

The `isInitialized()` method checks if the Wepin PinPad Libarary is initialized.

#### Returns

- \<Boolean> - Returns `true` if Wepin PinPad Libarary is already initialized, otherwise false.

### changeLanguage

```kotlin
wepinPin.changeLanguage("ko")
```

The `changeLanguage()` method changes the language of the widget.

#### Parameters

- `language` \<String> - The language to be displayed on the widget. Currently, only 'ko', 'en',
  and 'ja' are supported.

#### Returns

- CompletableFuture\<Boolean>
    - Returns `true` if success

#### Example

```kotlin
    wepinPin.changeLanguage("ko").whenComplete { res, err ->
    if (err == null) {
        println(res)
    } else {
        println(err)
    }
}
```

## ⏩ Method & Variable

Methods and Variables can be used after initialization of Wepin PIN Pad Library.

### generateRegistrationPINBlock

```kotlin
wepinPin.generateRegistrationPINBlock()
```

Generates a pin block for registration.
This method should only be used when the loginStatus is pinRequired.

#### Parameters

- void

#### Returns

- CompletableFuture\<RegistrationPinBlock>
    - uvd: \<EncUVD> - Encrypted PIN
        - b64Data \<String> - Data encrypted with the original key in b64SKey
        - b64SKey \<String> - A key that encrypts data encrypted with the Wepin's public key.
        - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are
          used in order.
    - hint: \<EncPinHint> - Hints in the encrypted PIN.
        - data \<String> - Encrypted hint data.
        - length \<String> - The length of the hint
        - version \<Int> - The version of the hint

#### Example

```kotlin
    wepinPin.generateRegistrationPINBlock().whenComplete { res, err ->
    if (err == null) {
        registerPin = RegistrationPinBlock(uvd = res!!.uvd, hint = res!!.hint)
        // You need to make a Wepin RESTful API request using the received data.
    } else {
        println(err)
    }
}
```

### generateAuthPINBlock

```kotlin
wepinPin.generateAuthPINBlock(3)
```

Generates a pin block for authentication.

#### Parameters

- `count` \<Int> - __optional__ If multiple PIN blocks are needed, please enter the number to
  generate. If the count value is not provided, it will default to 1.

#### Returns

- CompletableFuture\<AuthPinBlock>
    - uvdList: \<List<EncUVD>> - Encypted pin list
        - b64Data \<String> - Data encrypted with the original key in b64SKey
        - b64SKey \<String> - A key that encrypts data encrypted with the wepin's public key.
        - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are
          used in order
    - otp \<String> - __optional__ If OTP authentication is required, include the OTP.

#### Example

```kotlin    
    wepinPin.generateAuthPINBlock(3).whenComplete { res, err ->
    if (err == null) {
        authPin = AuthPinBlock(uvdList = res!!.uvdList, otp = res!!.otp)
        // You need to make a Wepin RESTful API request using the received data.
    } else {
        println(err)
    }
}
```

### generateChangePINBlock

```kotlin
wepinPin.generateChangePINBlock()
```

Generate pin block for changing the PIN.

#### Parameters

- void

#### Returns

- CompletableFuture\<ChangePinBlock>
    - uvd: \<EncUVD> - Encrypted PIN
        - b64Data \<String> - Data encrypted with the original key in b64SKey
        - b64SKey \<String> - A key that encrypts data encrypted with the wepin's public key.
        - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are
          used in order
    - newUVD: \<EncUVD> - New encrypted PIN
        - b64Data \<String> - Data encrypted with the original key in b64SKey
        - b64SKey \<String> - A key that encrypts data encrypted with the wepin's public key.
        - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are
          used in order
    - hint: \<EncPinHint> - Hints in the encrypted PIN
        - data \<String> - Encrypted hint data
        - length \<String> - The length of the hint
        - version \<Int> - The version of the hint
    - otp \<String> - __optional__ If OTP authentication is required, include the OTP.

#### Example

```kotlin
    wepinPin.generateChangePINBlock().whenComplete { res, err ->
    if (err == null) {
        changePin =
            ChangePinBlock(uvd = res!!.uvd, newUVD = res.newUVD, hint = res.hint, otp = res.otp)
        // You need to make a Wepin RESTful API request using the received data.
    } else {
        println(err)
    }
}
```

### generateAuthOTP

```kotlin
wepinPin.generateAuthOTPCode()
```

generate OTP.

#### Parameters

- void

#### Returns

- CompletableFuture\<AuthOTP>
    - code \<String> - The OTP entered by the user.

#### Example

```kotlin
    wepinPin.generateAuthOTPCode().whenComplete { res, err ->
    if (err == null) {
        authOTPCode = AuthOTP(res!!.code)
        // You need to make a Wepin RESTful API request using the received data.
    } else {
        println(err)
    }
}
```

### finalize

```kotlin
wepinPin.finalize()
```

The `finalize()` method finalizes the Wepin PinPad Libarary.

#### Parameters

- void

#### Returns

- <Boolean>

#### Example

```kotlin
wepinPin.finalize()
```
