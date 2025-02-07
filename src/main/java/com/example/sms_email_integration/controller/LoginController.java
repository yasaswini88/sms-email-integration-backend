package com.example.sms_email_integration.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.FirmLawyerDto;
import com.example.sms_email_integration.repository.FirmLawyerRepository;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private FirmLawyerRepository firmLawyersRepository;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email parameter is missing");
        }

        return firmLawyersRepository.getLawyerByEmail(email)
                .map(lawyer -> {
                    
                    FirmLawyerDto dto = new FirmLawyerDto(
                            lawyer.getLawyerId(),
                            lawyer.getLawyerName(),
                            lawyer.getLawyerMail(),
                            lawyer.getFirm() 
                    );
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
