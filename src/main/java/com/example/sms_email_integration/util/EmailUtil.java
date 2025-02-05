package com.example.sms_email_integration.util;

import org.springframework.util.StringUtils;

public final class EmailUtil {

    // Private constructor so you cannot instantiate this utility class
    private EmailUtil() {}

    /**
     * Extract just the pure email (like "someone@example.com") 
     * from a raw string like "Name <someone@example.com>".
     */
    public static String extractPureEmail(String rawFrom) {
        if (rawFrom == null) return null;

        // e.g. rawFrom: "Automach Project <projectautomach@gmail.com>"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*<(.*)>.*");
        java.util.regex.Matcher matcher = pattern.matcher(rawFrom);
        if (matcher.matches()) {
            // The content inside <...>
            return matcher.group(1).trim();
        }
        // Fallback if there are no angle brackets
        return rawFrom.trim();
    }

    /**
     * Parse client phone from the email subject, e.g. 
     * "Re: SMS from +17038620152 - Some Text"
     */
    public static String extractClientPhone(String subject) {
        if (!StringUtils.hasText(subject)) {
            return null;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("SMS from (\\+\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(subject);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
