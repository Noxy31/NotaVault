plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.0"  // Kotlin 1.9.0
    id("org.jetbrains.compose") version "1.5.0"     // Version compatible avec Kotlin 1.9.0
    application
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    
    // Dépendances Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.preview)
    
    // Autres dépendances pour ta logique métier et tests
    implementation("org.ktorm:ktorm-core:3.5.0")
    implementation("org.ktorm:ktorm-support-mysql:3.5.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("de.mkammerer:argon2-jvm:2.11")
    
    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass.set("org.example.MainKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}