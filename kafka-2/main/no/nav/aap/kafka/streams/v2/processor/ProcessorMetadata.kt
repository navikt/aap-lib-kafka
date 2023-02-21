package no.nav.aap.kafka.streams.v2.processor

data class ProcessorMetadata(
    val topic: String,
    val partition: Int,
    val offset: Long,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
)
