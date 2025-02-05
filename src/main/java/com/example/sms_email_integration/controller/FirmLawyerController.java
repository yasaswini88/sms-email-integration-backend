package com.example.sms_email_integration.controller;

import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.sms_email_integration.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/firm-lawyers")
public class FirmLawyerController {

    private final FirmLawyerRepository firmLawyerRepository;
    private final CustomerRepository customerRepository;

    // Constructor injection for both repositories:
    public FirmLawyerController(
            FirmLawyerRepository firmLawyerRepository,
            CustomerRepository customerRepository
    ) {
        this.firmLawyerRepository = firmLawyerRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/firm/{custiId}")
    public ResponseEntity<List<FirmLawyer>> getLawyersByFirmId(@PathVariable Long custiId) {
        List<FirmLawyer> lawyers = firmLawyerRepository.getLawyersByFirmId(custiId);
        return ResponseEntity.ok(lawyers);
    }

    @PostMapping("/firm/{custiId}")
    public ResponseEntity<FirmLawyer> createLawyerForFirm(@PathVariable Long custiId,
                                                          @RequestBody FirmLawyer newLawyer) {
        // 1) Find the firm
        Optional<Customer> optionalCustomer = customerRepository.findById(custiId);
        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // 2) Assign
        newLawyer.setFirm(optionalCustomer.get());
        // 3) Save
        FirmLawyer savedLawyer = firmLawyerRepository.save(newLawyer);
        return ResponseEntity.ok(savedLawyer);
    }
}
