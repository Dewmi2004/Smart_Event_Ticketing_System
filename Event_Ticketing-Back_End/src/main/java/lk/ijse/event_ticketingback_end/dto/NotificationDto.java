package lk.ijse.event_ticketingback_end.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDto {

    // ── Persisted fields ─────────────────────────────────────────────────────
    private int            notificationId;
    private int            userId;
    private String         type;      // BookingConfirmation | PaymentSuccess | PaymentFailed | EventUpdate | RefundProcessed | Promotion
    private String         channel;   // Email | SMS | Both
    private String         message;   // Plain-text summary (always populated)
    private String         status;    // Delivered | Pending | Failed
    private LocalDateTime  sentAt;

    // ── Delivery routing (used at dispatch time, NOT persisted) ──────────────
    private String toEmail;    // recipient email address
    private String toPhone;    // recipient phone  (SmsService normalises format)
    private String userName;   // used in the email greeting "Hi <userName>,"

    // ── Rich email payload fields (BookingConfirmation) ───────────────────────
    // Populated by BookingService when triggering a booking-confirmed notification.
    private Integer bookingId;
    private String  eventName;
    private String  eventDate;
    private String  eventLocation;
    private String  seatNumbers;
    private Double  totalAmount;
    private String  qrUrl;
}