plugins {
    kotlin("jvm") version "1.6.0"
}

group = "org.elsoft"
version = "1.0-SNAPSHOT"
var exposedVersion = "0.60.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("mysql:mysql-connector-java:8.0.28") // JDBC driver for MySQL

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}