package lk.ijse.event_ticketingback_end.dto;

import lombok.*;

import java.sql.Date;
import java.sql.Time;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventDto {
    private int event_id;
    private String event_name;
    private String location;
    private Date date;
    private Time time;
    private double ticket_price;
    private int total_seats;
    private String status;
}
