plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
//    jcenter() // or maven(url="https://dl.bintray.com/kotlin/dokka")
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.20")
}