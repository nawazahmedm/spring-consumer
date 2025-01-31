package com.javalearnings.amq.spring_consumer.service;

import com.javalearnings.amq.spring_consumer.entity.MessageEntity;
import com.javalearnings.amq.spring_consumer.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class MessageService {

    private final com.javalearnings.amq.spring_consumer.repository.MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Retryable(
        value = { Exception.class },
        maxAttemptsExpression = "${retry.max-attempts}",
        backoff = @Backoff(
            delayExpression = "${retry.initial-delay}",
            multiplierExpression = "${retry.multiplier}",
            maxDelayExpression = "${retry.max-delay}"
        )
    )
    public void processMessage(String message) {
        log.info("Processing Message: {}", message);

        if (message.contains("fail")) {
            throw new RuntimeException("Simulated DB Failure");
        }

        MessageEntity entity = new MessageEntity(UUID.randomUUID().toString(), message);
        messageRepository.save(entity);
        log.info("Message saved to DB successfully!");
    }
}
