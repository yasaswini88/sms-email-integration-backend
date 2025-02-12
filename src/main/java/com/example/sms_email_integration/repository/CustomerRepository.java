package com.example.sms_email_integration.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Custom finder to look up a customer by their Twilio phone number
    Optional<Customer> findByTwilioNumber(String twilioNumber);
    Optional<Customer> findByCustMail(String custMail);
    // Optional<Customer> findByCustiId(Long custiId);



 

    
    @Query(value = "SELECT * FROM customer WHERE custi_id = :custi_id AND enabled_assigned_lawyer = :enabled_assigned_lawyer " ,nativeQuery = true)

     Optional<Customer> findByCustiIdAndEnabledAssignedLawyer(Long custi_id, String enabled_assigned_lawyer);


   
}
