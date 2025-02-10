package com.example.sms_email_integration.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.sms_email_integration.dto.IntakeCheckDto;
import com.example.sms_email_integration.dto.NewCaseCheckDto;
import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.repository.ConversationRepository;

@Service
public class OpenAiService {

    @Value("${openai.apiKey}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String classifyCaseType(String userDescription) {
        try {
            // 1) Construct your prompt
            String prompt
                    = "Classify the following text into exactly ONE of these legal categories:\n"
                    + "  1) Personal Injury\n"
                    + "  2) Family Law\n"
                    + "  3) Criminal\n"
                    + "  4) Employment\n"
                    + "  5) Other\n\n"
                    + "Text: \"" + userDescription + "\"\n\n"
                    + "Reply ONLY with the category name (e.g. \"Personal Injury\"). No other text.";

            System.out.println(">> [OpenAiService] Using prompt:");
            System.out.println(prompt);

            String url = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");
            // The conversation has one "system" or "user" message:
            requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {
                    {
                        put("role", "user");
                        put("content", prompt);
                    }
                }
            });
            requestBody.put("temperature", 0.7);

            // 3) Construct headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + openAiApiKey);

            // 4) Make POST request
            org.springframework.http.HttpEntity<Map<String, Object>> httpEntity
                    = new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<Map> response
                    = restTemplate.postForEntity(url, httpEntity, Map.class);

            System.out.println(">> [OpenAiService] Received response, HTTP status: "
                    + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 5) Parse the response to get the "content" from the assistant
                // The structure is: { choices: [ { message: { content: "...classification..." } } ], ... }
                Map responseBody = response.getBody();
                // "choices" is a List
                java.util.List<Map<String, Object>> choices
                        = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = (String) message.get("content");
                    System.out.println(">> [OpenAiService] Extracted content from OpenAI: " + content);

                    return content.trim();
                }
            } else {
                System.out.println(">> [OpenAiService] No 'choices' in response!");
            }

        } catch (Exception e) {
            System.err.println("Error calling OpenAI: " + e.getMessage());
        }

        // If error or no classification, default to "Other"
        return "Other";
    }

    public IntakeCheckDto checkIntakeCompleteness(String userMessage) {
        try {
            // Prompt that asks for BOTH completeness + classification in JSON
            String prompt = "You are a legal-intake classifier. "
                    + "Given the user's SMS text, you must do two things:\n"
                    + "1) Decide if the user provided enough info (we generally want name, and a case description)."
                    + "2) If enough info, classify their case into exactly ONE of: Personal Injury, Family Law, Criminal, Employment, Other.\n"
                    + "Output strictly in JSON with fields: \"complete\" (true/false), \"caseType\" (string).\n"
                    + "If \"complete\" is false, set \"caseType\" = \"Unknown\".\n"
                    + "Now process this user text:\n"
                    + "\"" + userMessage + "\"";

            // 1) Build request body as you do in classifyCaseType
            String url = "https://api.openai.com/v1/chat/completions";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");
            requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {
                    {
                        put("role", "user");
                        put("content", prompt);
                    }
                }
            });
            requestBody.put("temperature", 0.7);

            // 2) Make POST request with headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + openAiApiKey);

            org.springframework.http.HttpEntity<Map<String, Object>> httpEntity
                    = new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<Map> response
                    = restTemplate.postForEntity(url, httpEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();
                java.util.List<Map<String, Object>> choices
                        = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = ((String) message.get("content")).trim();

                    // Now 'content' should be JSON like: {"complete": true, "caseType": "Employment"}
                    // We parse it. For a quick parse, you can do a naive substring or use a JSON parser library:
                    return parseIntakeCheckFromJson(content);
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling OpenAI (checkIntakeCompleteness): " + e.getMessage());
        }

        // fallback if error
        return new IntakeCheckDto(false, "Unknown");
    }

