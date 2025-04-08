plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
apply(plugin = "com.github.johnrengelman.shadow")

group = "org.elsoft"
version = "1.0-SNAPSHOT"
var exposedVersion = "0.60.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("mysql:mysql-connector-java:8.0.28") // JDBC driver for MySQL
    implementation(kotlin("script-runtime"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlin("script-util")) // Maybe necessary???

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("printClasspath") {
    doLast {
        val cp = sourceSets.main.get().runtimeClasspath
        println(cp.joinToString(":"))
    }
}

