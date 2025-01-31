package com.javalearnings.amq.spring_consumer.repository;

import com.javalearnings.amq.spring_consumer.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, String> {
}
