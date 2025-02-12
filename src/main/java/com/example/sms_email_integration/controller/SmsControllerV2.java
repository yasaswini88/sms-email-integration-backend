package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.IntakeCheckDto;
import com.example.sms_email_integration.dto.NewCaseCheckDto;
import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.entity.IncomingMessage;
import com.example.sms_email_integration.repository.ConversationRepository;
import com.example.sms_email_integration.repository.ConversationThreadRepository;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.repository.IncomingMessageRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.EmailService;
import com.example.sms_email_integration.service.OpenAiService;
import com.example.sms_email_integration.service.SmsService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v3")
public class SmsControllerV2 {

    private final IncomingMessageRepository incomingMessageRepository;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;
    private final ConversationService conversationService;
    private final FirmClientMappingRepository firmClientMappingRepository;
    private final SmsService smsService;

    @Autowired
    private OpenAiService openAiService;

    @Autowired
    private ConversationThreadRepository conversationThreadRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private FirmLawyerRepository firmLawyerRepository;

    @Autowired
    public SmsControllerV2(
            IncomingMessageRepository incomingMessageRepository,
            EmailService emailService,
            CustomerRepository customerRepository,
            ConversationService conversationService,
            FirmClientMappingRepository firmClientMappingRepository,
            SmsService smsService
    ) {
        this.incomingMessageRepository = incomingMessageRepository;
        this.emailService = emailService;
        this.customerRepository = customerRepository;
        this.conversationService = conversationService;
        this.firmClientMappingRepository = firmClientMappingRepository;
        this.smsService = smsService;
    }

