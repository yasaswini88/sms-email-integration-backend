package com.example.sms_email_integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.sms_email_integration.entity.FirmClientMapping;


@Repository
public interface FirmClientMappingRepository extends JpaRepository<FirmClientMapping, Long> {

    @Query(value = "SELECT * FROM firm_client_lawyer WHERE client_phone_number = :clientPhoneNumber AND custi_id = :custiId", nativeQuery = true)
    Optional<FirmClientMapping> findByClientPhoneNumberAndCustiId(String clientPhoneNumber, Long custiId);

    @Query(value = "SELECT * FROM firm_client_lawyer WHERE lawyer_id = :lawyerId AND client_phone_number = :phoneNumber", nativeQuery = true)
    List<FirmClientMapping> findByLawyerIdAndClientPhoneNumber(Long lawyerId, String phoneNumber);

    //  @Query(value = "SELECT * FROM firm_client_lawyer WHERE client_phone_number = :clientPhoneNumber", nativeQuery = true)
    // Optional<FirmClientMapping> findByClientPhoneNumber(String clientPhoneNumber);

    @Query(value = "SELECT * FROM firm_client_lawyer WHERE client_phone_number = :clientPhoneNumber", nativeQuery = true)
List<FirmClientMapping> findByClientPhoneNumber(String clientPhoneNumber);


    @Query(value = "SELECT * FROM firm_client_lawyer WHERE client_phone_number = :clientPhoneNumber AND custi_id = :custiId AND case_type = :caseType LIMIT 1", 
       nativeQuery = true)
    Optional<FirmClientMapping> findByPhoneFirmCaseType(String clientPhoneNumber, Long custiId, String caseType);

    @Query(value="SELECT * FROM firm_client_lawyer WHERE client_phone_number = :phoneNumber AND custi_id = :firmId AND case_type <> 'Unknown'",nativeQuery = true)
    List<FirmClientMapping> findNonUnknownMapping(String phoneNumber, Long firmId);


     @Query(value = "SELECT COUNT(*) FROM firm_client_lawyer WHERE lawyer_id IS NOT NULL AND custi_id = :firmId", nativeQuery = true)
    int countByLawyerIdIsNotNullAndFirmId(Long firmId);



    @Query(value = "SELECT COUNT(*), lawyer_id FROM firm_client_lawyer WHERE lawyer_id IS NOT NULL AND custi_id = :firmId GROUP BY lawyer_id", nativeQuery = true)
    List<Object[]> countByLawyerIdIsNotNullAndFirmIdGroupedByLawyerId(Long firmId);


    @Query(value = """
    SELECT *
      FROM firm_client_lawyer
     WHERE client_phone_number = :clientPhoneNumber
       AND twilio_number       = :twilioNumber
       AND custi_id           = :firmId
    """, nativeQuery = true)
Optional<FirmClientMapping> findByAniAndDnisAndFirmId(
    String clientPhoneNumber,
    String twilioNumber,
    Long firmId
);


@Query(value = """
    SELECT *
      FROM firm_client_lawyer
     WHERE client_phone_number = :ani
       AND twilio_number       = :dnis
    LIMIT 1
""", nativeQuery = true)
Optional<FirmClientMapping> findByAniAndDnis(String ani, String dnis);






    
}
