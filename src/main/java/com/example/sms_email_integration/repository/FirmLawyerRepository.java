package com.example.sms_email_integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.entity.FirmLawyer;


@Repository
public interface FirmLawyerRepository extends JpaRepository<FirmLawyer, Long> {

    // Find all lawyers associated with a specific firm (customer)
    @Query(value = "SELECT * FROM firm_lawyers WHERE custi_id = :custiId", nativeQuery = true)
    List<FirmLawyer> getLawyersByFirmId(Long custiId);

    // Find a lawyer by their phone number
    @Query(value = "SELECT * FROM firm_lawyers WHERE lawyer_id = :lawyerId", nativeQuery = true)
    FirmLawyer getLawyerByLawyerId(Long lawyerId);


    @Query(value = "SELECT * FROM firm_lawyers WHERE lawyer_mail = :email", nativeQuery = true)
    Optional<FirmLawyer> getLawyerByEmail(String email);

    @Query(value = "SELECT COUNT(*) FROM firm_lawyers WHERE custi_id = :custiId", nativeQuery = true)
    int countLawyersByFirmId(Long custiId);



}
