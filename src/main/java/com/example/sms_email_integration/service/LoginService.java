

package com.example.sms_email_integration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.FirmLawyerRepository;

@Service
public class LoginService {

    

    @Autowired
    private FirmLawyerRepository firmLawyersRepository;

    public ResponseEntity<FirmLawyer> isEmailPresent(String email) {
        return  firmLawyersRepository.getLawyerByEmail(email).isPresent() ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}