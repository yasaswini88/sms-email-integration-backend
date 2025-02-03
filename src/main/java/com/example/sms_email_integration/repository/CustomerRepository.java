package com.example.sms_email_integration.repository;

import com.example.sms_email_integration.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Custom finder to look up a customer by their Twilio phone number
    Optional<Customer> findByTwilioNumber(String twilioNumber);
}
