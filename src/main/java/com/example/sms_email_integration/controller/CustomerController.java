package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.CustomerDto;
import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.ConversationRepository;
import com.example.sms_email_integration.repository.ConversationThreadRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.CustomerService;
import com.example.sms_email_integration.service.EmailService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final FirmLawyerRepository firmLawyerRepository;
    private final ConversationRepository conversationRepository;
    private final FirmClientMappingRepository firmClientMappingRepository;
    
    @Autowired
private ConversationThreadRepository conversationThreadRepository;

@Autowired
    private ConversationService conversationService;



    private EmailService emailService;

    public CustomerController(CustomerService customerService, FirmLawyerRepository firmLawyerRepository, ConversationRepository conversationRepository, FirmClientMappingRepository firmClientMappingRepository, EmailService emailService) {
        this.customerService = customerService;
        this.firmLawyerRepository = firmLawyerRepository;
        this.conversationRepository = conversationRepository;
        this.firmClientMappingRepository = firmClientMappingRepository;
        this.emailService = emailService;
    }


    // CREATE
    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody CustomerDto customerDto) {
        CustomerDto created = customerService.createCustomer(customerDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // READ - Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable Long id) {
        CustomerDto dto = customerService.getCustomerById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    // READ - Get all
    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<CustomerDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long id,
                                                      @RequestBody CustomerDto customerDto) {
        CustomerDto updated = customerService.updateCustomer(id, customerDto);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }




@PutMapping("/assign/thread/{lawyerId}/{threadId}")
public ResponseEntity<String> assignLawyerToThread(@PathVariable Long lawyerId,
                                                  @PathVariable Long threadId) 
{
    // 1) Fetch the OLD thread
    ConversationThread oldThread = conversationThreadRepository.findById(threadId)
        .orElseThrow(() -> new RuntimeException("Thread not found with ID: " + threadId));

    // Mark old thread as INACTIVE
    oldThread.setStatus("INACTIVE");
    conversationThreadRepository.save(oldThread);

    // Store the old thread’s caseType so we can copy it
    String oldCaseType = oldThread.getCaseType();

    // 2) From the oldThread, get the phoneNumber
    String clientPhoneNumber = oldThread.getPhoneNumber();

    // 3) Fetch the new Lawyer
    FirmLawyer firmLawyer = firmLawyerRepository.getLawyerByLawyerId(lawyerId);
    if (firmLawyer == null) {
        return ResponseEntity.badRequest().body("Lawyer not found with ID: " + lawyerId);
    }

    // We'll need the Firm's ID as well, for the mapping lookup
    Long firmId = firmLawyer.getFirm().getCusti_id();

    // 4) Find an existing mapping by phoneNumber+firmId (NOT by old lawyerId),
    //    then update that row’s lawyer.  If no row exists for that phone => new mapping
    Optional<FirmClientMapping> existingMappingOpt =
        firmClientMappingRepository.findByClientPhoneNumberAndCustiId(clientPhoneNumber, firmId);

    FirmClientMapping savedMapping;
    if (existingMappingOpt.isPresent()) {
        // *** Update existing row => set new Lawyer
        FirmClientMapping mapping = existingMappingOpt.get();
        mapping.setFirmLawyer(firmLawyer); // reassign the lawyer
        savedMapping = firmClientMappingRepository.save(mapping);
    } else {
        // *** If truly no row => create a new mapping
        FirmClientMapping newMapping = new FirmClientMapping();
        newMapping.setFirmLawyer(firmLawyer);
        newMapping.setClientPhoneNumber(clientPhoneNumber);
        newMapping.setFirm(firmLawyer.getFirm());
        savedMapping = firmClientMappingRepository.save(newMapping);
    }

    // 5) CREATE A BRAND NEW THREAD FOR THE NEW LAWYER
    ConversationThread newThread = new ConversationThread();
    newThread.setPhoneNumber(oldThread.getPhoneNumber());
    newThread.setToNumber(oldThread.getToNumber());
    newThread.setCreatedAt(LocalDateTime.now());
    newThread.setStatus("ACTIVE");

    // Use the new lawyer's email
    newThread.setEmail(firmLawyer.getLawyerMail());

    // *** Copy the old caseType so we don't lose it
    newThread.setCaseType(oldCaseType);

    // For a new threadId, pick something unique (e.g., "phoneNumber-lawyerEmail")
    String newThreadId = clientPhoneNumber + "-" + firmLawyer.getLawyerMail();
    newThread.setThreadId(newThreadId);

    ConversationThread savedNewThread = conversationThreadRepository.save(newThread);

    // 6) (Optional) Forward the last message from old thread to the newly assigned lawyer
    List<Conversation> threadConversations =
        conversationRepository.findByConversationThread_ConversationThreadId(threadId);

    if (!threadConversations.isEmpty()) {
        // the last message in the old thread
        Conversation lastMessage = threadConversations.get(threadConversations.size() - 1);

        try {
            // Optionally email the last message content to the new lawyer
            emailService.sendEmail(
                firmLawyer.getLawyerMail(),          // new lawyer's email
                lastMessage.getSubject(),            // subject
                lastMessage.getMessage(),            // body
                savedNewThread.getPhoneNumber(),     // fromNumber for the custom header
                savedNewThread.getToNumber(),        // toNumber
                null                                 // TwilioMessageSid or any message ID
            );

            // Optionally store that forward in Conversation as an "OUTGOING EMAIL"
            // conversationService.saveConversation(
            //     savedNewThread.getPhoneNumber(),
            //     savedNewThread.getToNumber(),
            //     savedNewThread.getEmail(),
            //     lastMessage.getMessage(),
            //     "OUTGOING",
            //     "EMAIL",
            //     lastMessage.getSubject(),
            //     savedNewThread.getThreadId(),  
            //     "manual-forward-" + System.currentTimeMillis()
            // );

        } catch (Exception ex) {
            System.err.println("Error sending email to new lawyer: " + ex.getMessage());
        }
    }

    return ResponseEntity.ok(
        "Reassigned to new lawyer. OldThreadID=" + threadId 
        + " => INACTIVE, newThreadID=" + savedNewThread.getConversationThreadId() 
        + " with caseType=" + oldCaseType
    );
}




    @GetMapping("/{id}/lawyers")
    public ResponseEntity<List<FirmLawyer>> getLawyers(@PathVariable Long id) {
        List<FirmLawyer> lawyers = firmLawyerRepository.getLawyersByFirmId(id);
        return ResponseEntity.ok(lawyers);
    }

   
}
