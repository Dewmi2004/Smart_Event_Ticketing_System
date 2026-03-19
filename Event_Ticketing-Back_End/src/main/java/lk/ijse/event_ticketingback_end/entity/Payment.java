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

    // Links to the booking this payment is for
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // PayHere order_id we generate and send
    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    // PayHere returns this after payment
    @Column(name = "payhere_payment_id")
    private String payherePaymentId;

    // Amount charged
    @Column(nullable = false)
    private double amount;

    // LKR
    @Column(nullable = false)
    private String currency = "LKR";

    // PENDING | PAID | FAILED | REFUNDED
    @Column(nullable = false)
    private String status = "PENDING";

    // PayHere payment method (VISA, MASTER, etc.)
    private String method;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}