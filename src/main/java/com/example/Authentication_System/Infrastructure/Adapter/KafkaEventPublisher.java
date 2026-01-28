package com.example.Authentication_System.Infrastructure.Adapter;

import com.example.Authentication_System.Domain.model.UserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher {

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send("user-events", event);
    }
}