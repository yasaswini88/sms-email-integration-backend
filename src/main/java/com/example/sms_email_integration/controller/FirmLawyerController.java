package com.example.sms_email_integration.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;

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

    //      @PostMapping("/bulk-insert")
    // public ResponseEntity<?> bulkInsertLawyers(@RequestBody List<FirmLawyerDto> lawyerDtos) {
    //     try {
    //         List<FirmLawyer> lawyers = lawyerDtos.stream().map(dto -> {
    //             FirmLawyer lawyer = new FirmLawyer();
    //             lawyer.setLawyerName(dto.getLawyerName());
    //             lawyer.setLawyerMail(dto.getLawyerMail());
    //             lawyer.setLawyerPassword(dto.getLawyerPassword());
    //             lawyer.setLawyerRole(dto.getLawyerRole());

    //             // Find the firm (Customer) and set it
    //             Customer firm = customerRepository.findById(dto.getFirm().getCusti_id())
    //                     .orElseThrow(() -> new RuntimeException("Firm not found: " + dto.getFirm().getCusti_id()));
    //             lawyer.setFirm(firm);
    //             return lawyer;
    //         }).collect(Collectors.toList());

    //         firmLawyerRepository.saveAll(lawyers);  // Bulk insert operation
    //         return ResponseEntity.ok("Bulk insert successful");
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inserting data: " + e.getMessage());
    //     }
    // }

}
