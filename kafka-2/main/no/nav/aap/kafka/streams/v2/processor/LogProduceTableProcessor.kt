package no.nav.aap.kafka.streams.v2.processor

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.aap.kafka.streams.v2.KeyValue
import no.nav.aap.kafka.streams.v2.Table
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("secureLog")

internal class LogProduceTableProcessor<T>(
    private val table: Table<T & Any>,
) : Processor<T, T>("log-produced-${table.sourceTopicName}") {

    override fun process(metadata: ProcessorMetadata, keyValue: KeyValue<String, T>): T {
        when (keyValue.value) {
            null -> log.trace(
                "Produserer tombstone til KTable ${table.sourceTopicName}",
                kv("key", keyValue.key),
                kv("table", table.sourceTopicName),
                kv("store", table.stateStoreName),
                kv("partition", metadata.partition),
            )

            else -> log.trace(
                "Produserer til KTable ${table.sourceTopicName}",
                kv("key", keyValue.key),
                kv("table", table.sourceTopicName),
                kv("store", table.stateStoreName),
                kv("partition", metadata.partition),
                if (table.sourceTopic.logValues) kv("value", keyValue.value) else null,
            )
        }
        return keyValue.value
    }
}
