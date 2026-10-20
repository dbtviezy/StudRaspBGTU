plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ru.bgtu.schedule"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.bgtu.schedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        manifestPlaceholders["authScheme"] = "studraspbgtu"
    }

    flavorDimensions += "role"
    productFlavors {
        create("student") {
            dimension = "role"
            buildConfigField("boolean", "ADMIN", "false")
        }
        // Separate app with moderation tools; installs next to the student app.
        create("admin") {
            dimension = "role"
            applicationIdSuffix = ".admin"
            versionNameSuffix = "-admin"
            buildConfigField("boolean", "ADMIN", "true")
            manifestPlaceholders["authScheme"] = "studraspbgtu-admin"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
        buildConfig = true
    }
}

android.applicationVariants.all {
    val variant = this
    val version = variant.versionName.removeSuffix("-admin")
    val name = if (variant.flavorName == "admin") "StudRaspBGTU-Admin-v$version" else "StudRaspBGTU-v$version"
    variant.outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            if (variant.buildType.name == "release") "$name.apk" else "$name-${variant.buildType.name}.apk"
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(bom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
