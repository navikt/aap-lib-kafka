dependencies {
    api("org.apache.kafka:kafka-streams-test-utils:3.1.0") {
        exclude("org.rocksdb", "rocksdbjni") // included in :kafka
    }

    implementation("io.micrometer:micrometer-registry-prometheus:1.8.5")
    implementation(project(":kafka"))
    implementation(kotlin("test"))

    implementation("io.confluent:kafka-streams-avro-serde:7.0.1") {
        exclude("org.apache.kafka", "kafka-clients")
    }

    testImplementation(kotlin("test"))
}
