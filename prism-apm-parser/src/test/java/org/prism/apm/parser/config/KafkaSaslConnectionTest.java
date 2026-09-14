package org.prism.apm.parser.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = KafkaSaslConnectionTest.KafkaOnlyTestConfig.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@DirtiesContext
@ActiveProfiles({"kafka-sasl"})
@TestPropertySource(properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class KafkaSaslConnectionTest {

    static final String TOPIC = "parser-kafka-sasl-test";

    private static final EmbeddedKafkaZKBroker EMBEDDED_KAFKA_BROKER = createSaslBroker();

    static {
        EMBEDDED_KAFKA_BROKER.afterPropertiesSet();
    }

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    /**
     * Verifies that the local+kafka-sasl profiles contribute SASL/PLAIN client settings and
     * can produce/consume a message against a SASL-enabled embedded Kafka broker.
     */
    @Test
    void localKafkaSaslProfilesConnectToKafkaWithAuthentication() throws Exception {
        assertThat(kafkaProperties.getProperties())
                .containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                .containsEntry(SaslConfigs.SASL_MECHANISM, "PLAIN");
        assertThat(kafkaProperties.getProperties().get(SaslConfigs.SASL_JAAS_CONFIG))
                .contains("username=\"admin\"")
                .contains("password=\"change-me\"");

        String message = "sasl-message";
        kafkaTemplate.send(TOPIC, "sasl-key", message).get();

        try (Consumer<String, String> consumer = consumerFactory.createConsumer("sasl-test-group", "sasl-test")) {
            EMBEDDED_KAFKA_BROKER.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            assertThat(KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10)).value())
                    .isEqualTo(message);
        }
    }

    /**
     * Points Spring Kafka clients at the SASL listener because the broker also exposes a
     * PLAINTEXT listener for embedded broker startup bookkeeping.
     */
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KafkaSaslConnectionTest::saslBootstrapServers);
    }

    /**
     * Stops the programmatically created broker after the class finishes.
     */
    @AfterAll
    static void stopKafka() {
        EMBEDDED_KAFKA_BROKER.destroy();
    }

    /**
     * Returns the runtime port bound to the SASL_PLAINTEXT listener.
     */
    private static String saslBootstrapServers() {
        int port = EMBEDDED_KAFKA_BROKER.getKafkaServer(0)
                .boundPort(ListenerName.forSecurityProtocol(SecurityProtocol.SASL_PLAINTEXT));
        return "127.0.0.1:" + port;
    }

    /**
     * Creates a ZK-backed embedded broker with both PLAINTEXT and SASL_PLAINTEXT listeners.
     */
    private static EmbeddedKafkaZKBroker createSaslBroker() {
        EmbeddedKafkaZKBroker broker = new EmbeddedKafkaZKBroker(1, true, 1, TOPIC);
        broker.brokerProperties(Map.of(
                "listeners", "PLAINTEXT://127.0.0.1:0,SASL_PLAINTEXT://127.0.0.1:0",
                "listener.security.protocol.map", "PLAINTEXT:PLAINTEXT,SASL_PLAINTEXT:SASL_PLAINTEXT",
                "security.inter.broker.protocol", "PLAINTEXT",
                "sasl.enabled.mechanisms", "PLAIN",
                "listener.name.sasl_plaintext.plain.sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"change-me\" user_admin=\"change-me\";"
        ));
        return broker;
    }

    /**
     * Loads only Kafka auto-configuration so this test does not start the full parser application context.
     */
    @TestConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class KafkaOnlyTestConfig {
    }
}
