package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.RefundDto;
import java.util.List;

public interface RefundService {

    RefundDto requestRefund(int bookingId, String reason);

    RefundDto processRefund(int refundId);

    List<RefundDto> getAllRefunds();

    RefundDto getRefundByBooking(int bookingId);
}