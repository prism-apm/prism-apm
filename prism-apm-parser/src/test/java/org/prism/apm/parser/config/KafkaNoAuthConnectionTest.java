package org.prism.apm.parser.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = KafkaNoAuthConnectionTest.KafkaOnlyTestConfig.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = KafkaNoAuthConnectionTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class KafkaNoAuthConnectionTest {

    static final String TOPIC = "parser-kafka-no-auth-test";

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    /**
     * Verifies that the local profile does not contribute SASL client settings and can
     * produce/consume a message against a plain embedded Kafka broker.
     */
    @Test
    void localProfileConnectsToKafkaWithoutAuthentication() throws Exception {
        assertThat(kafkaProperties.getProperties())
                .doesNotContainKeys(
                        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                        SaslConfigs.SASL_MECHANISM,
                        SaslConfigs.SASL_JAAS_CONFIG
                );

        String message = "no-auth-message";
        kafkaTemplate.send(TOPIC, "no-auth-key", message).get();

        try (Consumer<String, String> consumer = consumerFactory.createConsumer("no-auth-test-group", "no-auth-test")) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            assertThat(KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10)).value())
                    .isEqualTo(message);
        }
    }

    /**
     * Loads only Kafka auto-configuration so this test does not start the full parser application context.
     */
    @TestConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class KafkaOnlyTestConfig {
    }
}
