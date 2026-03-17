package lk.ijse.event_ticketingback_end.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatDto {
    private int    seat_id;
    private int    eventId;
    private String seat_number;
    private String seat_type;
    private double price;
    private String status;
}