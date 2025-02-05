// package com.example.sms_email_integration.service;

// import com.example.sms_email_integration.dto.FirmLawyerDto;
// import com.example.sms_email_integration.entity.Customer;
// import com.example.sms_email_integration.entity.FirmLawyer;
// import com.example.sms_email_integration.repository.CustomerRepository;
// import com.example.sms_email_integration.repository.FirmLawyerRepository;
// import org.springframework.stereotype.Service;

// import java.util.List;
// import java.util.Optional;
// import java.util.stream.Collectors;

// @Service
// public class FirmLawyerService {

//     private final FirmLawyerRepository firmLawyerRepository;
//     private final CustomerRepository customerRepository;

//     public FirmLawyerService(FirmLawyerRepository firmLawyerRepository, CustomerRepository customerRepository) {
//         this.firmLawyerRepository = firmLawyerRepository;
//         this.customerRepository = customerRepository;
//     }

//     // CREATE: Add a new lawyer to a firm
//     public FirmLawyerDto createLawyer(FirmLawyerDto lawyerDto) {
//         Optional<Customer> firm = customerRepository.findById(lawyerDto.getCustiId());

//         if (firm.isEmpty()) {
//             throw new RuntimeException("Firm (Customer) not found with ID: " + lawyerDto.getCustiId());
//         }

//         FirmLawyer lawyer = new FirmLawyer(lawyerDto.getLawyerName(), lawyerDto.getLawyerMail(), firm.get());
//         FirmLawyer saved = firmLawyerRepository.save(lawyer);
//         return entityToDto(saved);
//     }

//     // READ: Get all lawyers for a firm
//     public List<FirmLawyerDto> getLawyersByFirm(Long custiId) {
//         List<FirmLawyer> lawyers = firmLawyerRepository.findByFirmLawyer_Custi_id(custiId);
//         return lawyers.stream().map(this::entityToDto).collect(Collectors.toList());
//     }

//     // READ: Get lawyer by ID
//     public FirmLawyerDto getLawyerById(Long lawyerId) {
//         Optional<FirmLawyer> optionalLawyer = firmLawyerRepository.findById(lawyerId);
//         return optionalLawyer.map(this::entityToDto).orElse(null);
//     }

//     // DELETE: Remove a lawyer from a firm
//     public boolean deleteLawyer(Long lawyerId) {
//         if (firmLawyerRepository.existsById(lawyerId)) {
//             firmLawyerRepository.deleteById(lawyerId);
//             return true;
//         }
//         return false;
//     }

//     private FirmLawyerDto entityToDto(FirmLawyer lawyer) {
//         return new FirmLawyerDto(lawyer.getLawyerId(), lawyer.getLawyerName(), lawyer.getLawyerMail(), lawyer.getFirm().getCusti_id());
//     }
// }
