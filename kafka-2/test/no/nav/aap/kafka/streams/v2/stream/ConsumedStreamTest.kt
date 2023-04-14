package no.nav.aap.kafka.streams.v2.stream

import net.logstash.logback.argument.StructuredArguments
import no.nav.aap.kafka.streams.v2.*
import no.nav.aap.kafka.streams.v2.concurrency.RaceConditionBuffer
import no.nav.aap.kafka.streams.v2.processor.Processor
import no.nav.aap.kafka.streams.v2.processor.ProcessorMetadata
import no.nav.aap.kafka.streams.v2.serde.JsonSerde
import org.apache.kafka.streams.errors.TopologyException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ConsumedStreamTest {

    @Test
    fun `map with metadata`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .mapWithMetadata { value, metadata -> value + metadata.topic }
                .produce(Topics.B)
        }

        kafka.inputTopic(Topics.A).produce("1", "hello:")

        val result = kafka.outputTopic(Topics.B).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals("hello:A", result["1"])
    }

    @Test
    fun `produce to topic`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A).produce(Topics.C)
            consume(Topics.B).produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("1", "a")
        kafka.inputTopic(Topics.B).produce("2", "b")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals("a", result["1"])
        assertEquals("b", result["2"])
        assertEquals(2, result.size)
    }

    @Test
    fun `use costom processor`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .processor(CustomProcessor())
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals("a.v2", result["1"])
    }

    @Test
    fun `use custom processor with table`() {
        val kafka = StreamsMock.withTopology {
            val table = consume(Tables.B)
            consume(Topics.A)
                .processor(CustomProcessorWithTable(table))
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.B).produce("1", ".v2")
        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals("a.v2", result["1"])
    }

    @Test
    fun `use custom processor with mapping`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .processor(object : Processor<String, Int>("named") {
                    override fun process(metadata: ProcessorMetadata, keyValue: KeyValue<String, String>): Int {
                        return keyValue.value.toInt() + 1
                    }
                })
                .map(Int::toString)
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("a", "1")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals("2", result["a"])
    }

    @Test
    fun `use custom processor in place`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .processor(object : Processor<String, String>("something") {
                    override fun process(metadata: ProcessorMetadata, keyValue: KeyValue<String, String>): String {
                        return keyValue.value
                    }
                })
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("a", "1")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals("1", result["a"])
    }

    @Test
    fun `secure log`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .secureLog {
                    info("test message $it")
                }
        }

        kafka.inputTopic(Topics.A).produce("1", "something")
    }

    @Test
    fun `secure log with structured arguments`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .secureLog {
                    info("test message $it", StructuredArguments.kv("test", "hey"))
                }
        }

        kafka.inputTopic(Topics.A).produce("1", "something")
    }

    @Test
    fun `secure log with key`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .secureLogWithKey { key, value ->
                    info("test message $key $value")
                }
        }

        kafka.inputTopic(Topics.A).produce("1", "something")
    }

    @Test
    fun `filter on key`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .filterKey { it == "3" }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A)
            .produce("1", "a")
            .produce("3", "b")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals("b", result["3"])
    }

    @Test
    fun `flat map and preserve the type`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .flatMapPreserveType { _, value -> listOf(value, value) }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToList()
        assertEquals(2, result.size)
        assertEquals(2, result.count { kv -> kv.key == "1" })
        assertTrue { result.all { kv -> kv.value == "a" } }
    }

    @Test
    fun `flat map key and value and preserve the type`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .flatMapKeyAndValuePreserveType { key, value -> listOf(KeyValue(key, value), KeyValue(value, key)) }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(2, result.size)
        assertEquals("1", result["a"])
        assertEquals("a", result["1"])
    }

    @Test
    fun `flat map`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .flatMap { _, _ -> listOf("a".hashCode(), "b".hashCode()) }
                .map { value -> value.toString() }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        val expectedHashCode = "b".hashCode().toString()
        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals(expectedHashCode, result["1"])
    }

    @Test
    fun `flat map to key and value`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .flatMapKeyAndValue { key, value ->
                    val hashKey = key.hashCode().toString()
                    val hashValue = value.hashCode().toString()

                    listOf(
                        KeyValue(hashKey, hashValue),
                        KeyValue(hashValue, hashKey)
                    )
                }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()
        assertEquals(2, result.size)
        assertEquals("a".hashCode().toString(), result["1".hashCode().toString()])
        assertEquals("1".hashCode().toString(), result["a".hashCode().toString()])
    }

    @Test
    fun `join with bufferable`() {
        val buffTopic = Topic("buff", JsonSerde.jackson<Buff>())
        val buffTable = Table(buffTopic)
        val buffer = RaceConditionBuffer<Buff>()

        val kafka = StreamsMock.withTopology {
            val buffKTable = consume(buffTable)

            consume(Topics.A)
                .joinWith(buffKTable, buffer)
                .map { a, buff -> buff.copy(value = buff.value + a) }
                .produce(buffTopic, buffer) { it }
        }

        kafka.inputTopic(buffTopic).produce("1", Buff("buff"))
        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(buffTopic).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals(Buff("buffa"), result["1"])
    }

    @Test
    fun `left join with bufferable`() {
        val buffTopic = Topic("buff", JsonSerde.jackson<Buff>())
        val buffTable = Table(buffTopic)
        val buffer = RaceConditionBuffer<Buff>()

        val kafka = StreamsMock.withTopology {
            val buffKTable = consume(buffTable)

            consume(Topics.A)
                .leftJoinWith(buffKTable, buffer)
                .map { a, buff -> buff?.let { Buff(it.value + a) } ?: Buff(a) }
                .produce(buffTopic, buffer) { it }
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        val result = kafka.outputTopic(buffTopic).readKeyValuesToMap()
        assertEquals(1, result.size)
        assertEquals(Buff("a"), result["1"])
    }

    @Test
    fun repartition() {
        val msg = assertThrows<TopologyException> {
            StreamsMock.withTopology {
                val ktable = consumeRepartitioned(Tables.B, 3)

                consume(Topics.A)
                    .repartition(4)
                    .joinWith(ktable)
                    .map { a, b -> a + b }
                    .produce(Topics.C)
            }
        }

        assertTrue { msg.stackTraceToString().contains("Following topics do not have the same number of partitions") }
    }

    @Test
    fun `for each`() {
        val result = mutableMapOf<String, String>()

        val kafka = StreamsMock.withTopology {
            consume(Topics.A).forEach { key, value -> result[key] = value }
        }

        kafka.inputTopic(Topics.A).produce("1", "a")

        assertEquals(result, mapOf("1" to "a"))
    }

    @Test
    fun `filter consumed topic`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .filter { it != "b" }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A)
            .produce("1", "a")
            .produce("2", "b")
            .produce("3", "c")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()

        assertEquals(2, result.size)
        assertNull(result["2"])
    }

    @Test
    fun `rekey consumed topic`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .rekey { "test:$it" }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A)
            .produce("1", "a")
            .produce("2", "b")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()

        assertEquals(2, result.size)
        assertEquals("a", result["test:a"])
        assertEquals("b", result["test:b"])
    }

    @Test
    fun `filter a filtered  stream`() {
        val kafka = StreamsMock.withTopology {
            consume(Topics.A)
                .filter { it.contains("nice") }
                .filter { it.contains("price") }
                .produce(Topics.C)
        }

        kafka.inputTopic(Topics.A)
            .produce("1", "niceprice")
            .produce("2", "nice")
            .produce("3", "price")

        val result = kafka.outputTopic(Topics.C).readKeyValuesToMap()

        assertEquals(1, result.size)
        assertNull(result["2"])
        assertNull(result["3"])
        assertEquals("niceprice", result["1"])
    }
}
