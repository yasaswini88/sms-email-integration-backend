package com.example.sms_email_integration.repository;

import com.example.sms_email_integration.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Get all messages in a single thread
    List<Conversation> findByThreadId(String threadId);
}
