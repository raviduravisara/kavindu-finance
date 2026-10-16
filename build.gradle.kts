// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 9 brings in KGP 2.2.10 for built-in Kotlin; this raises it to the "kotlin" version in libs.versions.toml.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}