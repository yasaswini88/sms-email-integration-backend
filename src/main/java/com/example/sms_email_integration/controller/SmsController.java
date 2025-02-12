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
@RequestMapping("/api/v2")
public class SmsController {

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
    public SmsController(
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

        List<FirmClientMapping> existingNonUnknownOpt
                = firmClientMappingRepository.findNonUnknownMapping(fromNumber, firmId);

        if (isAiEnabled(firmId)) {
            //continue doing AI code
            // assignLawyerUsingAI(firmId);

            boolean skipCaseTypeAiCheck = existingNonUnknownOpt.size() > 0;
            String caseType = "Unknown";

            if (skipCaseTypeAiCheck) {
                /**
                 * we need to identify if their message belongs to current or a
                 * new case thread
                 */
                String aiPromptToCheckCaseMapping = "You are a legal-intake classifier. Your task is to check if the incoming SMS " + "belongs to an existing case thread or a new one by going throught client & lawyer conversation. Given the client's SMS text, you must do the following:\n"
                        + "1) If the SMS belongs to an existing case thread. Please return the matching case id."
                        + "2) If the SMS is new and does not belong to any existing case thread, you must classify the case type into exactly ONE of: Personal Injury, Family Law, Criminal, Employment, Other.\n"
                        + "3) caseType does not match the existing caseTpe , If client is talking about about some other issue which he has that does not belong to original case Type then in that case return caseId : 0 , then given caseId : 0"
                        + "Output strictly in JSON with fields: \"caseType\" (string), \"caseId\" (number).\n"
                        + "Now process this user text:\n"
                        + "\"" + messageBody + "\"";

                System.out.println(" ------- existing case mapping found, so start process p2 ------");
                for (FirmClientMapping mapping : existingNonUnknownOpt) {
                    //System.out.println("Existing non-Unknown mapping: " + mapping.getFirmLawyer().getLawyerMail());

                    List<ConversationThread> threads = conversationThreadRepository.findByFromNumberAndToNumberAndEmail(toNumber, fromNumber, mapping.getFirmLawyer() != null ? mapping.getFirmLawyer().getLawyerMail() : customer.getCustMail());
                    for (ConversationThread thread : threads) {
                        List<Conversation> threadConversations = conversationRepository.findByConversationThread_ConversationThreadId(thread.getConversationThreadId());

                        aiPromptToCheckCaseMapping += " Case Id : " + thread.getConversationThreadId();
                        aiPromptToCheckCaseMapping += " Conversation for case id : ";
                        for (Conversation conversation : threadConversations) {
                            aiPromptToCheckCaseMapping += "" + (conversation.getChannel().equalsIgnoreCase("EMAIL") ? "Client : " : "Lawyer : ") + conversation.getMessage() + "\n";
                        }
                    }
                }
                System.out.println("AI Prompt: " + aiPromptToCheckCaseMapping);
                System.out.println("------------- end ai prompt --------------");

                // openAiService.checkCaseTypeExistingOrNew(aiPromptToCheckCaseMapping);
                NewCaseCheckDto dto = openAiService.checkCaseTypeExistingOrNew(aiPromptToCheckCaseMapping);
                Long returnedCaseId = dto.getCaseId();
                String returnedCaseType = dto.getCaseType();

                System.out.println(" >> AI returned: caseId=" + returnedCaseId + ", caseType=" + returnedCaseType);

                if (returnedCaseId > 0) {
                    // (A1) returnedCaseId > 0 => AI says "existing case"
                    Optional<ConversationThread> existingThreadOpt = conversationThreadRepository.findById(returnedCaseId);

                    if (existingThreadOpt.isPresent()) {
                        ConversationThread existingThread = existingThreadOpt.get();
                        // existingThreadOpt = conversationThreadRepository.findById(returnedCaseId + 1);
                        // ConversationThread conversationThread = existingThreadOpt.get();
                        String existingEmail = existingThread.getEmail();

                        System.out.println("Found existingThread ID=" + existingThread.getConversationThreadId()
                                + " with email=" + existingEmail);

                        System.out.println("Found existingThread ID=" + existingThread.getConversationThreadId());

                        // if (!existingThread.getCaseType().equalsIgnoreCase(returnedCaseType)) {
                        //     existingThread.setCaseType(returnedCaseType);
                        //     conversationThreadRepository.save(existingThread);
                        //     System.out.println("Updated existingThread to caseType=" + returnedCaseType);
                        // }
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

                        System.out.println("Saved new conversation,from SmsController  (line 179 ) ID=" + existingThread.getConversationThreadId());
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

                        Optional<FirmClientMapping> mappingOpt
                                = firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, returnedCaseType);

                        System.err.println("No thread found with ID=" + returnedCaseId);
                    }
                } else {
                    // (A2) returnedCaseId == 0 => AI says "new case"
                    System.out.println("AI says new case; caseType=" + returnedCaseType);

                    String safeCaseType = returnedCaseType.replaceAll("\\s+", "_");

                    // 1) Possibly create a new FirmClientMapping
                    Optional<FirmClientMapping> newCaseMappingOpt
                            = firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, safeCaseType);

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

                    // 3) Log the incoming SMS
                    System.out.println("Saved conversation,from SmsController  (line 269 ) ID=" + savedThread.getThreadId());
                    // 2) Build a new Conversation referencing that Thread
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

//  My code for swending email to lawyers via email in case of new case type goes here
                    Optional<FirmClientMapping> mappingOpt
                            = firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, safeCaseType);

                    FirmLawyer firmLawyer = mappingOpt.get().getFirmLawyer();

                    if (firmLawyer == null) {
                        String firmEmail = customer.getCustMail();
                        String subject = "SMS from " + fromNumber;
                        try {
                            emailService.sendEmail(
                                    firmEmail,
                                    subject,
                                    returnedCaseType, // use

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
                /**
                 * new customer with out any active cases
                 */

                System.out.println("No existing case mapping found");
                IntakeCheckDto intakeCheck = openAiService.checkIntakeCompleteness(messageBody);
                boolean isComplete = intakeCheck.isComplete();
                String initialCaseType = intakeCheck.getCaseType();

                String safeCaseType = initialCaseType.replaceAll("\\s+", "_");

                System.out.println("AI returned: complete=" + isComplete + ", caseType=" + initialCaseType);

                // 4) Check if we have an existing mapping for this phone number with Unknown type
                Optional<FirmClientMapping> mappingOpt
                        = firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, safeCaseType);

                FirmClientMapping firmClientMapping;
                if (mappingOpt.isEmpty()) {
                    // Check if there's an existing "Unknown" row for this phone & firm
                    Optional<FirmClientMapping> unknownOpt
                            = firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, "Unknown");

                    if (unknownOpt.isPresent() && !safeCaseType.equalsIgnoreCase("Unknown")) {

                        firmClientMapping = unknownOpt.get();
                        firmClientMapping.setCaseType(safeCaseType); // e.g. "Employment"
                        firmClientMappingRepository.save(firmClientMapping);
                        System.out.println("Upgraded existing Unknown row to caseType=" + initialCaseType);

                    } else {

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
                    System.out.println("Found existing FirmClientMapping for caseType=" + firmClientMapping.getCaseType());
                }

                // String safeCaseType = initialCaseType.replaceAll("\\s+", "_");
                String threadId = fromNumber + "-" + customer.getCustMail();

                List<ConversationThread> threads = conversationThreadRepository.findByFromNumberAndToNumber(toNumber, fromNumber);
                ConversationThread thread = new ConversationThread();
                if (threads.isEmpty()) {
                    System.out.println("Creating new Conversation thread from 336 from SMS controller");
                    thread = findOrCreateConversationThread(threadId, fromNumber, toNumber, customer.getCustMail(), customer.getCusti_id());
                }

                for (ConversationThread thread_temp : threads) {
                    System.out.println("Thread found: " + thread.getThreadId());
                    System.out.println("from number check " + thread_temp.getThreadId().toString().contains(fromNumber));
                    System.out.println("email check " + thread_temp.getThreadId().contains(customer.getCustMail()));
                    if (thread_temp.getThreadId().toString().contains(fromNumber)
                            && thread_temp.getThreadId().contains(customer.getCustMail())) {
                        System.out.println("Thread case found: " + thread_temp.getCaseType() == null);
                        // System.out.println("Thread case found: " + thread_temp.getCaseType().equalsIgnoreCase("Unknown"));

                        if (thread_temp.getCaseType().equalsIgnoreCase("Unknown")) {
                            thread_temp.setCaseType(safeCaseType);
                            conversationThreadRepository.save(thread_temp);
                        } else {
                            System.out.println("Creating new Conversation thread from line 352 from SMS controller");
                            findOrCreateConversationThread(threadId, fromNumber, toNumber, customer.getCustMail(), customer.getCusti_id());
                        }

                    } else {

                    }
                }

                //thread.setCustiId(customer.getCusti_id());
                FirmLawyer firmLawyer = firmClientMapping.getFirmLawyer();

                if (firmLawyer == null) {

                    if (isComplete) {
                        // If complete, forward this SMS to the firm's main email
                        String firmEmail = customer.getCustMail();
                        String subject = "SMS from " + fromNumber;
                        try {
                            emailService.sendEmail(
                                    firmEmail,
                                    subject,
                                    safeCaseType, // use

                                    messageBody,
                                    fromNumber,
                                    toNumber,
                                    messageSid + ": Email",
                                    null
                            );
                            System.out.println("[NEW] Forwarded COMPLETE SMS to " + firmEmail);

                            System.out.println("Safe case type: " + safeCaseType);

                            conversationService.saveConversation(
                                    fromNumber,
                                    toNumber,
                                    firmEmail, // indicates we sent an email
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
                        return;
                    } else {
                        // If intake incomplete, send an auto-reply
                        String autoReply = "Hello, thanks for contacting "
                                + customer.getCustName()
                                + "! Please reply with your name, address, and a brief description of your case.";

                        smsService.sendSms(fromNumber, toNumber, autoReply);

                        System.out.println("Safe case type: " + safeCaseType);

                        // Log it in conversation
                        // conversationService.saveConversation(
                        //         fromNumber,
                        //         toNumber,
                        //         customer.getCustMail(),
                        //         autoReply,
                        //         "OUTGOING",
                        //         "SMS",
                        //         null,
                        //         safeCaseType,
                        //         threadId,
                        //         "auto-" + System.currentTimeMillis()
                        // );
                        return;
                    }
                }

                // 6) If we DO have an assigned lawyer, we might want to do a
                // separate classification again for any new info:
                String userText = messageBody;
                String additionalCaseType = openAiService.classifyCaseType(userText);
                System.out.println("[SmsController] Additional classification returned: " + additionalCaseType);

                // Optionally update the thread with a new or updated caseType if you desire:
                if (!additionalCaseType.equalsIgnoreCase(thread.getCaseType())) {
                    thread.setCaseType(additionalCaseType);
                    conversationThreadRepository.save(thread);
                    System.out.println("Thread updated to new caseType: " + additionalCaseType);
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
            System.out.println("assigning a new lawyer using round robin");
            FirmLawyer firmLawyer = assignLawyerUsingRoundRobin(firmId, fromNumber);
            // create firm client mapping
            if (firmLawyer != null) {
                System.out.println("found lawyer -  round robin");
                FirmClientMapping firmClientMapping = new FirmClientMapping();
                firmClientMapping.setFirm(customer);
                firmClientMapping.setFirmLawyer(firmLawyer);
                firmClientMapping.setClientPhoneNumber(fromNumber);
                firmClientMapping.setCaseType("Unknown");
                firmClientMappingRepository.save(firmClientMapping);
            } 
        }

    }

    public boolean isAiEnabled(Long firmId) {
        Optional<Customer> optionalCustomer = customerRepository.findByCustiIdAndEnabledAssignedLawyer(firmId, "Ai_Enabled");
        return optionalCustomer.isPresent();
    }

    public FirmLawyer assignLawyerUsingRoundRobin(Long firmId, String fromNumber) {
        List<FirmLawyer> lawyers = firmLawyerRepository.getLawyersByFirmId(firmId);
        if (!lawyers.isEmpty()) {
            FirmLawyer lawyer = firmLawyerRepository.getLawyerRoundRobin(firmId,firmId);
            return lawyer;
        }
        return null;
    }

    /**
     * Finds an existing thread by its threadId, or creates a new one if none
     * found.
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

    @GetMapping("/printLawyerCounts")
    public void printLawyerCounts(@RequestParam Long firmId) {
        Long firmIdNew = 1L;
        List<Object[]> results = firmClientMappingRepository.countByLawyerIdIsNotNullAndFirmIdGroupedByLawyerId(firmIdNew);
        for (Object[] result : results) {
            Long count = ((Number) result[0]).longValue();
            Long lawyerId = ((Number) result[1]).longValue();
            System.out.println("Lawyer ID: " + lawyerId + " appears " + count + " times");
        }
    }

}
