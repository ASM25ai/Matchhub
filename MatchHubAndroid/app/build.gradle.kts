plugins { id("com.android.application") }
android {
    namespace = "com.matchhub.football"
    compileSdk = 35
    defaultConfig { applicationId = "com.matchhub.football"; minSdk = 24; targetSdk = 35; versionCode = 3; versionName = "3.0" }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
