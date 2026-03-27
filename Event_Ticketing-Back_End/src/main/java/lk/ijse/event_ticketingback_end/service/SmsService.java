package lk.ijse.event_ticketingback_end.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    // Initialise Twilio SDK once when Spring starts
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("[Twilio] SDK initialised — from={}", fromNumber);
    }

    /**
     * Send a booking confirmation SMS.
     * Called from PaymentServiceImpl after payment is confirmed.
     *
     * @param toPhone     customer phone e.g. "+94771234567"
     * @param bookingId   booking ID
     * @param eventName   event name
     * @param eventDate   event date string
     * @param seatNumbers comma-separated seat numbers e.g. "A1, A2"
     * @param totalAmount amount paid
     */
    public void sendBookingConfirmation(
            String toPhone,
            int    bookingId,
            String eventName,
            String eventDate,
            String seatNumbers,
            double totalAmount
    ) {
        String body = String.format(
                "🎫 EventHub Booking Confirmed!\n" +
                        "Booking #%d\n" +
                        "Event : %s\n" +
                        "Date  : %s\n" +
                        "Seats : %s\n" +
                        "Paid  : LKR %.2f\n" +
                        "Show this SMS or check your email for the QR ticket at the gate.",
                bookingId, eventName, eventDate, seatNumbers, totalAmount
        );

        sendSms(toPhone, body);
    }

    /**
     * Send a payment failure SMS so the customer knows to retry.
     */
    public void sendPaymentFailed(
            String toPhone,
            int    bookingId,
            String eventName
    ) {
        String body = String.format(
                "⚠️ EventHub Payment Failed\n" +
                        "Booking #%d for %s could not be completed.\n" +
                        "Please visit EventHub and try again.",
                bookingId, eventName
        );

        sendSms(toPhone, body);
    }

    /**
     * Core send method — all SMS go through here.
     * Logs success/failure without throwing, matching EmailService behaviour
     * (email failures are caught and logged, not rethrown).
     */
    private void sendSms(String toPhone, String body) {
        try {
            // Twilio requires E.164 format: +94771234567
            String normalised = normalisePhone(toPhone);

            Message message = Message.creator(
                    new PhoneNumber(normalised),
                    new PhoneNumber(fromNumber),
                    body
            ).create();

            log.info("[Twilio] SMS sent — to={} sid={}", normalised, message.getSid());

        } catch (Exception e) {
            // Log but never throw — SMS failure must not roll back DB changes
            log.error("[Twilio] SMS failed — to={} error={}", toPhone, e.getMessage());
        }
    }

    /**
     * Converts local SL numbers to E.164.
     * "0771234567" → "+94771234567"
     * Already E.164 numbers are returned as-is.
     */
    private String normalisePhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        phone = phone.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("+")) return phone;           // already E.164
        if (phone.startsWith("0"))  return "+94" + phone.substring(1); // local SL
        return "+" + phone;                                // assume already has country code
    }
}