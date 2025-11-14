plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm() //for unit tests
    applyDefaultHierarchyTemplate()

    listOf(macosX64(), macosArm64()).forEach {
        it.binaries {
            executable {
                entryPoint = "org.jetbrains.kotlin.doctor.main"
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("com.github.ajalt.clikt:clikt:4.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("co.touchlab:kermit:2.0.8")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.register<Exec>("assembleReleaseExecutableMacos") {
    dependsOn(":kdoctor:jvmTest", ":kdoctor:linkReleaseExecutableMacosX64", ":kdoctor:linkReleaseExecutableMacosArm64")
    commandLine("lipo", "-create", "-output", "kdoctor", "bin/macosX64/releaseExecutable/kdoctor.kexe", "bin/macosArm64/releaseExecutable/kdoctor.kexe")
    workingDir = layout.buildDirectory.get().asFile
    group = "Build"
    description = "Builds universal macOS binary"
}