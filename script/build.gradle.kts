plugins {
    kotlin("jvm") version "1.9.22"
    id("myproject.java-conventions")
}

dependencies {
    implementation(project(":api"))

    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")
}