// A helper to parse that JSON. 
// This is a simple example using naive parsing; 
// you can also use Jackson/Gson if you like.
    private IntakeCheckDto parseIntakeCheckFromJson(String json) {

        boolean complete = false;
        String caseType = "Unknown";

        if (json.contains("\"complete\"") && json.contains("true")) {
            complete = true;
        }
        // look for recognized case types
        if (json.contains("Family Law")) {
            caseType = "Family Law";
        } else if (json.contains("Criminal")) {
            caseType = "Criminal";
        } else if (json.contains("Employment")) {
            caseType = "Employment";
        } else if (json.contains("Personal Injury")) {
            caseType = "Personal Injury";
        } else if (json.contains("Other")) {
            caseType = "Other";
        }

        return new IntakeCheckDto(complete, caseType);
    }

    public NewCaseCheckDto checkCaseTypeExistingOrNew(String aiPromptToCheckCaseMapping) {
        try {

            String url = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");

            requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {
                    {
                        put("role", "user");
                        put("content", aiPromptToCheckCaseMapping);
                    }
                }
            });
            requestBody.put("temperature", 0.7);

            // 3) Construct headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + openAiApiKey);

            // 4) Make POST request
            org.springframework.http.HttpEntity<Map<String, Object>> httpEntity
                    = new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<Map> response
                    = restTemplate.postForEntity(url, httpEntity, Map.class);

            System.out.println(">> [OpenAiService] Received response, HTTP status: "
                    + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                Map responseBody = response.getBody();
                // "choices" is a List
                java.util.List<Map<String, Object>> choices
                        = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = (String) message.get("content");
                    System.out.println(">> [OpenAiService] Extracted content from OpenAI: " + content);

                    return parseNewCaseCheckFromJson(content);
                }
            } else {
                System.out.println(">> [OpenAiService] No 'choices' in response!");
            }

        } catch (Exception e) {
            System.err.println("Error calling OpenAI: " + e.getMessage());
        }

        return new NewCaseCheckDto(0L, "Other");
    }

    private NewCaseCheckDto parseNewCaseCheckFromJson(String json) {
        String caseType = "Other";
        Long caseId = 0L;

        try {
            Pattern p = Pattern.compile("\"caseId\"\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(json);
            if (m.find()) {
                caseId = Long.parseLong(m.group(1));
            }
        } catch (Exception e) {
            // fallback to 0
        }

        // 2) Find the caseType. One approach: check which known label appears in the JSON:
        if (json.contains("Personal Injury")) {
            caseType = "Personal Injury";
        } else if (json.contains("Family Law")) {
            caseType = "Family Law";
        } else if (json.contains("Criminal")) {
            caseType = "Criminal";
        } else if (json.contains("Employment")) {
            caseType = "Employment";
        } else if (json.contains("Other")) {
            caseType = "Other";
        }

        return new NewCaseCheckDto(caseId, caseType);
    }

    public String summarizeConversationThread( List<Conversation> threadConversations) {
        String prompt = "Summarize the conversation thread below in 2-3 sentences.\n";
        try {
            
            String url = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4");

            for (Conversation conversation : threadConversations) {
                prompt += "" + (conversation.getChannel().equalsIgnoreCase("EMAIL") ? "Client : " : "Lawyer : ") + conversation.getMessage() + "\n";
            }

            HashMap<String, String> promptMessage = new HashMap<String, String>();
            promptMessage.put("role", "user");
            promptMessage.put("content", prompt);

            requestBody.put("messages", new Object[]{
                promptMessage
            });
            requestBody.put("temperature", 0.7);
            // 3) Construct headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + openAiApiKey);

            // 4) Make POST request
            org.springframework.http.HttpEntity<Map<String, Object>> httpEntity
                    = new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<Map> response
                    = restTemplate.postForEntity(url, httpEntity, Map.class);

            System.out.println(">> [OpenAiService] Received response, HTTP status: "
                    + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                Map responseBody = response.getBody();
                // "choices" is a List
                java.util.List<Map<String, Object>> choices
                        = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = (String) message.get("content");
                    System.out.println(">> [OpenAiService] Extracted content from OpenAI: " + content);

                    return content;
                }
            } else {
                System.out.println(" Could not generate summary");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error calling OpenAI: " + e.getMessage());

        }

        return "No summary available";
    }

}
