import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "ca.michaelmaguire.dialett"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // the app's identity on devices and Google Play: permanent once uploaded to Play (owner, 2026-11-10)
        applicationId = "ca.michaelmaguire.languages.dialett.rogn"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Release signing: the key and password are in ~/.gradle/gradle.properties (never in this repo):
    //   dialett.release.storeFile, dialett.release.storePassword, dialett.release.keyAlias
    // Without them a release build is unsigned and can't be installed.
    val storeFile = providers.gradleProperty("dialett.release.storeFile").orNull
    signingConfigs {
        if (storeFile != null) create("release") {
            this.storeFile = file(storeFile)
            storePassword = providers.gradleProperty("dialett.release.storePassword").get()
            keyAlias = providers.gradleProperty("dialett.release.keyAlias").get()
            keyPassword = storePassword
        }
    }
    buildTypes {
        release {
            // R8: removes unused code and optimises the rest, so the app starts much faster than a debug build
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // No encrypted list of dependencies in the APK or bundle: only Google can read it, and F-Droid's
    // checks flag it as a possible tracking risk
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
