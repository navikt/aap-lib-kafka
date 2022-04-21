package no.nav.aap.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.processor.LogAndSkipOnInvalidTimestamp
import java.util.*

operator fun Properties.plus(properties: Properties): Properties = apply { putAll(properties) }
operator fun Properties.plus(properties: Map<String, String>): Properties = apply { putAll(properties) }

data class KafkaConfig(
    val applicationId: String,
    val brokers: String,
    val clientId: String,
    val truststorePath: String,
    val keystorePath: String,
    val credstorePsw: String,
    val schemaRegistryUrl: String?,
    val schemaRegistryUser: String?,
    val schemaRegistryPwd: String?,
) {
    internal val schemaRegistry: Properties = Properties().apply {
        if (schemaRegistryUrl != null) {
            this[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
            if (schemaRegistryUser != null && schemaRegistryPwd != null) {
                this[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
                this[SchemaRegistryClientConfig.USER_INFO_CONFIG] = "$schemaRegistryUser:$schemaRegistryPwd"
            }
        }
    }

    private val kStreams: Properties = Properties().apply {
        this[CommonClientConfigs.CLIENT_ID_CONFIG] = clientId
        this[StreamsConfig.APPLICATION_ID_CONFIG] = applicationId
        this[StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG] = "0"
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = GenericAvroSerde::class.java.name
        this[StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            ExitPointExceptionHandler::class.java.name
        this[StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG] = LogAndSkipOnInvalidTimestamp::class.java.name
        this[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            EntryPointExceptionHandler::class.java.name
    }

    val ssl: Properties = Properties().apply {
        if (keystorePath.isNotEmpty() && truststorePath.isNotEmpty() && credstorePsw.isNotEmpty()) {
            this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            this[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
            this[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
            this[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePsw
            this[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
            this[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
            this[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePsw
            this[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePsw
            this[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
        }
    }

    val consumer: Properties = kStreams + ssl + schemaRegistry + Properties().apply {
        this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        this[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 124_000
    }

    val producer: Properties = kStreams + schemaRegistry + ssl + Properties().apply {
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers
        this[ProducerConfig.ACKS_CONFIG] = "all"
        this[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "5"
        this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
    }
}
