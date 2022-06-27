package no.nav.aap.kafka.streams

import net.logstash.logback.argument.StructuredArguments.kv
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.streams.processor.StateRestoreListener
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val log = LoggerFactory.getLogger("kafka")


internal class RestoreListener : StateRestoreListener {
    private val durationForPartition = hashMapOf<Int, Long>()

    override fun onRestoreStart(partition: TopicPartition, storeName: String, startOffset: Long, endOffset: Long) {
        durationForPartition[partition.partition()] = System.currentTimeMillis()
    }

    override fun onRestoreEnd(partition: TopicPartition, storeName: String, totalRestored: Long) {
        val startMs = durationForPartition.getOrDefault(partition.partition(), Long.MAX_VALUE)
        val duration = (System.currentTimeMillis() - startMs).toDuration(DurationUnit.MILLISECONDS)

        log.info(
            "Gjennopprettet #$totalRestored meldinger på partisjon ${partition.partition()} på $duration",
            kv("partition", partition.partition()),
            kv("topic", partition.topic()),
            kv("store", storeName),
        )
    }

    override fun onBatchRestored(partition: TopicPartition, storeName: String, endOffset: Long, numRestored: Long) {
        // This is very noisy, Don't log anything
    }
}