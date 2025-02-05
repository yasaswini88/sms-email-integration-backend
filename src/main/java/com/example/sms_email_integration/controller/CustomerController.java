package com.example.sms_email_integration.controller;

import com.example.sms_email_integration.dto.CustomerDto;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.service.CustomerService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.repository.ConversationRepository;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.repository.*;
import com.example.sms_email_integration.entity.*;
import java.util.Optional;
import com.example.sms_email_integration.service.EmailService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final FirmLawyerRepository firmLawyerRepository;
    private final ConversationRepository conversationRepository;
    private final FirmClientMappingRepository firmClientMappingRepository;
    private EmailService emailService;

    public CustomerController(CustomerService customerService, FirmLawyerRepository firmLawyerRepository, ConversationRepository conversationRepository, FirmClientMappingRepository firmClientMappingRepository, EmailService emailService) {
        this.customerService = customerService;
        this.firmLawyerRepository = firmLawyerRepository;
        this.conversationRepository = conversationRepository;
        this.firmClientMappingRepository = firmClientMappingRepository;
        this.emailService = emailService;
    }


    // CREATE
    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody CustomerDto customerDto) {
        CustomerDto created = customerService.createCustomer(customerDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // READ - Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable Long id) {
        CustomerDto dto = customerService.getCustomerById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    // READ - Get all
    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<CustomerDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long id,
                                                      @RequestBody CustomerDto customerDto) {
        CustomerDto updated = customerService.updateCustomer(id, customerDto);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

   @PutMapping("/assign/{lawyerId}/{conversationId}")
    public ResponseEntity<String> assignLawyer(@PathVariable Long lawyerId, @PathVariable Long conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId);
        Optional<FirmClientMapping> firmClientMapping = firmClientMappingRepository.findByLawyerIdAndClientPhoneNumber(lawyerId,conversation.getPhoneNumber());
        FirmLawyer firmLawyer = firmLawyerRepository.getLawyerByLawyerId(lawyerId);
        FirmClientMapping savedfirmClientMapping = new FirmClientMapping();
        if(!firmClientMapping.isPresent()) {
            FirmClientMapping firmClientMapping1 = new FirmClientMapping();
            firmClientMapping1.setFirmLawyer(firmLawyer);
            firmClientMapping1.setClientPhoneNumber(conversation.getPhoneNumber());
            firmClientMapping1.setFirm(firmLawyer.getFirm());
            savedfirmClientMapping = firmClientMappingRepository.save(firmClientMapping1);
        }else{
            FirmClientMapping firmClientMapping2 = firmClientMapping.get();
            firmClientMapping2.setFirmLawyer(firmLawyer);
            savedfirmClientMapping = firmClientMappingRepository.save(firmClientMapping2);
        }


         try {
            emailService.sendEmail(
                    savedfirmClientMapping.getFirmLawyer().getLawyerMail(),    // dynamic email address
                    conversation.getSubject(),
                    conversation.getMessage(),
                    conversation.getPhoneNumber(),   // sets the custom header
                    conversation.getToNumber(),
                    null     

            );
            //System.out.println("Email forwarded successfully to " + senderEmail);
        } catch (Exception ex) {
            System.err.println("Error sending email: " + ex.getMessage());
        }
        return ResponseEntity.ok("");
    }


    @GetMapping("/{id}/lawyers")
    public ResponseEntity<List<FirmLawyer>> getLawyers(@PathVariable Long id) {
        List<FirmLawyer> lawyers = firmLawyerRepository.getLawyersByFirmId(id);
        return ResponseEntity.ok(lawyers);
    }
    


   
}
