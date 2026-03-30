package lk.ijse.event_ticketingback_end.service;

import lk.ijse.event_ticketingback_end.dto.BookingDto;
import java.util.List;

public interface BookingService {
    BookingDto saveBooking(BookingDto bookingDto);
    void updateBookingStatus(int bookingId, String status);

    // ✅ Refund — sets booking status to "Refunded" and frees the seats back to "Available"
    // This ensures the QR code is invalidated at scan time via the verify endpoint
    void refundBooking(int bookingId);

    void deleteBooking(int bookingId);
    List<BookingDto> getAllBookings();
    List<BookingDto> getBookingsByEmail(String email);
}