    @PostMapping(value = "/incoming-sms", consumes = "application/x-www-form-urlencoded")
    public void handleIncomingSms(
            @RequestParam("From") String fromNumber,
            @RequestParam("To") String toNumber,
            @RequestParam("Body") String messageBody,
            @RequestParam("MessageSid") String messageSid,
            HttpServletResponse response
    ) {

        System.out.println("Incoming SMS from " + fromNumber + " to " + toNumber + ": " + messageBody);

        // 1) Save incoming SMS to DB
        IncomingMessage incomingMsg = new IncomingMessage(fromNumber, toNumber, messageBody, LocalDateTime.now());
        incomingMessageRepository.save(incomingMsg);

        // 2) Log
        System.out.println("Incoming SMS from " + fromNumber + " to " + toNumber + ": " + messageBody);

        // 3) Identify the law-firm customer by 'toNumber'
        Optional<Customer> optionalCustomer = customerRepository.findByTwilioNumber(toNumber);
        if (optionalCustomer.isEmpty()) {
            System.err.println("No customer record found for Twilio number: " + toNumber);
            response.setContentType("text/plain");
            return;
        }

        Customer customer = optionalCustomer.get();
        Long firmId = customer.getCusti_id();

        // Check if this phoneNumber has a firmClientMapping that is not "Unknown"
        List<FirmClientMapping> existingNonUnknownOpt =
                firmClientMappingRepository.findNonUnknownMapping(fromNumber, firmId);

        // 4) If AI is enabled, proceed with AI-driven classification
        if (isAiEnabled(firmId)) {

            // If we already have a non-"Unknown" mapping, skip the initial caseType AI check
            boolean skipCaseTypeAiCheck = !existingNonUnknownOpt.isEmpty();
            if (skipCaseTypeAiCheck) {
                /***************************************************************
                 *  PART 2:
                 *  We have an existing phoneNumber <-> caseType mapping
                 *  Use AI to check if the incoming SMS belongs to an existing
                 *  thread or it should create a new case.
                 ***************************************************************/
                String aiPromptToCheckCaseMapping =
                        "You are a legal-intake classifier. Your task is to check if the incoming SMS "
                                + "belongs to an existing case thread or a new one by going through client & lawyer "
                                + "conversation. Given the client's SMS text, you must do the following:\n"
                                + "1) If the SMS belongs to an existing case thread. Please return the matching case id.\n"
                                + "2) If the SMS is new and does not belong to any existing case thread, you must classify "
                                + "the case type into exactly ONE of: Personal Injury, Family Law, Criminal, Employment, Other.\n"
                                + "3) If client is talking about some other issue that does not belong to the original "
                                + "case type, then return caseId=0.\n"
                                + "Output strictly in JSON with fields: \"caseType\" (string), \"caseId\" (number).\n"
                                + "Now process this user text:\n"
                                + "\"" + messageBody + "\"\n";

                System.out.println(" ------- existing case mapping found, so start process p2 ------");

                // Build context for the AI prompt from existing threads & conversations
                for (FirmClientMapping mapping : existingNonUnknownOpt) {
                    List<ConversationThread> threads =
                            conversationThreadRepository.findByFromNumberAndToNumberAndEmail(
                                    toNumber,
                                    fromNumber,
                                    (mapping.getFirmLawyer() != null)
                                            ? mapping.getFirmLawyer().getLawyerMail()
                                            : customer.getCustMail()
                            );

                    for (ConversationThread thread : threads) {
                        List<Conversation> threadConversations =
                                conversationRepository.findByConversationThread_ConversationThreadId(
                                        thread.getConversationThreadId()
                                );

                        aiPromptToCheckCaseMapping += " Case Id : " + thread.getConversationThreadId();
                        aiPromptToCheckCaseMapping += " Conversation for case id : ";

                        for (Conversation conversation : threadConversations) {
                            // Identify the speaker based on channel
                            String speakerLabel = conversation.getChannel().equalsIgnoreCase("EMAIL")
                                    ? "Client : "
                                    : "Lawyer : ";
                            aiPromptToCheckCaseMapping += speakerLabel + conversation.getMessage() + "\n";
                        }
                    }
                }

                System.out.println("AI Prompt: " + aiPromptToCheckCaseMapping);
                System.out.println("------------- end ai prompt --------------");

                // Query OpenAI
                NewCaseCheckDto dto = openAiService.checkCaseTypeExistingOrNew(aiPromptToCheckCaseMapping);
                Long returnedCaseId = dto.getCaseId();
                String returnedCaseType = dto.getCaseType();

                System.out.println(" >> AI returned: caseId=" + returnedCaseId + ", caseType=" + returnedCaseType);

                if (returnedCaseId > 0) {
                    // (A1) AI says "existing case"
                    Optional<ConversationThread> existingThreadOpt = conversationThreadRepository.findById(returnedCaseId);

                    if (existingThreadOpt.isPresent()) {
                        ConversationThread existingThread = existingThreadOpt.get();
                        String existingEmail = existingThread.getEmail();

                        System.out.println("Found existingThread ID=" + existingThread.getConversationThreadId()
                                + " with email=" + existingEmail);

                        // Forward the inbound SMS to the existing email thread
                        String forwardSubject = "SMS from " + fromNumber;
                        try {
                            emailService.sendEmail(
                                    existingEmail,
                                    forwardSubject,
                                    returnedCaseType,
                                    messageBody,
                                    fromNumber,
                                    toNumber,
                                    messageSid + ": Email",
                                    null
                            );
                            System.out.println("Forwarded inbound SMS to existing thread: " + existingEmail);
                        } catch (Exception ex) {
                            System.err.println("Error sending email to existing thread: " + ex.getMessage());
                        }

                        // (line 179 reference)
                        System.out.println("Saved new conversation, from SmsController (line 179) ID="
                                + existingThread.getConversationThreadId());

                        // Save the conversation
                        Conversation conversation = conversationService.saveConversation(
                                fromNumber,
                                toNumber,
                                existingThread.getEmail(),
                                messageBody,
                                "INCOMING",
                                "SMS",
                                null,
                                returnedCaseType,
                                existingThread.getThreadId(),
                                messageSid,
                                null
                        );
                        System.out.println("Saved new conversation, ID=" + conversation.getId());

                        // Optionally fetch the mapping for consistency (not mandatory)
                        firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, returnedCaseType);

                    } else {
                        // If the thread ID wasn't found in DB
                        System.err.println("No thread found with ID=" + returnedCaseId);
                    }

                } else {
                    // (A2) AI says "new case", returnedCaseId == 0
                    System.out.println("AI says new case; caseType=" + returnedCaseType);

                    String safeCaseType = returnedCaseType.replaceAll("\\s+", "_");

                    // 1) Possibly create a new FirmClientMapping
                    Optional<FirmClientMapping> newCaseMappingOpt =
                            firmClientMappingRepository.findByPhoneFirmCaseType(
                                    fromNumber, firmId, safeCaseType
                            );

                    FirmClientMapping newCaseMapping;
                    if (newCaseMappingOpt.isPresent()) {
                        newCaseMapping = newCaseMappingOpt.get();
                        System.out.println("Mapping already exists for caseType=" + returnedCaseType);
                    } else {
                        newCaseMapping = new FirmClientMapping();
                        newCaseMapping.setFirm(customer);
                        newCaseMapping.setFirmLawyer(null);
                        newCaseMapping.setClientPhoneNumber(fromNumber);
                        newCaseMapping.setCaseType(safeCaseType);
                        newCaseMapping = firmClientMappingRepository.save(newCaseMapping);
                        System.out.println("Created new mapping, caseType=" + returnedCaseType);
                    }

                    // 2) Create a brand-new thread
                    ConversationThread newThread = new ConversationThread();
                    newThread.setPhoneNumber(fromNumber);
                    newThread.setToNumber(toNumber);
                    newThread.setEmail(customer.getCustMail());
                    newThread.setCreatedAt(LocalDateTime.now());
                    newThread.setStatus("ACTIVE");
                    newThread.setCaseType(safeCaseType);

                    String newThreadId = fromNumber + "-" + safeCaseType + "-" + toNumber;
                    newThread.setThreadId(newThreadId);
                    newThread.setCustiId(firmId);

                    ConversationThread savedThread = conversationThreadRepository.save(newThread);
                    System.out.println("Created newThread ID=" + savedThread.getConversationThreadId());

                    // 3) Save the incoming SMS as a Conversation
                    // (line 269 reference)
                    System.out.println("Saved conversation, from SmsController (line 269) ID="
                            + savedThread.getThreadId());

                    Conversation conversation = conversationService.saveConversation(
                            fromNumber,
                            toNumber,
                            savedThread.getEmail(),
                            messageBody,
                            "INCOMING",
                            "SMS",
                            null,
                            returnedCaseType,
                            savedThread.getThreadId(),
                            messageSid,
                            savedThread
                    );

                    // Forward the new case SMS to either the firm or assigned lawyer
                    Optional<FirmClientMapping> mappingOpt =
                            firmClientMappingRepository.findByPhoneFirmCaseType(
                                    fromNumber, firmId, safeCaseType
                            );

                    FirmLawyer firmLawyer = mappingOpt.get().getFirmLawyer();
                    if (firmLawyer == null) {
                        // No lawyer assigned yet; forward to the firm's main email
                        String firmEmail = customer.getCustMail();
                        String subject = "SMS from " + fromNumber;
                        try {
                            emailService.sendEmail(
                                    firmEmail,
                                    subject,
                                    returnedCaseType,
                                    messageBody,
                                    fromNumber,
                                    toNumber,
                                    messageSid + ": Email",
                                    savedThread
                            );
                            System.out.println("[NEW] Forwarded COMPLETE SMS to " + firmEmail);
                            System.out.println("Safe case type: " + returnedCaseType);
                        } catch (Exception ex) {
                            System.err.println("Error forwarding first SMS via email: " + ex.getMessage());
                        }
                    } else {
                        // Lawyer is assigned
                        String assignedLawyerEmail = firmLawyer.getLawyerMail();
                        String subject = "SMS from " + fromNumber;
                        try {
                            emailService.sendEmail(
                                    assignedLawyerEmail,
                                    subject,
                                    returnedCaseType,
                                    messageBody,
                                    fromNumber,
                                    toNumber,
                                    messageSid + ": Email",
                                    savedThread
                            );
                            System.out.println("Email forwarded successfully to " + assignedLawyerEmail);
                        } catch (Exception ex) {
                            System.err.println("Error sending email to assigned lawyer: " + ex.getMessage());
                        }
                    }
                }

            } else {
                /***************************************************************
                 *  PART 3:
                 *  We do NOT have an existing "non-Unknown" mapping for this
                 *  phone number. Possibly a brand new lead or a known "Unknown"
                 *  mapping. We'll use AI for intake completeness & classification.
                 ***************************************************************/
                System.out.println("No existing case mapping found");
                IntakeCheckDto intakeCheck = openAiService.checkIntakeCompleteness(messageBody);
                boolean isComplete = intakeCheck.isComplete();
                String initialCaseType = intakeCheck.getCaseType();
                String safeCaseType = initialCaseType.replaceAll("\\s+", "_");

                System.out.println("AI returned: complete=" + isComplete + ", caseType=" + initialCaseType);

                // 4) Check if we have an existing mapping for this phone number with that caseType
                Optional<FirmClientMapping> mappingOpt =
                        firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, safeCaseType);

                FirmClientMapping firmClientMapping;
                if (mappingOpt.isEmpty()) {
                    // Check if there's an existing "Unknown" row for this phone & firm
                    Optional<FirmClientMapping> unknownOpt =
                            firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, "Unknown");

                    if (unknownOpt.isPresent() && !safeCaseType.equalsIgnoreCase("Unknown")) {
                        // Upgrade the existing "Unknown" row
                        firmClientMapping = unknownOpt.get();
                        firmClientMapping.setCaseType(safeCaseType);
                        firmClientMappingRepository.save(firmClientMapping);
                        System.out.println("Upgraded existing Unknown row to caseType=" + initialCaseType);
                    } else {
                        // Create a new mapping
                        firmClientMapping = new FirmClientMapping();
                        firmClientMapping.setFirm(customer);
                        firmClientMapping.setFirmLawyer(null);
                        firmClientMapping.setClientPhoneNumber(fromNumber);
                        firmClientMapping.setCaseType(safeCaseType);
                        firmClientMapping = firmClientMappingRepository.save(firmClientMapping);
                        System.out.println("Created new FirmClientMapping for caseType=" + initialCaseType);
                    }
                } else {
                    firmClientMapping = mappingOpt.get();
                    System.out.println("Found existing FirmClientMapping for caseType="
                            + firmClientMapping.getCaseType());
                }

