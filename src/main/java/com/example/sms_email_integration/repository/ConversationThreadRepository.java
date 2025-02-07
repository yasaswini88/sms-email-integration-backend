package com.example.sms_email_integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.dto.CaseStatusMetricsDTO;
import com.example.sms_email_integration.dto.CaseTypeMetricsDTO;
import com.example.sms_email_integration.entity.ConversationThread;

@Repository
public interface ConversationThreadRepository extends CrudRepository<ConversationThread, Long> {

    // Find by unique threadId
    Optional<ConversationThread> findByThreadId(String threadId);

    // Optionally: find by phoneNumber and email if needed
    Optional<ConversationThread> findByPhoneNumberAndEmail(String phoneNumber, String email);

    @Query("SELECT ct FROM ConversationThread ct WHERE ct.threadId = :threadId AND ct.status = 'ACTIVE'")
    Optional<ConversationThread> findActiveThreadByThreadId(String threadId);

    @Query(value="SELECT count(*) as count,case_type as caseType from conversation_thread where status !='INACTIVE' GROUP BY case_type",nativeQuery=true)   
    List<CaseTypeMetricsDTO> getActiveThreadCountByCaseType();


   @Query(value="SELECT count(*) as count,status as status from conversation_thread where status !='INACTIVE' GROUP BY status",nativeQuery=true)   
    List<CaseStatusMetricsDTO> getCaseStatusMetrics();

}
