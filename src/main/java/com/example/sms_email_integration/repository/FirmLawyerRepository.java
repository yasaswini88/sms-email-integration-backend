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

    @Query(value = "SELECT * FROM firm_lawyers ORDER BY lawyer_id ASC LIMIT 1", nativeQuery = true)
    Optional<FirmLawyer> getFirstLawyer();

    @Query(value = "SELECT * FROM firm_lawyers", nativeQuery = true)
List<FirmLawyer> getAllLawyers();

    @Query(value="select fl.* from firm_lawyers fl left join (SELECT COUNT(*) as count,lawyer_id FROM firm_client_lawyer WHERE custi_id = :custiId GROUP BY lawyer_id) fcl on fl.lawyer_id  = fcl.lawyer_id where fl.custi_id = :custiId2 order by fcl.count asc limit 1",nativeQuery = true)
    FirmLawyer getLawyerRoundRobin(Long custiId,Long custiId2);


}