                // Try to find or create a conversation thread
                String threadId = fromNumber + "-" + customer.getCustMail();
                List<ConversationThread> threads =
                        conversationThreadRepository.findByFromNumberAndToNumber(toNumber, fromNumber);

                ConversationThread thread = new ConversationThread();
                if (threads.isEmpty()) {
                    System.out.println("Creating new Conversation thread from line 336 from SMS controller");
                    thread = findOrCreateConversationThread(
                            threadId,
                            fromNumber,
                            toNumber,
                            customer.getCustMail(),
                            customer.getCusti_id()
                    );
                }

                // For each existing thread, check if it references this case
                for (ConversationThread threadTemp : threads) {
                    System.out.println("Thread found: " + threadTemp.getThreadId());
                    System.out.println("from number check: " + threadTemp.getThreadId().contains(fromNumber));
                    System.out.println("email check: " + threadTemp.getThreadId().contains(customer.getCustMail()));

                    if (threadTemp.getThreadId().contains(fromNumber)
                            && threadTemp.getThreadId().contains(customer.getCustMail())) {

                        System.out.println("Thread case found: " + (threadTemp.getCaseType() == null));
                        // If it's "Unknown", then set it to safeCaseType
                        if (threadTemp.getCaseType().equalsIgnoreCase("Unknown")) {
                            threadTemp.setCaseType(safeCaseType);
                            conversationThreadRepository.save(threadTemp);
                        } else {
                            // Otherwise, create a new conversation thread
                            System.out.println("Creating new Conversation thread from line 352 from SMS controller");
                            findOrCreateConversationThread(
                                    threadId,
                                    fromNumber,
                                    toNumber,
                                    customer.getCustMail(),
                                    customer.getCusti_id()
                            );
                        }
                    }
                }

