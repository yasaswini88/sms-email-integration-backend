package com.example.sms_email_integration.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
            }
            else {
                    System.out.println(">> [OpenAiService] No 'choices' in response!");
                }

        } catch (Exception e) {
            System.err.println("Error calling OpenAI: " + e.getMessage());
        }

        // If error or no classification, default to "Other"
        return "Other";
    }
}
