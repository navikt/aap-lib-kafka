package no.nav.aap.kafka

import no.nav.aap.kafka.serde.json.JsonSerde
import no.nav.aap.kafka.streams.Topic
import org.junit.Test
import kotlin.test.assertEquals

class KafkaFactoryTest {

    @Test
    fun consumer() {
        val config = defaultKafkaTestConfig.copy(credstorePsw = "")
        val consumer = KafkaFactory.createConsumer(config, Topic("topic", JsonSerde.jackson()))
        val groupId = consumer.groupMetadata().groupId()
        assertEquals("topic-1", groupId)
    }

    @Test
    fun producer() {
        val config = defaultKafkaTestConfig.copy(credstorePsw = "")
        KafkaFactory.createProducer(config, Topic("topic", JsonSerde.jackson()))
    }
}
