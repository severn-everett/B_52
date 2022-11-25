plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("org.springframework.boot") version "3.0.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "com.severett"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

apply(plugin = "kotlin")
apply(plugin = "kotlinx-serialization")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

tasks {
    withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.7"
            languageVersion = "1.7"
        }
    }
    test {
        useJUnitPlatform()
    }
}

dependencies {
    val junitVersion: String by project
    val kotlinCoroutinesVersion: String by project
    // Production dependencies
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
