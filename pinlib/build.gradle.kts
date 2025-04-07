plugins {
    id("maven-publish")
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
//    alias(libs.plugins.maven.publish)
}

val sdkVersion = project.findProperty("wepinAndroidSdkVersion") ?: "LOCAL-SNAPSHOT"
rootProject.extra["wepinAndroidSdkVersion"] = sdkVersion

android {
    namespace = "com.wepin.android.pinLib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        buildConfigField(
            "String",
            "LIBRARY_VERSION",
            "\"${rootProject.extra["wepinAndroidSdkVersion"]}\""
        )
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    //Wepin
//    api(project(":libs:common:commonLib"))
//    implementation(project(":libs:modal:modalLib"))
//    implementation(project(":libs:network:networkLib"))
//    implementation(project(":libs:storage:storageLib"))
//    implementation(project(":libs:session:sessionLib"))
//    api(project(":libs:login:loginLib"))

    api("io.wepin:wepin-android-sdk-common-v1:${sdkVersion}")
    implementation("io.wepin:wepin-android-sdk-network-v1:${sdkVersion}")
    implementation("io.wepin:wepin-android-sdk-modal-v1:${sdkVersion}")
    implementation("io.wepin:wepin-android-sdk-storage-v1:${sdkVersion}")
    implementation("io.wepin:wepin-android-sdk-session-v1:${sdkVersion}")
    api("com.github.WepinWallet:wepin-android-sdk-login-v1:v1.1.0-test2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

//mavenPublishing {
////    publishToMavenCentral(SonatypeHost.DEFAULT)
//    // or when publishing to https://s01.oss.sonatype.org
////    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
//    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
//
//    signAllPublications()
//    coordinates(
//        "io.wepin",
//        "wepin-android-sdk-pin-v1",
//        "${rootProject.extra["wepinAndroidSdkVersion"]}"
//    )
//
//    pom {
//        name.set(project.name)
//        description.set("Android common library for Wepin SDK")
//        inceptionYear.set("2025")
//        url.set("https://github.com/WepinWallet/wepin-android-sdk-pin-v1")
//        licenses {
//            license {
//                name.set("The Apache License, Version 2.0")
//                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//            }
//        }
//        developers {
//            developer {
//                id.set("IoTrust")
//                name.set("wepin.dev")
////                url.set("https://github.com/WepinWallet/wepin-android-sdk-login-v1")
//            }
//        }
//        scm {
////            url.set("https://github.com/WepinWallet/wepin-android-sdk-login-v1/")
////            connection.set("scm:git:git://github.com/WepinWallet/wepin-android-sdk-login-v1")
////            developerConnection.set("scm:git:ssh://git@github.com/WepinWallet/wepin-android-sdk-login-v1")
//        }
//    }
//}