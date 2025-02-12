package com.example.sms_email_integration.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.sms_email_integration.dto.CustomerDto;
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // CREATE
    public CustomerDto createCustomer(CustomerDto customerDto) {
        // Convert DTO to Entity
        Customer customer = new Customer(
                customerDto.getCustMail(),
                customerDto.getCustName(),
                customerDto.getTwilioNumber(),
                customerDto.getEnabledAssignedLawyer(),
                customerDto.getFirmAddress(),
                customerDto.getCity(),
                customerDto.getState(),
                customerDto.getZipCode()

        );

        // ------------------------------
        // 1) Set the new address fields
        // ------------------------------
        customer.setFirmAddress(customerDto.getFirmAddress());
        customer.setCity(customerDto.getCity());
        customer.setState(customerDto.getState());
        customer.setZipCode(customerDto.getZipCode());

        // Save entity
        Customer saved = customerRepository.save(customer);

        // Convert back to DTO
        return entityToDto(saved);
    }

    // READ - get by ID
    public CustomerDto getCustomerById(Long id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        return optionalCustomer.map(this::entityToDto).orElse(null);
    }

    // READ - get all
    public List<CustomerDto> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // UPDATE
    public CustomerDto updateCustomer(Long id, CustomerDto updatedDto) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        if (optionalCustomer.isEmpty()) {
            return null; // Or throw an exception
        }
        Customer existingCustomer = optionalCustomer.get();
        // Update fields
        existingCustomer.setCustMail(updatedDto.getCustMail());
        existingCustomer.setCustName(updatedDto.getCustName());
        existingCustomer.setTwilioNumber(updatedDto.getTwilioNumber());
        existingCustomer.setEnabledAssignedLawyer(updatedDto.getEnabledAssignedLawyer());

        // ------------------------------
        // 2) Update new address fields
        // ------------------------------
        existingCustomer.setFirmAddress(updatedDto.getFirmAddress());
        existingCustomer.setCity(updatedDto.getCity());
        existingCustomer.setState(updatedDto.getState());
        existingCustomer.setZipCode(updatedDto.getZipCode());

        // Save
        Customer saved = customerRepository.save(existingCustomer);
        // Convert back to DTO
        return entityToDto(saved);
    }

    // DELETE
    public boolean deleteCustomer(Long id) {
        Optional<Customer> optionalCustomer = customerRepository.findById(id);
        if (optionalCustomer.isPresent()) {
            customerRepository.delete(optionalCustomer.get());
            return true;
        }
        return false;
    }

    private CustomerDto entityToDto(Customer customer) {
        // Build a DTO from the Customer entity
        CustomerDto dto = new CustomerDto();
        dto.setCustiId(customer.getCusti_id());
        dto.setCustMail(customer.getCustMail());
        dto.setCustName(customer.getCustName());
        dto.setTwilioNumber(customer.getTwilioNumber());
        dto.setEnabledAssignedLawyer(customer.getEnabledAssignedLawyer());

        // ------------------------------
        // 3) Map new address fields
        // ------------------------------
        dto.setFirmAddress(customer.getFirmAddress());
        dto.setCity(customer.getCity());
        dto.setState(customer.getState());
        dto.setZipCode(customer.getZipCode());

        return dto;
    }
}


