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

    private int            notificationId;
    private int            userId;
    private String         type;
    private String         channel;
    private String         message;
    private String         status;
    private LocalDateTime  sentAt;

    private String toEmail;
    private String toPhone;
    private String userName;

    //  when triggering a booking-confirmed notification.
    private Integer bookingId;
    private String  eventName;
    private String  eventDate;
    private String  eventLocation;
    private String  seatNumbers;
    private Double  totalAmount;
    private String  qrUrl;
}