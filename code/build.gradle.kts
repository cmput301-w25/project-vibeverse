plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

// If you have a `buildscript {}` block for classpaths, that goes here as well.

allprojects {
    configurations.configureEach {
        exclude(group = "com.google.android", module = "annotations")
    }
}
