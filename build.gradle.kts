plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.shinriyo"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    flatDir {
        dirs("libs")
    }
}

intellij {
    // .jar acceptable versoin
    version.set("2024.1.7")
    // Ultimate version
    // type.set("IU")
    // community version
    type.set("IC")

    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
        // Dart 241.19416.15 Downloaded und unzipped .jar
        file("libs/dart.jar")
    ))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.isIncremental = true
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            languageVersion = "1.9"
            freeCompilerArgs += "-Xsuppress-deprecated-warnings"
        }
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        jvmArgs("-Xmx2g", "-XX:+HeapDumpOnOutOfMemoryError")
        autoReloadPlugins.set(false)
    }
}

kotlin {
    jvmToolchain(17)
}

// ビルドの安定性を向上させるための設定
gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
    }
}
