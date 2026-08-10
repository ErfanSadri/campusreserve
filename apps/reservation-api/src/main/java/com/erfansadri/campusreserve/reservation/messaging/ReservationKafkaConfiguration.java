package com.erfansadri.campusreserve.reservation.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
