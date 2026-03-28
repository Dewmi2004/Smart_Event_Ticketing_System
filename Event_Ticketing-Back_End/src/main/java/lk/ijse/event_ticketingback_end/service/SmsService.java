package lk.ijse.event_ticketingback_end.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class SmsService {

    @Value("${textlk.api-key}")
    private String apiKey;

    @Value("${textlk.sender-id}")
    private String senderId;

    private static final String API_URL = "https://app.text.lk/api/v3/sms/send";

    public void sendBookingConfirmation(
            String toPhone,
            int    bookingId,
            String eventName,
            String eventDate,
            String seatNumbers,
            double totalAmount
    ) {
        String message = String.format(
                "EventHub Booking Confirmed! " +
                        "Booking #%d | " +
                        "Event: %s | " +
                        "Date: %s | " +
                        "Seats: %s | " +
                        "Paid: LKR %.2f. " +
                        "Check your email for QR ticket.",
                bookingId, eventName, eventDate, seatNumbers, totalAmount
        );

        sendSms(toPhone, message);
    }

    public void sendPaymentFailed(
            String toPhone,
            int    bookingId,
            String eventName
    ) {
        String message = String.format(
                "EventHub: Payment failed for Booking #%d (%s). " +
                        "Please visit EventHub and try again.",
                bookingId, eventName
        );

        sendSms(toPhone, message);
    }

    private void sendSms(String toPhone, String message) {
        try {
            String normalised = normalisePhone(toPhone);

            String json = String.format(
                    "{\"recipient\":\"%s\",\"sender_id\":\"%s\",\"type\":\"plain\",\"message\":\"%s\"}",
                    normalised,
                    senderId,
                    message.replace("\"", "\\\"")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("[text.lk] SMS sent — to={} response={}", normalised, response.body());
            } else {
                log.error("[text.lk] SMS failed — to={} status={} body={}",
                        normalised, response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("[text.lk] SMS exception — to={} error={}", toPhone, e.getMessage());
        }
    }

    private String normalisePhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        phone = phone.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("+94")) return phone.substring(1); // +94... → 94...
        if (phone.startsWith("94"))  return phone;              // already correct
        if (phone.startsWith("0"))   return "94" + phone.substring(1); // 07... → 947...
        return "94" + phone;
    }
}