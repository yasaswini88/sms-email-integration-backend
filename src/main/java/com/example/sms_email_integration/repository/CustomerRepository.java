package com.example.sms_email_integration.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Custom finder to look up a customer by their Twilio phone number
    Optional<Customer> findByTwilioNumber(String twilioNumber);
    Optional<Customer> findByCustMail(String custMail);

   
}
