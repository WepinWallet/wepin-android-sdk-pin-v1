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

Wepin Pin Pad library for Android. This package is exclusively available for use in Android environments.

## ⏩ Get App ID and Key

After signing up for [Wepin Workspace](https://workspace.wepin.io/), navigate to the development tools menu, and enter the required information for each app platform to receive your App ID and App Key.

## ⏩ Requirements

- **Android**: API version **24** or newer is required.

## ⏩ Install
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
    > **<span style="font-size: 35px;"> !!Caution!! </span>**  We recommend using [the latest released version of the SDK](https://github.com/WepinWallet/wepin-android-sdk-pin-v1/releases)


## ⏩ Import SDK

```kotlin
  import com.wepin.android.pinlib.WepinPin;
```

## ⏩ Initialize

```kotlin
    val wepinPinParams =  WepinPinParams(
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
  - `defaultLanguage` \<String> - The language to be displayed on the widget (default: 'en'). Currently, only 'ko', 'en', and 'ja' are supported.

#### Returns
- CompletableFuture\<Boolean>
  -  Returns `true` if success

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

- \<Boolean> - Returns `true` if  Wepin PinPad Libarary is already initialized, otherwise false.

### changeLanguage

```kotlin
wepinPin.changeLanguage("ko")
```

The `changeLanguage()` method changes the language of the widget.

#### Parameters
- `language` \<String> - The language to be displayed on the widget. Currently, only 'ko', 'en', and 'ja' are supported.

#### Returns
- CompletableFuture\<Boolean>
  -  Returns `true` if success

#### Example

```kotlin
    wepinPin.changeLanguage("ko").whenComplete{ res, err ->
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
     - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are used in order.
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
wepinPin.generateAuthPINBlock()
```
Generates a pin block for authentication.

#### Parameters
 - void
   
#### Returns
 - CompletableFuture\<AuthPinBlock>
   - uvdList: \<List<EncUVD>> - Encypted pin list
     - b64Data \<String> - Data encrypted with the original key in b64SKey
     - b64SKey \<String> - A key that encrypts data encrypted with the wepin's public key.
     - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are used in order
   - otp \<String> - __optional__ If OTP authentication is required, include the OTP.

#### Example
```kotlin
    wepinPin.generateAuthPINBlock().whenComplete { res, err ->
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
     - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are used in order
   - newUVD: \<EncUVD> - New encrypted PIN
     - b64Data \<String> - Data encrypted with the original key in b64SKey
     - b64SKey \<String> - A key that encrypts data encrypted with the wepin's public key.
     - seqNum \<Int> - __optional__ Values to check for when using PIN numbers to ensure they are used in order
   - hint: \<EncPinHint> - Hints in the encrypted PIN
     - data \<String> - Encrypted hint data
     - length \<String> - The length of the hint
     - version \<Int> - The version of the hint
   - otp \<String> - __optional__ If OTP authentication is required, include the OTP.

#### Example
```kotlin
    wepinPin.generateChangePINBlock().whenComplete { res, err ->
        if (err == null) {
            changePin = ChangePinBlock(uvd = res!!.uvd, newUVD = res.newUVD, hint = res.hint, otp = res.otp)
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
   - code \<String> - __optional__ The OTP entered by the user.

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
 - void

#### Example
```kotlin
wepinPin.finalize()
```
