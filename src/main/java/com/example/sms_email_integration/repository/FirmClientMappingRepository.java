package com.example.sms_email_integration.repository;

import com.example.sms_email_integration.entity.FirmClientMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface FirmClientMappingRepository extends JpaRepository<FirmClientMapping, Long> {

    @Query(value = "SELECT * FROM firm_client_lawyer WHERE client_phone_number = :clientPhoneNumber AND custi_id = :custiId", nativeQuery = true)
    Optional<FirmClientMapping> findByClientPhoneNumberAndCustiId(String clientPhoneNumber, Long custiId);

    @Query(value = "SELECT * FROM firm_client_lawyer WHERE lawyer_id = :lawyerId AND client_phone_number = :phoneNumber", nativeQuery = true)
    Optional<FirmClientMapping> findByLawyerIdAndClientPhoneNumber(Long lawyerId, String phoneNumber);
}
