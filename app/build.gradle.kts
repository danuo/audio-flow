plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.audio_flow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.audio_flow"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations += mutableSetOf("en", "xxhdpi")
    }


    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        dataBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2023.09.00"))
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.ui:ui-graphics:1.5.1")
    // implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    // implementation("androidx.compose.material3:material3:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.1")

    implementation("androidx.appcompat:appcompat:1.6.1")

    // activity
    // implementation("androidx.activity:activity-compose:1.7.2")
    // implementation("androidx.activity:activity-ktx:1.7.2")

    // Room components
    implementation("androidx.room:room-runtime:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    androidTestImplementation("androidx.room:room-testing:2.5.2")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    // implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    // implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    // implementation("androidx.lifecycle:lifecycle-common-java8:2.6.1")

    // Kotlin components
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10")
    // api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // UI
    // implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // implementation("com.google.android.material:material:1.9.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Server
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-jetty-jvm:2.3.4")

    // Intent Communication
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
}
