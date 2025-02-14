package com.example.sms_email_integration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.sms_email_integration.dto.EmailIncomingDto;
import com.example.sms_email_integration.entity.EmailIncoming;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.EmailIncomingRepository;

@Service
public class EmailIncomingService {

    private final EmailIncomingRepository emailIncomingRepository;

    public EmailIncomingService(EmailIncomingRepository emailIncomingRepository) {
        this.emailIncomingRepository = emailIncomingRepository;
    }

    /**
     * Create and store a new EmailIncoming record.
     */
       public EmailIncomingDto createEmailIncoming(String clientPhoneNumber,
                                                FirmLawyer lawyer,
                                                Long firmId,
                                                String direction) {
        EmailIncoming entity = new EmailIncoming();
        entity.setClientPhoneNumber(clientPhoneNumber);
        entity.setLawyer(lawyer);
        entity.setCustiId(firmId);
        entity.setReceivedAt(LocalDateTime.now());

        // Set the direction
        entity.setDirection(direction); // "INCOMING" or "OUTGOING"

        EmailIncoming saved = emailIncomingRepository.save(entity);
        return entityToDto(saved);
    }

    public EmailIncomingDto getById(Long id) {
        Optional<EmailIncoming> optional = emailIncomingRepository.findById(id);
        return optional.map(this::entityToDto).orElse(null);
    }

    public List<EmailIncomingDto> getAll() {
        return emailIncomingRepository.findAll()
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // ... update/delete methods as needed ...

    private EmailIncomingDto entityToDto(EmailIncoming entity) {
        EmailIncomingDto dto = new EmailIncomingDto();
        dto.setId(entity.getId());
        dto.setClientPhoneNumber(entity.getClientPhoneNumber());
        dto.setCustiId(entity.getCustiId());
        dto.setReceivedAt(entity.getReceivedAt());
        
        // Lawyer’s email
        if (entity.getLawyer() != null) {
            dto.setLawyerEmail(entity.getLawyer().getLawyerMail());
        }
        return dto;
    }

    public List<EmailIncomingDto> getAllDescending() {
        List<EmailIncoming> entities = emailIncomingRepository.findByOrderByReceivedAtDesc();
        return entities.stream()
                       .map(this::entityToDto)  // Reuse your existing mapping method
                       .collect(Collectors.toList());
    }
    
}
