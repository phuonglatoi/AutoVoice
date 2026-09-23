plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // Bỏ dấu // nếu bạn dùng Kotlin
}

android {
    namespace = "com.example.autovoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.autovoice"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/AL2.0")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/io.netty.versions.properties")
        exclude("google/protobuf/*.proto")
        exclude("google/rpc/*.proto")
        exclude("google/api/*.proto")
        exclude("com/google/protobuf/*.proto")
        exclude("META-INF/services/com.google.protobuf.GeneratedExtensionRegistryLoader")
    }
}

dependencies {
    // AndroidX & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)

    // Unit Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.google.android.material:material:1.12.0") // Hoặc phiên bản mới hơn (quan trọng cho Material Components)


    // Đăng nhập bằng Google
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // UI CircleImage
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Mã hóa mật khẩu
    implementation("org.mindrot:jbcrypt:0.4")

    // Google Cloud Text-to-Speech & GRPC & Protobuf (FULL)
    implementation("com.google.cloud:google-cloud-texttospeech:2.13.0")
    implementation("io.grpc:grpc-okhttp:1.53.0")
    implementation("io.grpc:grpc-stub:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0") // Không dùng grpc-protobuf-lite
    implementation("com.google.protobuf:protobuf-java:3.21.12") // Dùng bản đầy đủ

    // Google Auth
    implementation("com.google.auth:google-auth-library-oauth2-http:1.16.0")
    implementation("com.google.auth:google-auth-library-credentials:1.16.0")

    // Multidex
    implementation("androidx.multidex:multidex:2.0.1")
}

configurations.all {
    resolutionStrategy {
        // Force các dependency đồng bộ version
        force("com.google.protobuf:protobuf-java:3.21.12")
        force("com.google.guava:guava:31.0.1-android")
        force("io.grpc:grpc-core:1.53.0")
        force("io.grpc:grpc-api:1.53.0")
        force("io.grpc:grpc-context:1.53.0")
        force("io.grpc:grpc-okhttp:1.53.0")
        force("io.grpc:grpc-protobuf:1.53.0")
        force("io.grpc:grpc-stub:1.53.0")
    }
}
