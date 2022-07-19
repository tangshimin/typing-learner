import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev745"
    kotlin("plugin.serialization") version "1.7.0"
}

group = "com.typinglearner"
version = "1.3.3"
repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.compose.material3:material3:1.0.1")
    implementation ("org.jetbrains.compose.material:material-icons-extended:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("uk.co.caprica:vlcj:4.7.1")
    implementation("com.formdev:flatlaf:2.3")
    implementation("org.apache.opennlp:opennlp-tools:1.9.4")
    implementation("org.apache.pdfbox:pdfbox:2.0.24")
    implementation("com.h2database:h2:2.1.212")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation(files("lib/ebml-reader-0.1.1.jar"))
    implementation(files("lib/subtitleConvert-1.0.2.jar"))
    implementation("org.apache.maven:maven-artifact:3.8.6")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")
    testImplementation(compose("org.jetbrains.compose.ui:ui-test-junit4"))
}



tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}
/**
 *  `src/main/resources` 文件夹里的文件会被打包到 typing-learner.jar 里面，然后通过 getResource 访问，
 *   只读文件可以放在 `src/main/resources` 文件夹里面，需要修改的文件不能放在这个文件夹里面
 */
compose.desktop {
    application {
        mainClass = "MainKt"
//        jvmArgs += listOf("-verbose:gc")
//        jvmArgs += listOf("-client")
        jvmArgs += listOf("-Xmx1G")
        jvmArgs += listOf("-Dfile.encoding=UTF-8")
        jvmArgs += listOf("-Dapple.awt.application.appearance=system")
//        jvmArgs += listOf("-XX:+PrintGCDetails")
//        jvmArgs += listOf("-XX:NativeMemoryTracking=summary")
//        jvmArgs += listOf("-XX:+UnlockDiagnosticVMOptions")
//        jvmArgs += listOf("-XX:+PrintNMTStatistics")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Typing Learner"
            packageVersion = version.toString()
            modules("java.sql")
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            copyright = "Copyright 2022 Shimin Tang. All rights reserved."
            licenseFile.set(project.file("LICENSE"))
            windows{
                dirChooser = true
                perUserInstall = true
                menuGroup = "Typing Learner"
                iconFile.set(project.file("src/main/resources/logo/logo.ico"))
            }
            macOS{
                iconFile.set(project.file("src/main/resources/logo/logo.icns"))
            }
        }
    }
}
