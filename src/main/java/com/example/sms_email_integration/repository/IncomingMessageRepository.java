package com.example.sms_email_integration.repository;

import com.example.sms_email_integration.entity.IncomingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingMessageRepository extends JpaRepository<IncomingMessage, Long> {

}
