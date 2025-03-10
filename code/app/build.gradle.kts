
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.vibeverse"
    compileSdk = 35
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vibeverse"
        minSdk = 24
        targetSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.storage)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation ("org.robolectric:robolectric:4.9")
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation ("com.firebaseui:firebase-ui-auth:7.2.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")
    // JUnit for unit tests.
    testImplementation("junit:junit:4.13.2")

    // Mockito core (you can use a version that suits your project).
    testImplementation("org.mockito:mockito-core:3.+")

    // PowerMock dependencies to allow mocking of static/final methods.
    testImplementation ("org.powermock:powermock-module-junit4:2.0.9")
    testImplementation ("org.powermock:powermock-api-mockito2:2.0.9")

    // (Optional) If you need inline mocking for final classes/methods with Mockito:
    // testImplementation 'org.mockito:mockito-inline:3.+'

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth:23.2.0")
    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    androidTestImplementation ("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation ("org.mockito:mockito-core:4.0.0")
    androidTestImplementation ("org.mockito:mockito-android:4.0.0")

    // For RecyclerView testing
    debugImplementation ("androidx.fragment:fragment-testing:1.6.2")

    // For mocking
    testImplementation ("org.mockito:mockito-inline:4.0.0")

    // For instrumentation tests
    androidTestImplementation ("androidx.test:runner:1.5.2")
    androidTestImplementation ("androidx.test:rules:1.5.0")



}

tasks.register<Javadoc>("javadoc") {
    // Convert the main source set directories to a FileTree.
    source = files(android.sourceSets["main"].java.srcDirs).asFileTree

    // Add the Android SDK JAR to the classpath.
    classpath += files("${android.sdkDirectory}/platforms/android-${android.compileSdk}/android.jar")

    // Use the setter method to configure the output directory.
    setDestinationDir(file("$buildDir/docs/javadoc"))
}