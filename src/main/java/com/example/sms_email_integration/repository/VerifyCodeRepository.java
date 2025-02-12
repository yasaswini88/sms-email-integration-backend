package com.example.sms_email_integration.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.sms_email_integration.entity.VerifyCode;

@Repository
public interface VerifyCodeRepository extends JpaRepository<VerifyCode, Long> {

    // Find code by lawyerId
    Optional<VerifyCode> findByLawyerId(Long lawyerId);

    // Optionally, find by lawyerId and code
    Optional<VerifyCode> findByLawyerIdAndCode(Long lawyerId, String code);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerifyCode vc WHERE vc.lawyerId = :lawyerId")
    void deleteAllByLawyerId(@Param("lawyerId") Long lawyerId);



}
