package com.example.sms_email_integration.repository;

import com.example.sms_email_integration.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Get all messages in a single thread
    List<Conversation> findByThreadId(String threadId);

    @Query(value = "SELECT * FROM conversations WHERE id = :conversationId", nativeQuery = true)
    Conversation findByConversationId(Long conversationId);
}
