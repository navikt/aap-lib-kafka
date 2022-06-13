package no.nav.aap.kafka.streams.store

import no.nav.aap.kafka.streams.Table
import no.nav.aap.kafka.streams.named
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private class StateStoreCleaner<K, V>(
    private val table: Table<V>,
    private val interval: Duration,
    private val keysToDelete: MutableList<K>,
) : Processor<K, V, Void, Void> {
    override fun process(record: Record<K, V>) {}

    override fun init(context: ProcessorContext<Void, Void>) {
        val store = context.getStateStore<KeyValueStore<K, ValueAndTimestamp<V>>>(table.stateStoreName)
        context.schedule(interval.toJavaDuration(), PunctuationType.WALL_CLOCK_TIME) {
            keysToDelete.removeIf { key ->
                val value = store.delete(key)
                secureLog.info("Deleted [${table.stateStoreName}] [$key] [$value]")
                true
            }
        }
    }

    private companion object {
        private val secureLog = LoggerFactory.getLogger("secureLog")
    }
}

fun <K, V> KTable<K, V>.scheduleCleanup(
    table: Table<V>,
    interval: Duration,
    keysToDelete: MutableList<K>,
) = toStream().process(
    ProcessorSupplier { StateStoreCleaner(table, interval, keysToDelete) },
    named("cleanup-${table.stateStoreName}"),
    table.stateStoreName
)
