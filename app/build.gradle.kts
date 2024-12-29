plugins {
    kotlin("jvm")
    id("myproject.java-conventions")
    application
}

dependencies {
    val clikt_version: String by project
    val pngtastic_version: String by project
    val kotlin_scripting_version: String by project

    implementation(project(":lib"))
    implementation(project(":script"))

    implementation("com.github.ajalt.clikt:clikt:$clikt_version")
    implementation("com.github.depsypher:pngtastic:$pngtastic_version")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlin_scripting_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlin_scripting_version")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlin_scripting_version")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "gurumirum.sad.app.MainKt"
    executableDir = "run"
    tasks.run.get().workingDir = File("run").apply { mkdir() }
}
