package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
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
import com.example.sms_email_integration.entity.VerifyCode;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.repository.VerifyCodeRepository;
import com.example.sms_email_integration.service.EmailService;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private FirmLawyerRepository firmLawyerRepository;

    @Autowired
    private VerifyCodeRepository verifyCodeRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String password = requestBody.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        Optional<FirmLawyer> optLawyer = firmLawyerRepository.getLawyerByEmail(email);
        if (optLawyer.isPresent()) {
            FirmLawyer lawyer = optLawyer.get();
            if (lawyer.getLawyerPassword().equals(password)) {

                // 1) Generate 4-digit code
                String fourDigitCode = generateRandom4DigitCode();

                // 2) Create or update a row in verify_code for this lawyerId
                verifyCodeRepository.deleteAllByLawyerId(lawyer.getLawyerId());
                VerifyCode vc = new VerifyCode(
                        lawyer.getLawyerId(),
                        fourDigitCode,
                        LocalDateTime.now()
                );
                verifyCodeRepository.save(vc);

                // 3) Send the code to the lawyer’s email
                try {
                    emailService.sendVerificationCode(lawyer.getLawyerMail(), fourDigitCode);
                } catch (Exception e) {
                    return ResponseEntity.status(500).body("Failed to send verification code");
                }

                // 4) Return a response telling the frontend “password is correct, now wait for code”
                return ResponseEntity.ok("PASSWORD_OK_NEED_CODE");

            } else {
                return ResponseEntity.status(401).body("Invalid password");
            }
        }
        // If not found
        return ResponseEntity.status(404).body("Lawyer not found");
    }
// Helper method to generate a random 4-digit code

    private String generateRandom4DigitCode() {
        int code = (int) (Math.random() * 9000) + 1000; // ensures a 4-digit code
        return String.valueOf(code);
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String code = requestBody.get("code");

        if (email == null || code == null) {
            return ResponseEntity.badRequest().body("Missing email or code");
        }

        // 1) Find the lawyer by email
        Optional<FirmLawyer> optLawyer = firmLawyerRepository.getLawyerByEmail(email);
        if (optLawyer.isEmpty()) {
            return ResponseEntity.status(404).body("Lawyer not found");
        }
        FirmLawyer lawyer = optLawyer.get();

        // 2) Look up the code row
        Optional<VerifyCode> vcOpt = verifyCodeRepository.findByLawyerIdAndCode(lawyer.getLawyerId(), code);
        if (vcOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid code");
        }

        VerifyCode vc = vcOpt.get();

        // 3) Check if code is older than 2 minutes
        LocalDateTime now = LocalDateTime.now();
        if (vc.getCreatedAt().plusMinutes(2).isBefore(now)) {
            // code expired
            verifyCodeRepository.delete(vc); // clean up
            return ResponseEntity.status(401).body("Code expired");
        }

        // 4) If code is valid, build your FirmLawyerDto
        FirmLawyerDto dto = new FirmLawyerDto(
                lawyer.getLawyerId(),
                lawyer.getLawyerName(),
                lawyer.getLawyerMail(),
                lawyer.getFirm(),
                lawyer.getLawyerPassword(),
                lawyer.getLawyerRole()
        );

        // 5) Remove the code so it can’t be reused
        verifyCodeRepository.delete(vc);

        // 6) Return the lawyer’s data => finalize login
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/resendCode")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing email");
        }

        Optional<FirmLawyer> optLawyer = firmLawyerRepository.getLawyerByEmail(email);
        if (optLawyer.isEmpty()) {
            return ResponseEntity.status(404).body("Lawyer not found");
        }

        FirmLawyer lawyer = optLawyer.get();
        // same code generation logic from your "login" method:
        String fourDigitCode = generateRandom4DigitCode();

        // remove old code if needed
        verifyCodeRepository.deleteAllByLawyerId(lawyer.getLawyerId());

        // create a new record
        VerifyCode vc = new VerifyCode(
                lawyer.getLawyerId(),
                fourDigitCode,
                LocalDateTime.now()
        );
        verifyCodeRepository.save(vc);

        // re-send code
        try {
            emailService.sendVerificationCode(lawyer.getLawyerMail(), fourDigitCode);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to resend verification code");
        }

        return ResponseEntity.ok("Code resent");
    }

}
