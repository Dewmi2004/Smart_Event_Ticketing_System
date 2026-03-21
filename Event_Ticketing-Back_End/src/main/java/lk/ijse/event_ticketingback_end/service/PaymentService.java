package lk.ijse.event_ticketingback_end.service;

import com.google.zxing.WriterException;
import lk.ijse.event_ticketingback_end.dto.PaymentDto;

import java.io.IOException;
import java.util.Map;

public interface PaymentService {

    /**
     * Creates a PENDING payment record and returns the
     * PayHere checkout parameters the frontend needs to
     * build the payment form and redirect.
     */
    Map<String, String> initiatePayment(int bookingId, String customerName,
                                        String customerEmail, String customerPhone);

    /**
     * Called by the PayHere webhook (notify_url).
     * Verifies the MD5 hash, marks the payment PAID,
     * updates booking status to Confirmed, and sends
     * the confirmation email with QR ticket.
     */
    void handleNotify(Map<String, String> params) throws IOException, WriterException;

    /**
     * Returns payment details for a given booking.
     */
    PaymentDto getPaymentByBooking(int bookingId);
}