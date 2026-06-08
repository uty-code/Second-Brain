package com.aimsgraph.config;

import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);

        // Force Virtual Threads for Kafka listeners to prevent Thread Pool exhaustion on blocking LLM calls
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-vt-");
        executor.setVirtualThreads(true);
        factory.getContainerProperties().setListenerTaskExecutor(executor);

        return factory;
    }
}
