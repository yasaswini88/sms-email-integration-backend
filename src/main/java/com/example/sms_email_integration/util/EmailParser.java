package com.example.sms_email_integration.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public class EmailParser {
    
    public static String extractNewEmailBody(String emailContent) {
        // Define the pattern that indicates the start of the previous email
        // Convert emailContent to UTF-8
        byte[] bytes = emailContent.getBytes(StandardCharsets.UTF_8);
        emailContent = new String(bytes, StandardCharsets.UTF_8);

        String patternString = "(?m)^On .* at .*";
        Pattern pattern = Pattern.compile(patternString,java.util.regex.Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(emailContent);

        // If the pattern is found, extract the content before it
        if (matcher.find()) {
            // System.out.println("Found pattern at position " + matcher.start());
            return emailContent.substring(0, matcher.start()).trim();
        }

        // If the pattern is not found, return the entire email content
        return emailContent.trim();
    }
}