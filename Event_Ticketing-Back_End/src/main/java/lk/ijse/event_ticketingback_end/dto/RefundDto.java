package lk.ijse.event_ticketingback_end.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundDto {
    private int    refundId;
    private int    paymentId;
    private int    bookingId;
    private double amount;
    private String reason;
    private String status;
    private String payhereRefundId;
    private String requestedAt;
    private String processedAt;
}