plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose")
	id("org.jetbrains.kotlin.plugin.serialization")
}

android {
	namespace = "com.origin.app"
	compileSdk = (System.getProperty("VERSION_COMPILE_SDK") ?: "34").toInt()

	defaultConfig {
		applicationId = "com.origin.app"
		minSdk = (System.getProperty("VERSION_MIN_SDK") ?: "24").toInt()
		targetSdk = (System.getProperty("VERSION_TARGET_SDK") ?: "34").toInt()
		versionCode = 1
		versionName = "1.0.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		debug {
			isMinifyEnabled = false
			// Development configuration
			buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.10:8080\"")
			buildConfigField("String", "ENVIRONMENT", "\"development\"")
			buildConfigField("boolean", "DEBUG_MODE", "true")
		}
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			// Production configuration
			buildConfigField("String", "API_BASE_URL", "\"https://your-domain.com\"")
			buildConfigField("String", "ENVIRONMENT", "\"production\"")
			buildConfigField("boolean", "DEBUG_MODE", "false")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions { jvmTarget = "17" }

	buildFeatures { 
		compose = true
		buildConfig = true
	}

	packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
	implementation(platform("androidx.compose:compose-bom:2024.06.00"))
	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3:1.2.1")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.navigation:navigation-compose:2.8.0")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
	implementation("androidx.datastore:datastore-preferences:1.1.1")
	implementation("io.coil-kt:coil-compose:2.6.0")

	// Ktor client
	implementation("io.ktor:ktor-client-core:2.3.12")
	implementation("io.ktor:ktor-client-okhttp:2.3.12")
	implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Google Places API
    implementation("com.google.android.libraries.places:places:3.3.0")
    
    // PDF generation
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("androidx.core:core-ktx:1.12.0")


	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")

	testImplementation("junit:junit:4.13.2")
	androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
	androidTestImplementation("androidx.test.ext:junit:1.2.1")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
