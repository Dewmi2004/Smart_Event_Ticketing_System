package lk.ijse.event_ticketingback_end.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private int    paymentId;
    private int    bookingId;
    private String orderId;
    private String payherePaymentId;
    private double amount;
    private String currency;
    private String status;
    private String method;
}