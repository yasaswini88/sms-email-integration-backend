package com.example.sms_email_integration.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.repository.ConversationThreadRepository;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private ConversationThreadRepository conversationThreadRepository;

    @GetMapping("/customer/caseType/{customerId}")
    public List getCaseDistributionByCustomer(@PathVariable Long customerId) {
        return conversationThreadRepository.getActiveThreadCountByCaseType(customerId);
    }

    @GetMapping("/customer/caseStatus/{customerId}")
    public List getCaseStatusMetricData(@PathVariable Long customerId) {
        return conversationThreadRepository.getCaseStatusMetrics(customerId);
    }
}