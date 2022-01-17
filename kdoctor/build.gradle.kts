plugins {
    kotlin("multiplatform")
}

kotlin {
    listOf(macosX64(), macosArm64()).forEach {
        it.binaries {
            executable {
                entryPoint = "org.jetbrains.kotlin.doctor.main"
            }
        }
    }

    sourceSets {
        /* Main source sets */
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
            }
        }
        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependencies {
                implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
            }
        }

        /* Main hierarchy */
        macosMain.dependsOn(commonMain)
        macosX64Main.dependsOn(macosMain)
        macosArm64Main.dependsOn(macosMain)

        /* Test source sets */
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val macosX64Test by getting
        val macosArm64Test by getting
        val macosTest by creating

        /* Test hierarchy */
        macosTest.dependsOn(commonTest)
        macosX64Test.dependsOn(macosTest)
        macosArm64Test.dependsOn(macosTest)
    }
}

tasks.register<Exec>("assembleReleaseExecutableMacos") {
    dependsOn("linkReleaseExecutableMacosX64", "linkReleaseExecutableMacosArm64")
    commandLine("lipo", "-create", "-output", "kdoctor", "bin/macosX64/releaseExecutable/kdoctor.kexe", "bin/macosArm64/releaseExecutable/kdoctor.kexe")
    workingDir = buildDir
    group = "Build"
    description = "Builds universal macOS binary"
}