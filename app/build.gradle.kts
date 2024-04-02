plugins {
    kotlin("jvm") version "1.9.22"
    id("myproject.java-conventions")
    application
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":script"))

    implementation("com.github.ajalt.clikt:clikt:4.3.0")
    implementation("com.github.depsypher:pngtastic:1.7")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "cnedclub.sad.app.MainKt"
    executableDir = "run"
}
