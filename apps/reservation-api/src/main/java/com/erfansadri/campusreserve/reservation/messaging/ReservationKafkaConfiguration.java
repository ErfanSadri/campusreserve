package com.erfansadri.campusreserve.reservation.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableScheduling
public class ReservationKafkaConfiguration {

    private static final long RETRY_BACKOFF_MILLIS = 250L;
    private static final long RETRY_ATTEMPTS = 2L;

    @Bean
    NewTopic reservationLifecycleTopic() {
        return TopicBuilder.name(ReservationLifecycleTopic.NAME)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic reservationLifecycleDltTopic() {
        return TopicBuilder.name(ReservationLifecycleTopic.DLT_NAME)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler reservationLifecycleErrorHandler(
            KafkaTemplate<String, ReservationLifecycleEvent> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                ReservationLifecycleTopic.DLT_NAME,
                                record.partition()));
        recoverer.setAppendOriginalHeaders(true);
        recoverer.setFailIfSendResultIsError(true);

        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(RETRY_BACKOFF_MILLIS, RETRY_ATTEMPTS));
    }

    @Bean
    @SuppressWarnings({ "rawtypes", "unchecked" })
    ConcurrentKafkaListenerContainerFactory<String, ReservationLifecycleEvent>
            kafkaListenerContainerFactory(
                    ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                    ConsumerFactory<String, ReservationLifecycleEvent> consumerFactory,
                    DefaultErrorHandler reservationLifecycleErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, ReservationLifecycleEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(
                (ConcurrentKafkaListenerContainerFactory) factory,
                (ConsumerFactory) consumerFactory);
        factory.setCommonErrorHandler(reservationLifecycleErrorHandler);
        return factory;
    }
}
