package com.example.sms_email_integration.entity;

import jakarta.persistence.*;

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

    // Constructors
    public FirmLawyer() {}

    public FirmLawyer(String lawyerName, String lawyerMail, Customer firm) {
        this.lawyerName = lawyerName;
        this.lawyerMail = lawyerMail;
        this.firm = firm;
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
}
