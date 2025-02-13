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

    @Column(name = "twilio_number", nullable = false, unique = false)
    private String twilioNumber;


    @Column(name = "enabled_assigned_lawyer", nullable = true)    // Default constructor
    private String EnabledAssignedLawyer;

     @Column(name = "firm_address", nullable = true)
    private String firmAddress;

    @Column(name = "city", nullable = true)
    private String city;

    @Column(name = "state", nullable = true)
    private String state;

    @Column(name = "zip_code", nullable = true)
    private String zipCode;


    public Customer() {}

    // Parameterized constructor
    public Customer(String custMail, String custName, String twilioNumber, String EnabledAssignedLawyer, String firmAddress, String city, String state, String zipCode) {
        this.custMail = custMail;
        this.custName = custName;
        this.twilioNumber = twilioNumber;
        this.EnabledAssignedLawyer = EnabledAssignedLawyer;
        this.firmAddress = firmAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;

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

    public String getFirmAddress() {
        return firmAddress;
    }


    public void setFirmAddress(String firmAddress) {
        this.firmAddress = firmAddress;
    }

    public String getCity() {
        return city;
    }

    
    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }


    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }


    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }





    @Override
    public String toString() {
        return "Customer{" +
                "custi_id=" + custi_id +
                ", custMail='" + custMail + '\'' +
                ", custName='" + custName + '\'' +
                ", twilioNumber='" + twilioNumber + '\'' +
                ", EnabledAssignedLawyer='" + EnabledAssignedLawyer + '\'' +
                ", firmAddress='" + firmAddress + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +

                '}';
    }
}
