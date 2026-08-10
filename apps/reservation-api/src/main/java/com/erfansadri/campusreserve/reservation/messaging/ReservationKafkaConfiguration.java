package com.erfansadri.campusreserve.reservation.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
public class ReservationKafkaConfiguration {

    @Bean
    NewTopic reservationLifecycleTopic() {
        return TopicBuilder.name(ReservationLifecycleTopic.NAME)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "campusreserve.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true)
class OutboxSchedulingConfiguration {
}
