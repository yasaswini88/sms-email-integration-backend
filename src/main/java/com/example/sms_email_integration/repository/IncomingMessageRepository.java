package com.example.sms_email_integration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.entity.IncomingMessage;

@Repository
public interface IncomingMessageRepository extends JpaRepository<IncomingMessage, Long> {

    List<IncomingMessage> findByOrderByReceivedAtDesc();

    

}
