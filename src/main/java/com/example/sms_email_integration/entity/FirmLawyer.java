package com.example.sms_email_integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "firm_lawyers")
public class FirmLawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lawyerId;

    @Column(name = "lawyer_name", nullable = false)
    private String lawyerName;

    @Column(name = "lawyer_mail", nullable = false, unique = true)
    private String lawyerMail;

    @ManyToOne
    @JoinColumn(name = "custi_id", nullable = false)  // Foreign key linking to Customer (Firm)
    private Customer firm;

    @Column(name = "lawyer_password", nullable = true)
    private String lawyerPassword;

        @Column(name = "lawyer_role", nullable = true)
    private String lawyerRole;

    // Constructors
    public FirmLawyer() {}

    public FirmLawyer(String lawyerName, String lawyerMail, Customer firm, String lawyerPassword, String lawyerRole) {
        this.lawyerName = lawyerName;
        this.lawyerMail = lawyerMail;
        this.firm = firm;
        this.lawyerPassword = lawyerPassword;
        this.lawyerRole = lawyerRole;
    }

    // Getters and Setters
    public Long getLawyerId() { return lawyerId; }
    public void setLawyerId(Long lawyerId) { this.lawyerId = lawyerId; }

    public String getLawyerName() { return lawyerName; }
    public void setLawyerName(String lawyerName) { this.lawyerName = lawyerName; }

    public String getLawyerMail() { return lawyerMail; }
    public void setLawyerMail(String lawyerMail) { this.lawyerMail = lawyerMail; }

    public Customer getFirm() { return firm; }
    public void setFirm(Customer firm) { this.firm = firm; }

    public String getLawyerPassword() { return lawyerPassword; }
    public void setLawyerPassword(String lawyerPassword) { this.lawyerPassword = lawyerPassword; }

    public String getLawyerRole() { return lawyerRole; }
    public void setLawyerRole(String lawyerRole) { this.lawyerRole = lawyerRole; }
    
}
