package lk.ijse.event_ticketingback_end.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookingId;

    private int userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private Date bookingDate;
    private double totalAmount;
    private String couponCode;
    private String status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_seats",
            joinColumns        = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "seat_id")
    )
    private List<Seat> seats;
}