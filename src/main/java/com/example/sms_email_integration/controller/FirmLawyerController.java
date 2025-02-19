package com.example.sms_email_integration.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;

@RestController
@RequestMapping("/api/firm-lawyers")
public class FirmLawyerController {

    private final FirmLawyerRepository firmLawyerRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    private FirmClientMappingRepository firmClientMappingRepository;

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

        @PostMapping("/bulk-insert")
public ResponseEntity<?> bulkInsertLawyers(
        @RequestBody List<HashMap<String, String>> lawyerDtos,
        @Param("custiId") Long custiId
) {
    try {
        System.out.println("lawyerDtos: " + lawyerDtos);
        System.err.println("custiId: " + custiId);

        // 1) Retrieve the firm by ID:
        Optional<Customer> optionalCustomer = customerRepository.findById(custiId);
        if (optionalCustomer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Customer theFirm = optionalCustomer.get();

        // 2) Process each row in the CSV:
        for (HashMap<String, String> lawyerDto : lawyerDtos) {

            // (A) Create or find the lawyer:
            //     (In your code, you're always creating a new lawyer with empty name.)
            FirmLawyer newLawyer = new FirmLawyer();
            newLawyer.setLawyerName("");
            newLawyer.setLawyerMail(lawyerDto.get("Lawyer Email"));  // from CSV column
            newLawyer.setLawyerPassword("1234");
            newLawyer.setLawyerRole("LAWYER");
            newLawyer.setFirm(theFirm);

            // Save the new lawyer
            firmLawyerRepository.save(newLawyer);

            // (B) Create the firm-client mapping:
            FirmClientMapping firmClientMapping = new FirmClientMapping();
            firmClientMapping.setFirm(theFirm);
            firmClientMapping.setFirmLawyer(newLawyer);
            firmClientMapping.setCaseType("Unknown"); // or parse from CSV if needed

            // Pull the client phone from CSV:
            // e.g. "Client Phone Number" => +13657774034
            firmClientMapping.setClientPhoneNumber(lawyerDto.get("Client Phone Number"));

            // Pull the Twilio Number from CSV:
            // e.g. "Twilio Number" => +17575688750
            firmClientMapping.setTwilioNumber(lawyerDto.get("Twilio Number"));

            // Save the new mapping row
            firmClientMappingRepository.save(firmClientMapping);
        }

        return ResponseEntity.ok("Data inserted successfully");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error inserting data: " + e.getMessage());
    }
}


}
