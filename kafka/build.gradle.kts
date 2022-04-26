dependencies {
    api("org.rocksdb:rocksdbjni:6.29.4.1") // M1 support
    api("org.apache.kafka:kafka-clients:3.1.0")

    api("org.apache.kafka:kafka-streams:3.1.0") {
        exclude("org.rocksdb", "rocksdbjni")
        exclude("org.apache.kafka", "kafka-clients")
    }

    api("io.confluent:kafka-streams-avro-serde:7.0.1") {
        exclude("org.apache.kafka", "kafka-clients")
    }

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")

    testImplementation(kotlin("test"))
}