                FirmLawyer firmLawyer = firmClientMapping.getFirmLawyer();
                if (firmLawyer == null) {
                    // If no lawyer is assigned
                    if (isComplete) {
                        // If intake is complete, forward SMS to firm's main email
                        String firmEmail = customer.getCustMail();
                        String subject = "SMS from " + fromNumber;
                        try {
                            emailService.sendEmail(
                                    firmEmail,
                                    subject,
                                    safeCaseType,
                                    messageBody,
                                    fromNumber,
                                    toNumber,
                                    messageSid + ": Email",
                                    null
                            );
                            System.out.println("[NEW] Forwarded COMPLETE SMS to " + firmEmail);
                            System.out.println("Safe case type: " + safeCaseType);

                            // Log the forwarded message in conversation
                            conversationService.saveConversation(
                                    fromNumber,
                                    toNumber,
                                    firmEmail,
                                    messageBody,
                                    "OUTGOING",
                                    "EMAIL",
                                    subject,
                                    safeCaseType,
                                    threadId,
                                    "auto-forward-" + System.currentTimeMillis(),
                                    thread
                            );

                        } catch (Exception ex) {
                            System.err.println("Error forwarding first SMS via email: " + ex.getMessage());
                        }
                    } else {
                        // If intake incomplete, send an auto-reply
                        String autoReply = "Hello, thanks for contacting "
                                + customer.getCustName()
                                + "! Please reply with your name, address, "
                                + "and a brief description of your case.";

                        smsService.sendSms(fromNumber, toNumber, autoReply);
                        System.out.println("Safe case type: " + safeCaseType);

                        // Optionally log the auto-reply in conversation
                        // (uncomment if you want to store outgoing SMS as well)
                        /*
                        conversationService.saveConversation(
                                fromNumber,
                                toNumber,
                                customer.getCustMail(),
                                autoReply,
                                "OUTGOING",
                                "SMS",
                                null,
                                safeCaseType,
                                threadId,
                                "auto-" + System.currentTimeMillis(),
                                thread
                        );
                        */
                    }
                    return;
                }

