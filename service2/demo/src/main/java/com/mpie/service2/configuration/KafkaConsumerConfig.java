package com.mpie.service2.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpie.service2.model.Book;
import com.mpie.service2.model.Category;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${service2.group.id}")
    private String groupId;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, Book> kafkaTemplate;

    @Value("${spring.kafka.dlt-topic}")
    private String dltTopic;

    private static final List<String> VALID_CATEGORIES = List.of("FANTASY", "SCIENCE-FICTION", "NAUKOWE");

    @Bean
    public DefaultErrorHandler errorHandler() {
        FixedBackOff backOff = new FixedBackOff(1000L, 1);

        return new DefaultErrorHandler((record, exception) -> {
            if (record instanceof ConsumerRecord) {
                ConsumerRecord<String, Book> consumerRecord = (ConsumerRecord<String, Book>) record;
                log.error("Record skipped and sent to DLT: {}. Error: {}", consumerRecord.value(), exception.getMessage());
                sendToDLT(consumerRecord);
            }
        }, backOff);
    }



    @Bean
    public ConsumerFactory<String, Book> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<Book> deserializer = new JsonDeserializer<>(Book.class);
        deserializer.setUseTypeMapperForKey(true);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<Book> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Book> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Book> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        factory.setBatchListener(true);

        factory.setRecordFilterStrategy(new RecordFilterStrategy<String, Book>() {
            @Override
            public boolean filter(ConsumerRecord<String, Book> record) {
                Book book = record.value();
                if (book != null && !VALID_CATEGORIES.contains(book.getCategory())) {
                    log.info("Event with category '{}' is skipped. It does not match the required categories.", book.getCategory());
                    return true;
                }
                return false;
            }
        });

        return factory;
    }

    private void sendToDLT(ConsumerRecord<String, Book> record) {
        // Send the failed message to the DLT
        log.info("Sending failed message to DLT: {}", record.value());
        kafkaTemplate.send(dltTopic, record.value());
    }

}
