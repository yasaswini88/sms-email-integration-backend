package com.example.sms_email_integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long custi_id;  

    @Column(name = "cust_mail", nullable = false)
    private String custMail;

    @Column(name = "cust_name", nullable = false)
    private String custName;

    @Column(name = "twilio_number", nullable = false, unique = true)
    private String twilioNumber;


    @Column(name = "enabled_assigned_lawyer", nullable = false)    // Default constructor
    private String EnabledAssignedLawyer;


    public Customer() {}

    // Parameterized constructor
    public Customer(String custMail, String custName, String twilioNumber, String EnabledAssignedLawyer) {
        this.custMail = custMail;
        this.custName = custName;
        this.twilioNumber = twilioNumber;
        this.EnabledAssignedLawyer = EnabledAssignedLawyer;
    }

    // Getters and Setters
    public Long getCusti_id() {
        return custi_id;
    }

    public void setCusti_id(Long custi_id) {
        this.custi_id = custi_id;
    }

    public String getCustMail() {
        return custMail;
    }

    public void setCustMail(String custMail) {
        this.custMail = custMail;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getTwilioNumber() {
        return twilioNumber;
    }

    public void setTwilioNumber(String twilioNumber) {
        this.twilioNumber = twilioNumber;
    }

    public String getEnabledAssignedLawyer() {
        return EnabledAssignedLawyer;
    }


    public void setEnabledAssignedLawyer(String EnabledAssignedLawyer) {
        this.EnabledAssignedLawyer = EnabledAssignedLawyer;
    }


    @Override
    public String toString() {
        return "Customer{" +
                "custi_id=" + custi_id +
                ", custMail='" + custMail + '\'' +
                ", custName='" + custName + '\'' +
                ", twilioNumber='" + twilioNumber + '\'' +
                ", EnabledAssignedLawyer='" + EnabledAssignedLawyer + '\'' +
                '}';
    }
}
