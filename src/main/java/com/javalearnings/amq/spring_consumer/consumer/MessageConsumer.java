package com.javalearnings.amq.spring_consumer.consumer;

import com.javalearnings.amq.spring_consumer.service.MessageService;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MessageConsumer {

    private final MessageService messageService;
    private final JmsTemplate jmsTemplate;
    private final ConcurrentHashMap<String, Integer> retryCountMap = new ConcurrentHashMap<>();

    public MessageConsumer(MessageService messageService, JmsTemplate jmsTemplate) {
        this.messageService = messageService;
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = "my-queue", containerFactory = "jmsListenerContainerFactory")
    public void receiveMessage(Message message, Session session) {
        try {
            if (message instanceof TextMessage textMessage) {
                String messageId = textMessage.getJMSMessageID();
                String payload = textMessage.getText();
                
                log.info("Received Message: {}", payload);

                // Track retry count
                int retryCount = retryCountMap.getOrDefault(messageId, 0);

                try {
                    messageService.processMessage(payload);
                    message.acknowledge(); // Acknowledge only on successful processing
                    retryCountMap.remove(messageId); // Clear retry count on success
                } catch (Exception e) {
                    retryCount++;
                    log.error("Processing failed for message {} (Attempt {}/3): {}", messageId, retryCount, e.getMessage());

                    if (retryCount >= 3) {
                        log.warn("Max retry attempts reached. Sending message to DLQ...");
                        jmsTemplate.convertAndSend("dlq-queue", payload);
                        message.acknowledge(); // Acknowledge to remove from the queue
                        retryCountMap.remove(messageId);
                    } else {
                        retryCountMap.put(messageId, retryCount);
                        session.recover(); // Retry processing the same message
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Critical error in message consumption: {}", ex.getMessage());
        }
    }
}
