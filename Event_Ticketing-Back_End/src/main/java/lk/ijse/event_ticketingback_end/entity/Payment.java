package lk.ijse.event_ticketingback_end.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    @Column(name = "payhere_payment_id")
    private String payherePaymentId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency = "LKR";

    @Column(nullable = false)
    private String status = "PENDING";

    private String method;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}