dependencies {
    api("org.apache.kafka:kafka-streams-test-utils:3.3.1") {
        exclude("org.apache.kafka", "kafka-clients")
    }

    implementation(project(":kafka-2"))

    implementation(kotlin("test"))

    implementation("io.micrometer:micrometer-registry-prometheus:1.10.2")
}
