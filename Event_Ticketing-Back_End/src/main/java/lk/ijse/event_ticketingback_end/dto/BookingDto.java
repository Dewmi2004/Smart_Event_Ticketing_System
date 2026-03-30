package lk.ijse.event_ticketingback_end.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.sql.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {

    private Integer bookingId;
    private Integer userId;
    private Integer eventId;
    private String  userEmail;
    private String  phone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date bookingDate;

    private double totalAmount;
    private String couponCode;
    private String status;
    private List<Integer> seatIds;

    private String eventName;
    private String eventLocation;
}