plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

extra["libraryVersion"] = "0.0.1"

android {
    namespace = "com.wepin.android.pinlib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "LIBRARY_VERSION", "\"${project.extra["libraryVersion"]}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true // buildConfig 기능을 활성화합니다.
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    libraryVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = when (name) {
                "release" -> "wepin-login-v${project.extra["libraryVersion"]}.aar"
                "debug" -> "debug-wepin-login-v${project.extra["libraryVersion"]}.aar"
                else -> throw IllegalArgumentException("Unsupported build variant: $name")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // ECDSA
    implementation ("org.bitcoinj:bitcoinj-core:0.15.10")

    // Encoding
    implementation ("com.google.code.gson:gson:2.9.1")

    // AppAuth
    implementation ("net.openid:appauth:0.11.1")

    // Encrypted Storage
    implementation ("androidx.security:security-crypto-ktx:1.1.0-alpha03")

    // becrypt
    implementation ("org.mindrot:jbcrypt:0.4")

    // Volley
    implementation ("com.android.volley:volley:1.2.1")

    // JWT decode
    implementation ("com.auth0:java-jwt:3.18.2")
    implementation("androidx.window:window:1.3.0")


    // Jackson Databind for JSON serialization/deserialization
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // Jackson Annotations
    implementation ("com.fasterxml.jackson.core:jackson-annotations:2.15.2")

    // Jackson Kotlin module
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.WepinWallet"
                artifactId = "wepin-android-sdk-pin-v1"
            }
        }
    }
}

