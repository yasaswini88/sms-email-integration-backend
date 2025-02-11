package com.example.sms_email_integration.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.FirmLawyerDto;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.FirmLawyerRepository;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private FirmLawyerRepository firmLawyerRepository;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String password = requestBody.get("password");  // <== read password from the request

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        Optional<FirmLawyer> optLawyer = firmLawyerRepository.getLawyerByEmail(email);
        if (optLawyer.isPresent()) {
            FirmLawyer lawyer = optLawyer.get();

            
            if (lawyer.getLawyerPassword().equals(password)) {
                // Build your DTO
               FirmLawyerDto dto = new FirmLawyerDto(
    lawyer.getLawyerId(),
    lawyer.getLawyerName(),
    lawyer.getLawyerMail(),
    lawyer.getFirm(),
    lawyer.getLawyerPassword(),
    lawyer.getLawyerRole()  // <--- new
);

                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.status(401).body("Invalid password");
            }
        }

        // If not found
        return ResponseEntity.status(404).body("Lawyer not found");
    }
}
