package com.example.sms_email_integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.entity.Conversation;


@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Get all messages in a single thread
    // List<Conversation> findByThreadId(String threadId);

    List<Conversation> findByConversationThread_ThreadId(String threadId);


    @Query(value = "SELECT * FROM conversations WHERE id = :conversationId", nativeQuery = true)
    Conversation findByConversationId(Long conversationId);

      List<Conversation> findByConversationThread_ConversationThreadId(Long conversationThreadId);

       @Query("SELECT c FROM Conversation c WHERE c.conversationThread.threadId = :threadId")
    List<Conversation> findAllByThreadId(String threadId);


    Optional<Conversation> findByMessageId(String messageId);

    @Query("SELECT c FROM Conversation c WHERE c.conversationThread.custiId = :custiId")
    List<Conversation> findByFirmId(Long custiId);

    // 1) All conversations in descending order of timestamp
List<Conversation> findAllByOrderByTimestampDesc();

// 2) All conversations by channel, descending
List<Conversation> findByChannelOrderByTimestampDesc(String channel);

// 3) All conversations by direction, descending
List<Conversation> findByDirectionOrderByTimestampDesc(String direction);


    
    
}
