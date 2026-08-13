package com.school.sms.service;

import com.school.sms.dao.SmsQueueDAO;
import com.school.sms.util.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends SMS via Africa's Talking. Because this app is offline-first, sending
 * is never allowed to block or fail a payment/worker-payment operation:
 *
 *   1. notify(...) is called right after a payment/event is recorded.
 *   2. It tries to send immediately.
 *   3. If that fails for any reason (no internet, API error), the message
 *      is queued in SmsQueue instead of being lost.
 *   4. Call sendPendingQueue() (e.g. from a "Send Pending SMS" button, or
 *      on app startup) to flush anything that queued up while offline.
 */
public class SmsService {

    private final SmsQueueDAO queueDAO = new SmsQueueDAO();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private String username() { return AppConfig.get("africastalking.username"); }
    private String apiKey()   { return AppConfig.get("africastalking.apiKey"); }
    private String baseUrl()  { return AppConfig.get("africastalking.baseUrl"); }

    /** Public entry point: try to send now, queue it if that fails. */
    public void notify(String recipientContact, String message) {
        if (recipientContact == null || recipientContact.isBlank()) {
            System.err.println("No contact on file — cannot send SMS: " + message);
            return;
        }
        boolean sentOk = trySend(recipientContact, message);
        if (!sentOk) {
            queueDAO.enqueue(recipientContact, message);
        }
    }

    /** Attempts to flush anything sitting in the offline queue. Call when internet is available. */
    public void sendPendingQueue() {
        List<SmsQueueDAO.QueuedSms> pending = queueDAO.getPending();
        for (SmsQueueDAO.QueuedSms item : pending) {
            boolean ok = trySend(item.recipientContact, item.message);
            if (ok) {
                queueDAO.markSent(item.id);
            }
            // if it fails again, it just stays pending for the next attempt
        }
    }

    private boolean trySend(String contact, String message) {
        try {
            String formBody = "username=" + urlEncode(username())
                    + "&to=" + urlEncode(normalizePhone(contact))
                    + "&message=" + urlEncode(message);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("apiKey", apiKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            } else {
                System.err.println("Africa's Talking returned status " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (HttpTimeoutException | java.net.ConnectException e) {
            // Most likely: no internet. This is the expected/common case for an offline-first app.
            System.err.println("SMS send failed (likely offline): " + e.getMessage());
            return false;
        } catch (IOException | InterruptedException e) {
            System.err.println("SMS send failed: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Ensures numbers are in international format for Africa's Talking (e.g. 077xxxxxxx -> +25677xxxxxxx). */
    private String normalizePhone(String contact) {
        String digits = contact.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith("0")) return "+256" + digits.substring(1); // Uganda country code
        if (digits.startsWith("256")) return "+" + digits;
        return digits;
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // --- Convenience builders for common notifications ---

    public static String buildPaymentReceivedMessage(String studentName, double amountPaid, double balance) {
        return String.format("Dear Parent, %s has cleared UGX %,.0f. Remaining balance: UGX %,.0f. Thank you.",
                studentName, amountPaid, balance);
    }

    public static String buildFullyClearedMessage(String studentName) {
        return String.format("Dear Parent, %s's school fees have been fully cleared. Thank you.", studentName);
    }
}
