package no.nav.aap.kafka.streams.v2.stream

import no.nav.aap.kafka.streams.v2.KStreamPair
import no.nav.aap.kafka.streams.v2.KeyValue
import no.nav.aap.kafka.streams.v2.extension.filterNotNull
import no.nav.aap.kafka.streams.v2.logger.Log
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named

class JoinedKStream<L : Any, R : Any> internal constructor(
    private val sourceTopicName: String,
    private val stream: KStream<String, KStreamPair<L, R>>,
    private val namedSupplier: () -> String
) {
    fun <LR : Any> map(joinFunction: (L, R) -> LR): MappedKStream<LR> {
        val mappedStream = stream.mapValues { (left, right) -> joinFunction(left, right) }
        return MappedKStream(sourceTopicName, mappedStream, namedSupplier)
    }

    fun filter(lambda: (KStreamPair<L, R>) -> Boolean): JoinedKStream<L, R> {
        val filteredStream = stream.filter { _, value -> lambda(value) }
        return JoinedKStream(sourceTopicName, filteredStream, namedSupplier)
    }

    fun branch(
        predicate: (KStreamPair<L, R>) -> Boolean,
        consumed: (MappedKStream<KStreamPair<L, R>>) -> Unit,
    ): BranchedMappedKStream<KStreamPair<L, R>> {
        val branchedStream = stream.split(Named.`as`("split-${namedSupplier()}"))
        return BranchedMappedKStream(sourceTopicName, branchedStream, namedSupplier).branch(predicate, consumed)
    }

    fun secureLog(log: Log.(left: L, right: R) -> Unit): JoinedKStream<L, R> {
        val loggedStream = stream.peek { _, (left, right) -> log.invoke(Log.secure, left, right) }
        return JoinedKStream(sourceTopicName, loggedStream, namedSupplier)
    }

    fun secureLogWithKey(log: Log.(key: String, left: L, right: R) -> Unit): JoinedKStream<L, R> {
        val loggedStream = stream.peek { key, (left, right) -> log.invoke(Log.secure, key, left, right) }
        return JoinedKStream(sourceTopicName, loggedStream, namedSupplier)
    }
}

class LeftJoinedKStream<L : Any, R : Any?> internal constructor(
    private val sourceTopicName: String,
    private val stream: KStream<String, KStreamPair<L, R>>,
    private val namedSupplier: () -> String

) {
    fun <LR : Any> map(mapper: (L, R) -> LR): MappedKStream<LR> {
        val mappedStream = stream.mapValues { (left, right) -> mapper(left, right) }
        return MappedKStream(sourceTopicName, mappedStream, namedSupplier)
    }

    fun <LR : Any> mapKeyValue(mapper: (String, L, R) -> KeyValue<String, LR>): MappedKStream<LR> {
        val mappedStream = stream.map { key, (left, right) -> mapper(key, left, right).toInternalKeyValue() }
        return MappedKStream(sourceTopicName, mappedStream, namedSupplier)
    }

    fun <LR> mapNotNull(mapper: (L, R) -> LR): MappedKStream<LR & Any> {
        val mappedStream = stream.mapValues { _, (left, right) -> mapper(left, right) }.filterNotNull()
        return MappedKStream(sourceTopicName, mappedStream, namedSupplier)
    }

    fun filter(lambda: (KStreamPair<L, R>) -> Boolean): LeftJoinedKStream<L, R> {
        val filteredStream = stream.filter { _, value -> lambda(value) }
        return LeftJoinedKStream(sourceTopicName, filteredStream, namedSupplier)
    }

    fun branch(
        predicate: (KStreamPair<L, R>) -> Boolean,
        consumed: (MappedKStream<KStreamPair<L, R>>) -> Unit,
    ): BranchedMappedKStream<KStreamPair<L, R>> {
        val branchedStream = stream.split(Named.`as`("split-${namedSupplier()}"))
        return BranchedMappedKStream(sourceTopicName, branchedStream, namedSupplier).branch(predicate, consumed)
    }

    fun secureLog(log: Log.(left: L, right: R) -> Unit): LeftJoinedKStream<L, R> {
        val loggedStream = stream.peek { _, (left, right) -> log.invoke(Log.secure, left, right) }
        return LeftJoinedKStream(sourceTopicName, loggedStream, namedSupplier)
    }

    fun secureLogWithKey(log: Log.(key: String, left: L, right: R) -> Unit): LeftJoinedKStream<L, R> {
        val loggedStream = stream.peek { key, (left, right) -> log.invoke(Log.secure, key, left, right) }
        return LeftJoinedKStream(sourceTopicName, loggedStream, namedSupplier)
    }
}