                // 6) If we have an assigned lawyer, classify again for additional info
                String additionalCaseType = openAiService.classifyCaseType(messageBody);
                System.out.println("[SmsController] Additional classification returned: " + additionalCaseType);

                // Optionally update the thread's caseType if it's different
                if (!threads.isEmpty()) {
                    // We only update the first thread found or you could loop
                    thread = threads.get(0);
                    if (!additionalCaseType.equalsIgnoreCase(thread.getCaseType())) {
                        thread.setCaseType(additionalCaseType);
                        conversationThreadRepository.save(thread);
                        System.out.println("Thread updated to new caseType: " + additionalCaseType);
                    }
                }

                // 7) Forward the SMS to the assigned lawyer's email
                String assignedLawyerEmail = firmLawyer.getLawyerMail();
                String subject = "SMS from " + fromNumber;
                try {
                    emailService.sendEmail(
                            assignedLawyerEmail,
                            subject,
                            additionalCaseType,
                            messageBody,
                            fromNumber,
                            toNumber,
                            messageSid + ": Email",
                            null
                    );
                    System.out.println("Email forwarded successfully to " + assignedLawyerEmail);
                } catch (Exception ex) {
                    System.err.println("Error sending email to assigned lawyer: " + ex.getMessage());
                }
            }

        } else {
            /***************************************************************
             *  PART 4:
             *  If AI is NOT enabled for this firm, then we assign a lawyer
             *  using round-robin logic and map the phoneNumber -> lawyer
             *  with caseType="Unknown".
             ***************************************************************/
           System.out.println("assigning a new lawyer using round robin");
    FirmLawyer firmLawyer = assignLawyerUsingRoundRobin(firmId, fromNumber);
    if (firmLawyer != null) {
        System.out.println("found lawyer - round robin");
        
        // 1) Create a new FirmClientMapping with "Unknown"
        FirmClientMapping firmClientMapping = new FirmClientMapping();
        firmClientMapping.setFirm(customer);
        firmClientMapping.setFirmLawyer(firmLawyer);
        firmClientMapping.setClientPhoneNumber(fromNumber);
        firmClientMapping.setCaseType("Unknown");
        firmClientMappingRepository.save(firmClientMapping);

        // 2) Create a new ConversationThread with the new lawyer’s email
        ConversationThread newThread = new ConversationThread();
        newThread.setPhoneNumber(fromNumber);
        newThread.setToNumber(toNumber);
        newThread.setEmail(firmLawyer.getLawyerMail());
        newThread.setCreatedAt(LocalDateTime.now());
        newThread.setStatus("ACTIVE");
        newThread.setCaseType("Unknown");
        newThread.setCustiId(firmId);

        // String newThreadId = fromNumber + toNumber;
        String newThreadId = fromNumber + "-" + firmLawyer.getLawyerMail();
        newThread.setThreadId(newThreadId);
        ConversationThread savedThread = conversationThreadRepository.save(newThread);

        // 3) Save the inbound SMS as a new conversation in that new thread
        Conversation conversation = conversationService.saveConversation(
                fromNumber,
                toNumber,
                firmLawyer.getLawyerMail(), // The new thread's email
                messageBody,
                "INCOMING",
                "SMS",
                null,           // no subject
                "Unknown",      // caseType
                newThreadId,
                messageSid,
                savedThread
        );

        // 4) Forward email to the newly assigned lawyer
        try {
            String subject = "SMS from " + fromNumber;
            emailService.sendEmail(
                    firmLawyer.getLawyerMail(),  // assigned lawyer email
                    subject,
                    "Unknown",
                    messageBody,
                    fromNumber,
                    toNumber,
                    messageSid + ": Email",
                    savedThread
            );
        } catch (Exception ex) {
            System.err.println("Error sending email to assigned lawyer: " + ex.getMessage());
        }
    }
        }
    }

    /**
     * Utility method to check if AI is enabled for a given firmId.
     */
    public boolean isAiEnabled(Long firmId) {
        Optional<Customer> optionalCustomer =
                customerRepository.findByCustiIdAndEnabledAssignedLawyer(firmId, "Ai_Enabled");
        return optionalCustomer.isPresent();
    }

    /**
     * Round-robin assignment of a lawyer for a given firm.
     */
    public FirmLawyer assignLawyerUsingRoundRobin(Long firmId, String fromNumber) {
        List<FirmLawyer> lawyers = firmLawyerRepository.getLawyersByFirmId(firmId);
        if (!lawyers.isEmpty()) {
            // Using a custom query that picks the next lawyer in a round-robin manner
            FirmLawyer lawyer = firmLawyerRepository.getLawyerRoundRobin(firmId, firmId);
            return lawyer;
        }
        return null;
    }

    /**
     * Finds an existing thread by its threadId, or creates a new one if none found.
     */
    private ConversationThread findOrCreateConversationThread(
            String threadId,
            String phoneNumber,
            String toNumber,
            String email,
            Long custiId
    ) {
        System.out.println("create conversation thread called from sms controller");
        Optional<ConversationThread> existingThreadOpt = conversationThreadRepository.findByThreadId(threadId);
        if (existingThreadOpt.isPresent()) {
            return existingThreadOpt.get();
        }

        ConversationThread newThread = new ConversationThread();
        newThread.setThreadId(threadId);
        newThread.setPhoneNumber(phoneNumber);
        newThread.setToNumber(toNumber);
        newThread.setEmail(email);
        newThread.setCreatedAt(LocalDateTime.now());
        newThread.setStatus("ACTIVE");
        newThread.setCaseType("Unknown");
        newThread.setCustiId(custiId);
        return conversationThreadRepository.save(newThread);
    }

    /**
     * A simple test endpoint to see the lawyer counts (just for debugging).
     */
    @GetMapping("/printLawyerCounts")
    public void printLawyerCounts(@RequestParam Long firmId) {
        Long firmIdNew = 1L;
        List<Object[]> results =
                firmClientMappingRepository.countByLawyerIdIsNotNullAndFirmIdGroupedByLawyerId(firmIdNew);
        for (Object[] result : results) {
            Long count = ((Number) result[0]).longValue();
            Long lawyerId = ((Number) result[1]).longValue();
            System.out.println("Lawyer ID: " + lawyerId + " appears " + count + " times");
        }
    }
}
