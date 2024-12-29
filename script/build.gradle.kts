plugins {
    kotlin("jvm")
    id("myproject.java-conventions")
}

dependencies {
    val kotlin_scripting_version: String by project

    implementation(project(":lib"))

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlin_scripting_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlin_scripting_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlin_scripting_version")
}
