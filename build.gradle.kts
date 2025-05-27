// ─── root build.gradle.kts ─────────────────────────────────────────────
plugins {
    // declare once, don't apply here
    kotlin("jvm")              version "2.1.20" apply false
    kotlin("plugin.spring")    version "2.1.20" apply false
    id("org.springframework.boot")            version "3.3.0" apply false
    id("io.spring.dependency-management")     version "1.1.4" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
    }

    plugins.withType<org.gradle.api.plugins.JavaPlugin> {
        extensions.configure<org.gradle.api.plugins.JavaPluginExtension>("java") {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) // or JVM_17
            }
        }
    }
}
