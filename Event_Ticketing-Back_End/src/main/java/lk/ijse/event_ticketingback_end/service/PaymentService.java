package lk.ijse.event_ticketingback_end.service;

import com.google.zxing.WriterException;
import lk.ijse.event_ticketingback_end.dto.PaymentDto;

import java.io.IOException;
import java.util.Map;

public interface PaymentService {


    Map<String, String> initiatePayment(int bookingId, String customerName,
                                        String customerEmail, String customerPhone);

    void handleNotify(Map<String, String> params) throws IOException, WriterException;

    PaymentDto getPaymentByBooking(int bookingId);
}