package org.prism.apm.parser.config;

import org.prism.apm.common.model.entity.ServiceParseConfigEntity;
import org.prism.apm.common.repository.ServiceParseConfigMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author john
 * @date 2024-10-17 14:41
 */

@Configuration
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "parser")
public class ServiceParseConfig {

    public static final String DEFAULT_TRACES_TOPIC = "opentelemetry-trace";
    public static final String DEFAULT_REPROCESSING_TOPIC = "reprocessing-opentelemetry-trace";

    public static final String TRACES_TOPIC = "${parser.kafka_topic:" + DEFAULT_TRACES_TOPIC + "}";
    public static final String REPROCESSING_TOPIC = "${parser.reprocessing_kafka_topic:" + DEFAULT_REPROCESSING_TOPIC + "}";


    @Value(TRACES_TOPIC)
    private String kafkaTopic;
    @Value(REPROCESSING_TOPIC)
    private String reprocessingKafkaTopic;
//    private String kafkaTopic = DEFAULT_TRACES_TOPIC;
//    private String reprocessingKafkaTopic = DEFAULT_REPROCESSING_TOPIC;

    @Autowired
    private ServiceParseConfigMapper mapper;

    private Map<String, ServiceParseConfigEntity> serviceParseConfigMap;

    @PostConstruct
    public void init() {
        List<ServiceParseConfigEntity> serviceParseConfigEntities = mapper.selectByAll(null);
        serviceParseConfigMap = new HashMap<>();
        for (ServiceParseConfigEntity config : serviceParseConfigEntities) {
            serviceParseConfigMap.put(config.getServiceName(), config);
        }
    }


    public boolean needParsed(String serviceName) {
        ServiceParseConfigEntity config = serviceParseConfigMap.get(serviceName);
        if (config == null) {
            log.info("{} is not in the list of services to be parsed.",
                    serviceName);
            return true;
        }

        Boolean parsed = config.getParsed();
        if (parsed == null || parsed == Boolean.FALSE) {
            log.info("{} is not in the list of services to be parsed.",
                    serviceName);
            return false;
        }

        return true;
    }
}
