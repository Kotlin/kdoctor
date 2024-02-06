plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    macosX64()
    macosArm64()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        commonMain {
            dependencies {
                api(project.dependencies.platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.0-RC"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }
        val jvmAndMacosMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain {
            dependsOn(jvmAndMacosMain)
        }
        macosMain {
            dependsOn(jvmAndMacosMain)
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.23.2")
                implementation("co.touchlab:kermit:2.0.3")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
            }
        }
        jvmTest {
            dependencies {
                implementation("co.touchlab:kermit:2.0.3")
            }
        }
    }

    applyDefaultHierarchyTemplate()
}

tasks {
    val jvmTest by getting(Test::class) {
        systemProperties["kotlinx.coroutines.test.default_timeout"] = "1s"
    }
}