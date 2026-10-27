package com.kaushik.ChildShield.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${whatsapp.api.enabled:true}")
    private boolean enabled;

    @Value("${whatsapp.api.url:https://graph.facebook.com/v21.0}")
    private String apiUrl;

    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api.access-token:}")
    private String accessToken;

    @Value("${whatsapp.api.app-id:}")
    private String appId;

    @Value("${whatsapp.api.app-secret:}")
    private String appSecret;

    @Getter
    @Value("${whatsapp.api.message-type:template}")
    private String messageType;

    @Getter
    @Value("${whatsapp.api.template.name:jaspers_market_order_confirmation_v1}")
    private String templateName;

    @Getter
    @Value("${whatsapp.api.template.language:en_US}")
    private String templateLanguage;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return enabled &&
                phoneNumberId != null && !phoneNumberId.isBlank() && !phoneNumberId.contains("YOUR_") &&
                accessToken != null && !accessToken.isBlank() && !accessToken.contains("YOUR_");
    }

    public String formatPhoneNumber(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits; // Default Indian country code
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return "91" + digits.substring(1);
        }
        return digits;
    }

    public boolean sendTemplateMessage(String toPhone, String tName, String langCode, List<String> bodyParameters) {
        String formattedTo = formatPhoneNumber(toPhone);
        if (formattedTo == null || formattedTo.isBlank()) {
            log.warn("Cannot send WhatsApp template message: recipient phone number is missing or invalid.");
            return false;
        }

        String effectiveTemplate = (tName != null && !tName.isBlank()) ? tName.trim() : templateName;
        String effectiveLang = (langCode != null && !langCode.isBlank()) ? langCode.trim() : templateLanguage;

        if (!isConfigured()) {
            log.info("[SIMULATED WhatsApp Template] To: {} | Template: {} (Lang: {}) | Params: {}",
                    formattedTo, effectiveTemplate, effectiveLang, bodyParameters);
            return true;
        }

        try {
            String endpoint = apiUrl.replaceAll("/+$", "") + "/" + phoneNumberId.trim() + "/messages";

            Map<String, Object> templateMap = new LinkedHashMap<>();
            templateMap.put("name", effectiveTemplate);
            templateMap.put("language", Map.of("code", effectiveLang));

            if (bodyParameters != null && !bodyParameters.isEmpty()) {
                List<Map<String, String>> params = new ArrayList<>();
                for (String p : bodyParameters) {
                    params.add(Map.of("type", "text", "text", p != null ? p : ""));
                }
                templateMap.put("components", List.of(
                        Map.of(
                                "type", "body",
                                "parameters", params
                        )
                ));
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", formattedTo);
            payload.put("type", "template");
            payload.put("template", templateMap);

            String requestBody = objectMapper.writeValueAsString(payload);
            log.info("Dispatching WhatsApp Template request to {}:\n{}", endpoint, requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("WhatsApp template message successfully dispatched to {}. Response: {}", formattedTo, response.body());
                return true;
            } else {
                log.error("Failed to dispatch WhatsApp template message to {}. Status: {}, Response: {}",
                        formattedTo, response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Exception occurred while dispatching WhatsApp template message to {}: {}", formattedTo, e.getMessage(), e);
            return false;
        }
    }

    public boolean sendTextMessage(String toPhone, String messageText) {
        String formattedTo = formatPhoneNumber(toPhone);
        if (formattedTo == null || formattedTo.isBlank()) {
            log.warn("Cannot send WhatsApp message: recipient phone number is missing or invalid.");
            return false;
        }

        if (!isConfigured()) {
            log.info("[SIMULATED WhatsApp Notification] To: {} | Message:\n{}", formattedTo, messageText);
            return true;
        }

        try {
            String endpoint = apiUrl.replaceAll("/+$", "") + "/" + phoneNumberId.trim() + "/messages";

            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", formattedTo,
                    "type", "text",
                    "text", Map.of(
                            "preview_url", false,
                            "body", messageText
                    )
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("WhatsApp text message successfully sent to {}. Response: {}", formattedTo, response.body());
                return true;
            } else {
                log.error("Failed to send WhatsApp text message to {}. Status: {}, Response: {}", formattedTo, response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Exception occurred while dispatching WhatsApp text message to {}: {}", formattedTo, e.getMessage());
            return false;
        }
    }

    public boolean sendOverdueVaccinationAlert(String guardianPhone, String guardianName, String childName, List<Map<String, String>> overdueDoses) {
        if (overdueDoses == null || overdueDoses.isEmpty()) {
            return false;
        }

        String greetingName = (guardianName != null && !guardianName.isBlank()) ? guardianName.trim() : "Parent/Guardian";
        String child = (childName != null && !childName.isBlank()) ? childName.trim() : "your child";

        if ("template".equalsIgnoreCase(messageType)) {
            // Build parameters matching template format (such as jaspers_market_order_confirmation_v1)
            // Parameter 1: Parent & Child
            String param1 = greetingName + " (Child: " + child + ")";

            // Parameter 2: Vaccine doses list
            StringBuilder sbDoses = new StringBuilder();
            for (int i = 0; i < overdueDoses.size(); i++) {
                if (i > 0) sbDoses.append(", ");
                sbDoses.append(overdueDoses.get(i).getOrDefault("name", "Vaccine"));
            }
            String param2 = "Overdue Vaccines: " + sbDoses;
            if (param2.length() > 60) {
                param2 = param2.substring(0, 57) + "...";
            }

            // Parameter 3: Due date / action
            String earliestDue = overdueDoses.get(0).getOrDefault("dueDate", "Immediate");
            String param3 = "Health Center Visit (Due: " + earliestDue + ")";

            return sendTemplateMessage(guardianPhone, templateName, templateLanguage, List.of(param1, param2, param3));
        } else {
            // Text mode (used when inside active 24-hour customer conversation window)
            StringBuilder sb = new StringBuilder();
            sb.append("Hello ").append(greetingName).append(",\n\n");
            sb.append("This is an important reminder from *ChildShield Vaccination Tracker*.\n");
            sb.append("The following vaccination(s) for your child *").append(child).append("* are currently *OVERDUE*:\n\n");

            for (Map<String, String> dose : overdueDoses) {
                String doseName = dose.getOrDefault("name", "Vaccine Dose");
                String dueDate = dose.getOrDefault("dueDate", "Past due");
                sb.append("• *").append(doseName).append("* (Due Date: ").append(dueDate).append(")\n");
            }

            sb.append("\n⚠️ Timely immunization protects ").append(child).append(" against preventable illnesses.\n");
            sb.append("Please visit your nearest Community Health Center or contact your assigned ASHA/ANM health worker as soon as possible to receive these doses.\n\n");
            sb.append("— ChildShield Rural Healthcare Team");

            return sendTextMessage(guardianPhone, sb.toString());
        }
    }
}
