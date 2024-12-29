plugins {
    kotlin("jvm")
    id("myproject.java-conventions")
}

dependencies {
    val kotlin_coroutines_version: String by project
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")
}

kotlin {
    jvmToolchain(17)
}

tasks.register("generateCode", Copy::class.java) {
    val templateContext = mapOf("version" to project.version)
    inputs.properties(templateContext) // for gradle up-to-date check
    from("src/template/kotlin")
    into(layout.buildDirectory.dir("generated/kotlin"))
    expand(templateContext)
}

sourceSets.main.get().kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin"))
tasks.getByName("compileKotlin").dependsOn("generateCode")
